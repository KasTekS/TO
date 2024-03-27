import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");

        String url = "jdbc:oracle:thin:@localhost:1521:xe";
        String username = "c##kamil";
        String password = "mazur";


        try {
            Connection connection = DriverManager.getConnection(url, username, password);
            if (connection != null) {
                System.out.println("Połączono z bazą danych Oracle!");
                Statement statement = connection.createStatement();
//                String createTypeQuery = "CREATE OR REPLACE TYPE book_type AS OBJECT (title VARCHAR2(200), author VARCHAR2(100), pages NUMBER, genre VARCHAR2(100))";
//                statement.execute(createTypeQuery);
//                System.out.println("Typ obiektowy book_type został pomyślnie utworzony.");




                Class<?> carClass = Class.forName("Car");

                Class<?> superClass = carClass.getSuperclass();


                if (superClass != null) {
                    System.out.println("Klasa Car dziedziczy po klasie: " + superClass.getName());
                } else {
                    System.out.println("Klasa Car nie ma klasy nadrzędnej.");
                }

                System.out.println("Pola klasy Car:");
                Field[] carFields = carClass.getDeclaredFields();
                for (Field field : carFields) {
                    System.out.println("- " + field.getName() + " (typ: " + field.getType().getSimpleName() + ")");
                }
                connection.close();
            }
        } catch (SQLException e) {
            System.out.println("Błąd podczas połączenia z bazą danych: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
