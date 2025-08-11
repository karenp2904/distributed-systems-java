import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConversionManager {
    private final ConversionEngine engine;
    private final int maxThreads;
    private final ExecutorService executorService;
    private final AtomicInteger completedCount;
    private final AtomicInteger failedCount;
    
    public interface ProgressCallback {
        void onProgress(int completed, int total, ConversionResult result);
        void onComplete(ConversionReport report);
    }
    
    public ConversionManager(String libreOfficePath) {
        this(libreOfficePath, Runtime.getRuntime().availableProcessors());
    }
    
    public ConversionManager(String libreOfficePath, int maxThreads) {
        this.engine = new ConversionEngine(libreOfficePath);
        this.maxThreads = maxThreads;
        this.executorService = Executors.newFixedThreadPool(maxThreads, r -> {
            Thread t = new Thread(r);
            t.setName("Converter-" + t.getId());
            return t;
        });
        this.completedCount = new AtomicInteger(0);
        this.failedCount = new AtomicInteger(0);
    }
    
    /**
     * Convierte múltiples archivos de forma concurrente
     */
    public Future<ConversionReport> convertFilesAsync(List<String> filePaths, 
                                                     String outputDirectory, 
                                                     ProgressCallback callback) {
        
        return CompletableFuture.supplyAsync(() -> {
            long overallStartTime = System.currentTimeMillis();
            List<ConversionResult> results = new ArrayList<>();
            List<Future<ConversionResult>> futures = new ArrayList<>();
            
            completedCount.set(0);
            failedCount.set(0);
            
            // Enviar tareas a los hilos
            for (String filePath : filePaths) {
                Future<ConversionResult> future = executorService.submit(() -> 
                    engine.convertSingleFile(filePath, outputDirectory)
                );
                futures.add(future);
            }
            
            // Recopilar resultados
            for (Future<ConversionResult> future : futures) {
                try {
                    ConversionResult result = future.get();
                    results.add(result);
                    
                    if (result.isSuccess()) {
                        completedCount.incrementAndGet();
                    } else {
                        failedCount.incrementAndGet();
                    }
                    
                    // Notificar progreso
                    if (callback != null) {
                        callback.onProgress(completedCount.get() + failedCount.get(), 
                                          filePaths.size(), result);
                    }
                    
                } catch (Exception e) {
                    ConversionResult errorResult = new ConversionResult(
                        "unknown", null, false, "Error del hilo: " + e.getMessage(), 
                        0, 0);
                    results.add(errorResult);
                    failedCount.incrementAndGet();
                }
            }
            
            long totalTime = System.currentTimeMillis() - overallStartTime;
            ConversionReport report = new ConversionReport(results, totalTime, maxThreads);
            
            if (callback != null) {
                callback.onComplete(report);
            }
            
            return report;
        }, executorService);
    }
    
    /**
     * Convierte archivos de forma síncrona
     */
    public ConversionReport convertFiles(List<String> filePaths, String outputDirectory) {
        try {
            return convertFilesAsync(filePaths, outputDirectory, null).get();
        } catch (Exception e) {
            throw new RuntimeException("Error en conversión síncrona", e);
        }
    }
    
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
    
    public int getMaxThreads() {
        return maxThreads;
    }
}