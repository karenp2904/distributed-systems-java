import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;
import java.util.List;


public class GUI extends JFrame {
    private JTextArea urlArea;
    private JSpinner threadSpinner;
    private JTextArea resultArea;
    private JProgressBar progressBar;
    
    // Configuración de benchmark
    private JSpinner maxThreadsSpinner;
    private JSpinner maxUrlsSpinner;
    private JSpinner iterationsSpinner;
    
    public GUI() {
        initUI();
    }
    
    private void initUI() {
        setTitle("Convertidor Web a PDF - Laboratorio 6");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Panel superior - URLs
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("URLs (una por línea)"));
        
        urlArea = new JTextArea(8, 50);
        urlArea.setText("https://www.google.com\nhttps://www.wikipedia.org\nhttps://www.github.com\nhttps://www.stackoverflow.com\nhttps://www.oracle.com\nhttps://www.java.com");
        urlArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        topPanel.add(new JScrollPane(urlArea));
        
        // Panel de control principal
        JPanel controlPanel = new JPanel(new GridLayout(2, 1));
        
        // Primera fila - Conversión básica
        JPanel basicPanel = new JPanel(new FlowLayout());
        basicPanel.setBorder(BorderFactory.createTitledBorder("Conversión Básica"));
        
        basicPanel.add(new JLabel("Hilos:"));
        threadSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 32, 1));
        basicPanel.add(threadSpinner);
        
        JButton convertBtn = new JButton("🔄 Convertir");
        convertBtn.addActionListener(this::convert);
        basicPanel.add(convertBtn);
        
        JButton clearBtn = new JButton("🗑️ Limpiar");
        clearBtn.addActionListener(e -> resultArea.setText(""));
        basicPanel.add(clearBtn);
        
        // Segunda fila - Configuración de Benchmark
        JPanel benchmarkPanel = new JPanel(new FlowLayout());
        benchmarkPanel.setBorder(BorderFactory.createTitledBorder("Configuración de Benchmark"));
        
        benchmarkPanel.add(new JLabel("Máx Hilos:"));
        maxThreadsSpinner = new JSpinner(new SpinnerNumberModel(16, 1, 32, 1));
        benchmarkPanel.add(maxThreadsSpinner);
        
        benchmarkPanel.add(new JLabel("Máx URLs:"));
        maxUrlsSpinner = new JSpinner(new SpinnerNumberModel(6, 1, 50, 1));
        benchmarkPanel.add(maxUrlsSpinner);
        
        benchmarkPanel.add(new JLabel("Iteraciones:"));
        iterationsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        benchmarkPanel.add(iterationsSpinner);
        
        JButton benchmarkBtn = new JButton("📊 Benchmark");
        benchmarkBtn.addActionListener(this::benchmark);
        benchmarkPanel.add(benchmarkBtn);
        
        JButton graphsBtn = new JButton("📈 Ver Gráficas");
        graphsBtn.addActionListener(this::openGraphsFolder);
        benchmarkPanel.add(graphsBtn);
        
        controlPanel.add(basicPanel);
        controlPanel.add(benchmarkPanel);
        
        topPanel.add(controlPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);
        
        // Área de resultados
        resultArea = new JTextArea(15, 50);
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        resultArea.setBackground(Color.BLACK);
        resultArea.setForeground(Color.GREEN);
        add(new JScrollPane(resultArea), BorderLayout.CENTER);
        
        // Barra de progreso
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Listo");
        add(progressBar, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
        
        appendResult("=== Convertidor Web a PDF ===\n");
        appendResult("Configurar benchmark y presionar Convertir o Benchmark\n");
        appendResult("ℹ️  Para benchmark recomendado: 3-5 iteraciones, 6-10 URLs\n\n");
    }
    
    private void convert(ActionEvent e) {
        List<String> urls = parseUrls();
        if (urls.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese al menos una URL válida");
            return;
        }
        
        int threads = (Integer) threadSpinner.getValue();
        
        SwingWorker<List<ConversionResult>, String> worker = new SwingWorker<List<ConversionResult>, String>() {
            @Override
            protected List<ConversionResult> doInBackground() throws Exception {
                publish("🚀 Iniciando conversión de " + urls.size() + " URLs con " + threads + " hilos...\n");
                progressBar.setIndeterminate(true);
                progressBar.setString("Convirtiendo...");
                
                PDFConverter converter = new PDFConverter();
                return converter.convertUrls(urls, threads);
            }
            
            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    appendResult(msg);
                }
            }
            
            @Override
            protected void done() {
                try {
                    List<ConversionResult> results = get();
                    int successful = 0;
                    
                    publish("\n📋 === RESULTADOS ===\n");
                    for (ConversionResult result : results) {
                        if (result.isSuccess()) {
                            publish("✅ " + result.getUrl() + " -> " + new File(result.getOutputPath()).getName() + "\n");
                            successful++;
                        } else {
                            publish("❌ " + result.getUrl() + " -> " + result.getError() + "\n");
                        }
                    }
                    publish(String.format("\n🎯 Exitosos: %d/%d (%.1f%%)\n\n", 
                        successful, results.size(), (successful * 100.0 / results.size())));
                    
                } catch (Exception ex) {
                    publish("💥 Error: " + ex.getMessage() + "\n");
                }
                
                progressBar.setIndeterminate(false);
                progressBar.setString("Completado");
            }
        };
        
        worker.execute();
    }
    
    private void benchmark(ActionEvent e) {
        List<String> urls = parseUrls();
        int maxUrls = (Integer) maxUrlsSpinner.getValue();
        
        if (urls.size() < 3) {
            JOptionPane.showMessageDialog(this, "Se necesitan al menos 3 URLs para benchmark");
            return;
        }

        // Configuración del benchmark
        int maxThreads = (Integer) maxThreadsSpinner.getValue();
        int iterations = (Integer) iterationsSpinner.getValue();
        
        List<String> finalUrls = urls.subList(0, Math.min(maxUrls, urls.size()));

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("📊 Configuración del Benchmark:\n");
                publish("   • URLs: " + finalUrls.size() + "\n");
                publish("   • Hilos máximos: " + maxThreads + "\n");
                publish("   • Iteraciones por configuración: " + iterations + "\n");
                publish("   • Tiempo estimado: " + ((maxThreads * iterations * 30) / 60) + " minutos\n");
                publish("⚠️  Iniciando benchmark...\n\n");
                
                progressBar.setIndeterminate(true);
                progressBar.setString("Ejecutando benchmark...");

                BenchmarkRunner runner = new BenchmarkRunner();
                
                // Configurar el benchmark con los parámetros de la GUI
                int[] threadRange = new int[maxThreads];
                for (int i = 0; i < maxThreads; i++) {
                    threadRange[i] = i + 1;
                }
                
                runner.setThreadCounts(threadRange);
                runner.setMaxUrls(maxUrls);
                runner.setIterations(iterations);
                
                runner.runBenchmark(finalUrls);
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) appendResult(msg);
            }

            @Override
            protected void done() {
                publish("✅ Benchmark completado!\n");
                publish("📁 Archivos generados:\n");
                publish("   • reports/benchmark_XXXXXX.txt (reporte detallado)\n");
                publish("   • reports/benchmark_data_XXXXXX.csv (datos CSV)\n");
                publish("   • reports/graficas/ (gráficas PNG)\n\n");
                
                progressBar.setIndeterminate(false);
                progressBar.setString("Benchmark terminado");

                // Mostrar diálogo de finalización
                String message = String.format(
                    "Benchmark completado!\n\n" +
                    "Configuración utilizada:\n" +
                    "• %d URLs procesadas\n" +
                    "• %d configuraciones de hilos (1-%d)\n" +
                    "• %d iteraciones por configuración\n\n" +
                    "Revisar carpeta 'reports' para informes y gráficas.",
                    finalUrls.size(), maxThreads, maxThreads, iterations
                );
                
                JOptionPane.showMessageDialog(GUI.this, message, "Benchmark Completo", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        };

        worker.execute();
    }
    
    private void openGraphsFolder(ActionEvent e) {
        try {
            File reportsDir = new File("reports");
            if (reportsDir.exists()) {
                Desktop.getDesktop().open(reportsDir);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "La carpeta 'reports' no existe.\nEjecute un benchmark primero.", 
                    "Carpeta no encontrada", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            appendResult("❌ Error abriendo carpeta: " + ex.getMessage() + "\n");
            JOptionPane.showMessageDialog(this, 
                "No se pudo abrir la carpeta automáticamente.\nNavegue manualmente a la carpeta 'reports'.", 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private List<String> parseUrls() {
        String[] lines = urlArea.getText().split("\n");
        List<String> urls = new ArrayList<>();
        
        for (String line : lines) {
            String url = line.trim();
            if (!url.isEmpty() && (url.startsWith("http://") || url.startsWith("https://"))) {
                urls.add(url);
            }
        }
        
        return urls;
    }
    
    private void appendResult(String text) {
        SwingUtilities.invokeLater(() -> {
            resultArea.append(text);
            resultArea.setCaretPosition(resultArea.getDocument().getLength());
        });
    }
}