import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

public class GeneradorGraficas {

    public static void generarGrafica(String outputDir, String fileName, List<Integer> datos) {
        int width = 800;
        int height = 600;

        // Crear la carpeta si no existe
        File directory = new File(outputDir);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("Carpeta creada: " + directory.getAbsolutePath());
            } else {
                System.out.println("No se pudo crear la carpeta: " + directory.getAbsolutePath());
            }
        }

        // Crear imagen
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // Fondo blanco
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Dibujar datos como barras (ejemplo)
        g.setColor(Color.BLUE);
        int barWidth = width / datos.size();
        for (int i = 0; i < datos.size(); i++) {
            int barHeight = datos.get(i);
            g.fillRect(i * barWidth, height - barHeight, barWidth - 5, barHeight);
        }

        g.dispose();

        // Guardar imagen
        String outputPath = new File(outputDir, fileName).getAbsolutePath();
        try {
            ImageIO.write(image, "png", new File(outputPath));
            System.out.println("Gráfica guardada en: " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
