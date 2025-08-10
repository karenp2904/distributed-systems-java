import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Interfaz gráfica de usuario
 */
public class GUI extends JFrame {
    private JTextArea urlArea;
    private JSpinner threadSpinner;
    private JTextArea resultArea;
    private JProgressBar progressBar;
    
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
        urlArea.setText("https://www.google.com\nhttps://www.wikipedia.org\nhttps://www.github.com\nhttps://www.stackoverflow.com");
        urlArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        topPanel.add(new JScrollPane(urlArea));
        
        // Panel de control
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(new JLabel("Hilos:"));
        threadSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 32, 1));
        controlPanel.add(threadSpinner);
        
        JButton convertBtn = new JButton("🔄 Convertir");
        convertBtn.addActionListener(this::convert);
        controlPanel.add(convertBtn);
        
        JButton benchmarkBtn = new JButton("📊 Benchmark");
        benchmarkBtn.addActionListener(this::benchmark);
        controlPanel.add(benchmarkBtn);
        
        JButton clearBtn = new JButton("🗑️ Limpiar");
        clearBtn.addActionListener(e -> resultArea.setText(""));
        controlPanel.add(clearBtn);
        
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
        appendResult("Ingrese URLs y presione Convertir o Benchmark\n\n");
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
                
                PDFConverter converter = new PDFConverter(); // No necesitas import!
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
        if (urls.size() < 5) {
            JOptionPane.showMessageDialog(this, "Se necesitan al menos 5 URLs para benchmark");
            return;
        }

        // ✅ Creamos nueva lista que sí es final o efectivamente final
        List<String> finalUrls = urls.subList(0, Math.min(32, urls.size()));

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("📊 Iniciando benchmark con " + finalUrls.size() + " URLs...\n");
                publish("⚠️  Esto puede tardar varios minutos\n\n");
                progressBar.setIndeterminate(true);
                progressBar.setString("Ejecutando benchmark...");

                BenchmarkRunner runner = new BenchmarkRunner();
                runner.runBenchmark(finalUrls); // ✅ usamos finalUrls
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) appendResult(msg);
            }

            @Override
            protected void done() {
                publish("✅ Benchmark completado!\n");
                publish("📁 Revisar carpeta 'reports' para el informe detallado\n\n");
                progressBar.setIndeterminate(false);
                progressBar.setString("Benchmark terminado");

                JOptionPane.showMessageDialog(GUI.this,
                    "Benchmark completado!\nRevisar carpeta 'reports' para el informe.",
                    "Benchmark Completo", JOptionPane.INFORMATION_MESSAGE);
            }
        };

        worker.execute();
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