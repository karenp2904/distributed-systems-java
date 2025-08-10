import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PDFConverter {
    private final String chromePath;

    public PDFConverter() {
        this.chromePath = findChromeExecutable();
        if (this.chromePath == null) {
            throw new RuntimeException("No se pudo encontrar Google Chrome instalado.");
        }
    }

    public List<ConversionResult> convertUrls(List<String> urls, int threads) {
        List<ConversionResult> results = Collections.synchronizedList(new ArrayList<>());
        File outputDir = new File("output");
        outputDir.mkdirs();

        // Pool controlado a "threads" hilos
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (String url : urls) {
            executor.submit(() -> {
                String fileName = url.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";
                String outputPath = new File(outputDir, fileName).getAbsolutePath();

                List<String> command = new ArrayList<>();
                command.add(chromePath);
                command.add("--headless");
                command.add("--disable-gpu");
                command.add("--print-to-pdf=" + outputPath);
                command.add(url);

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);

                try {
                    Process process = pb.start();

                    // Leer salida de Chrome (opcional)
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[chrome] " + line);
                    }

                    int exitCode = process.waitFor();
                    if (exitCode == 0 && new File(outputPath).exists()) {
                        results.add(new ConversionResult(url, outputPath, null)); // ✅ éxito
                    } else {
                        results.add(new ConversionResult(url, null, "Chrome no generó el PDF (exitCode=" + exitCode + ")"));
                    }
                } catch (Exception e) {
                    results.add(new ConversionResult(url, null, e.getMessage())); // ❌ fallo
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.MINUTES); // esperar que todos terminen
        } catch (InterruptedException e) {
            System.err.println("Error esperando la finalización de hilos: " + e.getMessage());
        }

        return results;
    }

      

    private String findChromeExecutable() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            String[] winPaths = {
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
            };
            for (String path : winPaths) {
                if (new File(path).exists()) return path;
            }
        } else if (os.contains("mac")) {
            String path = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
            if (new File(path).exists()) return path;
        } else {
            // Linux
            String[] commands = { "google-chrome", "chrome", "chromium", "chromium-browser" };
            for (String cmd : commands) {
                try {
                    ProcessBuilder pb = new ProcessBuilder("which", cmd);
                    Process p = pb.start();
                    p.waitFor();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String path = reader.readLine();
                    if (path != null && !path.isEmpty()) return path;
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}
