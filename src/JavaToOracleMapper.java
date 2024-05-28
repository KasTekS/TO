import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class JavaToOracleMapper {
    private static final Map<Class<?>, String> javaToOracleTypeMap = new HashMap<>();

    static {
        javaToOracleTypeMap.put(String.class, "VARCHAR2(2048)");
        javaToOracleTypeMap.put(Blob.class, "BLOB");
        javaToOracleTypeMap.put(Clob.class, "CLOB");
        javaToOracleTypeMap.put(LocalDate.class, "DATE");
        javaToOracleTypeMap.put(Float.class, "FLOAT");
        javaToOracleTypeMap.put(Double.class, "FLOAT");
        javaToOracleTypeMap.put(Integer.class, "NUMBER");
        javaToOracleTypeMap.put(Long.class, "NUMBER");
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

    private static String stripSizeSpecifications(String oracleType) {
        if (oracleType != null) {
            return oracleType.replaceAll("\\(.*\\)", "");
        }
        return "UNKNOWN"; // Return a default type or throw an exception if preferred
    }

    public static void generateOracleTypeScript(String className, Statement statement) {
        StringBuilder scriptBuilder = new StringBuilder();
        System.out.println(className);
        try {
            Class<?> clazz = Class.forName(className);
            scriptBuilder.append("CREATE OR REPLACE TYPE ").append(clazz.getSimpleName()).append("_type AS OBJECT (");

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

            // Add constructor declaration
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            if (constructors.length > 0) {
                Constructor<?> constructor = constructors[0]; // Assuming the class has one constructor
                scriptBuilder.append(", CONSTRUCTOR FUNCTION ").append(clazz.getSimpleName()).append("_type").append(mapConstructorParameters(constructor)).append(" RETURN SELF AS RESULT");
            }

            Method[] methods = clazz.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                String methodName = method.getName();
                Class<?> returnType = method.getReturnType();
                Class<?>[] parameterTypes = method.getParameterTypes();

                if (returnType != Void.TYPE) { // function
                    scriptBuilder.append(", MEMBER FUNCTION ").append(methodName);
                    if (parameterTypes.length > 0) {
                        scriptBuilder.append("(").append(mapMethodParameters(parameterTypes)).append(")");
                    }
                    scriptBuilder.append(" RETURN ").append(stripSizeSpecifications(mapJavaToOracle(returnType)));
                } else { // procedure
                    scriptBuilder.append(", MEMBER PROCEDURE ").append(methodName);
                    if (parameterTypes.length > 0) {
                        scriptBuilder.append("(").append(mapMethodParameters(parameterTypes)).append(")");
                    }
                }
            }

            scriptBuilder.append(") NOT FINAL;");
            executeScript(scriptBuilder.toString(), statement); // Execute the TYPE creation script

            generateOracleMethods(clazz, statement);
            generateOracleTypeBody(clazz, statement);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Nie można odnaleźć klasy " + className, e);
        }
    }

    private static String mapMethodParameters(Class<?>[] parameterTypes) {
        StringBuilder parameters = new StringBuilder();
        for (int j = 0; j < parameterTypes.length; j++) {
            if (j > 0) {
                parameters.append(", ");
            }
            parameters.append("p").append(j + 1).append(" IN ").append(stripSizeSpecifications(mapJavaToOracle(parameterTypes[j])));
        }
        return parameters.toString();
    }

    private static void generateOracleMethods(Class<?> clazz, Statement statement) {
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            StringBuilder methodScript = new StringBuilder();
            String methodName = method.getName();
            Class<?> returnType = method.getReturnType();
            Class<?>[] parameterTypes = method.getParameterTypes();

            if (returnType != Void.TYPE) { // function
                methodScript.append("CREATE OR REPLACE FUNCTION static_").append(methodName).append("(");
            } else { // procedure
                methodScript.append("CREATE OR REPLACE PROCEDURE static_").append(methodName).append("(");
            }

            // Dodanie parametru id
            methodScript.append("p_id IN NUMBER");

            if (parameterTypes.length > 0) {
                methodScript.append(", ");
            }

            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> paramType = parameterTypes[i];
                String paramName = "p" + (i + 1); // parameter name
                String oracleType = stripSizeSpecifications(mapJavaToOracle(paramType));
                methodScript.append(paramName).append(" IN ").append(oracleType);
                if (i < parameterTypes.length - 1) {
                    methodScript.append(", ");
                }
            }

            if (returnType != Void.TYPE) { // function
                methodScript.append(") RETURN ").append(stripSizeSpecifications(mapJavaToOracle(returnType)));
                methodScript.append(" AS LANGUAGE JAVA NAME '").append(clazz.getName()).append(".static_").append(methodName).append("(int");
            } else { // procedure
                methodScript.append(") AS LANGUAGE JAVA NAME '").append(clazz.getName()).append(".static_").append(methodName).append("(int");
            }

            for (int i = 0; i < parameterTypes.length; i++) {
                methodScript.append(", ").append(getJavaSignatureType(parameterTypes[i]));
            }

            if (returnType != Void.TYPE) {
                methodScript.append(") return ").append(getJavaSignatureType(returnType)).append("';");
            } else {
                methodScript.append(")';");
            }

            executeScript(methodScript.toString(), statement); // Execute the method script
        }
        generateConstructorSQLFunction(clazz, statement);
    }

    private static void generateOracleTypeBody(Class<?> clazz, Statement statement) {
        StringBuilder typeBodyScript = new StringBuilder();
        String typeName = clazz.getSimpleName() + "_type";

        typeBodyScript.append("CREATE OR REPLACE TYPE BODY ").append(typeName).append(" IS");

        // Dodanie konstruktora
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        if (constructors.length > 0) {
            Constructor<?> constructor = constructors[0];
            typeBodyScript.append("\nCONSTRUCTOR FUNCTION ").append(typeName).append(mapConstructorParameters(constructor)).append(" RETURN SELF AS RESULT IS\n");
            typeBodyScript.append("BEGIN\n");

            Field[] fields = clazz.getDeclaredFields();

            for (int i = 0; i < fields.length - 1; i++) {
                Field field = fields[i + 1];
                String fieldName = field.getName();
                typeBodyScript.append("  SELF.").append(fieldName).append(" := p").append(i + 2).append(";\n");
            }

            // Generate the dynamic SQL function for the constructor
            typeBodyScript.append("  SELF.id := create").append(clazz.getSimpleName()).append("(");
            for (int i = 0; i < fields.length; i++) {
                if (i > 0) {
                    typeBodyScript.append(", ");
                }
                typeBodyScript.append("p").append(i + 1);
            }
            typeBodyScript.append(");\n");

            typeBodyScript.append("  RETURN;\n");
            typeBodyScript.append("END;\n");
        }

        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            Class<?> returnType = method.getReturnType();
            Class<?>[] parameterTypes = method.getParameterTypes();

            if (returnType != Void.TYPE) { // function
                typeBodyScript.append("\nMEMBER FUNCTION ").append(methodName);
                if (parameterTypes.length == 0) {
                    typeBodyScript.append(" RETURN ").append(stripSizeSpecifications(mapJavaToOracle(returnType))).append(" IS\n");
                } else {
                    typeBodyScript.append("(").append(mapMethodParameters(parameterTypes)).append(") RETURN ").append(stripSizeSpecifications(mapJavaToOracle(returnType))).append(" IS\n");
                }
                typeBodyScript.append("BEGIN\n");
                typeBodyScript.append("RETURN static_").append(methodName).append("(self.id");
                for (int i = 0; i < parameterTypes.length; i++) {
                    typeBodyScript.append(", p").append(i + 1);
                }
                typeBodyScript.append(");\n");
                typeBodyScript.append("END ").append(methodName).append(";\n");
            } else { // procedure
                typeBodyScript.append("\nMEMBER PROCEDURE ").append(methodName);
                if (parameterTypes.length == 0) {
                    typeBodyScript.append(" IS\n");
                } else {
                    typeBodyScript.append("(").append(mapMethodParameters(parameterTypes)).append(") IS\n");
                }
                typeBodyScript.append("BEGIN\n");
                typeBodyScript.append("static_").append(methodName).append("(self.id");
                for (int i = 0;i< parameterTypes.length; i++) {
                    typeBodyScript.append(", p").append(i + 1);
                }
                typeBodyScript.append(");\n");
                typeBodyScript.append("END ").append(methodName).append(";\n");
            }
        }

        typeBodyScript.append("END ").append(typeName).append(";");
        executeScript(typeBodyScript.toString(), statement); // Execute the TYPE BODY script
    }

    private static void generateConstructorSQLFunction(Class<?> clazz, Statement statement) {
        StringBuilder constructorFunctionScript = new StringBuilder();
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        if (constructors.length > 0) {
            Constructor<?> constructor = constructors[0];
            constructorFunctionScript.append("CREATE OR REPLACE FUNCTION create").append(clazz.getSimpleName()).append("(");
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                if (i > 0) {
                    constructorFunctionScript.append(", ");
                }
                constructorFunctionScript.append("p").append(i + 1).append(" IN ").append(stripSizeSpecifications(mapJavaToOracle(parameterTypes[i])));
            }
            constructorFunctionScript.append(") RETURN NUMBER AS LANGUAGE JAVA NAME '").append(clazz.getName()).append(".create").append(clazz.getSimpleName()).append("(");
            for (int i = 0; i < parameterTypes.length; i++) {
                if (i > 0) {
                    constructorFunctionScript.append(", ");
                }
                constructorFunctionScript.append(getJavaSignatureType(parameterTypes[i]));
            }
            constructorFunctionScript.append(") return int';");
            executeScript(constructorFunctionScript.toString(), statement); // Execute the constructor function script
        }
    }

    private static String getJavaSignatureType(Class<?> javaType) {
        if (javaType == String.class) {
            return "java.lang.String";
        } else if (javaType == int.class || javaType == Integer.class) {
            return "int";
        } else if (javaType == double.class || javaType == Double.class) {
            return "double";
        } else if (javaType == float.class || javaType == Float.class) {
            return "float";
        } else if (javaType == long.class || javaType == Long.class) {
            return "long";
        } else if (javaType == boolean.class || javaType == Boolean.class) {
            return "boolean";
        } else if (javaType == byte.class || javaType == Byte.class) {
            return "byte";
        } else if (javaType == short.class || javaType == Short.class) {
            return "short";
        } else if (javaType == char.class || javaType == Character.class) {
            return "char";
        } else if (javaType == BigDecimal.class) {
            return "java.math.BigDecimal";
        } else if (javaType == Blob.class) {
            return "java.sql.Blob";
        } else if (javaType == Clob.class) {
            return "java.sql.Clob";
        } else if (javaType == Timestamp.class) {
            return "java.sql.Timestamp";
        } else if (javaType == LocalDate.class) {
            return "java.time.LocalDate";
        } else if (javaType == byte[].class) {
            return "byte[]";
        } else {
            return "java.lang.Object";
        }
    }

    private static String mapConstructorParameters(Constructor<?> constructor) {
        StringBuilder parameters = new StringBuilder("(");
        Class<?>[] parameterTypes = constructor.getParameterTypes();

        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                parameters.append(", ");
            }
            parameters.append("p").append(i + 1).append(" IN ").append(stripSizeSpecifications(mapJavaToOracle(parameterTypes[i])));
        }
        parameters.append(")");
        return parameters.toString();
    }

    private static void executeScript(String script, Statement statement) {
        try {
            statement.execute(script);
            System.out.println("Script executed successfully:\n" + script);
        } catch (SQLException e) {
            throw new RuntimeException("Error executing script:\n" + script, e);
        }
    }


}
