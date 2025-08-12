import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

public class PerfTestRunner {

    private final WindowsPDFConverter app;
    private final Path outputBase;

    public PerfTestRunner(WindowsPDFConverter app, Path outputBase) {
        this.app = app;
        this.outputBase = outputBase;
    }

    public Map<Integer, Long> runFullTest(List<String> files, int maxThreads, Consumer<String> logger) throws Exception {
        if (files.size() != 32) {
            throw new IllegalArgumentException("Se requieren exactamente 32 archivos (recibidos: " + files.size() + ")");
        }
        
        Map<Integer, Long> results = new LinkedHashMap<>();
        Map<Integer, List<Long>> perFileTimes = new LinkedHashMap<>();
        
        logger.accept("🔬 === INICIANDO BENCHMARK COMPLETO ===");
        logger.accept("📊 Archivos: " + files.size() + " | Hilos: 1-" + maxThreads);
        
        for (int threads = 1; threads <= maxThreads; threads++) {
            logger.accept("⚙️ Probando con " + threads + " hilos...");
            
            long startTime = System.currentTimeMillis();
            String testOutput = outputBase.resolve("test_" + threads + "_hilos").toString();
            
            try {
                List<String> converted = app.processFiles(files, testOutput, threads, msg -> {});
                long elapsed = System.currentTimeMillis() - startTime;
                
                results.put(threads, elapsed);
                logger.accept(String.format("✅ %d hilos → %d ms (%.2f seg)", threads, elapsed, elapsed/1000.0));
                
                cleanupTestDirectory(testOutput);
                
                Thread.sleep(500);
                
            } catch (Exception e) {
                logger.accept("❌ Error con " + threads + " hilos: " + e.getMessage());
                throw e;
            }
        }
        
        logger.accept("📈 Generando reporte final...");
        generateFinalReport(results, perFileTimes, files, logger);
        
        return results;
    }
    
    private void cleanupTestDirectory(String testOutput) {
        try {
            Path testPath = Paths.get(testOutput);
            if (Files.exists(testPath)) {
                Files.walk(testPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {}
                    });
            }
        } catch (Exception ignored) {}
    }
    
    private void generateFinalReport(Map<Integer, Long> results, Map<Integer, List<Long>> perFileTimes, 
                                   List<String> files, Consumer<String> logger) throws Exception {
        
        Map<String, String> systemProfile = SystemProfile.gather();
        Path reportDir = outputBase.resolve("reporte");
        
        ReportGenerator generator = new ReportGenerator();
        generator.generate(results, perFileTimes, files, systemProfile, reportDir, app, app.getLogArea());
        
        logger.accept("📊 Reporte completo generado en: " + reportDir.toString());
        logger.accept("📁 Archivos incluidos: CSV, Markdown, HTML, gráficos, capturas");
        
        Map.Entry<Integer, Long> best = results.entrySet().stream()
            .min(Map.Entry.comparingByValue()).orElse(null);
            
        if (best != null) {
            long baseTime = results.get(1);
            double speedup = (double) baseTime / best.getValue();
            logger.accept("🏆 RESULTADO ÓPTIMO: " + best.getKey() + " hilos (" + speedup + "x speedup)");
        }
    }
}