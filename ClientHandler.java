package server;


import commands.Command;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.*;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            //new Thread(() -> {
            server.getExecutorService().execute(() -> {
                try {
                    // установка сокет тайм аут
                    socket.setSoTimeout(120000);
                    //цикл аутентификации
                    while (true) {

                        String str = in.readUTF();

                        //если команда отключиться
                        if (str.equals(Command.END)) {
                            out.writeUTF(Command.END);
                            throw new RuntimeException("Клиент захотел отключиться");
                        }
                        //если команда аутентификации
                        if (str.startsWith(Command.AUTH)) {
                            String[] token = str.split("\\s", 3);
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server.getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);

                            login = token[1];
                            if (newNick != null) {
                                if (!server.isLoginAuthenticated(login)) {
                                    nickname = newNick;
                                    sendMsg(Command.AUTH_OK + " " + nickname);
                                    server.subscribe(this);
                                    socket.setSoTimeout(0);
                                    System.out.println("Клиент: " +
                                            socket.getRemoteSocketAddress() +
                                            " Подключён под ником: " + nickname);
                                    // sendMsg(SQLHandler.getMessageForNick(nickname));
                                    break;
                                } else {
                                    sendMsg("Ай некрасиво! Данная учётка занята!");
                                }
                            } else {
                                sendMsg("Шо ви торгуетесь?! Мы же не на привозе! Вводите, что должны, и не заставляйте Сеню напрягаться!");
                            }
                        }
                        //команда регистрации
                        if (str.startsWith(Command.REG)) {
                            String[] token = str.split("\\s", 4);
                            if (token.length < 4) {
                                continue;
                            }
                            boolean regSuccess = server.getAuthService()
                                    .registration(token[1], token[2], token[3]);
                            if (regSuccess) {
                                sendMsg(Command.REG_OK);

                            } else {
                                sendMsg(Command.REG_NO);
                            }
                        }
                    }

                    //work loop
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals(Command.END)) {
                                out.writeUTF(Command.END);
                                break;
                            }

                            if (str.startsWith(Command.PERSONAL_MSG)) {
                                String[] token = str.split("\\s", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.personalMsg(this, token[1], token[2]);
                            }
                            if (str.startsWith("/chgnick")) {
                                String[] token = str.split("\\s+", 2);
                                if (token.length < 2) {
                                    continue;
                                }
                                if (token[1].contains(" ")) {
                                    sendMsg("Ник не может содержать пробелов!");
                                    continue;
                                }
                                if (server.getAuthService().changeNick(this.nickname, token[1])) {
                                    sendMsg("/yournickis " + token[1]);
                                    sendMsg("Ваш ник изменён на " + token[1]);
                                    this.nickname = token[1];
                                    server.broadcastClientList();
                                } else {
                                    sendMsg("Не удалось изменить ник. Ник " + token[1] + " уже существует!");
                                }


                            }
                        } else {
                            server.broadcastMsg(this, str);
                        }

                    }
                } catch (SocketTimeoutException e) {
                    try {
                        out.writeUTF(Command.END);//SocketTimeoutException
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    System.out.println("Пользователь " + nickname + " отключился!");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // }).start();
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }

}
