package soapserver;

import java.util.ArrayList;
import java.util.List;

public class SongService {
    private List<Song> database;

    public SongService() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        database = new ArrayList<>();
        database.add(new Song("One bite on the Dust", "Rock", "Queen", "English", 1975));
        database.add(new Song("Bohemian Rhapsody", "Rock", "Queen", "English", 1975));
        database.add(new Song("Hotel California", "Rock", "Eagles", "English", 1976));
        database.add(new Song("Imagine", "Pop", "John Lennon", "English", 1971));
        database.add(new Song("Like a Rolling Stone", "Rock", "Bob Dylan", "English", 1965));
        database.add(new Song("Smells Like Teen Spirit", "Grunge", "Nirvana", "English", 1991));
        database.add(new Song("Billie Jean", "Pop", "Michael Jackson", "English", 1983));
        database.add(new Song("Satisfaction", "Rock", "The Rolling Stones", "English", 1965));
        database.add(new Song("Hey Jude", "Pop", "The Beatles", "English", 1968));
        database.add(new Song("Purple Haze", "Rock", "Jimi Hendrix", "English", 1967));
        database.add(new Song("Stairway to Heaven", "Rock", "Led Zeppelin", "English", 1971));
        database.add(new Song("La Vida es Una Fiesta", "Salsa", "Celia Cruz", "Spanish", 1982));
        database.add(new Song("Despacito", "Reggaeton", "Luis Fonsi", "Spanish", 2017));
        database.add(new Song("Careless Whisper", "Pop", "George Michael", "English", 1984));
        database.add(new Song("Sweet Child O' Mine", "Rock", "Guns N' Roses", "English", 1987));
        database.add(new Song("Thunderstruck", "Rock", "AC/DC", "English", 1990));
    }

    public List<Song> searchByTitle(String title) {
        List<Song> results = new ArrayList<>();
        for (Song song : database) {
            if (song.getTitle().toLowerCase().contains(title.toLowerCase())) {
                results.add(song);
            }
        }
        return results;
    }

    public List<Song> searchByGenre(String genre) {
        List<Song> results = new ArrayList<>();
        for (Song song : database) {
            if (song.getGenre().toLowerCase().contains(genre.toLowerCase())) {
                results.add(song);
            }
        }
        return results;
    }

    public List<Song> searchByAuthor(String author) {
        List<Song> results = new ArrayList<>();
        for (Song song : database) {
            if (song.getAuthor().toLowerCase().contains(author.toLowerCase())) {
                results.add(song);
            }
        }
        return results;
    }

    public List<Song> searchByMultipleCriteria(String title, String genre, String author) {
        List<Song> results = new ArrayList<>();
        for (Song song : database) {
            boolean matches = true;
            if (title != null && !title.isEmpty() && !song.getTitle().toLowerCase().contains(title.toLowerCase())) {
                matches = false;
            }
            if (genre != null && !genre.isEmpty() && !song.getGenre().toLowerCase().contains(genre.toLowerCase())) {
                matches = false;
            }
            if (author != null && !author.isEmpty() && !song.getAuthor().toLowerCase().contains(author.toLowerCase())) {
                matches = false;
            }
            if (matches) {
                results.add(song);
            }
        }
        return results;
    }
}
