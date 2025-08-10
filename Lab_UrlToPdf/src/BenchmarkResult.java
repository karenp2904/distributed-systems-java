/**
 * Almacena el resultado de una prueba de benchmark
 */
public class BenchmarkResult {
    private final int threads;
    private final long duration;
    
    public BenchmarkResult(int threads, long duration) {
        this.threads = threads;
        this.duration = duration;
    }
    
    public int getThreads() { 
        return threads; 
    }
    
    public long getDuration() { 
        return duration; 
    }
    
    @Override
    public String toString() {
        return String.format("Hilos: %d, Tiempo: %dms", threads, duration);
    }
}