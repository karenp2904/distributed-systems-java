import java.io.Serializable;

public class Song implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String title;
    private String genre;
    private String author;
    private String language;
    private int year;

    public Song(String title, String genre, String author, String language, int year) {
        this.title = title;
        this.genre = genre;
        this.author = author;
        this.language = language;
        this.year = year;
    }

    public String getTitle() { return title; }
    public String getGenre() { return genre; }
    public String getAuthor() { return author; }
    public String getLanguage() { return language; }
    public int getYear() { return year; }

    @Override
    public String toString() {
        return title + " by " + author + " (" + genre + ", " + language + ", " + year + ")";
    }
}