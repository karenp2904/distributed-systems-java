
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface InterfaceSong extends Remote {
    List<Song> searchByTitle(String title) throws RemoteException;
    List<Song> searchByGenre(String genre) throws RemoteException;
    List<Song> searchByAuthor(String author) throws RemoteException;
    List<Song> getAllSongs() throws RemoteException;

}