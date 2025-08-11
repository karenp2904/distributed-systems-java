import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class FileValidator {
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
        "docx", "doc", "pptx", "ppt", "xlsx", "xls", 
        "png", "jpg", "jpeg", "bmp", "gif", "tiff",
        "odt", "ods", "odp", "rtf", "txt"
    );
    
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final long fileSize;
        
        public ValidationResult(boolean valid, String message, long fileSize) {
            this.valid = valid;
            this.message = message;
            this.fileSize = fileSize;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public long getFileSize() { return fileSize; }
    }
    
    public static ValidationResult validateFile(String filePath) {
        Path path = Paths.get(filePath);
        
        // Verificar existencia
        if (!Files.exists(path)) {
            return new ValidationResult(false, "El archivo no existe", 0);
        }
        
        // Verificar que es un archivo (no directorio)
        if (!Files.isRegularFile(path)) {
            return new ValidationResult(false, "La ruta no corresponde a un archivo", 0);
        }
        
        // Verificar extensión
        String extension = getFileExtension(filePath).toLowerCase();
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            return new ValidationResult(false, 
                "Formato no soportado: ." + extension + 
                ". Soportados: " + String.join(", ", SUPPORTED_EXTENSIONS), 0);
        }
        
        // Verificar tamaño
        try {
            long size = Files.size(path);
            if (size == 0) {
                return new ValidationResult(false, "El archivo está vacío", 0);
            }
            
            // Advertir sobre archivos muy grandes (>100MB)
            if (size > 100 * 1024 * 1024) {
                return new ValidationResult(true, 
                    "Archivo grande (" + formatFileSize(size) + "), la conversión puede tomar tiempo", size);
            }
            
            return new ValidationResult(true, "Archivo válido", size);
            
        } catch (Exception e) {
            return new ValidationResult(false, "Error leyendo el archivo: " + e.getMessage(), 0);
        }
    }
    
    public static String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        return lastDot > 0 ? filePath.substring(lastDot + 1) : "";
    }
    
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    public static List<String> getSupportedExtensions() {
        return new ArrayList<>(SUPPORTED_EXTENSIONS);
    }
}
