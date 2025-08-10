import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

public class GeneradorGraficas {

    public static void generarReporte(String reportFileName) {
        if (reportFileName == null) return;
        
        try {
            generarGraficasIndividuales(reportFileName);
            generarGraficasComparativas();
        } catch (Exception e) {
            System.err.println("Error generando gráficas: " + e.getMessage());
        }
    }
    
    private static void generarGraficasIndividuales(String reportFileName) throws IOException {
        Map<Integer, Statistics> threadStats = parseReport(reportFileName);
        if (threadStats.isEmpty()) return;
        
        String baseName = Paths.get(reportFileName).getFileName().toString().replace(".txt", "");
        String outputDir = "reports/graficas/" + baseName;
        Files.createDirectories(Paths.get(outputDir));
        
        generarGraficaTiempoVsHilos(threadStats, outputDir, baseName + "_tiempo.png");
        generarGraficaSpeedup(threadStats, outputDir, baseName + "_speedup.png");
        generarGraficaEficiencia(threadStats, outputDir, baseName + "_eficiencia.png");
        
        System.out.println("Gráficas generadas en: " + outputDir);
    }
    
    private static void generarGraficasComparativas() throws IOException {
        List<String> reportes = buscarReportes();
        if (reportes.size() < 2) return;
        
        String outputDir = "reports/graficas/comparativas";
        Files.createDirectories(Paths.get(outputDir));
        
        Map<String, Map<Integer, Statistics>> allReports = new HashMap<>();
        
        for (String reporte : reportes) {
            Map<Integer, Statistics> stats = parseReport(reporte);
            String key = Paths.get(reporte).getFileName().toString()
                .replace("benchmark_", "").replace(".txt", "");
            allReports.put(key, stats);
        }
        
        generarComparativaTiempos(allReports, outputDir, "comparativa_tiempos.png");
        generarComparativaSpeedup(allReports, outputDir, "comparativa_speedup.png");
        
        System.out.println("Gráficas comparativas generadas en: " + outputDir);
    }
    
    private static List<String> buscarReportes() throws IOException {
        List<String> reportes = new ArrayList<>();
        Path reportsDir = Paths.get("reports");
        
        if (Files.exists(reportsDir)) {
            Files.walk(reportsDir)
                .filter(path -> path.toString().endsWith(".txt"))
                .filter(path -> path.getFileName().toString().startsWith("benchmark_"))
                .forEach(path -> reportes.add(path.toString()));
        }
        
        return reportes;
    }
    
    private static Map<Integer, Statistics> parseReport(String reportFileName) throws IOException {
        Map<Integer, Statistics> stats = new HashMap<>();
        List<String> lines = Files.readAllLines(Paths.get(reportFileName));
        
        Integer currentThreads = null;
        Statistics currentStats = null;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.startsWith("HILOS: ")) {
                currentThreads = Integer.parseInt(line.substring(7));
                currentStats = new Statistics();
            } else if (currentThreads != null && currentStats != null) {
                if (line.startsWith("Tiempo promedio: ")) {
                    // Parsear formato "2303,00" o "2303.00"
                    String timeStr = line.split(" ")[2].replace(",", ".");
                    currentStats.avgTime = Double.parseDouble(timeStr);
                } else if (line.startsWith("Desviación estándar: ")) {
                    String stdStr = line.split(" ")[2].replace(",", ".");
                    Double.parseDouble(stdStr);
                } else if (line.startsWith("Speedup: ")) {
                    String speedupStr = line.substring(9).replace("x", "").replace(",", ".");
                    currentStats.speedup = Double.parseDouble(speedupStr);
                } else if (line.startsWith("Eficiencia: ")) {
                    String effStr = line.substring(12).replace("%", "").replace(",", ".");
                    currentStats.efficiency = Double.parseDouble(effStr);
                } else if (line.isEmpty() && currentStats.avgTime > 0) {
                    stats.put(currentThreads, currentStats);
                    currentThreads = null;
                    currentStats = null;
                }
            }
        }
        
        // Agregar el último
        if (currentThreads != null && currentStats != null && currentStats.avgTime > 0) {
            stats.put(currentThreads, currentStats);
        }
        
        return stats;
    }
    
    private static void generarGraficaTiempoVsHilos(Map<Integer, Statistics> stats, 
                                                    String outputDir, String fileName) throws IOException {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        setupGraphics(g);
        
        // Título
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Tiempo Promedio vs Hilos", 50, 30);
        
        // Preparar datos
        List<Integer> threads = new ArrayList<>(stats.keySet());
        Collections.sort(threads);
        
        double maxTime = stats.values().stream().mapToDouble(s -> s.avgTime).max().orElse(1000);
        int maxThreads = Collections.max(threads);
        
        // Área del gráfico
        int gx = 80, gy = 60, gw = 650, gh = 450;
        
        // Ejes
        g.drawLine(gx, gy + gh, gx + gw, gy + gh); // X
        g.drawLine(gx, gy, gx, gy + gh); // Y
        
        // Etiquetas
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Hilos", gx + gw/2 - 20, 580);
        g.rotate(-Math.PI/2);
        g.drawString("Tiempo (ms)", -350, 20);
        g.rotate(Math.PI/2);
        
        // Datos
        g.setColor(Color.BLUE);
        g.setStroke(new BasicStroke(2));
        
        Point[] points = new Point[threads.size()];
        for (int i = 0; i < threads.size(); i++) {
            int thread = threads.get(i);
            double time = stats.get(thread).avgTime;
            
            int x = gx + (int)((double)thread / maxThreads * gw);
            int y = gy + gh - (int)(time / maxTime * gh);
            
            points[i] = new Point(x, y);
            g.fillOval(x - 3, y - 3, 6, 6);
            
            // Etiqueta
            g.setColor(Color.BLACK);
            g.drawString(String.format("%.0f", time), x + 5, y - 5);
            g.setColor(Color.BLUE);
        }
        
        // Conectar puntos
        for (int i = 0; i < points.length - 1; i++) {
            g.drawLine(points[i].x, points[i].y, points[i+1].x, points[i+1].y);
        }
        
        // Escala X
        g.setColor(Color.BLACK);
        for (int thread : threads) {
            int x = gx + (int)((double)thread / maxThreads * gw);
            g.drawLine(x, gy + gh, x, gy + gh + 5);
            g.drawString(String.valueOf(thread), x - 5, gy + gh + 18);
        }
        
        g.dispose();
        ImageIO.write(image, "PNG", new File(outputDir, fileName));
    }
    
    private static void generarGraficaSpeedup(Map<Integer, Statistics> stats, 
                                              String outputDir, String fileName) throws IOException {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        setupGraphics(g);
        
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Speedup vs Hilos", 50, 30);
        
        List<Integer> threads = new ArrayList<>(stats.keySet());
        Collections.sort(threads);
        threads.remove(Integer.valueOf(1)); // Remover 1 hilo
        
        if (threads.isEmpty()) return;
        
        double maxSpeedup = stats.values().stream().mapToDouble(s -> s.speedup).max().orElse(16);
        int maxThreads = Collections.max(threads);
        
        int gx = 80, gy = 60, gw = 650, gh = 450;
        
        // Ejes
        g.drawLine(gx, gy + gh, gx + gw, gy + gh);
        g.drawLine(gx, gy, gx, gy + gh);
        
        // Línea ideal
        g.setColor(Color.LIGHT_GRAY);
        g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        int idealEndX = gx + gw;
        int idealEndY = gy + gh - (int)(maxThreads / maxSpeedup * gh);
        g.drawLine(gx, gy + gh, idealEndX, idealEndY);
        g.drawString("Ideal", idealEndX - 40, idealEndY - 5);
        
        // Datos
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(2));
        
        Point[] points = new Point[threads.size()];
        for (int i = 0; i < threads.size(); i++) {
            int thread = threads.get(i);
            double speedup = stats.get(thread).speedup;
            
            int x = gx + (int)((double)thread / maxThreads * gw);
            int y = gy + gh - (int)(speedup / maxSpeedup * gh);
            
            points[i] = new Point(x, y);
            g.fillOval(x - 3, y - 3, 6, 6);
            
            g.setColor(Color.BLACK);
            g.drawString(String.format("%.1fx", speedup), x + 5, y - 5);
            g.setColor(Color.RED);
        }
        
        // Conectar puntos
        for (int i = 0; i < points.length - 1; i++) {
            g.drawLine(points[i].x, points[i].y, points[i+1].x, points[i+1].y);
        }
        
        g.dispose();
        ImageIO.write(image, "PNG", new File(outputDir, fileName));
    }
    
    private static void generarGraficaEficiencia(Map<Integer, Statistics> stats, 
                                                 String outputDir, String fileName) throws IOException {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        setupGraphics(g);
        
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Eficiencia vs Hilos", 50, 30);
        
        List<Integer> threads = new ArrayList<>(stats.keySet());
        Collections.sort(threads);
        threads.remove(Integer.valueOf(1));
        
        if (threads.isEmpty()) return;
        
        int maxThreads = Collections.max(threads);
        int gx = 80, gy = 60, gw = 650, gh = 450;
        
        // Ejes
        g.drawLine(gx, gy + gh, gx + gw, gy + gh);
        g.drawLine(gx, gy, gx, gy + gh);
        
        // Línea 100%
        g.setColor(Color.LIGHT_GRAY);
        g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        g.drawLine(gx, gy, gx + gw, gy);
        g.drawString("100%", gx + gw - 40, gy - 5);
        
        // Datos
        g.setColor(Color.GREEN);
        g.setStroke(new BasicStroke(2));
        
        Point[] points = new Point[threads.size()];
        for (int i = 0; i < threads.size(); i++) {
            int thread = threads.get(i);
            double efficiency = stats.get(thread).efficiency;
            
            int x = gx + (int)((double)thread / maxThreads * gw);
            int y = gy + gh - (int)(efficiency / 100.0 * gh);
            
            points[i] = new Point(x, y);
            g.fillOval(x - 3, y - 3, 6, 6);
            
            g.setColor(Color.BLACK);
            g.drawString(String.format("%.1f%%", efficiency), x + 5, y - 5);
            g.setColor(Color.GREEN);
        }
        
        // Conectar puntos
        for (int i = 0; i < points.length - 1; i++) {
            g.drawLine(points[i].x, points[i].y, points[i+1].x, points[i+1].y);
        }
        
        g.dispose();
        ImageIO.write(image, "PNG", new File(outputDir, fileName));
    }
    
    private static void generarComparativaTiempos(Map<String, Map<Integer, Statistics>> allReports, 
                                                  String outputDir, String fileName) throws IOException {
        BufferedImage image = new BufferedImage(1000, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        setupGraphics(g);
        
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Comparativa Tiempos", 50, 30);
        
        Set<Integer> allThreads = new TreeSet<>();
        for (Map<Integer, Statistics> report : allReports.values()) {
            allThreads.addAll(report.keySet());
        }
        
        double maxTime = allReports.values().stream()
            .flatMap(report -> report.values().stream())
            .mapToDouble(s -> s.avgTime)
            .max().orElse(1000);
        
        int maxThreads = Collections.max(allThreads);
        int gx = 100, gy = 60, gw = 750, gh = 400;
        
        // Ejes
        g.drawLine(gx, gy + gh, gx + gw, gy + gh);
        g.drawLine(gx, gy, gx, gy + gh);
        
        // Colores
        Color[] colors = {Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN};
        int colorIndex = 0;
        
        // Leyenda
        int legendY = gy + gh + 50;
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        
        for (String reportName : allReports.keySet()) {
            Map<Integer, Statistics> stats = allReports.get(reportName);
            Color color = colors[colorIndex % colors.length];
            
            g.setColor(color);
            g.setStroke(new BasicStroke(2));
            
            List<Integer> threads = new ArrayList<>(stats.keySet());
            Collections.sort(threads);
            
            Point prevPoint = null;
            for (int thread : threads) {
                double time = stats.get(thread).avgTime;
                
                int x = gx + (int)((double)thread / maxThreads * gw);
                int y = gy + gh - (int)(time / maxTime * gh);
                
                g.fillOval(x - 2, y - 2, 4, 4);
                
                if (prevPoint != null) {
                    g.drawLine(prevPoint.x, prevPoint.y, x, y);
                }
                prevPoint = new Point(x, y);
            }
            
            // Leyenda
            g.fillRect(gx, legendY + colorIndex * 15, 10, 10);
            g.setColor(Color.BLACK);
            g.drawString(reportName, gx + 15, legendY + colorIndex * 15 + 8);
            
            colorIndex++;
        }
        
        g.dispose();
        ImageIO.write(image, "PNG", new File(outputDir, fileName));
    }
    
    private static void generarComparativaSpeedup(Map<String, Map<Integer, Statistics>> allReports, 
                                                  String outputDir, String fileName) throws IOException {
        BufferedImage image = new BufferedImage(1000, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        setupGraphics(g);
        
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Comparativa Speedup", 50, 30);
        
        Set<Integer> allThreads = new TreeSet<>();
        for (Map<Integer, Statistics> report : allReports.values()) {
            allThreads.addAll(report.keySet());
        }
        allThreads.remove(1);
        
        double maxSpeedup = allReports.values().stream()
            .flatMap(report -> report.values().stream())
            .mapToDouble(s -> s.speedup)
            .max().orElse(16);
        
        int maxThreads = Collections.max(allThreads);
        int gx = 100, gy = 60, gw = 750, gh = 400;
        
        g.drawLine(gx, gy + gh, gx + gw, gy + gh);
        g.drawLine(gx, gy, gx, gy + gh);
        
        Color[] colors = {Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN};
        int colorIndex = 0;
        int legendY = gy + gh + 50;
        
        for (String reportName : allReports.keySet()) {
            Map<Integer, Statistics> stats = allReports.get(reportName);
            Color color = colors[colorIndex % colors.length];
            
            g.setColor(color);
            g.setStroke(new BasicStroke(2));
            
            List<Integer> threads = new ArrayList<>(stats.keySet());
            Collections.sort(threads);
            threads.remove(Integer.valueOf(1));
            
            Point prevPoint = null;
            for (int thread : threads) {
                double speedup = stats.get(thread).speedup;
                
                int x = gx + (int)((double)thread / maxThreads * gw);
                int y = gy + gh - (int)(speedup / maxSpeedup * gh);
                
                g.fillOval(x - 2, y - 2, 4, 4);
                
                if (prevPoint != null) {
                    g.drawLine(prevPoint.x, prevPoint.y, x, y);
                }
                prevPoint = new Point(x, y);
            }
            
            g.fillRect(gx, legendY + colorIndex * 15, 10, 10);
            g.setColor(Color.BLACK);
            g.drawString(reportName, gx + 15, legendY + colorIndex * 15 + 8);
            
            colorIndex++;
        }
        
        g.dispose();
        ImageIO.write(image, "PNG", new File(outputDir, fileName));
    }
    
    private static void setupGraphics(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, g.getClipBounds().width, g.getClipBounds().height);
        g.setColor(Color.BLACK);
    }
    
    private static class Statistics {
        double avgTime = 0;
        double speedup = 1;
        double efficiency = 100;
    }
}