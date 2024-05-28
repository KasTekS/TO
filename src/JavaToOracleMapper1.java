import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class JavaToOracleMapper1 {
    private static final Map<Class<?>, String> javaToOracleTypeMap = new HashMap<>();

    static {
        javaToOracleTypeMap.put(String.class, "VARCHAR2(2048)");
        javaToOracleTypeMap.put(Blob.class, "BLOB");
        javaToOracleTypeMap.put(Clob.class, "CLOB");
        javaToOracleTypeMap.put(LocalDate.class, "DATE");
        javaToOracleTypeMap.put(Float.class, "FLOAT");
        javaToOracleTypeMap.put(Double.class, "FLOAT");
        javaToOracleTypeMap.put(Integer.class, "NUMBER");
        javaToOracleTypeMap.put(Long.class, "LONG");
        javaToOracleTypeMap.put(byte[].class, "RAW");
        javaToOracleTypeMap.put(Timestamp.class, "TIMESTAMP");
        javaToOracleTypeMap.put(int.class, "NUMBER");
        javaToOracleTypeMap.put(double.class, "FLOAT");
        javaToOracleTypeMap.put(boolean.class, "NUMBER(1)");
        javaToOracleTypeMap.put(byte.class, "NUMBER(3)");
        javaToOracleTypeMap.put(short.class, "NUMBER(5)");
        javaToOracleTypeMap.put(char.class, "CHAR");
        javaToOracleTypeMap.put(BigDecimal.class, "NUMBER");
    }

    public static String mapJavaToOracle(Class<?> javaType) {
        return javaToOracleTypeMap.get(javaType);
    }

    public static void generateOracleTypeScript(String className, Statement statement) {
        StringBuilder scriptBuilder = new StringBuilder();

        try {
            Class<?> clazz = Class.forName(className);
            Class<?> superClass = clazz.getSuperclass();



            if (superClass != null && !superClass.getName().equals("java.lang.Object")) {
                generateOracleTypeScript(superClass.getName(), statement);
                scriptBuilder.append("CREATE OR REPLACE TYPE ").append(className).append("_type UNDER ").append(superClass.getSimpleName()).append("_type (");
            } else {
                scriptBuilder.append("CREATE OR REPLACE TYPE ").append(className).append("_type AS OBJECT (");
            }

            Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                String oracleType = mapJavaToOracle(fieldType);
                scriptBuilder.append(fieldName).append(" ").append(oracleType);
                if (i < fields.length - 1) {
                    scriptBuilder.append(", ");
                }
            }

            scriptBuilder.append(") NOT FINAL;");
            try {
                statement.execute(scriptBuilder.toString());
                System.out.println("Utworzono typ obiektowy dla klasy: " + className);
            } catch (SQLException e) {
                System.err.println("Błąd podczas wykonywania skryptu SQL dla klasy: " + className +" "+  e.getMessage());
                System.out.println("Pozostałe klasy zostaną utworzone poprawnie");
            }


        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Nie można odnaleźć klasy " + className, e);
        }
    }
}

