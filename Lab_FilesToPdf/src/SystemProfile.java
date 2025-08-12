import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SystemProfile {
    
    public static Map<String, String> gather() {
        Map<String, String> profile = new LinkedHashMap<>();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();
        
        profile.put("Fecha", java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        profile.put("OS", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        profile.put("OS Arch", System.getProperty("os.arch"));
        profile.put("Java Version", System.getProperty("java.version"));
        profile.put("Available Processors", String.valueOf(runtime.availableProcessors()));
        profile.put("Max JVM Memory (MB)", String.valueOf(runtime.maxMemory() / (1024 * 1024)));
        profile.put("Total JVM Memory (MB)", String.valueOf(runtime.totalMemory() / (1024 * 1024)));
        profile.put("Free JVM Memory (MB)", String.valueOf(runtime.freeMemory() / (1024 * 1024)));
        
        detectCPUInfo(profile);
        detectMemoryInfo(profile);
        detectStorageInfo(profile);
        detectSystemLoad(profile, osBean);
        
        return profile;
    }
    
    private static void detectCPUInfo(Map<String, String> profile) {
        try {
            if (isWindows()) {
                detectWindowsCPU(profile);
            } else {
                detectLinuxCPU(profile);
            }
        } catch (Exception e) {
            profile.put("CPU Model", "No detectado (" + e.getMessage() + ")");
        }
    }
    
    private static void detectWindowsCPU(Map<String, String> profile) throws Exception {
        try {
            ProcessBuilder pb = new ProcessBuilder("wmic", "cpu", "get", "name", "/format:list");
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            
            for (String line : output.split("\n")) {
                if (line.startsWith("Name=") && !line.trim().equals("Name=")) {
                    profile.put("CPU Model", line.substring(5).trim());
                    break;
                }
            }
            
            pb = new ProcessBuilder("wmic", "cpu", "get", "NumberOfCores,NumberOfLogicalProcessors", "/format:list");
            process = pb.start();
            output = new String(process.getInputStream().readAllBytes());
            
            String cores = "", threads = "";
            for (String line : output.split("\n")) {
                if (line.startsWith("NumberOfCores=")) {
                    cores = line.substring(14).trim();
                } else if (line.startsWith("NumberOfLogicalProcessors=")) {
                    threads = line.substring(26).trim();
                }
            }
            
            if (!cores.isEmpty() && !threads.isEmpty()) {
                profile.put("CPU Cores/Threads", cores + " cores, " + threads + " threads");
            }
            
        } catch (Exception e) {
            profile.put("CPU Model", "Windows - No detectado");
        }
    }
    
    private static void detectLinuxCPU(Map<String, String> profile) throws Exception {
        Path cpuinfo = Paths.get("/proc/cpuinfo");
        if (Files.exists(cpuinfo)) {
            List<String> lines = Files.readAllLines(cpuinfo);
            
            Optional<String> modelName = lines.stream()
                .filter(line -> line.toLowerCase().startsWith("model name"))
                .findFirst();
            
            if (modelName.isPresent()) {
                String[] parts = modelName.get().split(":", 2);
                if (parts.length > 1) {
                    profile.put("CPU Model", parts[1].trim());
                }
            }
            
            long physicalCores = lines.stream()
                .filter(line -> line.startsWith("cpu cores"))
                .findFirst()
                .map(line -> Long.parseLong(line.split(":")[1].trim()))
                .orElse(0L);
            
            long processors = lines.stream()
                .filter(line -> line.startsWith("processor"))
                .count();
            
            if (physicalCores > 0 && processors > 0) {
                profile.put("CPU Cores/Threads", physicalCores + " cores, " + processors + " threads");
            }
            
            Optional<String> cacheSize = lines.stream()
                .filter(line -> line.toLowerCase().startsWith("cache size"))
                .findFirst();
            
            if (cacheSize.isPresent()) {
                String[] parts = cacheSize.get().split(":", 2);
                if (parts.length > 1) {
                    profile.put("CPU Cache L3", parts[1].trim());
                }
            }
        }
    }
    
    private static void detectMemoryInfo(Map<String, String> profile) {
        try {
            if (isWindows()) {
                detectWindowsMemory(profile);
            } else {
                detectLinuxMemory(profile);
            }
        } catch (Exception e) {
            profile.put("Physical RAM", "No detectado");
        }
    }
    
    private static void detectWindowsMemory(Map<String, String> profile) throws Exception {
        try {
            ProcessBuilder pb = new ProcessBuilder("wmic", "computersystem", "get", "TotalPhysicalMemory", "/format:list");
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            
            for (String line : output.split("\n")) {
                if (line.startsWith("TotalPhysicalMemory=")) {
                    long bytes = Long.parseLong(line.substring(20).trim());
                    long mb = bytes / (1024 * 1024);
                    long gb = mb / 1024;
                    profile.put("Physical RAM", gb + " GB (" + mb + " MB)");
                    break;
                }
            }
        } catch (Exception e) {
            profile.put("Physical RAM", "Windows - No detectado");
        }
    }
    
    private static void detectLinuxMemory(Map<String, String> profile) throws Exception {
        Path meminfo = Paths.get("/proc/meminfo");
        if (Files.exists(meminfo)) {
            List<String> lines = Files.readAllLines(meminfo);
            
            Optional<String> memTotal = lines.stream()
                .filter(line -> line.startsWith("MemTotal"))
                .findFirst();
            
            if (memTotal.isPresent()) {
                String memLine = memTotal.get();
                String kbStr = memLine.replaceAll("\\D+", "").trim();
                if (!kbStr.isEmpty()) {
                    long kb = Long.parseLong(kbStr);
                    long mb = kb / 1024;
                    long gb = mb / 1024;
                    profile.put("Physical RAM", gb + " GB (" + mb + " MB)");
                }
            }
        }
    }
    
    private static void detectStorageInfo(Map<String, String> profile) {
        try {
            if (isWindows()) {
                detectWindowsStorage(profile);
            } else {
                detectLinuxStorage(profile);
            }
        } catch (Exception e) {
            profile.put("Storage Type", "No detectado");
        }
    }
    
    private static void detectWindowsStorage(Map<String, String> profile) throws Exception {
        try {
            ProcessBuilder pb = new ProcessBuilder("wmic", "diskdrive", "get", "Model,MediaType", "/format:list");
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            
            List<String> drives = new ArrayList<>();
            String currentModel = "";
            String currentType = "";
            
            for (String line : output.split("\n")) {
                line = line.trim();
                if (line.startsWith("Model=") && !line.equals("Model=")) {
                    currentModel = line.substring(6).trim();
                } else if (line.startsWith("MediaType=") && !line.equals("MediaType=")) {
                    currentType = line.substring(10).trim();
                    if (!currentModel.isEmpty()) {
                        String typeDesc = currentType.toLowerCase().contains("ssd") ? "SSD" : 
                                        currentType.toLowerCase().contains("fixed") ? "HDD" : "Unknown";
                        drives.add(currentModel + " (" + typeDesc + ")");
                        currentModel = "";
                        currentType = "";
                    }
                }
            }
            
            if (!drives.isEmpty()) {
                profile.put("Storage Drives", String.join("; ", drives));
            }
        } catch (Exception e) {
            profile.put("Storage Type", "Windows - No detectado");
        }
    }
    
    private static void detectLinuxStorage(Map<String, String> profile) throws Exception {
        Path sysBlock = Paths.get("/sys/block");
        if (Files.isDirectory(sysBlock)) {
            List<String> storageDevices = new ArrayList<>();
            
            List<Path> devices = Files.list(sysBlock)
                .filter(path -> !path.getFileName().toString().startsWith("loop"))
                .filter(path -> !path.getFileName().toString().startsWith("ram"))
                .collect(Collectors.toList());
            
            for (Path device : devices) {
                try {
                    String deviceName = device.getFileName().toString();
                    Path rotationalPath = device.resolve("queue/rotational");
                    
                    if (Files.exists(rotationalPath)) {
                        String rotational = Files.readString(rotationalPath).trim();
                        String type = "0".equals(rotational) ? "SSD/NVMe" : "HDD";
                        
                        Path sizePath = device.resolve("size");
                        String sizeInfo = "";
                        if (Files.exists(sizePath)) {
                            try {
                                long sectors = Long.parseLong(Files.readString(sizePath).trim());
                                long bytes = sectors * 512;
                                long gb = bytes / (1024 * 1024 * 1024);
                                if (gb > 0) sizeInfo = " (" + gb + "GB)";
                            } catch (Exception ignored) {}
                        }
                        
                        storageDevices.add(deviceName + sizeInfo + " - " + type);
                    }
                } catch (Exception ignored) {}
            }
            
            if (!storageDevices.isEmpty()) {
                profile.put("Storage Devices", String.join("; ", storageDevices));
            }
        }
    }
    
    private static void detectSystemLoad(Map<String, String> profile, OperatingSystemMXBean osBean) {
        try {
            double systemLoad = osBean.getSystemLoadAverage();
            if (systemLoad >= 0) {
                profile.put("System Load Average", String.format("%.2f", systemLoad));
            }
            
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                
                double cpuUsage = sunBean.getSystemCpuLoad() * 100;
                if (cpuUsage >= 0) {
                    profile.put("CPU Usage", String.format("%.1f%%", cpuUsage));
                }
                
                long totalPhysical = sunBean.getTotalPhysicalMemorySize();
                long freePhysical = sunBean.getFreePhysicalMemorySize();
                if (totalPhysical > 0 && freePhysical > 0) {
                    double memUsage = ((double)(totalPhysical - freePhysical) / totalPhysical) * 100;
                    profile.put("Memory Usage", String.format("%.1f%%", memUsage));
                }
            }
        } catch (Exception e) {
            profile.put("System Load", "No disponible");
        }
    }
    
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}