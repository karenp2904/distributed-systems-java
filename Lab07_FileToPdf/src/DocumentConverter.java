
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Clase principal para convertir documentos a PDF usando LibreOffice Headless
 */
public class DocumentConverter {
    
    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(
        Arrays.asList("docx", "doc", "pptx", "ppt", "xlsx", "xls", "png", "jpg", "jpeg")
    );
    
    private final String libreOfficePath;
    private final String outputDirectory;

    public DocumentConverter(String libreOfficePath, String outputDirectory) {
        this.libreOfficePath = libreOfficePath != null ? libreOfficePath : "soffice";
        this.outputDirectory = outputDirectory;
        
        // Crear directorio de salida si no existe
        createOutputDirectory();
    }

    /**
     * Constructor con ruta por defecto de LibreOffice
     */
    public DocumentConverter(String outputDirectory) {
        this(null, outputDirectory);
    }

    /**
     * Convierte un documento a PDF
     */
    public ConversionResult convertToPdf(String inputFilePath) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Validar archivo de entrada
            File inputFile = new File(inputFilePath);
            if (!inputFile.exists()) {
                return new ConversionResult(inputFilePath, 
                    "El archivo no existe", System.currentTimeMillis() - startTime);
            }

            if (!isSupported(inputFilePath)) {
                return new ConversionResult(inputFilePath, 
                    "Formato no soportado", System.currentTimeMillis() - startTime);
            }

            // Generar ruta de salida
            String outputPath = generateOutputPath(inputFilePath);
            
            // Ejecutar conversión con LibreOffice
            boolean success = executeLibreOfficeConversion(inputFilePath, outputPath);
            
            long conversionTime = System.currentTimeMillis() - startTime;
            
            if (success && new File(outputPath).exists()) {
                return new ConversionResult(inputFilePath, outputPath, conversionTime);
            } else {
                return new ConversionResult(inputFilePath, 
                    "La conversión falló o el archivo PDF no se generó", conversionTime);
            }
            
        } catch (Exception e) {
            long conversionTime = System.currentTimeMillis() - startTime;
            return new ConversionResult(inputFilePath, 
                "Error durante la conversión: " + e.getMessage(), conversionTime);
        }
    }

    /**
     * Ejecuta el comando de LibreOffice para convertir el archivo
     */
    private boolean executeLibreOfficeConversion(String inputPath, String outputPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                libreOfficePath,
                "--convert-to", "pdf",
                "--outdir", outputDirectory,
                inputPath,
                "--headless"
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Esperar hasta 60 segundos por la conversión
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            return process.exitValue() == 0;
            
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Genera la ruta de salida para el archivo PDF
     */
    private String generateOutputPath(String inputPath) {
        Path inputFile = Paths.get(inputPath);
        String fileName = inputFile.getFileName().toString();
        
        // Remover extensión y agregar .pdf
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            fileName = fileName.substring(0, lastDot);
        }
        fileName += ".pdf";
        
        return Paths.get(outputDirectory, fileName).toString();
    }

    /**
     * Verifica si el formato del archivo es soportado
     */
    private boolean isSupported(String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(extension);
    }

    /**
     * Obtiene la extensión del archivo
     */
    private String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        return lastDot > 0 ? filePath.substring(lastDot + 1) : "";
    }

    /**
     * Crea el directorio de salida si no existe
     */
    private void createOutputDirectory() {
        File dir = new File(outputDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Verifica si LibreOffice está disponible en el sistema
     */
    public boolean isLibreOfficeAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(libreOfficePath, "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            return process.exitValue() == 0;
            
        } catch (Exception e) {
            return false;
        }
    }
}