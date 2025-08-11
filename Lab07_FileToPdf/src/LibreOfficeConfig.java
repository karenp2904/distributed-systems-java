import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class LibreOfficeConfig {
    private static final List<String> WINDOWS_PATHS = Arrays.asList(
        "C:\\Program Files\\LibreOffice\\program\\soffice.exe",
        "C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe",
        "C:\\ProgramData\\Microsoft\\Windows\\Start Menu\\Programs\\LibreOffice\\soffice.exe"
    );
    
    private static final List<String> LINUX_PATHS = Arrays.asList(
        "/usr/bin/libreoffice",
        "/usr/bin/soffice",
        "/opt/libreoffice/program/soffice",
        "/snap/bin/libreoffice"
    );
    
    private static final List<String> MAC_PATHS = Arrays.asList(
        "/Applications/LibreOffice.app/Contents/MacOS/soffice"
    );
    
    public static String detectLibreOfficePath() {
        String os = System.getProperty("os.name").toLowerCase();
        List<String> pathsToCheck;
        
        if (os.contains("win")) {
            pathsToCheck = WINDOWS_PATHS;
        } else if (os.contains("mac")) {
            pathsToCheck = MAC_PATHS;
        } else {
            pathsToCheck = LINUX_PATHS;
        }
        
        // Buscar en rutas predefinidas
        for (String path : pathsToCheck) {
            if (Files.exists(Paths.get(path))) {
                System.out.println("LibreOffice encontrado en: " + path);
                return path;
            }
        }
        
        // Buscar en PATH del sistema
        if (isInSystemPath("soffice") || isInSystemPath("libreoffice")) {
            return os.contains("win") ? "soffice.exe" : "soffice";
        }
        
        throw new RuntimeException("LibreOffice no encontrado. Por favor instálelo o especifique la ruta manualmente.");
    }
    
    private static boolean isInSystemPath(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command, "--version");
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean validateLibreOfficePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        
        File file = new File(path);
        return file.exists() && file.canExecute();
    }
}

