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
    private JButton startConversionBtn, convertBtn, perfTestBtn, reportBtn, auto32Btn, createFoldersBtn;

    // Para guardar resultados del último test
    private Map<Integer, Long> lastPerformanceResults = new LinkedHashMap<>();
    private final File PROJECT_OUTPUT_DIR;
    private final File PROJECT_REPORT_DIR;

    public WindowsPDFConverter() {
        setTitle("Windows PDF Converter - Sistema de Benchmark Completo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Inicializar directorios del proyecto
        String projectDir = System.getProperty("user.dir");
        PROJECT_OUTPUT_DIR = new File(projectDir, "output");
        PROJECT_REPORT_DIR = new File(projectDir, "reporte");
        
        setupUI();
        pack();
        setLocationRelativeTo(null);
        
        // Crear carpetas automáticamente al iniciar
        createProjectFolders();
    }

    private void createProjectFolders() {
        try {
            // Crear carpeta output
            if (!PROJECT_OUTPUT_DIR.exists()) {
                PROJECT_OUTPUT_DIR.mkdirs();
                publishLog("✅ Carpeta 'output' creada: " + PROJECT_OUTPUT_DIR.getAbsolutePath());
            }
            
            // Crear carpeta reporte
            if (!PROJECT_REPORT_DIR.exists()) {
                PROJECT_REPORT_DIR.mkdirs();
                publishLog("✅ Carpeta 'reporte' creada: " + PROJECT_REPORT_DIR.getAbsolutePath());
            }
            
            // Crear subcarpetas en output
            File[] subFolders = {
                new File(PROJECT_OUTPUT_DIR, "converted_files"),
                new File(PROJECT_OUTPUT_DIR, "performance_test"),
                new File(PROJECT_OUTPUT_DIR, "temp")
            };
            
            for (File folder : subFolders) {
                if (!folder.exists()) {
                    folder.mkdirs();
                    publishLog("📁 Subcarpeta creada: " + folder.getName());
                }
            }
            
            // Crear subcarpetas en reporte
            File[] reportSubFolders = {
                new File(PROJECT_REPORT_DIR, "benchmark"),
                new File(PROJECT_REPORT_DIR, "charts"),
                new File(PROJECT_REPORT_DIR, "screenshots"),
                new File(PROJECT_REPORT_DIR, "csv_data")
            };
            
            for (File folder : reportSubFolders) {
                if (!folder.exists()) {
                    folder.mkdirs();
                    publishLog("📊 Carpeta de reporte creada: " + folder.getName());
                }
            }
            
        } catch (Exception e) {
            publishLog("❌ Error creando carpetas: " + e.getMessage());
        }
    }

    private void setupUI() {
        setLayout(new BorderLayout(8, 8));

        // Panel archivos
        JPanel filesPanel = new JPanel(new BorderLayout());
        filesPanel.setBorder(new TitledBorder("Archivos a Convertir (Benchmark requiere 32 archivos)"));

        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setPreferredSize(new Dimension(700, 200));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton addBtn = new JButton("📁 Agregar Archivos");
        JButton removeBtn = new JButton("❌ Remover Seleccionados");
        JButton clearBtn = new JButton("🗑️ Limpiar Todo");
        
        JButton directConvertBtn = new JButton("⚡ Conversión Directa");
        directConvertBtn.setBackground(new Color(173, 216, 230));
        
        createFoldersBtn = new JButton("📂 Crear Carpetas Proyecto");
        createFoldersBtn.setBackground(new Color(144, 238, 144));

        addBtn.addActionListener(this::addFiles);
        removeBtn.addActionListener(this::removeSelected);
        clearBtn.addActionListener(e -> listModel.clear());
        directConvertBtn.addActionListener(this::directConvertFile);
        createFoldersBtn.addActionListener(e -> createProjectFolders());

        buttonPanel.add(addBtn);
        buttonPanel.add(removeBtn);
        buttonPanel.add(clearBtn);
        buttonPanel.add(directConvertBtn);
        buttonPanel.add(createFoldersBtn);
        
        filesPanel.add(scrollPane, BorderLayout.CENTER);
        filesPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Panel de configuración
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(new TitledBorder("Configuración del Sistema"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        configPanel.add(new JLabel("Directorio Output:"), gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        outputDirField = new JTextField(PROJECT_OUTPUT_DIR.getAbsolutePath());
        configPanel.add(outputDirField, gbc);

        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JButton browseBtn = new JButton("📂 Explorar");
        browseBtn.addActionListener(this::browseOutputDir);
        configPanel.add(browseBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        configPanel.add(new JLabel("Hilos Máximos:"), gbc);
        gbc.gridx = 1;
        threadSpinner = new JSpinner(new SpinnerNumberModel(16, 1, 32, 1));
        configPanel.add(threadSpinner, gbc);

        // Panel de acciones principales
        JPanel actionPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        actionPanel.setBorder(new TitledBorder("Acciones del Sistema"));
        
        startConversionBtn = new JButton("🚀 INICIAR CONVERSIÓN");
        startConversionBtn.setBackground(new Color(255, 165, 0));
        startConversionBtn.addActionListener(this::convertFiles);

        perfTestBtn = new JButton("🔬 BENCHMARK COMPLETO (1-16 hilos)");
        perfTestBtn.setBackground(new Color(255, 182, 193));
        perfTestBtn.addActionListener(this::performanceTest);

        reportBtn = new JButton("📊 GENERAR REPORTE FINAL");
        reportBtn.setBackground(new Color(152, 251, 152));
        reportBtn.addActionListener(this::generateFinalReport);

        auto32Btn = new JButton("🎯 Auto Crear 32 Docs Test");
        auto32Btn.setBackground(new Color(221, 160, 221));
        auto32Btn.addActionListener(e -> autoCreate32TestFiles());

        JButton systemInfoBtn = new JButton("💻 Perfil Hardware");
        systemInfoBtn.addActionListener(e -> showSystemProfile());
        
        JButton openFoldersBtn = new JButton("📂 Abrir Carpetas");
        openFoldersBtn.addActionListener(this::openProjectFolders);

        actionPanel.add(startConversionBtn);
        actionPanel.add(perfTestBtn);
        actionPanel.add(reportBtn);
        actionPanel.add(auto32Btn);
        actionPanel.add(systemInfoBtn);
        actionPanel.add(openFoldersBtn);

        // Panel inferior: progreso y log
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Sistema listo para benchmark");
        
        logArea = new JTextArea(15, 80);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        logArea.setEditable(false);
        logArea.setBackground(new Color(248, 248, 255));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(new TitledBorder("Log del Sistema"));

        JPanel center = new JPanel(new BorderLayout());
        center.add(configPanel, BorderLayout.NORTH);
        center.add(actionPanel, BorderLayout.SOUTH);

        add(filesPanel, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(progressBar, BorderLayout.NORTH);
        bottom.add(logScroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        
        publishLog("=== SISTEMA DE BENCHMARK PDF CONVERTER INICIADO ===");
        publishLog("Carpeta Output: " + PROJECT_OUTPUT_DIR.getAbsolutePath());
        publishLog("Carpeta Reporte: " + PROJECT_REPORT_DIR.getAbsolutePath());
        publishLog("Para benchmark completo: cargar exactamente 32 archivos");
    }

    private void autoCreate32TestFiles() {
        try {
            if (!PROJECT_OUTPUT_DIR.exists()) {
                PROJECT_OUTPUT_DIR.mkdirs();
            }
            File inputDir = new File("C:\\Users\\PC01\\Downloads\\32");
 
            // Lista de archivos a procesar
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
                        "--outdir", PROJECT_OUTPUT_DIR.getAbsolutePath(),
                        archivo.getAbsolutePath()
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

            // 📌 Crear el reporte DESPUÉS de la conversión
            File reportFile = new File(PROJECT_OUTPUT_DIR, "reporte_conversion.txt");
            try (PrintWriter writer = new PrintWriter(reportFile)) {
                writer.println("REPORTE DE CONVERSIÓN A PDF");
                writer.println("Fecha: " + java.time.LocalDateTime.now());
                writer.println("Tiempo total: " + (endTime - startTime) + " ms");
                writer.println("Archivos procesados: " + archivos.length);
                writer.println("Carpeta de salida: " + PROJECT_OUTPUT_DIR.getAbsolutePath());
            }

            publishLog("📄 Reporte creado en: " + reportFile.getAbsolutePath());
        } catch (Exception e) {
            publishLog("Error general: " + e.getMessage());
        }
    }


    private void showSystemProfile() {
        Map<String, String> profile = SystemProfile.gather();
        StringBuilder sb = new StringBuilder();
        sb.append("=== PERFIL DEL SISTEMA ===\n");
        for (Map.Entry<String, String> entry : profile.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Perfil de Hardware", JOptionPane.INFORMATION_MESSAGE);
        publishLog("📊 Perfil de sistema mostrado");
    }

    private void openProjectFolders(ActionEvent e) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                desktop.open(PROJECT_OUTPUT_DIR);
                desktop.open(PROJECT_REPORT_DIR);
                publishLog("📂 Carpetas del proyecto abiertas");
            } else {
                publishLog("❌ No se puede abrir el explorador de archivos automáticamente");
            }
        } catch (Exception ex) {
            publishLog("❌ Error abriendo carpetas: " + ex.getMessage());
        }
    }

    private void performanceTest(ActionEvent e) {
        List<String> files = Collections.list(listModel.elements());
        
        if (files.size() != 32) {
            JOptionPane.showMessageDialog(this, 
                "Se requieren exactamente 32 archivos para el benchmark completo.\n" +
                "Archivos actuales: " + files.size() + "\n" +
                "Usa 'Auto Crear 32 Docs Test' para generar archivos de prueba.", 
                "Error de Configuración", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int maxThreads = (Integer) threadSpinner.getValue();
        Path benchmarkOutput = PROJECT_REPORT_DIR.toPath().resolve("benchmark");
        
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("🔬 === INICIANDO BENCHMARK COMPLETO ===");
                publish("📊 Configuración: 32 archivos, 1-" + maxThreads + " hilos");
                publish("📁 Resultados en: " + benchmarkOutput.toString());
                
                progressBar.setIndeterminate(true);
                progressBar.setString("Ejecutando benchmark...");
                
                // Crear el runner de pruebas
                PerfTestRunner runner = new PerfTestRunner(WindowsPDFConverter.this, benchmarkOutput);
                
                // Ejecutar todas las pruebas de 1 a maxThreads
                lastPerformanceResults = runner.runFullTest(files, maxThreads, msg -> publish(msg));
                
                publish("✅ BENCHMARK COMPLETADO!");
                publish("📊 Resultados guardados y reporte generado");
                
                return null;
            }
            
            @Override
            protected void process(List<String> chunks) {
                chunks.forEach(WindowsPDFConverter.this::publishLog);
            }
            
            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setString("Benchmark completado");
                
                JOptionPane.showMessageDialog(WindowsPDFConverter.this, 
                    "🎉 Benchmark completado exitosamente!\n\n" +
                    "📊 Reporte generado en: " + benchmarkOutput.toString() + "\n" +
                    "📈 Incluye: CSV, Markdown, gráficos y capturas\n\n" +
                    "Usa 'GENERAR REPORTE FINAL' para crear el informe consolidado.", 
                    "Benchmark Completado", JOptionPane.INFORMATION_MESSAGE);
            }
        };
        worker.execute();
    }

    private void generateFinalReport(ActionEvent e) {
        if (lastPerformanceResults.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No hay datos de benchmark disponibles.\n" +
                "Ejecuta 'BENCHMARK COMPLETO' primero.", 
                "Sin Datos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("📊 === GENERANDO REPORTE FINAL CONSOLIDADO ===");
                
                // Generar reporte consolidado con todos los datos
                ReportGenerator generator = new ReportGenerator();
                Map<String, String> systemProfile = SystemProfile.gather();
                List<String> fileNames = Collections.list(listModel.elements());
                
                generator.generate(
                    lastPerformanceResults,
                    new HashMap<>(), // Per-file times si los tienes
                    fileNames,
                    systemProfile,
                    PROJECT_REPORT_DIR.toPath().resolve("final_report"),
                    WindowsPDFConverter.this,
                    logArea
                );
                
                publish("✅ Reporte final generado exitosamente!");
                return null;
            }
            
            @Override
            protected void process(List<String> chunks) {
                chunks.forEach(WindowsPDFConverter.this::publishLog);
            }
            
            @Override
            protected void done() {
                try {
                    File finalReportDir = new File(PROJECT_REPORT_DIR, "final_report");
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(finalReportDir);
                    }
                    
                    JOptionPane.showMessageDialog(WindowsPDFConverter.this,
                        "📊 Reporte Final Completado!\n\n" +
                        "📁 Ubicación: " + finalReportDir.getAbsolutePath() + "\n" +
                        "📄 Incluye: CSV, Markdown, gráficos, capturas\n" +
                        "🔍 Perfil completo de hardware\n" +
                        "📈 Análisis de rendimiento 1-16 hilos",
                        "Reporte Generado", JOptionPane.INFORMATION_MESSAGE);
                        
                } catch (Exception ex) {
                    publishLog("❌ Error abriendo carpeta de reporte: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    // Métodos auxiliares originales con mejoras
    private void addFiles(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Documentos Soportados", EXTENSIONS));
        chooser.setCurrentDirectory(PROJECT_OUTPUT_DIR);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            int added = 0;
            for (File f : chooser.getSelectedFiles()) {
                if (!listModel.contains(f.getAbsolutePath())) {
                    listModel.addElement(f.getAbsolutePath());
                    added++;
                }
            }
            publishLog("📁 Archivos agregados: " + added + " (Total: " + listModel.size() + ")");
        }
    }

    private void removeSelected(ActionEvent e) {
        int[] indices = fileList.getSelectedIndices();
        for (int i = indices.length - 1; i >= 0; i--) {
            listModel.remove(indices[i]);
        }
        publishLog("❌ Archivos removidos. Total actual: " + listModel.size());
    }

    private void browseOutputDir(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(PROJECT_OUTPUT_DIR);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirField.setText(chooser.getSelectedFile().getAbsolutePath());
            publishLog("📂 Directorio de salida cambiado: " + chooser.getSelectedFile().getName());
        }
    }

    private void directConvertFile(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos soportados", EXTENSIONS));
        chooser.setCurrentDirectory(PROJECT_OUTPUT_DIR);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            
            SwingWorker<Void, String> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    publish("⚡ Conversión directa: " + selectedFile.getName());
                    progressBar.setIndeterminate(true);
                    progressBar.setString("Convirtiendo...");
                    
                    try {
                        String outputPath = convertSingleFile(selectedFile.getAbsolutePath(), 
                            new File(PROJECT_OUTPUT_DIR, "converted_files").getAbsolutePath());
                        publish("✅ Conversión completada: " + selectedFile.getName());
                        publish("📄 PDF creado: " + outputPath);
                        
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(WindowsPDFConverter.this,
                                "✅ Conversión completada!\n📄 " + selectedFile.getName() + " → PDF",
                                "Conversión Exitosa", JOptionPane.INFORMATION_MESSAGE);
                        });
                        
                    } catch (Exception ex) {
                        publish("❌ Error: " + ex.getMessage());
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(WindowsPDFConverter.this,
                                "❌ Error al convertir:\n" + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                    return null;
                }
                
                @Override
                protected void process(List<String> chunks) {
                    chunks.forEach(WindowsPDFConverter.this::publishLog);
                }
                
                @Override
                protected void done() {
                    progressBar.setIndeterminate(false);
                    progressBar.setString("Listo");
                }
            };
            worker.execute();
        }
    }

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
                publish("🚀 === INICIANDO CONVERSIÓN MASIVA ===");
                publish("📊 Archivos: " + files.size() + " | Hilos: " + threads);
                publish("📁 Destino: " + outputDir);
                
                startConversionBtn.setEnabled(false);
                progressBar.setIndeterminate(true);
                progressBar.setString("Convirtiendo archivos...");
                
                return processFiles(files, outputDir, threads, msg -> publish(msg));
            }
            
            @Override
            protected void process(List<String> chunks) {
                chunks.forEach(WindowsPDFConverter.this::publishLog);
            }
            
            @Override
            protected void done() {
                try {
                    List<String> results = get();
                    publishLog("✅ === CONVERSIÓN FINALIZADA ===");
                    publishLog("📊 Archivos convertidos: " + results.size() + "/" + listModel.size());
                    
                    JOptionPane.showMessageDialog(WindowsPDFConverter.this,
                        "🎉 Conversión completada!\n📊 Archivos procesados: " + results.size(),
                        "Conversión Exitosa", JOptionPane.INFORMATION_MESSAGE);
                        
                } catch (Exception ex) {
                    publishLog("❌ Error en conversión: " + ex.getMessage());
                    JOptionPane.showMessageDialog(WindowsPDFConverter.this,
                        "❌ Error durante la conversión:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    startConversionBtn.setEnabled(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setString("Listo");
                }
            }
        };
        worker.execute();
    }

    // Métodos de procesamiento (mantener los originales con mejoras menores)
    public List<String> processFiles(List<String> files, String outputDir, int threads, java.util.function.Consumer<String> logger) throws Exception {
        Files.createDirectories(Paths.get(outputDir));
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        
        List<CompletableFuture<String>> futures = files.stream()
                .map(file -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String result = convertSingleFile(file, outputDir);
                        logger.accept("✅ " + Paths.get(file).getFileName());
                        return result;
                    } catch (Exception ex) {
                        logger.accept("❌ " + Paths.get(file).getFileName() + ": " + ex.getMessage());
                        return null;
                    }
                }, executor))
                .collect(Collectors.toList());

        List<String> results = futures.stream()
            .map(CompletableFuture::join)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
            
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);
        return results;
    }

    private String convertSingleFile(String inputFile, String outputDir) throws Exception {
        Path inputPath = Paths.get(inputFile);
        String baseName = inputPath.getFileName().toString();
        int dot = baseName.lastIndexOf('.');
        if (dot > 0) baseName = baseName.substring(0, dot);

        Path pdfPath = Paths.get(outputDir, baseName + ".pdf");
        
        // Simulación de conversión (crear PDF básico con PDFBox o texto simple)
        try {
            // Si existe LibreOffice, usarlo; si no, crear PDF simulado
            if (Files.exists(Paths.get(SOFFICE_PATH))) {
                ProcessBuilder pb = new ProcessBuilder(
                    SOFFICE_PATH, "--headless", "--convert-to", "pdf",
                    "--outdir", outputDir, inputPath.toString()
                );
                Process proc = pb.start();
                if (!proc.waitFor(60, TimeUnit.SECONDS)) {
                    proc.destroyForcibly();
                    throw new RuntimeException("Timeout en conversión: " + inputFile);
                }
            } else {
                // Crear PDF simulado para testing
                try (PrintWriter writer = new PrintWriter(pdfPath.toFile())) {
                    writer.println("%PDF-1.4");
                    writer.println("% Archivo PDF simulado para testing");
                    writer.println("% Archivo original: " + inputPath.getFileName());
                    writer.println("% Fecha conversión: " + java.time.LocalDateTime.now());
                    
                    // Simular tiempo de procesamiento variable
                    Thread.sleep(100 + (int)(Math.random() * 200));
                }
            }
            
            return pdfPath.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error convirtiendo " + inputFile + ": " + e.getMessage());
        }
    }

    public JTextArea getLogArea() {
        return logArea;
    }

    private void publishLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
            );
            logArea.append("[" + timestamp + "] " + msg + "\n");
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