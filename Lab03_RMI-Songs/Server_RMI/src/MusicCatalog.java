
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MusicCatalog extends UnicastRemoteObject implements InterfaceSong {
    private List<Song> songs;

    public MusicCatalog() throws RemoteException {
        initializeSongs();
    }

    private void initializeSongs() {
        songs = new ArrayList<>();
        // Taylor Swift songs
        songs.add(new Song("Love Story", "Pop", "Taylor Swift", "English", 2008));
        songs.add(new Song("Shake It Off", "Pop", "Taylor Swift", "English", 2014));
        songs.add(new Song("Blank Space", "Pop", "Taylor Swift", "English", 2014));
        songs.add(new Song("Bad Blood", "Pop", "Taylor Swift", "English", 2014));
        songs.add(new Song("Cardigan", "Folk-Pop", "Taylor Swift", "English", 2020));
        songs.add(new Song("Willow", "Folk-Pop", "Taylor Swift", "English", 2020));
        songs.add(new Song("Anti-Hero", "Pop", "Taylor Swift", "English", 2022));
        songs.add(new Song("Lover", "Pop", "Taylor Swift", "English", 2019));
        // Morat songs
        songs.add(new Song("Cómo Te Atreves", "Pop Latino", "Morat", "Spanish", 2016));
        songs.add(new Song("Mi Nuevo Vicio", "Pop Latino", "Morat", "Spanish", 2015));
        songs.add(new Song("Cuando Nadie Ve", "Pop Latino", "Morat", "Spanish", 2018));
        songs.add(new Song("A Dónde Vamos", "Pop Latino", "Morat", "Spanish", 2019));
        songs.add(new Song("No Se Va", "Pop Latino", "Morat", "Spanish", 2019));
        songs.add(new Song("Bajo la Mesa", "Pop Latino", "Morat", "Spanish", 2020));
        songs.add(new Song("Enamórate de Alguien Más", "Pop Latino", "Morat", "Spanish", 2020));
        songs.add(new Song("Presiento", "Pop Latino", "Morat", "Spanish", 2019));
    }

    @Override
    public List<Song> searchByTitle(String title) throws RemoteException {
        return songs.stream()
                .filter(song -> song.getTitle().toLowerCase().contains(title.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Song> searchByGenre(String genre) throws RemoteException {
        return songs.stream()
                .filter(song -> song.getGenre().toLowerCase().contains(genre.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Song> searchByAuthor(String author) throws RemoteException {
        return songs.stream()
                .filter(song -> song.getAuthor().toLowerCase().contains(author.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    public List<Song> getAllSongs() throws RemoteException {
        return new ArrayList<>(songs);
    }

   
}