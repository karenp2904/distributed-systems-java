public class BenchmarkResult {
    private final int threads;
    private final long timeMs;

    public BenchmarkResult(int threads, long timeMs) {
        this.threads = threads;
        this.timeMs = timeMs;
    }

    public int getThreads() {
        return threads;
    }

    public long getTimeMs() {
        return timeMs;
    }
}

