public class PdfConverterApplication {
    private static final String APP_VERSION = "2.0.0";
    private static final String LIBREOFFICE_PATH_WINDOWS = "C:\\ProgramData\\Microsoft\\Windows\\Start Menu\\Programs\\LibreOffice\\soffice.exe";
    
    public static void main(String[] args) {
        // Banner de inicio
        printBanner();
        
        // Verificar instalación de LibreOffice
        if (!verifyLibreOfficeInstallation()) {
            System.err.println("❌ LibreOffice no encontrado. Por favor, instálelo antes de continuar.");
            System.exit(1);
        }
        
        // Ejecutar aplicación
        if (args.length > 0 && args[0].equals("--gui")) {
            runGUI();
        } else {
            runCLI(args);
        }
    }
    
    private static void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                 CONVERSOR PDF EMPRESARIAL                   ║");
        System.out.println("║                     Versión " + APP_VERSION + "                          ║");
        System.out.println("║                                                              ║");
        System.out.println("║  🔄 Conversión masiva de documentos                         ║");
        System.out.println("║  🧵 Procesamiento multi-hilo                                ║");
        System.out.println("║  📊 Reportes detallados de rendimiento                      ║");
        System.out.println("║  🎨 Interfaz gráfica moderna                                ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
    
    private static boolean verifyLibreOfficeInstallation() {
        try {
            String detectedPath = LibreOfficeConfig.detectLibreOfficePath();
            System.out.println("✅ LibreOffice encontrado: " + detectedPath);
            return true;
        } catch (Exception e) {
            System.err.println("❌ " + e.getMessage());
            System.err.println();
            System.err.println("💡 Para instalar LibreOffice:");
            System.err.println("   Windows: Descargar desde https://www.libreoffice.org/");
            System.err.println("   Ubuntu: sudo apt install libreoffice");
            System.err.println("   macOS: brew install --cask libreoffice");
            return false;
        }
    }
    
    private static void runGUI() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Configurar Look and Feel moderno
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
                
                // Configuraciones adicionales para apariencia moderna
                UIManager.put("Button.arc", 10);
                UIManager.put("Component.arc", 10);
                UIManager.put("ProgressBar.arc", 10);
                UIManager.put("TextComponent.arc", 10);
                
            } catch (Exception e) {
                System.err.println("⚠️ No se pudo establecer el Look and Feel del sistema");
            }
            
            new ModernConverterGUI().setVisible(true);
            System.out.println("🎨 Interfaz gráfica iniciada");
        });
    }
    
    private static void runCLI(String[] args) {
        System.out.println("💻 Modo línea de comandos");
        new PdfConverterCLI().run(args);
    }
}
