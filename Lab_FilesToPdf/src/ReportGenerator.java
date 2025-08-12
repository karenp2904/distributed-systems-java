import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.category.*;

public class ReportGenerator {

    private static final DecimalFormat df = new DecimalFormat("#0.00");

    /**
     * allResults: Map<threads, totalTimeMs>
     * perFileTimes: optional Map<threads, List<Long>> if you captured per-file times
     */
    public void generate(Map<Integer, Long> allResults,
                         Map<Integer, List<Long>> perFileTimes,
                         List<String> files,
                         Map<String, String> systemProfile,
                         Path outputDir,
                         JFrame mainFrame,
                         JTextArea logArea) throws Exception {

        Files.createDirectories(outputDir);

        // 1) CSV
        Path csv = outputDir.resolve("reporte_benchmark.csv");
        try (BufferedWriter w = Files.newBufferedWriter(csv)) {
            w.write("Threads,TotalTimeMs,TotalTimeSec");
            w.newLine();
            for (Map.Entry<Integer, Long> e : allResults.entrySet()) {
                w.write(e.getKey() + "," + e.getValue() + "," + (e.getValue() / 1000.0));
                w.newLine();
            }
        }

        // 2) Markdown / texto completo
        Path md = outputDir.resolve("reporte_benchmark.md");
        try (BufferedWriter w = Files.newBufferedWriter(md)) {
            w.write("# Informe de Benchmark - Conversión (32 archivos)\n\n");
            w.write("**Fecha:** " + systemProfile.getOrDefault("Fecha", "") + "\n\n");
            w.write("## Perfil de hardware\n");
            for (Map.Entry<String, String> e : systemProfile.entrySet()) {
                if (e.getKey().equals("Fecha")) continue;
                w.write("- " + e.getKey() + ": " + e.getValue() + "\n");
            }
            w.write("\n## Configuración del benchmark\n");
            w.write("- Archivos procesados: " + files.size() + "\n");
            w.write("- Hilos probados: " + allResults.keySet() + "\n\n");

            w.write("## Tabla de resultados\n\n");
            w.write("| Hilos | Tiempo (ms) | Tiempo (s) |\n");
            w.write("|---:|---:|---:|\n");
            for (Map.Entry<Integer, Long> e : allResults.entrySet()) {
                w.write("| " + e.getKey() + " | " + e.getValue() + " | " + df.format(e.getValue() / 1000.0) + " |\n");
            }
            w.write("\n");

            if (perFileTimes != null && !perFileTimes.isEmpty()) {
                w.write("## Tiempos individuales (por hilo)\n");
                for (Map.Entry<Integer, List<Long>> e : perFileTimes.entrySet()) {
                    w.write("### " + e.getKey() + " hilos\n");
                    List<Long> l = e.getValue();
                    for (int i = 0; i < l.size(); i++) {
                        w.write("- File " + (i+1) + ": " + l.get(i) + " ms\n");
                    }
                    w.write("\n");
                }
            }

            // Análisis: encontrar mínimo promedio (en este caso totalTime)
            Optional<Map.Entry<Integer, Long>> best = allResults.entrySet().stream().min(Map.Entry.comparingByValue());
            if (best.isPresent()) {
                int bestThreads = best.get().getKey();
                long bestTime = best.get().getValue();
                long baseTime = allResults.getOrDefault(1, bestTime);
                double speedup = baseTime / (double) bestTime;
                double improvement = (baseTime - bestTime) / (double) baseTime * 100;
                w.write("## Análisis y conclusiones\n");
                w.write("- Configuración óptima: **" + bestThreads + " hilos**\n");
                w.write("- Tiempo óptimo: **" + bestTime + " ms**\n");
                w.write("- Speedup vs 1 hilo: **" + df.format(speedup) + "x**\n");
                w.write("- Mejora máxima: **" + df.format(improvement) + "%**\n\n");
            } else {
                w.write("No se pudo determinar configuración óptima.\n");
            }

            w.write("## Archivos analizados\n");
            for (int i = 0; i < files.size(); i++) {
                w.write((i+1) + ". " + files.get(i) + "\n");
            }
        }

        // 3) Generar gráfico con JFreeChart
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (Map.Entry<Integer, Long> e : allResults.entrySet()) {
                dataset.addValue(e.getValue(), "Tiempo (ms)", e.getKey());
            }
            JFreeChart chart = ChartFactory.createLineChart(
                    "Benchmark: hilos vs tiempo (ms)",
                    "Hilos",
                    "Tiempo (ms)",
                    dataset,
                    PlotOrientation.VERTICAL,
                    false, true, false
            );

            int w = 1000, h = 600;
            Path chartP = outputDir.resolve("benchmark_chart.png");
            ChartUtils.saveChartAsPNG(chartP.toFile(), chart, w, h);
        } catch (NoClassDefFoundError cnfe) {
            // No JFreeChart presente
            System.err.println("JFreeChart no encontrado: genera el CSV/MD y crea el gráfico manualmente si lo deseas.");
        }

        // 4) Capturas de pantalla: logArea y mainFrame
        if (logArea != null) {
            BufferedImage img = new BufferedImage(logArea.getWidth(), logArea.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            logArea.paint(g);
            g.dispose();
            ImageIO.write(img, "png", outputDir.resolve("log_capture.png").toFile());
        }
        if (mainFrame != null) {
            BufferedImage img = new BufferedImage(mainFrame.getWidth(), mainFrame.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            mainFrame.paint(g);
            g.dispose();
            ImageIO.write(img, "png", outputDir.resolve("frame_capture.png").toFile());
        }

    }
}
