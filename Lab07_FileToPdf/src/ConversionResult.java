
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ConversionResult {
    private final String inputPath;
    private final String outputPath;
    private final boolean success;
    private final String message;
    private final long conversionTimeMs;
    private final LocalDateTime timestamp;
    private final long fileSizeBytes;
    private final String threadName;
    
    public ConversionResult(String inputPath, String outputPath, boolean success, 
                          String message, long conversionTimeMs, long fileSizeBytes) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.success = success;
        this.message = message;
        this.conversionTimeMs = conversionTimeMs;
        this.timestamp = LocalDateTime.now();
        this.fileSizeBytes = fileSizeBytes;
        this.threadName = Thread.currentThread().getName();
    }
    
    // Getters
    public String getInputPath() { return inputPath; }
    public String getOutputPath() { return outputPath; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public long getConversionTimeMs() { return conversionTimeMs; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public String getThreadName() { return threadName; }
    
    public String getFormattedTime() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    public String getFormattedDuration() {
        if (conversionTimeMs < 1000) {
            return conversionTimeMs + "ms";
        } else {
            return String.format("%.2fs", conversionTimeMs / 1000.0);
        }
    }
    
    public String getFormattedFileSize() {
        if (fileSizeBytes < 1024) return fileSizeBytes + " B";
        if (fileSizeBytes < 1024 * 1024) return String.format("%.1f KB", fileSizeBytes / 1024.0);
        return String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024.0));
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s -> %s (%s) [%s] Thread: %s", 
            getFormattedTime(), 
            inputPath, 
            outputPath != null ? outputPath : "FAILED", 
            getFormattedDuration(),
            success ? "OK" : "ERROR: " + message,
            threadName);
    }
}
+