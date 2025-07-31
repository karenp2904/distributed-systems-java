import java.io.*;
import java.net.*;
import java.util.List;

public class JSocketServer {

    private int port;
    private ServerSocket serverSk;
    private Socket clientSk;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private ServerHandler handler;

    public JSocketServer(int port) {
        try {
            this.port = port;
            this.serverSk = new ServerSocket(port, 100);
            this.handler = new ServerHandler();
            System.out.println("\n[Server]: Escuchando en el puerto " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listening() {
        try {
            System.out.println("\n[Server]: Esperando conexiones...");
            this.clientSk = this.serverSk.accept();
            this.oos = new ObjectOutputStream(this.clientSk.getOutputStream());
            this.oos.flush();
            this.ois = new ObjectInputStream(this.clientSk.getInputStream());
            System.out.println("\n[Server]: Cliente conectado.");

            while (true) {
                try {
                    String input = (String) this.ois.readObject();
                    if (input == null) {
                        System.out.println("[Server]: Se recibió null. Cerrando conexión...");
                        break;
                    }

                    System.out.println("[Server]: Consulta recibida -> " + input);

                    List<Song> results = handleQuery(input);

                    if (results == null) {
                        // opción inválida
                        this.oos.writeObject("❌ Opción de búsqueda inválida. Usa title:, author: o genre:");
                    } else if (results.isEmpty()) {
                        this.oos.writeObject("🔍 No se encontraron resultados.");
                    } else {
                        this.oos.writeObject(results); // enviar lista válida
                    }

                    this.oos.flush();

                } catch (ClassNotFoundException | IOException e) {
                    System.out.println("[Server]: Error al recibir datos. Esperando una nueva consulta...");
                }
            }
        } catch (IOException e) {
            System.out.println("[Server]: Error al aceptar conexión.");
        } finally {
            closeService();
        }
    }

    private List<Song> handleQuery(String input) {
        if (input == null || !input.contains(":")) return null;

        String[] parts = input.split(":", 2);
        String type = parts[0].toLowerCase();
        String query = parts[1].trim();

        return switch (type) {
            case "title" -> handler.searchByTitle(query);
            case "author" -> handler.searchByAuthor(query);
            case "genre" -> handler.searchByGenre(query);
            default -> null;
        };
    }

    private void closeService() {
        try {
            if (ois != null) ois.close();
            if (oos != null) oos.close();
            if (clientSk != null) clientSk.close();
            if (serverSk != null) serverSk.close();
            System.out.println("[Server]: Conexión terminada.");
        } catch (IOException e) {
            System.out.println("[Server]: No se pudo cerrar la conexión.");
        }
    }
}
