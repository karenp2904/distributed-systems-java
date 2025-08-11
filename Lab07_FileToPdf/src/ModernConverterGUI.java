import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Future;

public class ModernConverterGUI extends JFrame {
    private DefaultListModel<String> fileListModel;
    private JList<String> fileList;
    private JTextField outputDirField;
    private JTextArea logArea;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton convertButton;
    private JButton addButton;
    private JButton removeButton;
    private JButton clearButton;
    
    private ConversionManager conversionManager;
    private Future<ConversionReport> currentConversion;
    
    public ModernConverterGUI() {
        try {
            // Configurar LibreOffice
            String libreOfficePath = LibreOfficeConfig.detectLibreOfficePath();
            conversionManager = new ConversionManager(libreOfficePath);
            initializeGUI();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, 
                "Error inicializando LibreOffice: " + e.getMessage(),
                "Error de Configuración", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    private void initializeGUI() {
        setTitle("Conversor de Documentos a PDF - Versión Pro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        // Panel principal con márgenes
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Header con título y logo
        JPanel headerPanel = createHeaderPanel();
        
        // Panel de archivos
        JPanel filesPanel = createFilesPanel();
        
        // Panel de configuración
        JPanel configPanel = createConfigPanel();
        
        // Panel de control
        JPanel controlPanel = createControlPanel();
        
        // Panel de progreso y log
        JPanel bottomPanel = createBottomPanel();
        
        // Ensamblar interfaz
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(filesPanel, BorderLayout.CENTER);
        centerPanel.add(configPanel, BorderLayout.SOUTH);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.EAST);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(45, 45, 48));
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("🔄 Conversor de Documentos a PDF");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel subtitleLabel = new JLabel("Conversión rápida y eficiente con LibreOffice");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(200, 200, 200));
        
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.add(titleLabel, BorderLayout.CENTER);
        textPanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        panel.add(textPanel, BorderLayout.WEST);
        return panel;
    }
    
    private JPanel createFilesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 100)), 
            "📁 Archivos a Convertir"));
        
        // Lista de archivos
        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileList.setCellRenderer(new FileListCellRenderer());
        
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        scrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
        
        // Botones de gestión de archivos
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        addButton = createStyledButton("➕ Agregar", new Color(34, 139, 34));
        removeButton = createStyledButton("➖ Remover", new Color(220, 20, 60));
        clearButton = createStyledButton("🗑️ Limpiar", new Color(255, 140, 0));
        
        addButton.addActionListener(this::addFiles);
        removeButton.addActionListener(this::removeSelectedFiles);
        clearButton.addActionListener(e -> {
            fileListModel.clear();
            updateStatus("Lista de archivos limpiada");
        });
        
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 100)), 
            "⚙️ Configuración"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Directorio de salida
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("📂 Directorio de salida:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        outputDirField = new JTextField(System.getProperty("user.dir"));
        outputDirField.setPreferredSize(new Dimension(300, 25));
        panel.add(outputDirField, gbc);
        
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JButton browseButton = createStyledButton("📁", new Color(70, 130, 180));
        browseButton.addActionListener(this::browseOutputDirectory);
        panel.add(browseButton, gbc);
        
        return panel;
    }
    
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Botón principal de conversión
        convertButton = new JButton("🚀 CONVERTIR A PDF");
        convertButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        convertButton.setPreferredSize(new Dimension(200, 50));
        convertButton.setBackground(new Color(0, 123, 255));
        convertButton.setForeground(Color.WHITE);
        convertButton.setFocusPainted(false);
        convertButton.setBorder(BorderFactory.createRaisedBevelBorder());
        convertButton.addActionListener(this::startConversion);
        
        gbc.gridy = 0;
        panel.add(convertButton, gbc);
        
        // Botón de cancelar
        JButton cancelButton = createStyledButton("⏹️ Cancelar", new Color(220, 53, 69));
        cancelButton.addActionListener(this::cancelConversion);
        cancelButton.setEnabled(false);
        
        gbc.gridy = 1;
        panel.add(cancelButton, gbc);
        
        // Botón de reporte
        JButton reportButton = createStyledButton("📊 Ver Reporte", new Color(108, 117, 125));
        reportButton.addActionListener(this::showDetailedReport);
        
        gbc.gridy = 2;
        panel.add(reportButton, gbc);
        
        // Información del sistema
        gbc.gridy = 3;
        JPanel infoPanel = new JPanel(new GridLayout(0, 1, 2, 2));
        infoPanel.setBorder(BorderFactory.createTitledBorder("ℹ️ Info del Sistema"));
        
        infoPanel.add(new JLabel("Hilos: " + Runtime.getRuntime().availableProcessors()));
        infoPanel.add(new JLabel("Java: " + System.getProperty("java.version")));
        infoPanel.add(new JLabel("SO: " + System.getProperty("os.name")));
        
        panel.add(infoPanel, gbc);
        
        return panel;
    }
    
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        
        // Barra de progreso
        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        progressPanel.setBorder(BorderFactory.createTitledBorder("📈 Progreso"));
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Listo para convertir");
        
        statusLabel = new JLabel("Estado: Esperando archivos...");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.add(statusLabel, BorderLayout.SOUTH);
        
        // Área de log
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("📝 Log de Conversión"));
        
        logArea = new JTextArea(8, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBackground(new Color(248, 249, 250));
        logArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        logPanel.add(logScrollPane, BorderLayout.CENTER);
        
        // Botones del log
        JPanel logButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearLogButton = createStyledButton("🗑️ Limpiar Log", new Color(108, 117, 125));
        JButton saveLogButton = createStyledButton("💾 Guardar Log", new Color(40, 167, 69));
        
        clearLogButton.addActionListener(e -> logArea.setText(""));
        saveLogButton.addActionListener(this::saveLog);
        
        logButtonPanel.add(clearLogButton);
        logButtonPanel.add(saveLogButton);
        
        logPanel.add(logButtonPanel, BorderLayout.SOUTH);
        
        panel.add(progressPanel, BorderLayout.NORTH);
        panel.add(logPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JButton createStyledButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setPreferredSize(new Dimension(120, 30));
        
        // Efecto hover
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(backgroundColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(backgroundColor);
            }
        });
        
        return button;
    }
    
    // Renderer personalizado para la lista de archivos
    private static class FileListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            String filePath = value.toString();
            String fileName = new File(filePath).getName();
            String extension = FileValidator.getFileExtension(filePath).toLowerCase();
            
            // Iconos por tipo de archivo
            String icon = getFileIcon(extension);
            setText(icon + " " + fileName);
            setToolTipText(filePath);
            
            // Colores alternados
            if (!isSelected) {
                setBackground(index % 2 == 0 ? Color.WHITE : new Color(248, 249, 250));
            }
            
            return this;
        }
        
        private String getFileIcon(String extension) {
            switch (extension) {
                case "docx": case "doc": return "📄";
                case "xlsx": case "xls": return "📊";
                case "pptx": case "ppt": return "📽️";
                case "png": case "jpg": case "jpeg": return "🖼️";
                default: return "📎";
            }
        }
    }
    
    private void addFiles(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setDialogTitle("Seleccionar archivos para convertir");
        
        // Filtros por tipo de archivo
        String[] extensions = FileValidator.getSupportedExtensions().toArray(new String[0]);
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Documentos soportados (" + String.join(", ", extensions) + ")", extensions);
        fileChooser.setFileFilter(filter);
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            int addedCount = 0;
            
            for (File file : selectedFiles) {
                String path = file.getAbsolutePath();
                if (!fileListModel.contains(path)) {
                    FileValidator.ValidationResult validation = FileValidator.validateFile(path);
                    if (validation.isValid()) {
                        fileListModel.addElement(path);
                        addedCount++;
                    } else {
                        logMessage("❌ " + file.getName() + ": " + validation.getMessage());
                    }
                }
            }
            
            updateStatus("Agregados " + addedCount + " archivo(s) válido(s)");
            logMessage("➕ Agregados " + addedCount + " archivos a la lista");
        }
    }
    
    private void removeSelectedFiles(ActionEvent e) {
        int[] selectedIndices = fileList.getSelectedIndices();
        if (selectedIndices.length > 0) {
            for (int i = selectedIndices.length - 1; i >= 0; i--) {
                fileListModel.remove(selectedIndices[i]);
            }
            updateStatus("Removidos " + selectedIndices.length + " archivo(s)");
            logMessage("➖ Removidos " + selectedIndices.length + " archivos de la lista");
        }
    }
    
    private void browseOutputDirectory(ActionEvent e) {
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirChooser.setDialogTitle("Seleccionar directorio de salida");
        dirChooser.setCurrentDirectory(new File(outputDirField.getText()));
        
        if (dirChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String selectedPath = dirChooser.getSelectedFile().getAbsolutePath();
            outputDirField.setText(selectedPath);
            updateStatus("Directorio de salida: " + selectedPath);
        }
    }
    
    private void startConversion(ActionEvent e) {
        if (fileListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Por favor, agregue archivos para convertir",
                "Sin archivos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Preparar lista de archivos
        List<String> filePaths = new ArrayList<>();
        for (int i = 0; i < fileListModel.size(); i++) {
            filePaths.add(fileListModel.getElementAt(i));
        }
        
        String outputDir = outputDirField.getText().trim();
        if (outputDir.isEmpty()) {
            outputDir = System.getProperty("user.dir");
            outputDirField.setText(outputDir);
        }
        
        // Configurar interfaz para conversión
        setConversionMode(true);
        progressBar.setValue(0);
        progressBar.setMaximum(filePaths.size());
        progressBar.setString("Iniciando conversión...");
        
        logMessage("🚀 Iniciando conversión de " + filePaths.size() + " archivo(s)");
        logMessage("📂 Directorio de salida: " + outputDir);
        logMessage("🧵 Hilos disponibles: " + conversionManager.getMaxThreads());
        
        // Ejecutar conversión asíncrona
        currentConversion = conversionManager.convertFilesAsync(filePaths, outputDir, 
            new ConversionManager.ProgressCallback() {
                @Override
                public void onProgress(int completed, int total, ConversionResult result) {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(completed);
                        progressBar.setString(completed + "/" + total + " completados");
                        
                        String status = result.isSuccess() ? "✅" : "❌";
                        String fileName = new File(result.getInputPath()).getName();
                        logMessage(status + " " + fileName + " (" + result.getFormattedDuration() + 
                                 ") [" + result.getThreadName() + "]");
                        
                        updateStatus("Procesando: " + completed + "/" + total);
                    });
                }
                
                @Override
                public void onComplete(ConversionReport report) {
                    SwingUtilities.invokeLater(() -> {
                        setConversionMode(false);
                        showCompletionDialog(report);
                        updateStatus("Conversión completada: " + report.getSuccessCount() + 
                                   "/" + report.getResults().size() + " exitosos");
                    });
                }
            });
    }
    
    private void cancelConversion(ActionEvent e) {
        if (currentConversion != null && !currentConversion.isDone()) {
            currentConversion.cancel(true);
            setConversionMode(false);
            updateStatus("Conversión cancelada por el usuario");
            logMessage("⏹️ Conversión cancelada");
            progressBar.setString("Cancelado");
        }
    }
    
    private void setConversionMode(boolean converting) {
        convertButton.setEnabled(!converting);
        addButton.setEnabled(!converting);
        removeButton.setEnabled(!converting);
        clearButton.setEnabled(!converting);
        // cancelButton.setEnabled(converting); // Necesitarías guardar referencia
    }
    
    private void showCompletionDialog(ConversionReport report) {
        StringBuilder message = new StringBuilder();
        message.append("Conversión completada!\n\n");
        message.append("📊 Resumen:\n");
        message.append("• Exitosos: ").append(report.getSuccessCount()).append("\n");
        message.append("• Fallidos: ").append(report.getFailedCount()).append("\n");
        message.append("• Tiempo total: ").append(String.format("%.2f segundos", report.getTotalTimeMs() / 1000.0)).append("\n");
        message.append("• Hilos utilizados: ").append(report.getThreadsUsed()).append("\n");
        
        if (report.getSuccessCount() > 0) {
            message.append("\n📁 PDFs generados en:\n");
            message.append(outputDirField.getText());
        }
        
        JOptionPane.showMessageDialog(this, message.toString(), 
            "Conversión Completada", JOptionPane.INFORMATION_MESSAGE);
        
        // Preguntar si abrir directorio
        if (report.getSuccessCount() > 0) {
            int option = JOptionPane.showConfirmDialog(this, 
                "¿Desea abrir el directorio con los PDFs generados?", 
                "Abrir Directorio", JOptionPane.YES_NO_OPTION);
            
            if (option == JOptionPane.YES_OPTION) {
                openDirectory(outputDirField.getText());
            }
        }
    }
    
    private void showDetailedReport(ActionEvent e) {
        // Esta función se implementaría para mostrar el reporte completo
        JDialog reportDialog = new JDialog(this, "Reporte Detallado", true);
        reportDialog.setLayout(new BorderLayout());
        
        JTextArea reportArea = new JTextArea(20, 80);
        reportArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        reportArea.setEditable(false);
        reportArea.setText("Reporte detallado se mostraría aquí...\n" +
                          "Incluiría estadísticas completas de hilos, tiempos, etc.");
        
        JScrollPane scrollPane = new JScrollPane(reportArea);
        reportDialog.add(scrollPane, BorderLayout.CENTER);
        
        JButton closeButton = new JButton("Cerrar");
        closeButton.addActionListener(ev -> reportDialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(closeButton);
        reportDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        reportDialog.pack();
        reportDialog.setLocationRelativeTo(this);
        reportDialog.setVisible(true);
    }
    
    private void saveLog(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("conversion_log_" + 
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                java.nio.file.Files.write(fileChooser.getSelectedFile().toPath(), 
                    logArea.getText().getBytes());
                updateStatus("Log guardado exitosamente");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error guardando log: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void openDirectory(String directoryPath) {
        try {
            Desktop.getDesktop().open(new File(directoryPath));
        } catch (Exception ex) {
            logMessage("❌ Error abriendo directorio: " + ex.getMessage());
        }
    }
    
    private void updateStatus(String status) {
        statusLabel.setText("Estado: " + status);
    }
    
    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
