import java.util.*;
import java.util.concurrent.Future;

public class PdfConverterCLI {
    private ConversionManager conversionManager;
    private boolean verboseMode = false;
    
    public static void main(String[] args) {
        new PdfConverterCLI().run(args);
    }
    
    public void run(String[] args) {
        try {
            CLIOptions options = parseArguments(args);
            
            if (options.showHelp) {
                printUsage();
                return;
            }
            
            if (options.showGUI) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
                    } catch (Exception e) { /* Ignorar */ }
                    new ModernConverterGUI().setVisible(true);
                });
                return;
            }
            
            if (options.filePaths.isEmpty()) {
                System.err.println("❌ Error: No se especificaron archivos para convertir");
                printUsage();
                return;
            }
            
            // Inicializar conversor
            String libreOfficePath = options.customLibreOfficePath != null ? 
                options.customLibreOfficePath : LibreOfficeConfig.detectLibreOfficePath();
            
            conversionManager = new ConversionManager(libreOfficePath, options.maxThreads);
            verboseMode = options.verbose;
            
            // Ejecutar conversión
            executeConversion(options);
            
        } catch (Exception e) {
            System.err.println("❌ Error fatal: " + e.getMessage());
            if (verboseMode) {
                e.printStackTrace();
            }
            System.exit(1);
        } finally {
            if (conversionManager != null) {
                conversionManager.shutdown();
            }
        }
    }
    
    private void executeConversion(CLIOptions options) {
        System.out.println("🚀 Iniciando conversión...");
        System.out.println("📁 Archivos: " + options.filePaths.size());
        System.out.println("📂 Salida: " + options.outputDirectory);
        System.out.println("🧵 Hilos: " + options.maxThreads);
        System.out.println("⚙️ LibreOffice: " + (options.customLibreOfficePath != null ? 
            options.customLibreOfficePath : "Auto-detectado"));
        System.out.println();
        
        long startTime = System.currentTimeMillis();
        
        if (options.async) {
            executeAsyncConversion(options);
        } else {
            executeSyncConversion(options);
        }
    }
    
    private void executeSyncConversion(CLIOptions options) {
        ConversionReport report = conversionManager.convertFiles(options.filePaths, options.outputDirectory);
        printDetailedReport(report);
    }
    
    private void executeAsyncConversion(CLIOptions options) {
        Future<ConversionReport> future = conversionManager.convertFilesAsync(
            options.filePaths, options.outputDirectory, 
            new ConversionManager.ProgressCallback() {
                @Override
                public void onProgress(int completed, int total, ConversionResult result) {
                    String status = result.isSuccess() ? "✅" : "❌";
                    String fileName = new java.io.File(result.getInputPath()).getName();
                    System.out.printf("[%d/%d] %s %s (%s) [%s]%n", 
                        completed, total, status, fileName, 
                        result.getFormattedDuration(), result.getThreadName());
                }
                
                @Override
                public void onComplete(ConversionReport report) {
                    System.out.println("\n🎯 Conversión completada!");
                    printDetailedReport(report);
                }
            });
        
        try {
            ConversionReport report = future.get();
            
            if (options.generateJsonReport) {
                saveJsonReport(report, options.outputDirectory);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error en conversión asíncrona: " + e.getMessage());
        }
    }
    
    private void printDetailedReport(ConversionReport report) {
        System.out.println(report.generateDetailedReport());
        
        // Mostrar estadísticas de hilos si es modo verbose
        if (verboseMode) {
            System.out.println("🧵 ESTADÍSTICAS DETALLADAS POR HILO:");
            Map<String, List<ConversionResult>> threadStats = report.getResultsByThread();
            for (Map.Entry<String, List<ConversionResult>> entry : threadStats.entrySet()) {
                System.out.println("  " + entry.getKey() + ":");
                for (ConversionResult result : entry.getValue()) {
                    System.out.println("    " + result);
                }
            }
        }
    }
    
    private void saveJsonReport(ConversionReport report, String outputDirectory) {
        try {
            String jsonReport = report.generateJsonReport();
            String reportPath = outputDirectory + "/conversion_report_" + 
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json";
            
            java.nio.file.Files.write(java.nio.file.Paths.get(reportPath), jsonReport.getBytes());
            System.out.println("📊 Reporte JSON guardado en: " + reportPath);
            
        } catch (Exception e) {
            System.err.println("❌ Error guardando reporte JSON: " + e.getMessage());
        }
    }
    
    private CLIOptions parseArguments(String[] args) {
        CLIOptions options = new CLIOptions();
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-o":
                case "--output":
                    if (i + 1 < args.length) {
                        options.outputDirectory = args[++i];
                    }
                    break;
                case "-l":
                case "--libreoffice":
                    if (i + 1 < args.length) {
                        options.customLibreOfficePath = args[++i];
                    }
                    break;
                case "-t":
                case "--threads":
                    if (i + 1 < args.length) {
                        options.maxThreads = Integer.parseInt(args[++i]);
                    }
                    break;
                case "-v":
                case "--verbose":
                    options.verbose = true;
                    break;
                case "--async":
                    options.async = true;
                    break;
                case "--json-report":
                    options.generateJsonReport = true;
                    break;
                case "--gui":
                    options.showGUI = true;
                    break;
                case "-h":
                case "--help":
                    options.showHelp = true;
                    break;
                default:
                    if (!args[i].startsWith("-")) {
                        options.filePaths.add(args[i]);
                    }
                    break;
            }
        }
        
        return options;
    }
    
    private void printUsage() {
        System.out.println("🔄 CONVERSOR DE DOCUMENTOS A PDF");
        System.out.println("Uso: java PdfConverterCLI [opciones] archivo1 archivo2 ...");
        System.out.println();
        System.out.println("Opciones:");
        System.out.println("  -o, --output DIR         Directorio de salida (por defecto: directorio actual)");
        System.out.println("  -l, --libreoffice PATH   Ruta personalizada a LibreOffice");
        System.out.println("  -t, --threads NUM        Número máximo de hilos (por defecto: CPUs disponibles)");
        System.out.println("  -v, --verbose            Modo verbose con información detallada");
        System.out.println("  --async                  Conversión asíncrona con progreso en tiempo real");
        System.out.println("  --json-report            Generar reporte en formato JSON");
        System.out.println("  --gui                    Abrir interfaz gráfica");
        System.out.println("  -h, --help               Mostrar esta ayuda");
        System.out.println();
        System.out.println("Formatos soportados:");
        System.out.println("  " + String.join(", ", FileValidator.getSupportedExtensions()));
        System.out.println();
        System.out.println("Ejemplos:");
        System.out.println("  java PdfConverterCLI --gui");
        System.out.println("  java PdfConverterCLI documento.docx presentacion.pptx");
        System.out.println("  java PdfConverterCLI -o /ruta/salida -t 4 --async archivo.xlsx");
        System.out.println("  java PdfConverterCLI --verbose --json-report *.docx");
    }
    
    private static class CLIOptions {
        List<String> filePaths = new ArrayList<>();
        String outputDirectory = System.getProperty("user.dir");
        String customLibreOfficePath = null;
        int maxThreads = Runtime.getRuntime().availableProcessors();
        boolean verbose = false;
        boolean async = false;
        boolean generateJsonReport = false;
        boolean showGUI = false;
        boolean showHelp = false;
    }
}
