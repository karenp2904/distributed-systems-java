
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class MusicServer {
    
    public static void main(String[] args) {
        try {
            MusicCatalog server = new MusicCatalog();            
            Registry registry = LocateRegistry.createRegistry(1090);
            registry.rebind("MusicCatalog", server);
            
            System.out.println("[Music Server]: Server ready");
        } catch (Exception e) {
            System.err.println("[Music Server]: Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}