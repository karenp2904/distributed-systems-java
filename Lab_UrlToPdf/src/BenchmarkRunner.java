import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BenchmarkRunner {
    // Configuración del benchmark
    private static final int[] DEFAULT_THREAD_COUNTS = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
    private static final int ITERATIONS_PER_THREAD = 5;
    
    // Configuración personalizable
    private int[] threadCounts = DEFAULT_THREAD_COUNTS;
    private int maxUrls = 32;
    private int iterations = ITERATIONS_PER_THREAD;

    public void setThreadCounts(int[] threadCounts) {
        this.threadCounts = threadCounts;
    }
    
    public void setMaxUrls(int maxUrls) {
        this.maxUrls = maxUrls;
    }
    
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public void runBenchmark(List<String> urls) {
        System.out.println("=== BENCHMARK INICIADO ===");
        System.out.printf("Configuración: %d iteraciones por hilo, máximo %d URLs%n", iterations, maxUrls);
        
        if (urls.isEmpty()) {
            System.err.println("No hay URLs para procesar.");
            return;
        }
        
        // Limitar URLs
        List<String> testUrls = urls.subList(0, Math.min(maxUrls, urls.size()));
        System.out.printf("Usando %d URLs para el benchmark%n", testUrls.size());
        
        Map<Integer, List<BenchmarkResult>> allResults = new HashMap<>();
        PDFConverter converter = new PDFConverter();

        for (int threads : threadCounts) {
            System.out.printf("Ejecutando con %d hilo(s) - %d iteraciones... ", threads, iterations);
            
            List<BenchmarkResult> threadResults = new ArrayList<>();
            
            for (int i = 1; i <= iterations; i++) {
                System.out.printf("(%d/%d) ", i, iterations);
                
                long startTime = System.nanoTime();
                try {
                    converter.convertUrls(testUrls, threads);
                } catch (Exception e) {
                    System.err.println("Error en iteración " + i + ": " + e.getMessage());
                    continue;
                }
                long endTime = System.nanoTime();

                long durationMs = (endTime - startTime) / 1_000_000;
                threadResults.add(new BenchmarkResult(threads, durationMs));
                
                // Pequeña pausa entre iteraciones
                try { Thread.sleep(500); } 
                catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
            
            allResults.put(threads, threadResults);
            
            // Calcular promedio para mostrar
            double avgTime = threadResults.stream()
                .mapToLong(BenchmarkResult::getDuration)
                .average()
                .orElse(0.0);
            
            System.out.printf("Promedio: %.1f ms%n", avgTime);
        }

        String reportFileName = generateDetailedReport(allResults, testUrls);
        saveCSV(allResults);
        
        // Generar gráficas
        try {
            GeneradorGraficas.generarReporte(reportFileName);
        } catch (Exception e) {
            System.err.println("Error generando gráficas: " + e.getMessage());
        }
    }

    private String generateDetailedReport(Map<Integer, List<BenchmarkResult>> allResults, List<String> urls) {
        try {
            Files.createDirectories(Paths.get("reports"));
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "reports/benchmark_" + timestamp + ".txt";
            
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(filename)))) {
                // Encabezado del reporte
                writer.println("========================================");
                writer.println("  REPORTE DETALLADO DE BENCHMARK");
                writer.println("  Convertidor Web a PDF");
                writer.println("========================================");
                writer.println();
                writer.println("FECHA: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.println();
                
                // Información del sistema
                writer.println("PERFIL DE HARDWARE:");
                writer.println("- CPU Arquitectura: " + System.getProperty("os.arch"));
                writer.println("- Núcleos disponibles: " + Runtime.getRuntime().availableProcessors());
                writer.println("- Memoria JVM máxima: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
                writer.println("- Memoria JVM libre: " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + " MB");
                writer.println("- Sistema Operativo: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
                writer.println("- Java Version: " + System.getProperty("java.version"));
                writer.println();
                
                // Configuración del benchmark
                writer.println("CONFIGURACIÓN DEL BENCHMARK:");
                writer.println("- URLs procesadas: " + urls.size());
                writer.println("- Iteraciones por configuración: " + iterations);
                writer.println("- Hilos probados: " + Arrays.toString(threadCounts));
                writer.println();
                
                writer.println("URLs UTILIZADAS:");
                for (int i = 0; i < urls.size(); i++) {
                    writer.println((i + 1) + ". " + urls.get(i));
                }
                writer.println();
                
                // Resultados detallados
                writer.println("RESULTADOS DETALLADOS:");
                writer.println("========================================");
                
                // Calcular baseline (1 hilo)
                List<BenchmarkResult> baselineResults = allResults.get(1);
                double baselineAvg = baselineResults != null ? 
                    baselineResults.stream().mapToLong(BenchmarkResult::getDuration).average().orElse(0.0) : 0.0;
                
                for (int threads : threadCounts) {
                    List<BenchmarkResult> results = allResults.get(threads);
                    if (results == null || results.isEmpty()) continue;
                    
                    writer.println("HILOS: " + threads);
                    writer.println("----------------------------------------");
                    
                    // Estadísticas detalladas
                    LongSummaryStatistics stats = results.stream()
                        .mapToLong(BenchmarkResult::getDuration)
                        .summaryStatistics();
                    
                    writer.println("Iteraciones realizadas: " + results.size());
                    writer.printf("Tiempo promedio: %.2f ms (%.3f seg)%n", stats.getAverage(), stats.getAverage() / 1000.0);
                    writer.println("Tiempo mínimo: " + stats.getMin() + " ms");
                    writer.println("Tiempo máximo: " + stats.getMax() + " ms");
                    
                    // Desviación estándar
                    double variance = results.stream()
                        .mapToDouble(r -> Math.pow(r.getDuration() - stats.getAverage(), 2))
                        .average()
                        .orElse(0.0);
                    double stdDev = Math.sqrt(variance);
                    writer.printf("Desviación estándar: %.2f ms%n", stdDev);
                    
                    // Eficiencia y mejora
                    if (baselineAvg > 0 && threads > 1) {
                        double speedup = baselineAvg / stats.getAverage();
                        double efficiency = speedup / threads * 100;
                        double improvement = ((baselineAvg - stats.getAverage()) / baselineAvg) * 100;
                        
                        writer.printf("Speedup: %.2fx%n", speedup);
                        writer.printf("Eficiencia: %.1f%%%n", efficiency);
                        writer.printf("Mejora respecto a 1 hilo: %.1f%%%n", improvement);
                    }
                    
                    // Tiempos individuales
                    writer.println("Tiempos individuales:");
                    for (int i = 0; i < results.size(); i++) {
                        writer.printf("  Iteración %d: %d ms%n", i + 1, results.get(i).getDuration());
                    }
                    writer.println();
                }
                
                // Resumen y análisis
                writer.println("ANÁLISIS Y CONCLUSIONES:");
                writer.println("========================================");
                
                // Encontrar configuración óptima
                double bestTime = Double.MAX_VALUE;
                int bestThreads = 1;
                for (int threads : threadCounts) {
                    List<BenchmarkResult> results = allResults.get(threads);
                    if (results != null && !results.isEmpty()) {
                        double avg = results.stream().mapToLong(BenchmarkResult::getDuration).average().orElse(0.0);
                        if (avg < bestTime) {
                            bestTime = avg;
                            bestThreads = threads;
                        }
                    }
                }
                
                writer.println("- Configuración óptima: " + bestThreads + " hilos");
                writer.printf("- Tiempo óptimo promedio: %.2f ms%n", bestTime);
                
                if (baselineAvg > 0) {
                    double maxSpeedup = baselineAvg / bestTime;
                    writer.printf("- Speedup máximo: %.2fx%n", maxSpeedup);
                    writer.printf("- Mejora máxima: %.1f%%%n", ((baselineAvg - bestTime) / baselineAvg) * 100);
                }
                
                // Recomendaciones
                writer.println();
                writer.println("RECOMENDACIONES:");
                int cores = Runtime.getRuntime().availableProcessors();
                writer.println("- Núcleos del sistema: " + cores);
                writer.println("- Rango óptimo sugerido: " + (cores/2) + "-" + (cores*2) + " hilos");
                
                writer.println();
                writer.println("========================================");
                writer.println("Reporte generado automáticamente");
                writer.println("========================================");
            }
            
            System.out.println("Reporte detallado generado: " + filename);
            return filename;
            
        } catch (IOException e) {
            System.err.println("Error generando reporte: " + e.getMessage());
            return null;
        }
    }

    private void saveCSV(Map<Integer, List<BenchmarkResult>> allResults) {
        try {
            Files.createDirectories(Paths.get("reports"));
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "reports/benchmark_data_" + timestamp + ".csv";
            
            try (PrintWriter pw = new PrintWriter(filename)) {
                pw.println("Hilos,Iteracion,TiempoMs,TiempoSeg");
                
                for (int threads : threadCounts) {
                    List<BenchmarkResult> results = allResults.get(threads);
                    if (results != null) {
                        for (int i = 0; i < results.size(); i++) {
                            BenchmarkResult r = results.get(i);
                            pw.printf("%d,%d,%d,%.3f%n", 
                                r.getThreads(), i + 1, r.getDuration(), r.getDuration() / 1000.0);
                        }
                    }
                }
            }
            System.out.println("CSV detallado guardado: " + filename);
        } catch (IOException e) {
            System.err.println("Error guardando CSV: " + e.getMessage());
        }
    }
}