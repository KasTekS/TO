import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class LoadJavaToOracle {
    public static void main(String[] args) {
        String dbUrl = "jdbc:oracle:thin:@localhost:1521:xe";
        String username = "";
        String password = "mazur";

        String vehicleClassPath = "C:\\Users\\Kamil\\Desktop\\TO\\untitled\\out\\Vehicle.class";

        try {
            // Dodaj obie klasy do polecenia loadjava
            String command = "cmd /c loadjava -user " + username + "/" + password + "@localhost:1521:xe -resolve -force \"" + vehicleClassPath + "\"";
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            processOutput(process);

            if (exitCode == 0) {
                System.out.println("Klasy zostały pomyślnie załadowane do bazy danych Oracle.");
            } else {
                System.out.println("Wystąpił błąd podczas ładowania klas do bazy danych Oracle. Kod wyjścia: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void processOutput(Process process) throws IOException {
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String line;

        System.out.println("Output from the command:");
        while ((line = inputReader.readLine()) != null) {
            System.out.println(line);
        }

        System.out.println("Errors from the command if any:");
        while ((line = errorReader.readLine()) != null) {
            System.out.println(line);
        }
    }
}

