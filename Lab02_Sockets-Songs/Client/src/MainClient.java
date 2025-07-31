import java.util.Scanner;

public class MainClient {
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean repeat;
        repeat=true;
        do {
            JSocketClient client = new JSocketClient("localhost", 1802);

            System.out.println("\n Buscar Canciones");
            System.out.println("1. Buscar por título");
            System.out.println("2. Buscar por autor");
            System.out.println("3. Buscar por género");
            System.out.println("0. Salir");
            System.out.print("Seleccione una opción: ");
            String option = scanner.nextLine();

            if (option.equals("0")) {
                System.out.println(" Gracias por usar el buscador.");
                break;
            }

            String type = "";
            if (option.equals("1")) {
                type = "title";
            } else if (option.equals("2")) {
                type = "author";
            } else if (option.equals("3")) {
                type = "genre";
            } else {
                System.out.println("Opción inválida.");
                continue;
            }

            System.out.print("🔍 Ingrese el texto de búsqueda: ");
            String query = scanner.nextLine();

            // Enviar consulta y repetir si fue inválida
            repeat = !client.request(type + ":" + query);

        } while (repeat);

        scanner.close();
    }
}
