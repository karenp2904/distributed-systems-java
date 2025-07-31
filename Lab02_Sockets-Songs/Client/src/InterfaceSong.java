import java.util.List;

public interface InterfaceSong {
    List<Song> searchByTitle(String title);
    List<Song> searchByAuthor(String author);
    List<Song> searchByGenre(String genre);
}

