import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CLIRunner {
    public void run(String[] args) {
        if (args.length < 2) {
            printUsage();
            return;
        }
        
        String command = args[1];
        switch (command) {
            case "convert":
                if (args.length < 3) {
                    System.err.println("Error: Archivo de URLs requerido");
                    return;
                }
                convert(args[2], args.length > 3 ? Integer.parseInt(args[3]) : 4);
                break;
                
            case "benchmark":
                if (args.length < 3) {
                    System.err.println("Error: Archivo de URLs requerido");
                    return;
                }
                benchmark(args[2]);
                break;
                
            default:
                System.err.println("Comando desconocido: " + command);
                printUsage();
        }
    }
    
    private void convert(String filename, int threads) {
        try {
            List<String> urls = Files.readAllLines(Paths.get(filename));
            urls.removeIf(String::isEmpty);
            
            PDFConverter converter = new PDFConverter(); // No necesitas import!
            
            System.out.println("Convirtiendo " + urls.size() + " URLs con " + threads + " hilos...");
            List<ConversionResult> results = converter.convertUrls(urls, threads);
            
            System.out.println("\n=== RESULTADOS ===");
            int successful = 0;
            for (ConversionResult result : results) {
                if (result.isSuccess()) {
                    System.out.println("✓ " + result.getUrl() + " -> " + new File(result.getOutputPath()).getName());
                    successful++;
                } else {
                    System.out.println("✗ " + result.getUrl() + " -> " + result.getError());
                }
            }
            System.out.printf("Exitosos: %d/%d%n", successful, results.size());
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    private void benchmark(String filename) {
        try {
            List<String> urls = Files.readAllLines(Paths.get(filename));
            urls.removeIf(String::isEmpty);
            urls = urls.subList(0, Math.min(32, urls.size()));
            
            System.out.println("Ejecutando benchmark con " + urls.size() + " URLs...");
            
            BenchmarkRunner runner = new BenchmarkRunner(); // No necesitas import!
            runner.runBenchmark(urls);
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    private void printUsage() {
        System.out.println("Uso:");
        System.out.println("  java WebToPdfConverter --cli convert <urls.txt> [threads]");
        System.out.println("  java WebToPdfConverter --cli benchmark <urls.txt>");
        System.out.println("  java WebToPdfConverter (GUI)");
        System.out.println();
        System.out.println("Ejemplos:");
        System.out.println("  java WebToPdfConverter --cli convert urls.txt 4");
        System.out.println("  java WebToPdfConverter --cli benchmark test_urls.txt");
    }
}