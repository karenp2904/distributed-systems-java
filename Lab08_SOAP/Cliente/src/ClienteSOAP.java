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

public class ClienteSOAP {
    private static final String ENDPOINT_SOAP = "http://localhost:9090";
    private Scanner scanner;
    
    public ClienteSOAP() {
        this.scanner = new Scanner(System.in);
    }
    
    public static void main(String[] args) {
        ClienteSOAP cliente = new ClienteSOAP();
        cliente.iniciarCliente();
    }
    
    public void iniciarCliente() {
        System.out.println("=== BIBLIOTECA MUSICAL MORAT & TAYLOR SWIFT ===");
        System.out.println("Conectado al servidor SOAP.");
        
        int opcion = 0;
        while (opcion != 6) {
            mostrarMenu();
            opcion = scanner.nextInt();
            scanner.nextLine();
            
            switch (opcion) {
                case 1:
                    buscarPorNombre();
                    break;
                case 2:
                    buscarPorGenero();
                    break;
                case 3:
                    buscarPorAutor();
                    break;
                case 4:
                    buscarPorCriterios();
                    break;
                case 5:
                    mostrarTodasLasCanciones();
                    break;
                case 6:
                    System.out.println("¡Gracias por usar la biblioteca musical!");
                    break;
                default:
                    System.out.println("Opción no válida. Intente de nuevo.");
            }
        }
    }
    
    private void mostrarMenu() {
        System.out.println("\n=== MENÚ PRINCIPAL ===");
        System.out.println("1. Buscar por nombre de canción");
        System.out.println("2. Buscar por género");
        System.out.println("3. Buscar por autor/artista");
        System.out.println("4. Búsqueda por múltiples criterios");
        System.out.println("5. Mostrar todas las canciones");
        System.out.println("6. Salir");
        System.out.print("Seleccione una opción: ");
    }
    
    private void buscarPorNombre() {
        System.out.print("Ingrese el nombre de la canción: ");
        String nombre = scanner.nextLine();
        
        String requestSOAP = crearRequestSOAP("buscarPorNombre", 
            "<nombre>" + escaparXml(nombre) + "</nombre>");
        
        try {
            String respuesta = enviarRequestSOAP(requestSOAP);
            List<Song> resultados = parseRespuestaBusqueda(respuesta);
            mostrarResultados(resultados);
        } catch (Exception e) {
            System.out.println("[Cliente]: Error buscando por nombre: " + e.getMessage());
        }
    }
    
    private void buscarPorGenero() {
        System.out.print("Ingrese el género: ");
        String genero = scanner.nextLine();
        
        String requestSOAP = crearRequestSOAP("buscarPorGenero", 
            "<genero>" + escaparXml(genero) + "</genero>");
        
        try {
            String respuesta = enviarRequestSOAP(requestSOAP);
            List<Song> resultados = parseRespuestaBusqueda(respuesta);
            mostrarResultados(resultados);
        } catch (Exception e) {
            System.out.println("[Cliente]: Error buscando por género: " + e.getMessage());
        }
    }
    
    private void buscarPorAutor() {
        System.out.print("Ingrese el nombre del autor/artista: ");
        String autor = scanner.nextLine();
        
        String requestSOAP = crearRequestSOAP("buscarPorAutor", 
            "<autor>" + escaparXml(autor) + "</autor>");
        
        try {
            String respuesta = enviarRequestSOAP(requestSOAP);
            List<Song> resultados = parseRespuestaBusqueda(respuesta);
            mostrarResultados(resultados);
        } catch (Exception e) {
            System.out.println("[Cliente]: Error buscando por autor: " + e.getMessage());
        }
    }
    
    private void buscarPorCriterios() {
        System.out.println("=== BÚSQUEDA AVANZADA ===");
        System.out.print("Nombre (presione Enter para omitir): ");
        String nombre = scanner.nextLine();
        System.out.print("Género (presione Enter para omitir): ");
        String genero = scanner.nextLine();
        System.out.print("Autor (presione Enter para omitir): ");
        String autor = scanner.nextLine();
        
        String parametros = "<nombre>" + escaparXml(nombre) + "</nombre>" +
                           "<genero>" + escaparXml(genero) + "</genero>" +
                           "<autor>" + escaparXml(autor) + "</autor>";
        
        String requestSOAP = crearRequestSOAP("buscarPorCriterios", parametros);
        
        try {
            String respuesta = enviarRequestSOAP(requestSOAP);
            List<Song> resultados = parseRespuestaBusqueda(respuesta);
            mostrarResultados(resultados);
        } catch (Exception e) {
            System.out.println("[Cliente]: Error en búsqueda múltiple: " + e.getMessage());
        }
    }
    
    private void mostrarTodasLasCanciones() {
        String parametros = "<nombre></nombre><genero></genero><autor></autor>";
        String requestSOAP = crearRequestSOAP("buscarPorCriterios", parametros);
        
        try {
            String respuesta = enviarRequestSOAP(requestSOAP);
            List<Song> resultados = parseRespuestaBusqueda(respuesta);
            mostrarResultados(resultados);
        } catch (Exception e) {
            System.out.println("[Cliente]: Error obteniendo todas las canciones: " + e.getMessage());
        }
    }
    
    private String crearRequestSOAP(String nombreMetodo, String parametros) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
               "xmlns:srv=\"http://musicalibrary.service/\">" +
               "<soap:Header/>" +
               "<soap:Body>" +
               "<srv:" + nombreMetodo + ">" +
               parametros +
               "</srv:" + nombreMetodo + ">" +
               "</soap:Body>" +
               "</soap:Envelope>";
    }
    
    @SuppressWarnings("deprecation")
    private String enviarRequestSOAP(String requestSOAP) throws Exception {
        URL url = new URL(ENDPOINT_SOAP);
        HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
        
        conexion.setRequestMethod("POST");
        conexion.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conexion.setRequestProperty("SOAPAction", "");
        conexion.setDoOutput(true);
        
        try (OutputStream os = conexion.getOutputStream()) {
            os.write(requestSOAP.getBytes("UTF-8"));
        }
        
        StringBuilder respuesta = new StringBuilder();
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(conexion.getInputStream()))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                respuesta.append(linea);
            }
        }
        
        return respuesta.toString();
    }
    
    private List<Song> parseRespuestaBusqueda(String respuestaXml) throws Exception {
        List<Song> canciones = new ArrayList<>();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(respuestaXml.getBytes()));
        
        NodeList elementosSong = doc.getElementsByTagName("song");
        
        for (int i = 0; i < elementosSong.getLength(); i++) {
            Element elementoSong = (Element) elementosSong.item(i);
            Song cancion = new Song();
            
            NodeList titulos = elementoSong.getElementsByTagName("titulo");
            if (titulos.getLength() > 0) {
                cancion.setTitulo(titulos.item(0).getTextContent());
            }
            
            NodeList generos = elementoSong.getElementsByTagName("genero");
            if (generos.getLength() > 0) {
                cancion.setGenero(generos.item(0).getTextContent());
            }
            
            NodeList autores = elementoSong.getElementsByTagName("autor");
            if (autores.getLength() > 0) {
                cancion.setAutor(autores.item(0).getTextContent());
            }
            
            NodeList idiomas = elementoSong.getElementsByTagName("idioma");
            if (idiomas.getLength() > 0) {
                cancion.setIdioma(idiomas.item(0).getTextContent());
            }
            
            NodeList anos = elementoSong.getElementsByTagName("ano");
            if (anos.getLength() > 0) {
                try {
                    cancion.setAnoLanzamiento(Integer.parseInt(anos.item(0).getTextContent()));
                } catch (NumberFormatException e) {
                    cancion.setAnoLanzamiento(0);
                }
            }
            
            canciones.add(cancion);
        }
        
        return canciones;
    }
    
    private void mostrarResultados(List<Song> resultados) {
        System.out.println("\n=== RESULTADOS DE BÚSQUEDA ===");
        if (resultados.isEmpty()) {
            System.out.println("No se encontraron canciones que coincidan con los criterios.");
        } else {
            System.out.println("Se encontraron " + resultados.size() + " canción(es):\n");
            for (int i = 0; i < resultados.size(); i++) {
                Song cancion = resultados.get(i);
                System.out.println((i + 1) + ". " + cancion.getTitulo());
                System.out.println("   Artista: " + cancion.getAutor());
                System.out.println("   Género: " + cancion.getGenero());
                System.out.println("   Idioma: " + cancion.getIdioma());
                System.out.println("   Año: " + cancion.getAnoLanzamiento());
                System.out.println();
            }
        }
    }
    
    private String escaparXml(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}