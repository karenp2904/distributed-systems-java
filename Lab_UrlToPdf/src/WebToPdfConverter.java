import javax.swing.*;

/**
 * Laboratorio 6 - Convertidor Web a PDF
 * Clase principal - NO necesita imports de tus otras clases
 */
public class WebToPdfConverter {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--cli")) {
            // No necesitas import para CLIRunner porque está en el mismo directorio
            new CLIRunner().run(args);
        } else {
            // Ejecutar GUI
            SwingUtilities.invokeLater(() -> new GUI().setVisible(true));
        }
    }
}