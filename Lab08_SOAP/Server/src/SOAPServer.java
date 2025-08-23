package soapserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SOAPServer {
    private final int port;
    private final SOAPProcessor processor;

    public SOAPServer(int port) {
        this.port = port;
        SongService songService = new SongService();
        this.processor = new SOAPProcessor(songService);
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[SOAP Server]: Servidor iniciado en puerto " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, processor)).start();
            }
        }
    }
}
