package server;

import commands.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int PORT = 8787;
    private ServerSocket server;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    private List<ClientHandler> clients;
    private ExecutorService executorService;
    private AuthService authService;

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public Server() {
        executorService = Executors.newCachedThreadPool();
        clients = new CopyOnWriteArrayList<>();
        //authService = new SimpleAuthService();
        if (!SQLHandler.connect()){
            throw new RuntimeException("Внимание! Не удалось подключться к базе!");
        }
        authService = new DBAuthService();

        try {
            server = new ServerSocket(PORT);
            System.out.println("Сервер подключен.");

            while (true) {
                socket = server.accept();
                System.out.println("Клиент подключен.");
                System.out.println("Клиент:" + socket.getRemoteSocketAddress());
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            SQLHandler.disconnect();
            try {

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {

                server.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastMsg(ClientHandler sender, String msg) {
        String message = String.format("[ %s ]: %s", sender.getNickname(), msg);
        SQLHandler.addMessage(sender.getNickname(), "null", msg, currentDateAndTime());
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }

    public void personalMsg(ClientHandler sender, String receiver, String msg) {
        String message = String.format("[ %s ] to [ %s ]: %s", sender.getNickname(), receiver, msg);
        for (ClientHandler c : clients) {
            if (c.getNickname().equals(receiver)) {
                c.sendMsg(message);
                SQLHandler.addMessage(sender.getNickname(), receiver, msg, currentDateAndTime());
                if (!c.equals(sender)) {
                    sender.sendMsg(message);
                }
                return;
            }
        }
        sender.sendMsg("Пользователь " + receiver + " не найден!");
    }
    public String currentDateAndTime(){
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date = new Date(System.currentTimeMillis());
        return (formatter.format(date));
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isLoginAuthenticated(String login) {
        for (ClientHandler c : clients) {
            if (c.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder(Command.CLIENT_LIST);

        for (ClientHandler c : clients) {
            sb.append(" ").append(c.getNickname());
        }

        String msg = sb.toString();

        for (ClientHandler c : clients) {
            c.sendMsg(msg);
        }
    }

}
