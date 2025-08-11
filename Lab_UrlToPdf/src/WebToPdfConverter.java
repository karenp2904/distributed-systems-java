import javax.swing.*;


public class WebToPdfConverter {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--cli")) {
            new CLIRunner().run(args);
        } else {
            // Ejecutar GUI
            SwingUtilities.invokeLater(() -> new GUI().setVisible(true));
        }
    }
}