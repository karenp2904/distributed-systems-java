package soapserver;

import java.io.*;
import java.net.Socket;
//maneja cada conexion
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final SOAPProcessor processor;

    public ClientHandler(Socket socket, SOAPProcessor processor) {
        this.clientSocket = socket;
        this.processor = processor;
    }

    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line;
            int contentLength = 0;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.substring(16).trim());
                }
            }

            char[] buffer = new char[contentLength];
            reader.read(buffer, 0, contentLength);
            String soapRequest = new String(buffer);

            String soapResponse = processor.processSOAPRequest(soapRequest);

            writer.println("HTTP/1.1 200 OK");
            writer.println("Content-Type: text/xml; charset=utf-8");
            writer.println("Content-Length: " + soapResponse.length());
            writer.println();
            writer.print(soapResponse);
            writer.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
