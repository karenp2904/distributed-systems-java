
import java.util.Arrays;
import java.util.List;

public class PdfConverterExample {
    public static void main(String[] args) {
        try {
            // Configurar LibreOffice (ajustar ruta según tu sistema)
            String libreOfficePath = "C:\\ProgramData\\Microsoft\\Windows\\Start Menu\\Programs\\LibreOffice\\soffice.exe";
            ConversionManager manager = new ConversionManager(libreOfficePath, 4);
            
            // Lista de archivos para convertir
            List<String> archivos = Arrays.asList(
                "C:\\Documentos\\archivo-1.docx",
                "C:\\Documentos\\archivo-2.xlsx", 
                "C:\\Documentos\\archivo-3.pptx",
                "C:\\Documentos\\imagen.png"
            );
            
            String directorioSalida = "C:\\PDFs";
            
            System.out.println("🚀 Iniciando conversión programática...");
            
            // Conversión con callback de progreso
            manager.convertFilesAsync(archivos, directorioSalida, 
                new ConversionManager.ProgressCallback() {
                    @Override
                    public void onProgress(int completed, int total, ConversionResult result) {
                        System.out.printf("Progreso: %d/%d - %s%n", 
                            completed, total, result.isSuccess() ? "✅" : "❌");
                    }
                    
                    @Override
                    public void onComplete(ConversionReport report) {
                        System.out.println("\n📊 CONVERSIÓN COMPLETADA");
                        System.out.println(report.generateDetailedReport());
                        
                        // Obtener rutas de PDFs exitosos
                        List<String> pdfGenerados = report.getSuccessfulPdfPaths();
                        System.out.println("\n📁 PDFs generados:");
                        pdfGenerados.forEach(path -> System.out.println("  " + path));
                    }
                }
            );
            
            // Mantener el programa corriendo para la conversión asíncrona
            Thread.sleep(30000); // Esperar 30 segundos
            
            manager.shutdown();
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}