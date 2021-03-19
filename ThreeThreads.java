public class ThreeThreads {

    static  Object MON = new Object();
    static volatile String letter = "A";

    public static void main(String[] args) {

        new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                synchronized (MON) {
                    while(!letter.equals("A")){
                        try {
                            MON.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println(letter);
                    letter = "B";
                    MON.notifyAll();
                }
            }
        }).start();

        new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                synchronized (MON) {
                    while(!letter.equals("B")){
                        try {
                            MON.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println(letter);
                    letter = "C";
                    MON.notifyAll();
                }
            }
        }).start();

        new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                synchronized (MON) {
                    while(!letter.equals("C")){
                        try {
                            MON.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println(letter);
                    letter = "A";
                    MON.notifyAll();
                }
            }
        }).start();
    }
}
