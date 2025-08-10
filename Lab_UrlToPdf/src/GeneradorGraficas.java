import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

public class GeneradorGraficas {

    public static void generarReporte(String reportFileName) {
        if (reportFileName == null) return;
        
        try {
            // Generar gráficas para el reporte individual
            generarGraficasIndividuales(reportFileName);
            
            // Generar gráficas comparativas entre todos los reportes
            generarGraficasComparativas();
            
        } catch (Exception e) {
            System.err.println("Error generando gráficas: " + e.getMessage());
        }
    }
    
    private static void generarGraficasIndividuales(String reportFileName) throws IOException {
        // Leer datos del reporte
        Map<Integer, Statistics> threadStats = parseReport(reportFileName);
        if (threadStats.isEmpty()) return;
        
        String baseName = Paths.get(reportFileName).getFileName().toString().replace(".txt", "");
        String outputDir = "reports/graficas/" + baseName;
        Files.createDirectories(Paths.get(outputDir));
        
        // Gráfica 1: Tiempo promedio vs Hilos
        generarGraficaTiempoVsHilos(threadStats, outputDir, baseName + "_tiempo_promedio.png");
        
        // Gráfica 2: Speedup vs Hilos
        generarGraficaSpeedup(threadStats, outputDir, baseName + "_speedup.png");
        
        // Gráfica 3: Eficiencia vs Hilos
        generarGraficaEficiencia(threadStats, outputDir, baseName + "_eficiencia.png");
        
        // Gráfica 4: Desviación estándar vs Hilos
        generarGraficaDesviacion(threadStats, outputDir, baseName + "_desviacion.png");
        
        System.out.println("Gráficas individuales generadas en: " + outputDir);
    }
    
    private static void generarGraficasComparativas() throws IOException {
        // Buscar todos los reportes
        List<String> reportes = buscarReportes();
        if (reportes.size() < 2) return;
        
        String outputDir = "reports/graficas/comparativas";
        Files.createDirectories(Paths.get(outputDir));
        
        Map<String, Map<Integer, Statistics>> allReports = new HashMap<>();
        
        for (String reporte : reportes) {
            Map<Integer, Statistics> stats = parseReport(reporte);
            String key = Paths.get(reporte).getFileName().toString().replace("benchmark_", "").replace(".txt", "");
            allReports.put(key, stats);
        }
        
        // Gráficas comparativas
        generarComparativaTiempos(allReports, outputDir, "comparativa_tiempos.png");
        generarComparativaSpeedup(allReports, outputDir, "comparativa_speedup.png");
        generarComparativaEficiencia(allReports, outputDir, "comparativa_eficiencia.png");
        
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
                    String[] parts = line.split(" ");
                    currentStats.avgTime = Double.parseDouble(parts[2]);
                } else if (line.startsWith("Tiempo mínimo: ")) {
                    currentStats.minTime = Long.parseLong(line.split(" ")[2]);
                } else if (line.startsWith("Tiempo máximo: ")) {
                    currentStats.maxTime = Long.parseLong(line.split(" ")[2]);
                } else if (line.startsWith("Desviación estándar: ")) {
                    String[] parts = line.split(" ");
                    currentStats.stdDev = Double.parseDouble(parts[2]);
                } else if (line.startsWith("Speedup: ")) {
                    String speedupStr = line.substring(9).replace("x", "");
                    currentStats.speedup = Double.parseDouble(speedupStr);
                } else if (line.startsWith("Eficiencia: ")) {
                    String effStr = line.substring(12).replace("%", "");
                    currentStats.efficiency = Double.parseDouble(effStr);
                } else if (line.isEmpty() && currentStats.avgTime > 0) {
                    stats.put(currentThreads, currentStats);
                    currentThreads = null;
                    currentStats = null;
                }
            }
        }
        
        // Agregar el último si no terminó con línea vacía
        if (currentThreads != null && currentStats != null && currentStats.avgTime > 0) {
            stats.put(currentThreads, currentStats);
        }
        
        return stats;
    }
    
    private static void generarGraficaTiempoVsHilos(Map<Integer, Statistics> stats, String outputDir, String fileName) throws IOException {
        int width = 800, height = 600;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        setupGraphics(g, width, height);
        
        // Título
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Tiempo Promedio vs Número de Hilos", 50, 30);
        
        // Preparar datos
        List<Integer> threads = new ArrayList<>(stats.keySet());
        Collections.sort(threads);
        
        double maxTime = stats.values().stream().mapToDouble(s -> s.avgTime).max().orElse(1000);
        int maxThreads = Collections.max(threads);
        
        // Área del gráfico
        int graphX = 80, graphY = 60, graphW = width - 150, graphH = height - 120;
        
        // Ejes
        g.drawLine(graphX, graphY + graphH, graphX + graphW, graphY + graphH); // X
        g.drawLine(graphX, graphY, graphX, graphY + graphH); // Y
        
        // Etiquetas de ejes
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Número de Hilos", graphX + graphW/2 - 50, height - 20);
        
        Graphics2D g2 = (Graphics2D) g.create();
        g2.rotate(-Math.PI/2);
        g2.drawString("Tiempo (ms)", -height/2 - 30, 20);
        g2.dispose();
        
        // Dibujar puntos y líneas
        g.setColor(Color.BLUE);
        g.setStroke(new BasicStroke(2));
        
        Point[] points = new Point[threads.size()];
        for (int i = 0; i < threads.size(); i++) {
            int thread = threads.get(i);
            double time = stats.get(thread).avgTime;
            
            int x = graphX + (int)((double)thread / maxThreads * graphW);
            int y = graphY + graphH - (int)(time / maxTime * graphH);
            
            points[i] = new Point(x, y);
            g.fillOval(x - 3, y - 3, 6, 6);
            
            // Etiqueta del punto
            g.setColor(Color.BLACK);
            g.drawString(String.format("%.1f", time), x + 5, y - 5);
            g.setColor(Color.BLUE);
        }
        
        // Conectar puntos
        for (int i = 0; i < points.length - 1; i++) {
            g.drawLine(points[i].x, points[i].y, points[i+1].x, points[i+1].y);
        }
        
        // Escala X
        g.setColor(Color.BLACK);
        for (int thread : threads) {
            int x = graphX + (int)((double)thread / maxThreads * graphW);
            g.drawLine(x, graphY + graphH, x, graphY + graphH + 5);
            g.drawString(String.valueOf(thread), x - 5, graphY + graphH + 18);
        }
        
        g.dispose();
        ImageIO.write(image, "PNG", new File(outputDir, fileName));
    }
    
    private static void generarGraficaSpeedup(Map<Integer, Statistics> stats, String outputDir, String fileName) throws IOException {
        int width = 800, height = 600;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        setupGraphics(g, width, height);
        
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Speedup vs Número de Hilos", 50, 30);
        
        List<Integer> threads = new ArrayList<>(stats.keySet());
        Collections.sort(threads);
        threads.remove(Integer.valueOf(1)); // Remover 1 hilo (speedup = 1)
        
        if (threads.isEmpty()) return;
        
        double maxSpeedup = stats.values().stream().mapToDouble(s -> s.speedup).max().orElse(16);
        int maxThreads = Collections.max(threads);
        
        int graphX = 80, graphY = 60, graphW = width - 150, graphH = height - 120;
        
        // Ejes
        g.drawLine(graphX, graphY + graphH, graphX + graphW, graphY + graphH);
        g.drawLine(graphX, graphY, graphX, graphY + graphH);
        
        // Línea ideal (speedup = threads)
        g.setColor(Color.LIGHT_GRAY);
        g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        int idealEndX = graphX + graphW;
        int idealEndY = graphY + graphH - (int)(maxThreads / maxSpeedup * graphH);
        g.drawLine(graphX, graphY + graphH, idealEndX, idealEndY);
        g.drawString("Speedup Ideal", idealEndX - 80, idealEndY - 5);
        
        // Datos reales
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(2));
        
        Point[] points = new Point[threads.size()];
        for (int i = 0; i < threads.size(); i++) {
            int thread = threads.get(i);
            double speedup = stats.get(thread).speedup;
            
            int x = graphX + (int)((double)thread / maxThreads * graphW);
            int y = graphY + graphH - (int)(speedup / maxSpeedup * graphH);
            
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
    
    private static void generarGraficaEficiencia(Map<Integer, Statistics> stats, String outputDir, String fileName) throws IOException {
        int width = 800, height = 600;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        setupGraphics(g, width, height);
        
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Eficiencia vs Número de Hilos", 50, 30);
        
        List<Integer> threads = new ArrayList<>(stats.keySet());
        Collections.sort(threads);
        threads.remove(Integer.valueOf(1));
        
        if (threads.isEmpty()) return;
        
        int maxThreads = Collections.max(threads);
        int graphX = 80, graphY = 60, graphW = width - 150, graphH = height - 120;
        
        // Ejes
        g.drawLine(graphX, graphY + graphH, graphX + graphW, graphY + graphH);
        g.drawLine(graphX, graphY, graphX, graphY + graphH);
        
        // Línea de 100% eficiencia
        g.setColor(Color.LIGHT_GRAY);
        g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        int perfectEffY = graphY;
        g.drawLine(graphX, perfectEffY, graphX + graphW, perfectEffY);
        g.drawString("100% Eficiencia", graphX + graphW - 100, perfectEffY - 5);
        
        // Datos
        g.setColor(Color.GREEN);
        g.setStroke(new BasicStroke(2));
        
        Point[] points = new Point[threads.size()];
        for (int i = 0; i < threads.size(); i++) {
            int thread = threads.get(i);
            double efficiency = stats.get(thread).efficiency;
            
            int x = graphX + (int)((double)thread / maxThreads * graphW);
            int y = graphY + graphH - (int)(efficiency / 100.0 * graphH);
            
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
    
    private static void generarGraficaDesviacion(Map<Integer, Statistics> stats, String outputDir, String fileName) throws IOException {
        int width = 800, height = 600;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        setupGraphics(g, width, height);
        
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Desviación Estándar vs Número de Hilos", 50, 30);
        
        List<Integer> threads = new ArrayList<>(stats.keySet());
        Collections.sort(threads);
        
        double maxStdDev = stats.values().stream().mapToDouble(s -> s.stdDev).max().orElse(100);
        int maxThreads = Collections.max(threads);
        
        int graphX = 80, graphY = 60, graphW = width - 150, graphH = height - 120;
        
        // Ejes
        g.drawLine(graphX, graphY + graphH, graphX + graphW, graphY + graphH);
        g.drawLine(graphX, graphY, graphX, graphY + graphH);
        
        // Barras
        int barWidth = graphW / threads.size();
        for (int i = 0; i < threads.size(); i++) {
            int thread = threads.get(i);
            double stdDev = stats.get(thread).stdDev;
            
            int x = graphX + i * barWidth;
            int barHeight = (int)(stdDev / maxStdDev * graphH);
            int y = graphY + graphH - barHeight;
            
            g.setColor(Color.ORANGE);
            g.fillRect(x + 5, y, barWidth - 10, barHeight);
            
            g.setColor(Color.BLACK);
            g.drawString(String.valueOf(thread), x + barWidth/2 - 5, graphY + graphH + 15);
            g.drawString(String.format("%.1f", stdDev), x + barWidth/2 - 10, y - 5);
        }
        
        g.dispose();
        ImageIO.write(image, "PNG", new File(outputDir, fileName));
    }
    
    private static void generarComparativaTiempos(Map<String, Map<Integer, Statistics>> allReports, 
                                                   String outputDir, String fileName) throws IOException {
        int width = 1000, height = 600;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        setupGraphics(g, width, height);
        
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Comparativa de Tiempos - Todos los Reportes", 50, 30);
        
        // Obtener todos los hilos únicos
        Set<Integer> allThreads = new TreeSet<>();
        for (Map<Integer, Statistics> report : allReports.values()) {
            allThreads.addAll(report.keySet());
        }
        
        // Encontrar máximo tiempo
        double maxTime = allReports.values().stream()
            .flatMap(report -> report.values().stream())
            .mapToDouble(s -> s.avgTime)
            .max().orElse(1000);
        
        int maxThreads = Collections.max(allThreads);
        int graphX = 100, graphY = 60, graphW = width - 200, graphH = height - 150;
        
        // Ejes
        g.drawLine(graphX, graphY + graphH, graphX + graphW, graphY + graphH);
        g.drawLine(graphX, graphY, graphX, graphY + graphH);
        
        // Colores para cada reporte
        Color[] colors = {Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN};
        int colorIndex = 0;
        
        // Leyenda
        int legendY = height - 100;
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        
        for (String reportName : allReports.keySet()) {
            Map<Integer, Statistics> stats = allReports.get(reportName);
            Color color = colors[colorIndex % colors.length];
            
            // Dibujar línea del reporte
            g.setColor(color);
            g.setStroke(new BasicStroke(2));
            
            List<Integer> threads = new ArrayList<>(stats.keySet());
            Collections.sort(threads);
            
            Point prevPoint = null;
            for (int thread : threads) {
                double time = stats.get(thread).avgTime;
                
                int x = graphX + (int)((double)thread / maxThreads * graphW);
                int y = graphY + graphH - (int)(time / maxTime * graphH);
                
                g.fillOval(x - 2, y - 2, 4, 4);
                
                if (prevPoint != null) {
                    g.drawLine(prevPoint.x, prevPoint.y, x, y);
                }
                prevPoint = new Point(x, y);
            }
            
            // Leyenda
            g.fillRect(graphX, legendY + colorIndex * 15, 10, 10);
            g.setColor(Color.BLACK);
            g.drawString(reportName, graphX + 15, legendY + colorIndex * 15 + 8);
            
            colorIndex++;
        }
        
        g.dispose();
        ImageIO.write(image, "PNG", new File(outputDir, fileName));
    }
    
    private static void generarComparativaSpeedup(Map<String, Map<Integer, Statistics>> allReports, 
                                                  String outputDir, String fileName) throws IOException {
        int width = 1000, height = 600;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        setupGraphics(g, width, height);
        
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Comparativa de Speedup - Todos los Reportes", 50, 30);
        
        Set<Integer> allThreads = new TreeSet<>();
        for (Map<Integer, Statistics> report : allReports.values()) {
            allThreads.addAll(report.keySet());
        }
        allThreads.remove(1); // Remover 1 hilo
        
        double maxSpeedup = allReports.values().stream()
            .flatMap(report -> report.values().stream())
            .mapToDouble(s -> s.speedup)
            .max().orElse(16);
        
        int maxThreads = Collections.max(allThreads);
        int graphX = 100, graphY = 60, graphW = width - 200, graphH = height - 150;
        
        g.drawLine(graphX, graphY + graphH, graphX + graphW, graphY + graphH);
        g.drawLine(graphX, graphY, graphX, graphY + graphH);
        
        Color[] colors = {Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN};
        int colorIndex = 0;
        int legendY = height - 100;
        
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
                
                int x = graphX + (int)((double)thread / maxThreads * graphW);
                int y = graphY + graphH - (int)(speedup / maxSpeedup * graphH);
                
                g.fillOval(x - 2, y - 2, 4, 4);
                
                if (prevPoint != null) {
                    g.drawLine(prevPoint.x, prevPoint.y, x, y);
                }
                prevPoint = new Point(x, y);
            }
            
            g.fillRect(graphX, legendY + colorIndex * 15, 10, 10);
            g.setColor(Color.BLACK);
            g.drawString(reportName, graphX + 15, legendY + colorIndex * 15 + 8);
            
            colorIndex++;
        }
        
        g.dispose();
        ImageIO.write(image, "PNG", new File(outputDir, fileName));
    }
    
    private static void generarComparativaEficiencia(Map<String, Map<Integer, Statistics>> allReports, 
                                                     String outputDir, String fileName) throws IOException {
        int width = 1000, height = 600;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        setupGraphics(g, width, height);
        
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Comparativa de Eficiencia - Todos los Reportes", 50, 30);
        
        Set<Integer> allThreads = new TreeSet<>();
        for (Map<Integer, Statistics> report : allReports.values()) {
            allThreads.addAll(report.keySet());
        }
        allThreads.remove(1);
        
        int maxThreads = Collections.max(allThreads);
        int graphX = 100, graphY = 60, graphW = width - 200, graphH = height - 150;
        
        g.drawLine(graphX, graphY + graphH, graphX + graphW, graphY + graphH);
        g.drawLine(graphX, graphY, graphX, graphY + graphH);
        
        Color[] colors = {Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN};
        int colorIndex = 0;
        int legendY = height - 100;
        
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
                double efficiency = stats.get(thread).efficiency;
                
                int x = graphX + (int)((double)thread / maxThreads * graphW);
                int y = graphY + graphH - (int)(efficiency / 100.0 * graphH);
                
                g.fillOval(x - 2, y - 2, 4, 4);
                
                if (prevPoint != null) {
                    g.drawLine(prevPoint.x, prevPoint.y, x, y);
                }
                prevPoint = new Point(x, y);
            }
            
            g.fillRect(graphX, legendY + colorIndex * 15, 10, 10);
            g.setColor(Color.BLACK);
            g.drawString(reportName, graphX + 15, legendY + colorIndex * 15 + 8);
            
            colorIndex++;
        }
        
        g.dispose();
        ImageIO.write(image, "PNG", new File(outputDir, fileName));
    }
    
    private static void setupGraphics(Graphics2D g, int width, int height) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLACK);
    }
    
    // Clase auxiliar para almacenar estadísticas
    private static class Statistics {
        double avgTime = 0;
        long minTime = 0;
        long maxTime = 0;
        double stdDev = 0;
        double speedup = 1;
        double efficiency = 100;
    }
}