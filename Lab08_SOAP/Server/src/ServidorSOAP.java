import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ServidorSOAP {
    private List<Song> baseDatos;
    private ServerSocket socketServidor;
    
    public ServidorSOAP() {
        inicializarBaseDatos();
    }
    
    private void inicializarBaseDatos() {
        baseDatos = new ArrayList<>();
        
        // Morat
        baseDatos.add(new Song("Cómo Te Atreves", "Pop Latino", "Morat", "Español", 2016));
        baseDatos.add(new Song("Besos En Guerra", "Pop Latino", "Morat", "Español", 2017));
        baseDatos.add(new Song("Cuando Nadie Ve", "Pop Latino", "Morat", "Español", 2015));
        baseDatos.add(new Song("A Dónde Vamos", "Pop Latino", "Morat", "Español", 2017));
        baseDatos.add(new Song("Yo Contigo, Tú Conmigo", "Pop Latino", "Morat", "Español", 2017));
        baseDatos.add(new Song("De Cero", "Pop Latino", "Morat", "Español", 2018));
        baseDatos.add(new Song("Presiento", "Pop Latino", "Morat", "Español", 2021));
        baseDatos.add(new Song("Salir Con Vida", "Pop Latino", "Morat", "Español", 2019));
        baseDatos.add(new Song("No Hay Más Que Hablar", "Pop Latino", "Morat", "Español", 2019));
        
        // Taylor Swift
        baseDatos.add(new Song("Love Story", "Pop", "Taylor Swift", "Inglés", 2008));
        baseDatos.add(new Song("Shake It Off", "Pop", "Taylor Swift", "Inglés", 2014));
        baseDatos.add(new Song("Bad Blood", "Pop", "Taylor Swift", "Inglés", 2014));
        baseDatos.add(new Song("Anti-Hero", "Pop", "Taylor Swift", "Inglés", 2022));
        baseDatos.add(new Song("Blank Space", "Pop", "Taylor Swift", "Inglés", 2014));
        baseDatos.add(new Song("Look What You Made Me Do", "Pop", "Taylor Swift", "Inglés", 2017));
        baseDatos.add(new Song("Karma", "Pop", "Taylor Swift", "Inglés", 2022));
        baseDatos.add(new Song("22", "Pop", "Taylor Swift", "Inglés", 2012));
        baseDatos.add(new Song("We Are Never Ever Getting Back Together", "Pop", "Taylor Swift", "Inglés", 2012));
        baseDatos.add(new Song("Cruel Summer", "Pop", "Taylor Swift", "Inglés", 2019));
    }
    
    public void iniciar(int puerto) throws IOException {
        socketServidor = new ServerSocket(puerto);
                        System.out.println("[Servidor SOAP]: Iniciado en puerto " + puerto);
        System.out.println("[Servidor SOAP]: Escuchando conexiones...");
        System.out.println("[Servidor SOAP]: Base de datos cargada con " + baseDatos.size() + " canciones");
        
        while (true) {
            Socket socketCliente = socketServidor.accept();
            new Thread(new ManejadorCliente(socketCliente)).start();
        }
    }
    
    private class ManejadorCliente implements Runnable {
        private Socket socketCliente;
        
        public ManejadorCliente(Socket socket) {
            this.socketCliente = socket;
        }
        
        public void run() {
            try {
                BufferedReader lector = new BufferedReader(
                    new InputStreamReader(socketCliente.getInputStream()));
                PrintWriter escritor = new PrintWriter(
                    socketCliente.getOutputStream(), true);
                
                StringBuilder headers = new StringBuilder();
                String linea;
                int longitudContenido = 0;
                
                // Leer headers HTTP
                while ((linea = lector.readLine()) != null && !linea.isEmpty()) {
                    headers.append(linea).append("\n");
                    if (linea.startsWith("Content-Length:")) {
                        longitudContenido = Integer.parseInt(linea.substring(15).trim());
                    }
                }
                
                // Leer cuerpo SOAP
                StringBuilder cuerpoSOAP = new StringBuilder();
                if (longitudContenido > 0) {
                    char[] buffer = new char[longitudContenido];
                    int bytesLeidos = lector.read(buffer, 0, longitudContenido);
                    if (bytesLeidos > 0) {
                        cuerpoSOAP.append(buffer, 0, bytesLeidos);
                    }
                }
                
                String requestSOAP = cuerpoSOAP.toString();
                
                String respuestaSOAP = procesarRequestSOAP(requestSOAP);
                
                escritor.println("HTTP/1.1 200 OK");
                escritor.println("Content-Type: text/xml; charset=utf-8");
                escritor.println("Content-Length: " + respuestaSOAP.length());
                escritor.println();
                escritor.print(respuestaSOAP);
                escritor.flush();
                
                socketCliente.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private String procesarRequestSOAP(String requestSOAP) {
        try {
            System.out.println("[Servidor SOAP]: Procesando request...");
            
            // Validar que el request no esté vacío
            if (requestSOAP == null || requestSOAP.trim().isEmpty()) {
                System.out.println("[Servidor SOAP]: Request vacío recibido");
                return crearFaultSOAP("Request vacío");
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(requestSOAP.getBytes("UTF-8")));
            
            String[] metodosPermitidos = {"buscarPorNombre", "buscarPorGenero", "buscarPorAutor", "buscarPorCriterios"};
            
            for (String metodo : metodosPermitidos) {
                NodeList nodosMetodo = doc.getElementsByTagName("*");
                for (int i = 0; i < nodosMetodo.getLength(); i++) {
                    Element elemento = (Element) nodosMetodo.item(i);
                    String nombreLocal = elemento.getLocalName();
                    
                    if (metodo.equals(nombreLocal)) {
                        switch (metodo) {
                            case "buscarPorNombre":
                                String nombre = obtenerParametro(elemento, "nombre");
                                return crearRespuestaSOAP(buscarPorNombre(nombre), "buscarPorNombreResponse");
                                
                            case "buscarPorGenero":
                                String genero = obtenerParametro(elemento, "genero");
                                return crearRespuestaSOAP(buscarPorGenero(genero), "buscarPorGeneroResponse");
                                
                            case "buscarPorAutor":
                                String autor = obtenerParametro(elemento, "autor");
                                return crearRespuestaSOAP(buscarPorAutor(autor), "buscarPorAutorResponse");
                                
                            case "buscarPorCriterios":
                                String paramNombre = obtenerParametro(elemento, "nombre");
                                String paramGenero = obtenerParametro(elemento, "genero");
                                String paramAutor = obtenerParametro(elemento, "autor");
                                return crearRespuestaSOAP(
                                    buscarPorCriterios(paramNombre, paramGenero, paramAutor), 
                                    "buscarPorCriteriosResponse");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return crearFaultSOAP("Solicitud inválida");
    }
    
    private String obtenerParametro(Element elementoMetodo, String nombreParam) {
        NodeList params = elementoMetodo.getElementsByTagName(nombreParam);
        if (params.getLength() > 0) {
            return params.item(0).getTextContent();
        }
        return "";
    }
    
    private String crearRespuestaSOAP(List<Song> canciones, String nombreMetodo) {
        StringBuilder respuesta = new StringBuilder();
        respuesta.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        respuesta.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
        respuesta.append("<soap:Body>");
        respuesta.append("<ns:").append(nombreMetodo).append(" xmlns:ns=\"http://musicalibrary.service/\">");
        
        for (Song cancion : canciones) {
            respuesta.append("<song>");
            respuesta.append("<titulo>").append(escaparXml(cancion.getTitulo())).append("</titulo>");
            respuesta.append("<genero>").append(escaparXml(cancion.getGenero())).append("</genero>");
            respuesta.append("<autor>").append(escaparXml(cancion.getAutor())).append("</autor>");
            respuesta.append("<idioma>").append(escaparXml(cancion.getIdioma())).append("</idioma>");
            respuesta.append("<ano>").append(cancion.getAnoLanzamiento()).append("</ano>");
            respuesta.append("</song>");
        }
        
        respuesta.append("</ns:").append(nombreMetodo).append(">");
        respuesta.append("</soap:Body>");
        respuesta.append("</soap:Envelope>");
        
        return respuesta.toString();
    }
    
    private String crearFaultSOAP(String mensaje) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
               "<soap:Body>" +
               "<soap:Fault>" +
               "<faultcode>Server</faultcode>" +
               "<faultstring>" + escaparXml(mensaje) + "</faultstring>" +
               "</soap:Fault>" +
               "</soap:Body>" +
               "</soap:Envelope>";
    }
    
    private String escaparXml(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
    
    // Métodos de búsqueda
    public List<Song> buscarPorNombre(String nombre) {
        List<Song> resultados = new ArrayList<>();
        for (Song cancion : baseDatos) {
            if (cancion.getTitulo().toLowerCase().contains(nombre.toLowerCase())) {
                resultados.add(cancion);
            }
        }
        return resultados;
    }
    
    public List<Song> buscarPorGenero(String genero) {
        List<Song> resultados = new ArrayList<>();
        for (Song cancion : baseDatos) {
            if (cancion.getGenero().toLowerCase().contains(genero.toLowerCase())) {
                resultados.add(cancion);
            }
        }
        return resultados;
    }
    
    public List<Song> buscarPorAutor(String autor) {
        List<Song> resultados = new ArrayList<>();
        for (Song cancion : baseDatos) {
            if (cancion.getAutor().toLowerCase().contains(autor.toLowerCase())) {
                resultados.add(cancion);
            }
        }
        return resultados;
    }
    
    public List<Song> buscarPorCriterios(String nombre, String genero, String autor) {
        List<Song> resultados = new ArrayList<>();
        for (Song cancion : baseDatos) {
            boolean coincide = true;
            if (nombre != null && !nombre.isEmpty() && !cancion.getTitulo().toLowerCase().contains(nombre.toLowerCase())) {
                coincide = false;
            }
            if (genero != null && !genero.isEmpty() && !cancion.getGenero().toLowerCase().contains(genero.toLowerCase())) {
                coincide = false;
            }
            if (autor != null && !autor.isEmpty() && !cancion.getAutor().toLowerCase().contains(autor.toLowerCase())) {
                coincide = false;
            }
            if (coincide) {
                resultados.add(cancion);
            }
        }
        return resultados;
    }
    
    public static void main(String[] args) {
        try {
            ServidorSOAP servidor = new ServidorSOAP();
            servidor.iniciar(9090);
        } catch (IOException e) {
            System.err.println("[Servidor SOAP]: Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}