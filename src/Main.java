import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Podaj adres bazy danych (np. localhost:1521:xe):");
        String dbUrl = scanner.nextLine();
        System.out.println("Podaj nazwę użytkownika:");
        String username = scanner.nextLine();
        System.out.println("Podaj hasło:");
        String password = scanner.nextLine();

        try (Connection connection = DriverManager.getConnection("jdbc:oracle:thin:@" + dbUrl, username, password)) {
            Statement statement = connection.createStatement();

            boolean exit = false;

            JavaToOracleMapper1 mapper1 = new JavaToOracleMapper1();
            StaticMethodEnhancer enhancer = new StaticMethodEnhancer();
            JavaToOracleMapper mapper = new JavaToOracleMapper();

            while (!exit) {
                System.out.println("Wybierz opcję:");
                System.out.println("1. Mapowanie klasy Java na Oracle bardzo prosta, nie polecana");
                System.out.println("2. Dodanie metod statycznych do klasy za pomocą zeby mogło zadziałać rozbudowane mapowanie");
                System.out.println("3. Rozbudowane mapowanie klasy Java na Oracle (mapowanie klas i konstruktorów)");
                System.out.println("4. Wyjście");

                int choice = scanner.nextInt();
                scanner.nextLine(); // Pobranie znaku nowej linii

                switch (choice) {
                    case 1:
                        System.out.println("Podaj nazwę klasy do zmapowania:");
                        String className1 = scanner.nextLine();
                        try {
                            mapper1.generateOracleTypeScript(className1, statement);
                        } catch (Exception e) {
                            System.out.println("Błąd: " + e.getMessage());
                        }
                        break;
                    case 2:
                        System.out.println("Podaj nazwę klasy do dodania metod statycznych:");
                        String className2 = scanner.nextLine();
                        enhancer.enhanceClass(className2, dbUrl, username, password);
                        break;
                    case 3:
                        System.out.println("Podaj nazwę klasy do zmapowania :");
                        String className3 = scanner.nextLine();
                        mapper.generateOracleTypeScript(className3, statement);
                        break;
                    case 4:
                        exit = true;
                        break;
                    default:
                        System.out.println("Nieprawidłowy wybór. Spróbuj ponownie.");
                }
            }

            statement.close();
        } catch (SQLException e) {
            System.out.println("Błąd połączenia z bazą danych: " + e.getMessage());
        }

        scanner.close();
    }
}
