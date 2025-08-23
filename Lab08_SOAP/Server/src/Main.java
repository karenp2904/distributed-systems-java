package soapserver;

public class Main {
    public static void main(String[] args) {
        try {
            SOAPServer server = new SOAPServer(8080);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

