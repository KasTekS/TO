import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class JavaToOracleMapper {
    private static final Map<Class<?>, String> javaToOracleTypeMap = new HashMap<>();

    static {
        javaToOracleTypeMap.put(String.class, "VARCHAR2");
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

    public static String generateOracleTypeScript(String className) {
        StringBuilder scriptBuilder = new StringBuilder();
        scriptBuilder.append("CREATE OR REPLACE TYPE ").append(className).append("_type AS OBJECT (");

        try {
            Class<?> clazz = Class.forName(className);
            // Mapowanie pól
            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                java.lang.reflect.Field field = fields[i];
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                String oracleType = mapJavaToOracle(fieldType);
                scriptBuilder.append(fieldName).append(" ").append(oracleType);
                if (i < fields.length - 1) {
                    scriptBuilder.append(", ");
                }
            }



            scriptBuilder.append(");");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Nie można odnaleźć klasy " + className, e);
        }

        return scriptBuilder.toString();
    }

    public static String generateOracleTypeBodyScript(String className) {
        StringBuilder scriptBuilder = new StringBuilder();
        scriptBuilder.append("CREATE OR REPLACE TYPE BODY ").append(className).append("_type AS");

        try {
            Class<?> clazz = Class.forName(className);
            Field[] fields = clazz.getDeclaredFields();

            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            for (Constructor<?> constructor : constructors) {
                scriptBuilder.append("\n  CONSTRUCTOR FUNCTION ").append(className).append("_type (");

                Parameter[] parameters = constructor.getParameters();
                String aa = constructor.toString();
                System.out.println(aa);
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    scriptBuilder.append(parameter.getName()).append(" ").append(mapJavaToOracle(parameter.getType()));
                    if (i < parameters.length - 1) {
                        scriptBuilder.append(", ");
                    }
                }

                scriptBuilder.append(") RETURN SELF AS RESULT IS\n    BEGIN\n");
                for (Parameter parameter : parameters) {
                    scriptBuilder.append("      SELF.").append(parameter.getName()).append(" := ").append(parameter.getName()).append(";\n");
                }
                scriptBuilder.append("      RETURN;\n    END;\n");
            }


            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                scriptBuilder.append("\n  MEMBER FUNCTION ").append(method.getName()).append(" RETURN ");
                scriptBuilder.append(mapJavaToOracle(method.getReturnType())).append(" IS\n    BEGIN\n");
                if (method.getReturnType() != void.class && method.getReturnType() != Void.class) {
                    scriptBuilder.append("      RETURN ");
                }
                scriptBuilder.append(method.getName()).append("");
                Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    scriptBuilder.append(parameter.getName());
                    if (i < parameters.length - 1) {
                        scriptBuilder.append(", ");
                    }
                }
                scriptBuilder.append(";\n    END;\n");
            }

            scriptBuilder.append("END;");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Nie można odnaleźć klasy " + className, e);
        }

        return scriptBuilder.toString();
    }

    public static void main(String[] args) {
        String className = "Car";
        String oracleScript = generateOracleTypeBodyScript(className);
        String oracleScript2 = generateOracleTypeScript(className);
        System.out.println(oracleScript2);
        System.out.println(oracleScript);
    }
}