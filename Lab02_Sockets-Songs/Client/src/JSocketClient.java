import java.io.*;
import java.net.*;
import java.util.List;

public class JSocketClient {

    private InetAddress address;
    private int port;
    private Socket clientSk;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    public JSocketClient(String address, int port) {
        try {
            this.port = port;
            this.address = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public boolean request(String query) {
        try {
            this.clientSk = new Socket(this.address, this.port);
            this.oos = new ObjectOutputStream(this.clientSk.getOutputStream());
            this.oos.flush();
            this.ois = new ObjectInputStream(this.clientSk.getInputStream());
            System.out.println("\n[Client]: Conexión establecida con el servidor.");

            send(query);

            Object response = this.ois.readObject();

            if (response instanceof String msg) {
                System.out.println("[Client]: " + msg);
                return false; // fue un error o aviso → repetir
            } else if (response instanceof List<?> resultList) {
                if (resultList.isEmpty()) {
                    System.out.println("[Client]: 🔍 No se encontraron resultados.");
                } else {
                    System.out.println("[Client]: Resultados:");
                    for (Object obj : resultList) {
                        System.out.println(" - " + obj);
                    }
                }
                return true; // fue exitosa
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Client]: Error al comunicarse con el servidor.");
        } finally {
            closeService();
        }

        return false;
    }

    private void send(String data) {
        try {
            this.oos.writeObject(data);
            this.oos.flush();
        } catch (IOException e) {
            System.out.println("[Client]: No se pudo enviar la solicitud.");
        }
    }

    private void closeService() {
        try {
            if (ois != null) ois.close();
            if (oos != null) oos.close();
            if (clientSk != null) clientSk.close();
            System.out.println("[Client]: Conexión cerrada.");
        } catch (IOException e) {
            System.out.println("[Client]: Error al cerrar conexión.");
        }
    }
}
