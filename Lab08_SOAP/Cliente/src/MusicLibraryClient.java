import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import java.io.ByteArrayInputStream;

public class MusicLibraryClient {
    
    private static final String SOAP_ENDPOINT = "http://localhost:8080";
    private Scanner scanner;

    public MusicLibraryClient() {
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        MusicLibraryClient client = new MusicLibraryClient();
        client.launch();
    }

    // Arranca el cliente interactivo
    public void launch() {
        System.out.println("=== CATÁLOGO DE CANCIONES ===");
        System.out.println("Conectado correctamente al servicio SOAP.");

        int option = 0;
        while (option != 6) {
            showMenu();
            option = scanner.nextInt();
            scanner.nextLine();

            switch (option) {
                case 1 -> searchByAuthor();
                case 2 -> searchByMultipleCriteriaMenu();
                case 3 -> showAllSongs();
                case 4 -> searchByTitle();
                case 5 -> searchByGenre();
                case 6 -> System.out.println("Cerrando cliente. ¡Hasta la próxima!");
                default -> System.out.println("Opción no válida. Inténtalo otra vez.");
            }
        }
    }

    // Menú principal
    private void showMenu() {
        System.out.println("\n=== MENÚ ===");
        System.out.println("1. Buscar por autor");
        System.out.println("2. Búsqueda avanzada (varios filtros)");
        System.out.println("3. Mostrar todas las canciones");
        System.out.println("4. Buscar por título");
        System.out.println("5. Buscar por género");
        System.out.println("6. Salir");
        System.out.print("Seleccione una opción: ");
    }

    // Métodos de búsqueda
    private void searchByAuthor() {
        System.out.print("Escriba el autor: ");
        String author = scanner.nextLine();
        executeSearch("searchByAuthor", "<arg0>" + escapeXml(author) + "</arg0>");
    }

    private void searchByTitle() {
        System.out.print("Escriba el título: ");
        String title = scanner.nextLine();
        executeSearch("searchByTitle", "<arg0>" + escapeXml(title) + "</arg0>");
    }

    private void searchByGenre() {
        System.out.print("Escriba el género: ");
        String genre = scanner.nextLine();
        executeSearch("searchByGenre", "<arg0>" + escapeXml(genre) + "</arg0>");
    }

    private void searchByMultipleCriteriaMenu() {
        System.out.println("=== BÚSQUEDA AVANZADA ===");
        System.out.print("Título (dejar en blanco para omitir): ");
        String title = scanner.nextLine();
        System.out.print("Género (dejar en blanco para omitir): ");
        String genre = scanner.nextLine();
        System.out.print("Autor (dejar en blanco para omitir): ");
        String author = scanner.nextLine();

        String params = "<arg0>" + escapeXml(title) + "</arg0>" +
                        "<arg1>" + escapeXml(genre) + "</arg1>" +
                        "<arg2>" + escapeXml(author) + "</arg2>";
        executeSearch("searchByMultipleCriteria", params);
    }

    private void showAllSongs() {
        String params = "<arg0></arg0><arg1></arg1><arg2></arg2>";
        executeSearch("searchByMultipleCriteria", params);
    }

    // Centraliza ejecución de búsqueda
    private void executeSearch(String method, String parameters) {
        String soapRequest = createSOAPRequest(method, parameters);
        try {
            String response = sendSOAPRequest(soapRequest);
            List<Song> results = parseSearchResponse(response);
            displayResults(results);
        } catch (Exception e) {
            System.out.println("[Music Client]: Error en la búsqueda: " + e.getMessage());
        }
    }

    // Crea la estructura del sobre SOAP
    private String createSOAPRequest(String methodName, String parameters) {
        return """
               <?xml version="1.0" encoding="UTF-8"?>
               <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                              xmlns:ser="http://service.musiclibrary.com/">
                 <soap:Header/>
                 <soap:Body>
                   <ser:%s>%s</ser:%s>
                 </soap:Body>
               </soap:Envelope>
               """.formatted(methodName, parameters, methodName);
    }

    // Envía la petición SOAP y recupera la respuesta
    private String sendSOAPRequest(String soapRequest) throws Exception {
        URL url = new URL(SOAP_ENDPOINT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        connection.setRequestProperty("SOAPAction", "");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(soapRequest.getBytes("UTF-8"));
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    // Interpreta la respuesta SOAP y crea objetos Song
    private List<Song> parseSearchResponse(String xmlResponse) throws Exception {
        List<Song> songs = new ArrayList<>();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes()));

        NodeList nodes = doc.getElementsByTagName("return");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            Song song = new Song();

            if (element.getElementsByTagName("title").getLength() > 0) {
                song.setTitle(element.getElementsByTagName("title").item(0).getTextContent());
            }
            if (element.getElementsByTagName("genre").getLength() > 0) {
                song.setGenre(element.getElementsByTagName("genre").item(0).getTextContent());
            }
            if (element.getElementsByTagName("author").getLength() > 0) {
                song.setAuthor(element.getElementsByTagName("author").item(0).getTextContent());
            }
            if (element.getElementsByTagName("language").getLength() > 0) {
                song.setLanguage(element.getElementsByTagName("language").item(0).getTextContent());
            }
            if (element.getElementsByTagName("year").getLength() > 0) {
                try {
                    song.setYear(Integer.parseInt(element.getElementsByTagName("year").item(0).getTextContent()));
                } catch (NumberFormatException e) {
                    song.setYear(0);
                }
            }
            songs.add(song);
        }
        return songs;
    }

    // Muestra en consola los resultados
    private void displayResults(List<Song> results) {
        System.out.println("\n=== RESULTADOS DE LA BÚSQUEDA ===");
        if (results.isEmpty()) {
            System.out.println("No se hallaron coincidencias.");
        } else {
            System.out.println("Se localizaron " + results.size() + " canción(es):\n");
            for (int i = 0; i < results.size(); i++) {
                Song song = results.get(i);
                System.out.println((i + 1) + ". " + song.getTitle());
                System.out.println("   Autor: " + song.getAuthor());
                System.out.println("   Género: " + song.getGenre());
                System.out.println("   Idioma: " + song.getLanguage());
                System.out.println("   Año: " + song.getYear());
                System.out.println();
            }
        }
    }

    // Escapa caracteres especiales de XML
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
