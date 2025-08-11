import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadMonitor {
    private final ThreadMXBean threadBean;
    private final Map<String, ThreadInfo> threadInfoMap;
    private final long startTime;
    
    public static class ThreadInfo {
        private final String threadName;
        private final long threadId;
        private long startTime;
        private long endTime;
        private int tasksCompleted;
        private long totalCpuTime;
        private long totalUserTime;
        
        public ThreadInfo(String threadName, long threadId) {
            this.threadName = threadName;
            this.threadId = threadId;
            this.startTime = System.currentTimeMillis();
            this.tasksCompleted = 0;
        }
        
        public void taskCompleted() {
            this.tasksCompleted++;
        }
        
        public void finish() {
            this.endTime = System.currentTimeMillis();
        }
        
        // Getters
        public String getThreadName() { return threadName; }
        public long getThreadId() { return threadId; }
        public long getDurationMs() { return endTime - startTime; }
        public int getTasksCompleted() { return tasksCompleted; }
        public double getTasksPerSecond() {
            long durationSeconds = getDurationMs() / 1000;
            return durationSeconds > 0 ? (double) tasksCompleted / durationSeconds : 0;
        }
    }
    
    public ThreadMonitor() {
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.threadInfoMap = new ConcurrentHashMap<>();
        this.startTime = System.currentTimeMillis();
    }
    
    public void registerThread(String threadName) {
        long threadId = Thread.currentThread().getId();
        threadInfoMap.put(threadName, new ThreadInfo(threadName, threadId));
    }
    
    public void recordTaskCompletion(String threadName) {
        ThreadInfo info = threadInfoMap.get(threadName);
        if (info != null) {
            info.taskCompleted();
        }
    }
    
    public void finishThread(String threadName) {
        ThreadInfo info = threadInfoMap.get(threadName);
        if (info != null) {
            info.finish();
        }
    }
    
    public ThreadReport generateReport() {
        long totalDuration = System.currentTimeMillis() - startTime;
        return new ThreadReport(new ArrayList<>(threadInfoMap.values()), totalDuration);
    }
    
    public static class ThreadReport {
        private final List<ThreadInfo> threadInfos;
        private final long totalDurationMs;
        
        public ThreadReport(List<ThreadInfo> threadInfos, long totalDurationMs) {
            this.threadInfos = threadInfos;
            this.totalDurationMs = totalDurationMs;
        }
        
        public String generateDetailedReport() {
            StringBuilder sb = new StringBuilder();
            
            sb.append("🧵 REPORTE DETALLADO DE HILOS\n");
            sb.append("═══════════════════════════════════════════════════════════\n\n");
            
            sb.append("📊 RESUMEN GENERAL\n");
            sb.append("├─ Duración total: ").append(String.format("%.2f segundos", totalDurationMs / 1000.0)).append("\n");
            sb.append("├─ Hilos utilizados: ").append(threadInfos.size()).append("\n");
            sb.append("├─ Tareas totales: ").append(getTotalTasks()).append("\n");
            sb.append("├─ Rendimiento global: ").append(String.format("%.2f tareas/segundo", getOverallTasksPerSecond())).append("\n");
            sb.append("└─ Eficiencia promedio: ").append(String.format("%.1f%%", getAverageEfficiency())).append("\n\n");
            
            sb.append("📈 ESTADÍSTICAS POR HILO\n");
            threadInfos.stream()
                .sorted(Comparator.comparing(ThreadInfo::getTasksCompleted).reversed())
                .forEach(info -> {
                    sb.append("├─ ").append(info.getThreadName()).append("\n");
                    sb.append("│  ├─ Tareas completadas: ").append(info.getTasksCompleted()).append("\n");
                    sb.append("│  ├─ Duración: ").append(String.format("%.2f segundos", info.getDurationMs() / 1000.0)).append("\n");
                    sb.append("│  ├─ Rendimiento: ").append(String.format("%.2f tareas/segundo", info.getTasksPerSecond())).append("\n");
                    sb.append("│  └─ Eficiencia: ").append(String.format("%.1f%%", 
                        (double) info.getDurationMs() / totalDurationMs * 100)).append("\n");
                });
            
            return sb.toString();
        }
        
        private int getTotalTasks() {
            return threadInfos.stream().mapToInt(ThreadInfo::getTasksCompleted).sum();
        }
        
        private double getOverallTasksPerSecond() {
            double totalSeconds = totalDurationMs / 1000.0;
            return totalSeconds > 0 ? getTotalTasks() / totalSeconds : 0;
        }
        
        private double getAverageEfficiency() {
            if (threadInfos.isEmpty()) return 0;
            
            double totalEfficiency = threadInfos.stream()
                .mapToDouble(info -> (double) info.getDurationMs() / totalDurationMs * 100)
                .sum();
            
            return totalEfficiency / threadInfos.size();
        }
    }
}
