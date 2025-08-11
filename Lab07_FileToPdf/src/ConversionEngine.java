import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ConversionEngine {
    private final String libreOfficePath;
    private final int timeoutSeconds;
    
    public ConversionEngine(String libreOfficePath) {
        this(libreOfficePath, 120); // 2 minutos por defecto
    }
    
    public ConversionEngine(String libreOfficePath, int timeoutSeconds) {
        this.libreOfficePath = libreOfficePath;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    /**
     * Convierte un archivo individual a PDF
     */
    public ConversionResult convertSingleFile(String inputPath, String outputDirectory) {
        long startTime = System.currentTimeMillis();
        
        // Validar archivo
        FileValidator.ValidationResult validation = FileValidator.validateFile(inputPath);
        if (!validation.isValid()) {
            return new ConversionResult(inputPath, null, false, validation.getMessage(), 
                System.currentTimeMillis() - startTime, 0);
        }
        
        try {
            // Crear directorio de salida
            Files.createDirectories(Paths.get(outputDirectory));
            
            // Generar ruta de salida
            String outputPath = generateOutputPath(inputPath, outputDirectory);
            
            // Ejecutar conversión
            boolean success = executeConversion(inputPath, outputDirectory);
            
            long conversionTime = System.currentTimeMillis() - startTime;
            
            if (success && Files.exists(Paths.get(outputPath))) {
                return new ConversionResult(inputPath, outputPath, true, "Conversión exitosa", 
                    conversionTime, validation.getFileSize());
            } else {
                return new ConversionResult(inputPath, null, false, "Fallo en la conversión", 
                    conversionTime, validation.getFileSize());
            }
            
        } catch (Exception e) {
            long conversionTime = System.currentTimeMillis() - startTime;
            return new ConversionResult(inputPath, null, false, "Error: " + e.getMessage(), 
                conversionTime, validation.getFileSize());
        }
    }
    
    /**
     * Ejecuta el comando de LibreOffice
     */
    private boolean executeConversion(String inputPath, String outputDirectory) 
            throws IOException, InterruptedException {
        
        ProcessBuilder pb = new ProcessBuilder(
            libreOfficePath,
            "--headless",
            "--convert-to", "pdf",
            "--outdir", outputDirectory,
            inputPath
        );
        
        pb.redirectErrorStream(true);
        
        System.out.println("Ejecutando: " + String.join(" ", pb.command()));
        
        Process process = pb.start();
        
        // Capturar salida del proceso
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println("LibreOffice: " + line);
            }
        }
        
        // Esperar con timeout
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Timeout después de " + timeoutSeconds + " segundos");
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            System.err.println("LibreOffice salió con código: " + exitCode);
            System.err.println("Salida: " + output.toString());
        }
        
        return exitCode == 0;
    }
    
    /**
     * Genera la ruta del archivo PDF de salida
     */
    private String generateOutputPath(String inputPath, String outputDirectory) {
        Path inputFile = Paths.get(inputPath);
        String fileName = inputFile.getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        return Paths.get(outputDirectory, baseName + ".pdf").toString();
    }
}