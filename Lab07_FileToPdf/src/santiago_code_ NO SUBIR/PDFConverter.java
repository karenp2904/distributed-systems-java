import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PDFConverter {
    private static final String LIBREOFFICE_CMD = "libreoffice";
    private static final Set<String> SUPPORTED_FORMATS = Set.of("docx", "pptx", "xlsx", "png", "odt", "odp", "ods");
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== PDF Converter ===");
        System.out.println("1. Conversión individual");
        System.out.println("2. Análisis de rendimiento");
        System.out.print("Seleccione opción: ");
        
        int option = scanner.nextInt();
        scanner.nextLine();
        
        switch (option) {
            case 1:
                runSingleConversion(scanner);
                break;
            case 2:
                runPerformanceAnalysis(scanner);
                break;
            default:
                System.out.println("Opción inválida");
        }
        
        scanner.close();
    }
    
    private static void runSingleConversion(Scanner scanner) {
        System.out.print("Ingrese rutas de archivos (separadas por coma): ");
        String input = scanner.nextLine();
        String[] paths = input.split(",");
        
        System.out.print("Directorio de salida (Enter para directorio actual): ");
        String outputDir = scanner.nextLine().trim();
        if (outputDir.isEmpty()) {
            outputDir = System.getProperty("user.dir");
        }
        
        System.out.print("Número de hilos (Enter para 1): ");
        String threadsInput = scanner.nextLine().trim();
        int threads = threadsInput.isEmpty() ? 1 : Integer.parseInt(threadsInput);
        
        List<String> inputFiles = Arrays.stream(paths)
                .map(String::trim)
                .filter(path -> !path.isEmpty())
                .toList();
        
        long startTime = System.currentTimeMillis();
        List<String> convertedFiles = convertToPDF(inputFiles, outputDir, threads);
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n=== Resultados ===");
        System.out.println("Tiempo total: " + (endTime - startTime) + " ms");
        System.out.println("Archivos convertidos:");
        convertedFiles.forEach(System.out::println);
    }
    
    private static void runPerformanceAnalysis(Scanner scanner) {
        System.out.print("Ingrese 32 rutas de archivos (separadas por coma): ");
        String input = scanner.nextLine();
        String[] paths = input.split(",");
        
        if (paths.length != 32) {
            System.out.println("Error: Se requieren exactamente 32 archivos");
            return;
        }
        
        System.out.print("Directorio de salida: ");
        String outputDir = scanner.nextLine().trim();
        
        List<String> inputFiles = Arrays.stream(paths)
                .map(String::trim)
                .toList();
        
        performanceAnalysis(inputFiles, outputDir);
    }
    
    public static List<String> convertToPDF(List<String> inputFiles, String outputDir, int threadCount) {
        List<String> convertedFiles = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        try {
            Files.createDirectories(Paths.get(outputDir));
            
            List<Future<String>> futures = new ArrayList<>();
            
            for (String inputFile : inputFiles) {
                Future<String> future = executor.submit(() -> {
                    try {
                        return convertSingleFile(inputFile, outputDir);
                    } catch (Exception e) {
                        System.err.println("Error convirtiendo " + inputFile + ": " + e.getMessage());
                        return null;
                    }
                });
                futures.add(future);
            }
            
            for (Future<String> future : futures) {
                try {
                    String result = future.get();
                    if (result != null) {
                        convertedFiles.add(result);
                    }
                } catch (Exception e) {
                    System.err.println("Error obteniendo resultado: " + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error creando directorio de salida: " + e.getMessage());
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        return convertedFiles;
    }
    
    private static String convertSingleFile(String inputFile, String outputDir) throws Exception {
        Path inputPath = Paths.get(inputFile);
        
        if (!Files.exists(inputPath)) {
            throw new FileNotFoundException("Archivo no encontrado: " + inputFile);
        }
        
        String extension = getFileExtension(inputPath.getFileName().toString()).toLowerCase();
        if (!SUPPORTED_FORMATS.contains(extension)) {
            throw new UnsupportedOperationException("Formato no soportado: " + extension);
        }
        
        String outputFileName = inputPath.getFileName().toString();
        int lastDot = outputFileName.lastIndexOf('.');
        if (lastDot > 0) {
            outputFileName = outputFileName.substring(0, lastDot);
        }
        outputFileName += ".pdf";
        
        Path outputPath = Paths.get(outputDir, outputFileName);
        
        ProcessBuilder pb = new ProcessBuilder(
            LIBREOFFICE_CMD,
            "--headless",
            "--convert-to", "pdf",
            "--outdir", outputDir,
            inputPath.toAbsolutePath().toString()
        );
        
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Timeout en conversión de: " + inputFile);
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.lines().reduce("", (a, b) -> a + "\n" + b);
                throw new RuntimeException("Error en LibreOffice (código " + exitCode + "): " + output);
            }
        }
        
        if (!Files.exists(outputPath)) {
            throw new RuntimeException("Archivo PDF no fue creado: " + outputPath);
        }
        
        return outputPath.toAbsolutePath().toString();
    }
    
    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
    
    private static void performanceAnalysis(List<String> inputFiles, String outputDir) {
        System.out.println("\n=== Iniciando análisis de rendimiento ===");
        System.out.println("Archivos a procesar: " + inputFiles.size());
        
        Map<Integer, Long> results = new LinkedHashMap<>();
        
        for (int threads = 1; threads <= 16; threads++) {
            System.out.println("\nPrueba con " + threads + " hilo(s)...");
            
            String testOutputDir = outputDir + "/test_" + threads + "_threads_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            long startTime = System.currentTimeMillis();
            List<String> converted = convertToPDF(inputFiles, testOutputDir, threads);
            long endTime = System.currentTimeMillis();
            
            long executionTime = endTime - startTime;
            results.put(threads, executionTime);
            
            System.out.println("Tiempo: " + executionTime + " ms");
            System.out.println("Archivos convertidos: " + converted.size());
            
            // Limpieza opcional de archivos de prueba
            try {
                Files.walk(Paths.get(testOutputDir))
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignorar errores de limpieza
                        }
                    });
            } catch (IOException e) {
                // Ignorar errores de limpieza
            }
        }
        
        printResults(results);
        saveResultsToFile(results, outputDir);
    }
    
    private static void printResults(Map<Integer, Long> results) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("RESULTADOS DE RENDIMIENTO");
        System.out.println("=".repeat(50));
        System.out.printf("%-8s | %-12s | %-10s%n", "Hilos", "Tiempo (ms)", "Mejora");
        System.out.println("-".repeat(35));
        
        long baselineTime = results.get(1);
        
        for (Map.Entry<Integer, Long> entry : results.entrySet()) {
            int threads = entry.getKey();
            long time = entry.getValue();
            double improvement = ((double) baselineTime / time);
            
            System.out.printf("%-8d | %-12d | %.2fx%n", threads, time, improvement);
        }
        
        // Encontrar configuración óptima
        int optimalThreads = results.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(1);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("ANÁLISIS:");
        System.out.println("Configuración óptima: " + optimalThreads + " hilos");
        System.out.println("Tiempo mínimo: " + results.get(optimalThreads) + " ms");
        System.out.printf("Mejora sobre 1 hilo: %.2fx%n", 
            (double) baselineTime / results.get(optimalThreads));
    }
    
    private static void saveResultsToFile(Map<Integer, Long> results, String outputDir) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path csvFile = Paths.get(outputDir, "performance_results_" + timestamp + ".csv");
            
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(csvFile))) {
                writer.println("Hilos,Tiempo_ms,Mejora_vs_1_hilo");
                
                long baselineTime = results.get(1);
                for (Map.Entry<Integer, Long> entry : results.entrySet()) {
                    int threads = entry.getKey();
                    long time = entry.getValue();
                    double improvement = ((double) baselineTime / time);
                    
                    writer.printf("%d,%d,%.2f%n", threads, time, improvement);
                }
            }
            
            System.out.println("\nResultados guardados en: " + csvFile.toAbsolutePath());
            
        } catch (IOException e) {
            System.err.println("Error guardando resultados: " + e.getMessage());
        }
    }
}