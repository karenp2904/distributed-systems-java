import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ServerHandler {
    private List<Song> songDB;

    public ServerHandler() {
        songDB = new ArrayList<>();

        try {
            File file = new File("./lib/Songs.json");
            if (!file.exists()) {
                System.out.println("[ServerHandler]: El archivo songs.json no fue encontrado.");
                return;
            }

            Gson gson = new Gson();
            FileReader reader = new FileReader(file, StandardCharsets.UTF_8);

            Type songListType = new TypeToken<ArrayList<Song>>() {}.getType();
            songDB = gson.fromJson(reader, songListType);
            reader.close();

            if (songDB == null) {
                songDB = new ArrayList<>();
                System.out.println("[ServerHandler]: songs.json está vacío o mal formado.");
            } else {
                System.out.println("[ServerHandler]: Se cargaron " + songDB.size() + " canciones.");
            }

        } catch (Exception e) {
            System.out.println("[ServerHandler]: Error al cargar songs.json:");
            e.printStackTrace();
            songDB = new ArrayList<>();
        }
    }

    public List<Song> getAllSongs() {
        return songDB;
    }

    public List<Song> searchByTitle(String title) {
        return songDB.stream()
                .filter(song -> song.getTitle().toLowerCase().contains(title.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Song> searchByAuthor(String author) {
        return songDB.stream()
                .filter(song -> song.getAuthor().toLowerCase().contains(author.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Song> searchByGenre(String genre) {
        return songDB.stream()
                .filter(song -> song.getGenre().toLowerCase().contains(genre.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Song> searchByLanguage(String language) {
        return songDB.stream()
                .filter(song -> song.getLanguage().toLowerCase().contains(language.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Song> searchByYear(int year) {
        return songDB.stream()
                .filter(song -> song.getYear() == year)
                .collect(Collectors.toList());
    }
}
