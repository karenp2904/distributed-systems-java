import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BenchmarkRunner {
    private static final int[] THREAD_COUNTS = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};

    public void runBenchmark(List<String> urls) {
        System.out.println("=== BENCHMARK INICIADO ===");
        if (urls.isEmpty()) {
            System.err.println("No hay URLs para procesar.");
            return;
        }
        
        List<BenchmarkResult> results = new ArrayList<>();
        PDFConverter converter = new PDFConverter();

        for (int threads : THREAD_COUNTS) {
            System.out.printf("Ejecutando con %d hilo(s)... ", threads);
            long startTime = System.nanoTime();
            try {
                converter.convertUrls(urls, threads);
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                continue;
            }
            long endTime = System.nanoTime();

            long durationMs = (endTime - startTime) / 1_000_000;
            results.add(new BenchmarkResult(threads, durationMs));

            System.out.printf("%d ms%n", durationMs);
            try { Thread.sleep(1000); } 
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }

        saveCSV(results);
        generateReport(results);
        
        // Llamar a las gráficas con datos reales
        try {
            GeneradorGraficas.generarDesdeCSV("reports/benchmark_data.csv");
        } catch (Exception e) {
            System.err.println("Error generando gráficas: " + e.getMessage());
        }
    }

    private void saveCSV(List<BenchmarkResult> results) {
        try {
            Files.createDirectories(Paths.get("reports"));
            try (PrintWriter pw = new PrintWriter("reports/benchmark_data.csv")) {
                pw.println("Hilos,TiempoMs");
                for (BenchmarkResult r : results) {
                    pw.printf("%d,%d%n", r.getThreads(), r.getDuration());
                }
            }
            System.out.println("CSV de resultados guardado en reports/benchmark_data.csv");
        } catch (IOException e) {
            System.err.println("Error guardando CSV: " + e.getMessage());
        }
    }

    private void generateReport(List<BenchmarkResult> results) {
        try {
            Files.createDirectories(Paths.get("reports"));
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "reports/benchmark_" + timestamp + ".md";
            
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(filename)))) {
                writer.println("# Informe de Rendimiento - Convertidor Web a PDF");
                writer.println("**Fecha:** " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.println();
                writer.println("## Perfil de Hardware");
                writer.println("- **CPU:** " + System.getProperty("os.arch"));
                writer.println("- **Núcleos disponibles:** " + Runtime.getRuntime().availableProcessors());
                writer.println("- **Memoria JVM:** " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
                writer.println("- **Sistema Operativo:** " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
                writer.println();
                writer.println("## Resultados");
                writer.println("| Hilos | Tiempo (ms) | Tiempo (seg) | Mejora (%) |");
                writer.println("|-------|-------------|--------------|------------|");
                
                long baseTime = results.get(0).getDuration();
                for (BenchmarkResult result : results) {
                    double improvement = ((double)(baseTime - result.getDuration()) / baseTime) * 100;
                    writer.printf("| %d | %d | %.2f | %.1f |%n", 
                        result.getThreads(), result.getDuration(), 
                        result.getDuration() / 1000.0, improvement);
                }

                writer.println();
                BenchmarkResult fastest = results.stream()
                    .min(Comparator.comparingLong(BenchmarkResult::getDuration))
                    .orElse(null);
                if (fastest != null) {
                    writer.println("- **Configuración óptima:** " + fastest.getThreads() + " hilos");
                    writer.println("- **Tiempo mínimo:** " + fastest.getDuration() + "ms");
                    writer.println("- **Mejora máxima:** " + String.format("%.1f%%", 
                        ((double)(baseTime - fastest.getDuration()) / baseTime) * 100));
                }
            }
            System.out.println("Reporte generado: " + filename);
        } catch (IOException e) {
            System.err.println("Error generando reporte: " + e.getMessage());
        }
    }
}
