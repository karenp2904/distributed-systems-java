
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MainClient {
    
    public static void main(String[] args) {
        try {
            // Conectar al registro RMI
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 1090);
            InterfaceSong songService = (InterfaceSong) registry.lookup("MusicCatalog");
            
            // Crear cliente con el objeto remoto
            MusicClient client = new MusicClient(songService);
            client.run();
        } catch (Exception e) {
            System.err.println("[Music Client]: Excepción en el cliente: " + e.toString());
            e.printStackTrace();
        }
    }
}