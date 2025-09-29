import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.*;

public class ReportGenerator {

    private static final DecimalFormat df = new DecimalFormat("#0.00");
    private static final DecimalFormat dfPercent = new DecimalFormat("#0.0");

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

        // Ordenar resultados por número de hilos
        TreeMap<Integer, Long> sortedResults = new TreeMap<>(allResults);

        // 1) CSV expandido con más métricas
        Path csv = outputDir.resolve("reporte_benchmark.csv");
        try (BufferedWriter w = Files.newBufferedWriter(csv)) {
            w.write("Threads,TotalTimeMs,TotalTimeSec,Speedup,Efficiency,ImprovementPercent");
            w.newLine();
            
            Long baseTime = sortedResults.get(1); // Tiempo con 1 hilo como referencia
            if (baseTime == null) {
                baseTime = sortedResults.values().iterator().next();
            }
            
            for (Map.Entry<Integer, Long> e : sortedResults.entrySet()) {
                int threads = e.getKey();
                long timeMs = e.getValue();
                double timeSec = timeMs / 1000.0;
                double speedup = baseTime / (double) timeMs;
                double efficiency = speedup / threads * 100;
                double improvement = (baseTime - timeMs) / (double) baseTime * 100;
                
                w.write(threads + "," + timeMs + "," + df.format(timeSec) + "," + 
                       df.format(speedup) + "," + df.format(efficiency) + "," + 
                       df.format(improvement));
                w.newLine();
            }
        }

        // 2) Markdown mejorado con análisis detallado
        Path md = outputDir.resolve("reporte_benchmark.md");
        try (BufferedWriter w = Files.newBufferedWriter(md)) {
            w.write("# Informe de Benchmark - Análisis de Paralelización\n\n");
            w.write("**Fecha:** " + systemProfile.getOrDefault("Fecha", "") + "\n\n");
            
            w.write("## Perfil del Sistema\n");
            for (Map.Entry<String, String> e : systemProfile.entrySet()) {
                if (e.getKey().equals("Fecha")) continue;
                w.write("- **" + e.getKey() + ":** " + e.getValue() + "\n");
            }
            
            w.write("\n## Configuración del Benchmark\n");
            w.write("- **Archivos procesados:** " + files.size() + "\n");
            w.write("- **Configuraciones de hilos probadas:** " + sortedResults.keySet() + "\n");
            w.write("- **Rango de hilos:** " + Collections.min(sortedResults.keySet()) + 
                   " - " + Collections.max(sortedResults.keySet()) + "\n\n");

            // Tabla de resultados expandida
            w.write("## Resultados Detallados\n\n");
            w.write("| Hilos | Tiempo (ms) | Tiempo (s) | Speedup | Eficiencia (%) | Mejora (%) |\n");
            w.write("|------:|------------:|-----------:|--------:|---------------:|-----------:|\n");
            
            Long baseTime = sortedResults.get(1);
            if (baseTime == null) {
                baseTime = sortedResults.values().iterator().next();
            }
            
            for (Map.Entry<Integer, Long> e : sortedResults.entrySet()) {
                int threads = e.getKey();
                long timeMs = e.getValue();
                double speedup = baseTime / (double) timeMs;
                double efficiency = speedup / threads * 100;
                double improvement = (baseTime - timeMs) / (double) baseTime * 100;
                
                w.write("| " + threads + " | " + timeMs + " | " + df.format(timeMs / 1000.0) + 
                       " | " + df.format(speedup) + "x | " + dfPercent.format(efficiency) + 
                       " | " + dfPercent.format(improvement) + " |\n");
            }
            w.write("\n");

            // Análisis detallado
            Optional<Map.Entry<Integer, Long>> best = sortedResults.entrySet().stream()
                    .min(Map.Entry.comparingByValue());
            
            if (best.isPresent()) {
                int bestThreads = best.get().getKey();
                long bestTime = best.get().getValue();
                double bestSpeedup = baseTime / (double) bestTime;
                double bestEfficiency = bestSpeedup / bestThreads * 100;
                double maxImprovement = (baseTime - bestTime) / (double) baseTime * 100;
                
                w.write("## Análisis de Rendimiento\n\n");
                w.write("### Configuración Óptima\n");
                w.write("- **Número de hilos óptimo:** " + bestThreads + "\n");
                w.write("- **Tiempo mínimo:** " + bestTime + " ms (" + df.format(bestTime / 1000.0) + " s)\n");
                w.write("- **Speedup máximo:** " + df.format(bestSpeedup) + "x\n");
                w.write("- **Eficiencia óptima:** " + dfPercent.format(bestEfficiency) + "%\n");
                w.write("- **Mejora máxima:** " + dfPercent.format(maxImprovement) + "%\n\n");
                
                // Análisis de escalabilidad
                w.write("### Análisis de Escalabilidad\n");
                analyzeScalability(w, sortedResults, baseTime);
            }

            // Tiempos individuales si están disponibles
            if (perFileTimes != null && !perFileTimes.isEmpty()) {
                w.write("\n## Tiempos por Archivo\n");
                for (Map.Entry<Integer, List<Long>> e : perFileTimes.entrySet()) {
                    w.write("### Configuración: " + e.getKey() + " hilos\n");
                    List<Long> times = e.getValue();
                    for (int i = 0; i < Math.min(times.size(), files.size()); i++) {
                        w.write("- **" + files.get(i) + ":** " + times.get(i) + " ms\n");
                    }
                    w.write("\n");
                }
            }

            w.write("## Archivos Procesados\n");
            for (int i = 0; i < files.size(); i++) {
                w.write((i+1) + ". `" + files.get(i) + "`\n");
            }
        }

        // 3) Generar múltiples gráficos
        generateCharts(sortedResults, outputDir);

        // 4) Capturas de pantalla
        captureScreenshots(logArea, mainFrame, outputDir);
    }

    private void analyzeScalability(BufferedWriter w, TreeMap<Integer, Long> results, Long baseTime) 
            throws IOException {
        
        List<Integer> threadCounts = new ArrayList<>(results.keySet());
        
        // Encontrar el punto de rendimientos decrecientes
        Integer peakThreads = null;
        Double peakEfficiency = 0.0;
        
        for (Integer threads : threadCounts) {
            if (threads == 1) continue;
            double speedup = baseTime / (double) results.get(threads);
            double efficiency = speedup / threads * 100;
            
            if (efficiency > peakEfficiency) {
                peakEfficiency = efficiency;
                peakThreads = threads;
            }
        }
        
        if (peakThreads != null) {
            w.write("- **Punto de máxima eficiencia:** " + peakThreads + " hilos (" + 
                   dfPercent.format(peakEfficiency) + "% eficiencia)\n");
        }
        
        // Detectar saturación
        if (threadCounts.size() > 2) {
            int maxThreads = Collections.max(threadCounts);
            int secondMaxThreads = threadCounts.get(threadCounts.size() - 2);
            
            long maxTime = results.get(maxThreads);
            long secondMaxTime = results.get(secondMaxThreads);
            
            if (maxTime >= secondMaxTime) {
                w.write("- **⚠️ Saturación detectada:** El rendimiento no mejora o empeora con " + 
                       maxThreads + " hilos\n");
            }
        }
        
        w.write("\n");
    }

    private void generateCharts(TreeMap<Integer, Long> results, Path outputDir) {
        try {
            // Gráfico 1: Tiempo vs Hilos
            DefaultCategoryDataset timeDataset = new DefaultCategoryDataset();
            for (Map.Entry<Integer, Long> e : results.entrySet()) {
                timeDataset.addValue(e.getValue(), "Tiempo (ms)", e.getKey().toString());
            }
            
            JFreeChart timeChart = ChartFactory.createLineChart(
                    "Rendimiento: Hilos vs Tiempo de Ejecución",
                    "Número de Hilos",
                    "Tiempo (ms)",
                    timeDataset,
                    PlotOrientation.VERTICAL,
                    true, true, false
            );
            
            customizeChart(timeChart);
            ChartUtils.saveChartAsPNG(outputDir.resolve("tiempo_vs_hilos.png").toFile(), 
                                     timeChart, 1200, 800);

            // Gráfico 2: Speedup vs Hilos
            DefaultCategoryDataset speedupDataset = new DefaultCategoryDataset();
            Long baseTime = results.get(1);
            if (baseTime == null) baseTime = results.values().iterator().next();
            
            for (Map.Entry<Integer, Long> e : results.entrySet()) {
                double speedup = baseTime / (double) e.getValue();
                speedupDataset.addValue(speedup, "Speedup", e.getKey().toString());
                speedupDataset.addValue(e.getKey(), "Speedup Ideal", e.getKey().toString());
            }
            
            JFreeChart speedupChart = ChartFactory.createLineChart(
                    "Análisis de Speedup",
                    "Número de Hilos",
                    "Speedup (x)",
                    speedupDataset,
                    PlotOrientation.VERTICAL,
                    true, true, false
            );
            
            customizeChart(speedupChart);
            ChartUtils.saveChartAsPNG(outputDir.resolve("speedup_vs_hilos.png").toFile(), 
                                     speedupChart, 1200, 800);

        } catch (Exception e) {
            System.err.println("Error generando gráficos: " + e.getMessage());
        }
    }

    private void customizeChart(JFreeChart chart) {
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        LineAndShapeRenderer renderer = new LineAndShapeRenderer();
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShapesFilled(0, true);
        plot.setRenderer(renderer);
        
        // Personalizar colores
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.GRAY);
        plot.setDomainGridlinePaint(Color.GRAY);
    }

    private void captureScreenshots(JTextArea logArea, JFrame mainFrame, Path outputDir) {
        try {
            if (logArea != null) {
                BufferedImage img = new BufferedImage(
                    Math.max(logArea.getWidth(), 1), 
                    Math.max(logArea.getHeight(), 1), 
                    BufferedImage.TYPE_INT_RGB
                );
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                logArea.paint(g);
                g.dispose();
                ImageIO.write(img, "png", outputDir.resolve("log_capture.png").toFile());
            }
            
            if (mainFrame != null) {
                BufferedImage img = new BufferedImage(
                    Math.max(mainFrame.getWidth(), 1), 
                    Math.max(mainFrame.getHeight(), 1), 
                    BufferedImage.TYPE_INT_RGB
                );
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                mainFrame.paint(g);
                g.dispose();
                ImageIO.write(img, "png", outputDir.resolve("interfaz_capture.png").toFile());
            }
        } catch (Exception e) {
            System.err.println("Error capturando pantallas: " + e.getMessage());
        }
    }
}