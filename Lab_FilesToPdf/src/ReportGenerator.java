



import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class ReportGenerator {

    public ReportGenerator() {
        // Constructor vacío para poder instanciar sin parámetros
    }

   public void generateReport(Map<Integer, Long> results) {
    try {
        File outputDir = new File("reporte");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File reportFile = new File(outputDir, "benchmark_report.txt");
        try (FileWriter writer = new FileWriter(reportFile)) {
            for (Map.Entry<Integer, Long> entry : results.entrySet()) {
                writer.write(entry.getKey() + " hilos -> " + entry.getValue() + " ms\n");
            }
        }


    } catch (IOException e) {
        e.printStackTrace();
    }
}



}
