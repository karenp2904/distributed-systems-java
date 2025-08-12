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
  

    private static final String[] EXTENSIONS = {"docx", "xlsx", "pptx", "png", "odt", "ods", "odp", "txt"};

    private JList<String> fileList;
    private DefaultListModel<String> listModel;
    private JTextField outputDirField;
    private JSpinner threadSpinner;
    private JTextArea logArea;
    private JProgressBar progressBar;
    private JButton startConversionBtn, convertBtn, perfTestBtn, reportBtn, auto32Btn;

    // para guardar resultados del último test
    private Map<Integer, Long> lastPerformanceResults = new LinkedHashMap<>();
        File inputDir = new File("C:\\Users\\PC01\\Downloads\\32");
// Obtiene el directorio del proyecto
File outputDir = new File(System.getProperty("user.dir"), "reporte");

    public WindowsPDFConverter() {
        setTitle("Windows PDF Converter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setupUI();
        pack();
        setLocationRelativeTo(null);
    }

    private void setupUI() {
        setLayout(new BorderLayout(8, 8));
        // Directorio por defecto dentro del proyecto
        String projectOutputDir = System.getProperty("user.dir") + File.separator + "output";
        try { Files.createDirectories(Paths.get(projectOutputDir)); } catch (IOException ex) { ex.printStackTrace(); }

        // Panel archivos
        JPanel filesPanel = new JPanel(new BorderLayout());
        filesPanel.setBorder(new TitledBorder("Archivos a Convertir"));

        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setPreferredSize(new Dimension(600, 180));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton addBtn = new JButton("Agregar Archivos"); addBtn.setForeground(Color.BLACK);
        JButton removeBtn = new JButton("Remover Seleccionados"); removeBtn.setForeground(Color.BLACK);
        JButton clearBtn = new JButton("Limpiar Todo"); clearBtn.setForeground(Color.BLACK);

        JButton directConvertBtn = new JButton("Seleccionar y Convertir a PDF");
        directConvertBtn.setBackground(new Color(173, 216, 230));
        directConvertBtn.setForeground(Color.BLACK);
        directConvertBtn.addActionListener(this::directConvertFile);

        addBtn.addActionListener(this::addFiles);
        removeBtn.addActionListener(this::removeSelected);
        clearBtn.addActionListener(e -> listModel.clear());

        buttonPanel.add(addBtn); buttonPanel.add(removeBtn); buttonPanel.add(clearBtn); buttonPanel.add(directConvertBtn);
        filesPanel.add(scrollPane, BorderLayout.CENTER);
        filesPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Config panel
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(new TitledBorder("Configuración"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        configPanel.add(new JLabel("Directorio Salida:"), gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        outputDirField = new JTextField(projectOutputDir);
        configPanel.add(outputDirField, gbc);

        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JButton browseBtn = new JButton("..."); browseBtn.setForeground(Color.BLACK);
        browseBtn.addActionListener(this::browseOutputDir);
        configPanel.add(browseBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        configPanel.add(new JLabel("Hilos:"), gbc);
        gbc.gridx = 1;
        threadSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 16, 1));
        configPanel.add(threadSpinner, gbc);

        // Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        startConversionBtn = new JButton("🚀 INICIAR CONVERSIÓN"); startConversionBtn.setForeground(Color.BLACK);
        startConversionBtn.addActionListener(this::convertFiles);

        convertBtn = new JButton("Convertir a PDF"); convertBtn.setForeground(Color.BLACK);
        convertBtn.addActionListener(this::convertFiles);

        perfTestBtn = new JButton("Test de Rendimiento (usar 32)"); perfTestBtn.setForeground(Color.BLACK);
        perfTestBtn.addActionListener(this::performanceTest);

        reportBtn = new JButton("Generar Reporte"); reportBtn.setForeground(Color.BLACK);
        reportBtn.addActionListener(e -> {
            if (lastPerformanceResults.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay resultados. Ejecuta el Test de Rendimiento primero.", "Sin datos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                // Dentro de WindowsPDFConverter

                JOptionPane.showMessageDialog(this, "Reporte generado en: " + outputDirField.getText(), "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error generando reporte:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        auto32Btn = new JButton("Auto 32 docs (crear)"); auto32Btn.setForeground(Color.BLACK);
        auto32Btn.addActionListener(e -> {
            File inputDir = new File("C:\\Users\\PC01\\Downloads\\32");
            File outputDir = new File(projectOutputDir);
            autoCreate32(inputDir, outputDir);
        });

        actionPanel.add(startConversionBtn);
        actionPanel.add(convertBtn);
        actionPanel.add(perfTestBtn);
        actionPanel.add(reportBtn);
        actionPanel.add(auto32Btn);

        // Bottom panel log + progress
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        logArea = new JTextArea(12, 80);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(new TitledBorder("Log"));

        JPanel center = new JPanel(new BorderLayout());
        center.add(configPanel, BorderLayout.NORTH);
        center.add(actionPanel, BorderLayout.SOUTH);

        add(filesPanel, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(progressBar, BorderLayout.NORTH);
        bottom.add(logScroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }


    // Agregar archivos manualmente
    private void addFiles(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Documentos Soportados", EXTENSIONS));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File f : chooser.getSelectedFiles()) {
                if (!listModel.contains(f.getAbsolutePath())) listModel.addElement(f.getAbsolutePath());
            }
        }
    }

    private void removeSelected(ActionEvent e) {
        int[] idx = fileList.getSelectedIndices();
        for (int i = idx.length -1; i >= 0; i--) listModel.remove(idx[i]);
    }

    private void browseOutputDir(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(new File(outputDirField.getText()));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

  // Método para procesar automáticamente 32 archivos PDF
    private void autoCreate32(File inputDir, File outputDir) {
        try {
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // Lista de archivos a procesar (pueden ser DOCX, XLSX, PPTX, etc.)
            File[] archivos = inputDir.listFiles((dir, name) -> 
                name.endsWith(".docx") || name.endsWith(".xlsx") || name.endsWith(".pptx")
            );

            if (archivos == null || archivos.length == 0) {
                publishLog("No se encontraron archivos para convertir.");
                return;
            }

            publishLog("Archivos encontrados: " + archivos.length);
            long startTime = System.currentTimeMillis();

            // Procesar en paralelo
            Arrays.stream(archivos).parallel().forEach(archivo -> {
                try {
                    publishLog("Convirtiendo: " + archivo.getName());

                    ProcessBuilder pb = new ProcessBuilder(
                        SOFFICE_PATH,
                        "--headless",
                        "--convert-to", "pdf",
                        "--outdir", outputDir.getAbsolutePath(),
                        archivo.getAbsolutePath() // ← aquí usamos el archivo real
                    );
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    process.waitFor();

                    publishLog("✅ Convertido: " + archivo.getName());
                } catch (Exception e) {
                    publishLog("❌ Error con " + archivo.getName() + ": " + e.getMessage());
                }
            });

            long endTime = System.currentTimeMillis();
            publishLog("Conversión finalizada. Tiempo total: " + (endTime - startTime) + " ms");

            
        } catch (Exception e) {
            publishLog("Error general: " + e.getMessage());
        }
    }



    private String LocalDateTimeNow() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    // Conversión directa con JFileChooser
    private void directConvertFile(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos soportados", EXTENSIONS));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            SwingWorker<Void, String> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    publishLog("Convirtiendo: " + selectedFile.getName());
                    progressBar.setIndeterminate(true);
                    try {
                        convertSingleFile(selectedFile.getAbsolutePath(), outputDir);
                        publishLog("Conversión completada: " + selectedFile.getName());
                        JOptionPane.showMessageDialog(WindowsPDFConverter.this, "Conversión completada:\n" + selectedFile.getName(), "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        publishLog("Error: " + ex.getMessage());
                        JOptionPane.showMessageDialog(WindowsPDFConverter.this, "Error al convertir:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    return null;
                }
                @Override protected void done() { progressBar.setIndeterminate(false); }
            };
            worker.execute();
        }
    }

    // Convertir lista de archivos
    private void convertFiles(ActionEvent e) {
        if (listModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay archivos seleccionados", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<String> files = Collections.list(listModel.elements());
        String outputDir = outputDirField.getText();
        int threads = (Integer) threadSpinner.getValue();

        SwingWorker<List<String>, String> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                publishLog("Iniciando conversión de " + files.size() + " archivos...");
                publishLog("Directorio: " + outputDir);
                publishLog("Hilos: " + threads);
                startConversionBtn.setEnabled(false); convertBtn.setEnabled(false);
                progressBar.setIndeterminate(true);
                return processFiles(files, outputDir, threads, msg -> publishLog(msg));
            }
            @Override protected void process(List<String> chunks) { chunks.forEach(this::publish); }
            @Override protected void done() {
                try {
                    List<String> results = get();
                    publishLog("Conversión finalizada. Convertidos: " + results.size());
                    JOptionPane.showMessageDialog(WindowsPDFConverter.this, "Conversión completada!\nArchivos: " + results.size());
                } catch (Exception ex) { publishLog("Error: " + ex.getMessage()); }
                finally { startConversionBtn.setEnabled(true); convertBtn.setEnabled(true); progressBar.setIndeterminate(false); }
            }
        };
        worker.execute();
    }

    // Test de rendimiento automático: requiere exactamente 32 archivos en la lista
    private void performanceTest(ActionEvent e) {
        if (listModel.size() != 32) {
            JOptionPane.showMessageDialog(this, "Se requieren exactamente 32 archivos para el test. Usa 'Auto 32 docs' o selecciona 32 archivos.", "Requisito", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> files = Collections.list(listModel.elements());
        String baseOutput = outputDirField.getText() + File.separator + "performance_test";

        SwingWorker<Map<Integer, Long>, String> worker = new SwingWorker<>() {
            @Override
            protected Map<Integer, Long> doInBackground() throws Exception {
                publishLog("=== INICIANDO TEST DE RENDIMIENTO ===");
                perfTestBtn.setEnabled(false); startConversionBtn.setEnabled(false); convertBtn.setEnabled(false);
                progressBar.setMaximum(16); progressBar.setValue(0);
                Map<Integer, Long> results = new LinkedHashMap<>();

                for (int t = 1; t <= 16; t++) {
                    publishLog("Probando con " + t + " hilos...");
                    progressBar.setValue(t - 1);
                    long start = System.currentTimeMillis();
                    processFiles(files, baseOutput + File.separator + "test_" + t, t, msg -> {}); // no log por cada archivo para no saturar
                    long time = System.currentTimeMillis() - start;
                    results.put(t, time);
                    publishLog(String.format("  %d hilos -> %d ms", t, time));

                    // limpieza de test dir
                    try {
                        Path p = Paths.get(baseOutput + File.separator + "test_" + t);
                        if (Files.exists(p)) {
                            Files.walk(p).sorted(Comparator.reverseOrder()).forEach(pp -> { try { Files.deleteIfExists(pp); } catch (Exception ex) {} });
                        }
                    } catch (Exception ex) { /* ignore */ }
                }
                progressBar.setValue(16);
                return results;
            }

            @Override protected void process(List<String> chunks) { chunks.forEach(this::publish); }
            @Override protected void done() {
                try {
                    Map<Integer, Long> results = get();
                    lastPerformanceResults = results;
                    // Al finalizar, generar automáticamente el reporte
                    ReportGenerator generator = new ReportGenerator(); 
                    generator.generateReport(results);
                    publishLog("Test finalizado y reporte generado en: " + outputDirField.getText());
                    JOptionPane.showMessageDialog(WindowsPDFConverter.this, "Test completado y reporte generado.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    publishLog("Error en test: " + ex.getMessage());
                    JOptionPane.showMessageDialog(WindowsPDFConverter.this, "Error en test:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    perfTestBtn.setEnabled(true); startConversionBtn.setEnabled(true); convertBtn.setEnabled(true);
                    progressBar.setValue(0);
                }
            }
        };

        worker.execute();
    }

    // Ejecuta la conversión en paralelo con ExecutorService y retorna archivos convertidos
    private List<String> processFiles(List<String> files, String outputDir, int threads, java.util.function.Consumer<String> logger) throws Exception {
        Files.createDirectories(Paths.get(outputDir));
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<CompletableFuture<String>> futures = files.stream()
                .map(file -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String result = convertSingleFile(file, outputDir);
                        logger.accept("✅ Convertido: " + Paths.get(file).getFileName());
                        return result;
                    } catch (Exception ex) {
                        logger.accept("❌ Error en " + Paths.get(file).getFileName() + ": " + ex.getMessage());
                        return null;
                    }
                }, executor))
                .collect(Collectors.toList());

        List<String> results = futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).collect(Collectors.toList());
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        return results;
    }

    // Usa soffice para convertir (o simula para .txt -> copia a .pdf si soffice no existe)
    private String convertSingleFile(String inputFile, String outputDir) throws Exception {
        Path inputPath = Paths.get(inputFile);
        String baseName = inputPath.getFileName().toString();
        int dot = baseName.lastIndexOf('.');
        if (dot > 0) baseName = baseName.substring(0, dot);

        // Si soffice existe lo llamamos; si no, simulamos creando un PDF simple (copia o texto)
        if (Files.exists(Paths.get(SOFFICE_PATH))) {
            ProcessBuilder pb = new ProcessBuilder(
                    SOFFICE_PATH, "--headless", "--convert-to", "pdf",
                    "--outdir", outputDir, inputPath.toString()
            );
            Process proc = pb.start();
            if (!proc.waitFor(60, TimeUnit.SECONDS)) {
                proc.destroyForcibly();
                throw new RuntimeException("Timeout convirtiendo: " + inputFile);
            }
            Path pdfPath = Paths.get(outputDir, baseName + ".pdf");
            if (!Files.exists(pdfPath)) {
                throw new RuntimeException("PDF no creado: " + pdfPath);
            }
            return pdfPath.toString();
        } else {
            // Simulación: si es .txt, crear PDF básico (texto) con PDFBox; si no .txt, crear empty.pdf
            Path pdfPath = Paths.get(outputDir, baseName + ".pdf");
            try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
                org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
                doc.addPage(page);

                try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                            new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.newLineAtOffset(50, 700);
                    cs.showText("Archivo original: " + inputPath.getFileName());
                    cs.endText();
                }

                doc.save(pdfPath.toFile());
            }
            return pdfPath.toString();
                    }
    }

    private void publishLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new WindowsPDFConverter().setVisible(true);
        });
    }
}
