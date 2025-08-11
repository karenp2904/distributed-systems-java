import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class WindowsPDFConverter extends JFrame {
    private static final String SOFFICE_PATH = "C:\\Program Files\\LibreOffice\\program\\soffice.exe";
    private static final String[] EXTENSIONS = {"docx", "xlsx", "pptx", "png", "odt", "ods", "odp"};
    
    private JList<String> fileList;
    private DefaultListModel<String> listModel;
    private JTextField outputDirField;
    private JSpinner threadSpinner;
    private JTextArea logArea;
    private JProgressBar progressBar;
    private JButton convertBtn, perfTestBtn;
    
    public WindowsPDFConverter() {
        setTitle("Windows PDF Converter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setupUI();
        pack();
        setLocationRelativeTo(null);
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        
        // Panel de archivos
        JPanel filesPanel = new JPanel(new BorderLayout());
        filesPanel.setBorder(new TitledBorder("Archivos a Convertir"));
        
        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setPreferredSize(new Dimension(500, 150));
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addBtn = new JButton("Agregar Archivos");
        JButton removeBtn = new JButton("Remover Seleccionados");
        JButton clearBtn = new JButton("Limpiar Todo");
        
        addBtn.addActionListener(this::addFiles);
        removeBtn.addActionListener(this::removeSelected);
        clearBtn.addActionListener(e -> listModel.clear());
        
        buttonPanel.add(addBtn);
        buttonPanel.add(removeBtn);
        buttonPanel.add(clearBtn);
        
        filesPanel.add(scrollPane, BorderLayout.CENTER);
        filesPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Panel de configuración
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(new TitledBorder("Configuración"));
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Directorio salida
        gbc.gridx = 0; gbc.gridy = 0;
        configPanel.add(new JLabel("Directorio Salida:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        outputDirField = new JTextField(System.getProperty("user.home") + "\\Desktop\\PDFs");
        configPanel.add(outputDirField, gbc);
        
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JButton browseBtn = new JButton("...");
        browseBtn.addActionListener(this::browseOutputDir);
        configPanel.add(browseBtn, gbc);
        
        // Hilos
        gbc.gridx = 0; gbc.gridy = 1;
        configPanel.add(new JLabel("Hilos:"), gbc);
        
        gbc.gridx = 1;
        threadSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 16, 1));
        configPanel.add(threadSpinner, gbc);
        
        // Botones principales
        JPanel actionPanel = new JPanel(new FlowLayout());
        convertBtn = new JButton("Convertir a PDF");
        perfTestBtn = new JButton("Test de Rendimiento (32 archivos)");
        
        convertBtn.addActionListener(this::convertFiles);
        perfTestBtn.addActionListener(this::performanceTest);
        
        actionPanel.add(convertBtn);
        actionPanel.add(perfTestBtn);
        
        // Barra de progreso
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        
        // Log area
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(new TitledBorder("Log"));
        
        add(filesPanel, BorderLayout.NORTH);
        add(configPanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(progressBar, BorderLayout.NORTH);
        bottomPanel.add(logScroll, BorderLayout.CENTER);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void addFiles(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Documentos Soportados", EXTENSIONS));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File file : chooser.getSelectedFiles()) {
                if (!listModel.contains(file.getAbsolutePath())) {
                    listModel.addElement(file.getAbsolutePath());
                }
            }
        }
    }
    
    private void removeSelected(ActionEvent e) {
        int[] indices = fileList.getSelectedIndices();
        for (int i = indices.length - 1; i >= 0; i--) {
            listModel.remove(indices[i]);
        }
    }
    
    private void browseOutputDir(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(new File(outputDirField.getText()));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void convertFiles(ActionEvent e) {
        if (listModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay archivos seleccionados");
            return;
        }
        
        List<String> files = Collections.list(listModel.elements());
        String outputDir = outputDirField.getText();
        int threads = (Integer) threadSpinner.getValue();
        
        SwingWorker<List<String>, String> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                publish("Iniciando conversión...");
                convertBtn.setEnabled(false);
                progressBar.setIndeterminate(true);
                
                return WindowsPDFConverter.this.processFiles(files, outputDir, threads, this::publish);
            }
            
            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    logArea.append(msg + "\n");
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                }
            }
            
            @Override
            protected void done() {
                try {
                    List<String> results = get();
                    publish("=== CONVERSIÓN COMPLETADA ===");
                    publish("Archivos convertidos: " + results.size());
                    results.forEach(this::publish);
                } catch (Exception ex) {
                    publish("Error: " + ex.getMessage());
                } finally {
                    convertBtn.setEnabled(true);
                    progressBar.setIndeterminate(false);
                }
            }
        };
        
        worker.execute();
    }
    
    private void performanceTest(ActionEvent e) {
        if (listModel.size() != 32) {
            JOptionPane.showMessageDialog(this, "Se requieren exactamente 32 archivos para el test");
            return;
        }
        
        List<String> files = Collections.list(listModel.elements());
        String outputDir = outputDirField.getText() + "\\performance_test";
        
        SwingWorker<Map<Integer, Long>, String> worker = new SwingWorker<>() {
            @Override
            protected Map<Integer, Long> doInBackground() throws Exception {
                publish("=== INICIANDO TEST DE RENDIMIENTO ===");
                perfTestBtn.setEnabled(false);
                progressBar.setMaximum(16);
                progressBar.setValue(0);
                
                Map<Integer, Long> results = new LinkedHashMap<>();
                
                for (int t = 1; t <= 16; t++) {
                    publish("Probando con " + t + " hilos...");
                    progressBar.setValue(t - 1);
                    
                    long start = System.currentTimeMillis();
                    processFiles(files, outputDir + "\\test_" + t, t, msg -> {});
                    long time = System.currentTimeMillis() - start;
                    
                    results.put(t, time);
                    publish(String.format("  %d hilos: %d ms", t, time));
                    
                    // Limpieza
                    try {
                        Files.walk(Paths.get(outputDir + "\\test_" + t))
                            .sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try { Files.deleteIfExists(p); } catch (Exception ex) {}
                            });
                    } catch (Exception ex) {}
                }
                
                progressBar.setValue(16);
                return results;
            }
            
            @Override
            protected void process(List<String> chunks) {
                chunks.forEach(msg -> {
                    logArea.append(msg + "\n");
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
            }
            
            @Override
            protected void done() {
                try {
                    Map<Integer, Long> results = get();
                    generateReport(results);
                } catch (Exception ex) {
                    publish("Error en test: " + ex.getMessage());
                } finally {
                    perfTestBtn.setEnabled(true);
                    progressBar.setValue(0);
                }
            }
        };
        
        worker.execute();
    }
    
    private List<String> processFiles(List<String> files, String outputDir, int threads, 
                                     java.util.function.Consumer<String> logger) throws Exception {
        Files.createDirectories(Paths.get(outputDir));
        
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<CompletableFuture<String>> futures = files.stream()
            .map(file -> CompletableFuture.supplyAsync(() -> {
                try {
                    return convertSingleFile(file, outputDir);
                } catch (Exception e) {
                    logger.accept("Error en " + Paths.get(file).getFileName() + ": " + e.getMessage());
                    return null;
                }
            }, executor))
            .collect(Collectors.toList());
        
        List<String> results = futures.stream()
            .map(CompletableFuture::join)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        
        return results;
    }
    
    private String convertSingleFile(String inputFile, String outputDir) throws Exception {
        Path inputPath = Paths.get(inputFile);
        String baseName = inputPath.getFileName().toString();
        int dot = baseName.lastIndexOf('.');
        if (dot > 0) baseName = baseName.substring(0, dot);
        
        ProcessBuilder pb = new ProcessBuilder(
            SOFFICE_PATH, "--headless", "--convert-to", "pdf",
            "--outdir", outputDir, inputPath.toString()
        );
        
        Process proc = pb.start();
        if (!proc.waitFor(30, TimeUnit.SECONDS)) {
            proc.destroyForcibly();
            throw new RuntimeException("Timeout convirtiendo: " + inputFile);
        }
        
        Path pdfPath = Paths.get(outputDir, baseName + ".pdf");
        if (!Files.exists(pdfPath)) {
            throw new RuntimeException("PDF no creado: " + pdfPath);
        }
        
        return pdfPath.toString();
    }
    
    private void generateReport(Map<Integer, Long> results) {
        logArea.append("\n" + "=".repeat(60) + "\n");
        logArea.append("REPORTE DE RENDIMIENTO\n");
        logArea.append("=".repeat(60) + "\n");
        logArea.append(String.format("%-6s | %-10s | %-8s\n", "Hilos", "Tiempo(ms)", "Mejora"));
        logArea.append("-".repeat(30) + "\n");
        
        long baseline = results.get(1);
        int optimal = results.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(1);
        
        for (Map.Entry<Integer, Long> entry : results.entrySet()) {
            int threads = entry.getKey();
            long time = entry.getValue();
            double improvement = (double) baseline / time;
            
            String line = String.format("%-6d | %-10d | %.2fx", threads, time, improvement);
            if (threads == optimal) line += " (ÓPTIMO)";
            
            logArea.append(line + "\n");
        }
        
        logArea.append("\n=== CONCLUSIONES ===\n");
        logArea.append("Configuración óptima: " + optimal + " hilos\n");
        logArea.append(String.format("Mejora máxima: %.2fx\n", (double) baseline / results.get(optimal)));
        
        // Guardar CSV
        try {
            Path csvFile = Paths.get(outputDirField.getText(), "performance_report.csv");
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(csvFile))) {
                pw.println("Hilos,Tiempo_ms,Mejora");
                results.forEach((t, time) -> 
                    pw.printf("%d,%d,%.2f\n", t, time, (double) baseline / time));
            }
            logArea.append("Reporte guardado en: " + csvFile + "\n");
        } catch (Exception e) {
            logArea.append("Error guardando reporte: " + e.getMessage() + "\n");
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getLookAndFeel());
            } catch (Exception e) {
                // Use default look and feel
            }
            new WindowsPDFConverter().setVisible(true);
        });
    }
}