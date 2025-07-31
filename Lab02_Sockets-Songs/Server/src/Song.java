import java.io.Serializable;

public class Song implements Serializable {
    private String title, author, genre, language;
    private int year;

    public Song(String title, String author, String genre, String language, int year) {
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.language = language;
        this.year = year;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public String getLanguage() { return language; }
    public int getYear() { return year; }

    @Override
    public String toString() {
        return title + " - " + author + " [" + genre + ", " + language + ", " + year + "]";
    }
}
