
import java.util.List;
import java.util.Scanner;

public class MusicClient {
    private InterfaceSong songService;
    private Scanner scanner;

    public MusicClient(InterfaceSong songService) {
        this.songService = songService;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        System.out.println("\n Music Library Explorer ");
        System.out.println("Connected to the music server!");
        while (true) {
            showMenu();
            String input = scanner.nextLine().trim();
            if (!input.matches("[1-5]")) {
                System.out.println("Please enter a valid option (1-5).");
                continue;
            }
            int choice = Integer.parseInt(input);
            if (choice == 5) {
                System.out.println("Thank you for using Music Library Explorer!");
                break;
            }
            handleOption(choice);
        }
        scanner.close();
    }

    private void showMenu() {
        System.out.println("\n=== Music Explorer Options ===");
        System.out.println("1) Find Songs by Title");
        System.out.println("2) Find Songs by Artist");
        System.out.println("3) Find Songs by Genre");
        System.out.println("4) All songs");
        System.out.println("5) Exit");
        System.out.print("Choose an option (1-5): ");
    }

    private void handleOption(int option) {
        try {
            switch (option) {
                case 1:
                    findByTitle();
                    break;
                case 2:
                    findByAuthor();
                    break;
                case 3:
                    findByGenre();
                    break;  
                case 4:
                    showAllSongs();
                    break;             
            }
            
        } catch (Exception e) {
            System.out.println("Error during operation: " + e.getMessage());
        }
    }

    private void findByTitle() throws Exception {
        System.out.print("Enter a song title (or partial title): ");
        String title = scanner.nextLine().trim();
        List<Song> results = songService.searchByTitle(title);
        printResults(results, "Results for title search: '" + title + "'");
    }

    private void findByAuthor() throws Exception {
        System.out.print("Enter an artist name (or partial name): ");
        String author = scanner.nextLine().trim();
        List<Song> results = songService.searchByAuthor(author);
        printResults(results, "Results for artist search: '" + author + "'");
    }

    private void findByGenre() throws Exception {
        System.out.print("Enter a genre (or partial genre): ");
        String genre = scanner.nextLine().trim();
        List<Song> results = songService.searchByGenre(genre);
        printResults(results, "Results for genre search: '" + genre + "'");
    }

    private void showAllSongs() throws Exception {
        List<Song> results = songService.getAllSongs();
        printResults(results, "All Available Songs");
    }

    private void printResults(List<Song> results, String header) {
        System.out.println("\n " + header + " ");
        if (results.isEmpty()) {
            System.out.println("No songs matched your query.");
        } else {
            System.out.println("Total songs found: " + results.size());
            System.out.println("------------------------------");
            for (int i = 0; i < results.size(); i++) {
                Song song = results.get(i);
                System.out.println("Song " + (i + 1) + ":");
                System.out.println("  Title: " + song.getTitle());
                System.out.println("  Artist: " + song.getAuthor());
                System.out.println("  Genre: " + song.getGenre());
                System.out.println("  Language: " + song.getLanguage());
                System.out.println("  Year: " + song.getYear());
                System.out.println("------------------------------");
            }
        }
    }
}