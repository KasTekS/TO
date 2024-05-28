import javassist.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;

public class StaticMethodEnhancer {

    public static void enhanceClass(String className, String dbUrl, String username, String password) {
        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass cc = pool.get(className);

            // Sprawdzanie czy klasa posiada pole "id"
            boolean hasIdField = false;
            for (Field field : Class.forName(className).getDeclaredFields()) {
                if (field.getName().equals("id") && field.getType() == int.class) {
                    hasIdField = true;
                    break;
                }
            }

            if (!hasIdField) {
                throw new IllegalArgumentException("Class " + className + " must have an int field named 'id'.");
            }

            // Generowanie mapy do przechowywania instancji obiektów
            CtClass mapClass = pool.get("java.util.concurrent.ConcurrentHashMap");
            CtField mapField = new CtField(mapClass, "instances", cc);
            mapField.setModifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL);
            cc.addField(mapField, CtField.Initializer.byExpr("null"));

            // Dodaj blok statyczny do inicjalizacji mapy
            CtConstructor staticConstructor = cc.makeClassInitializer();
            staticConstructor.setBody("{ instances = new java.util.concurrent.ConcurrentHashMap(); }");

            // Generowanie metod dodawania i pobierania obiektów
            addInstanceMethods(cc, pool, className);

            // Uzyskanie informacji o metodach za pomocą refleksji
            Method[] methods = Class.forName(className).getDeclaredMethods();
            for (Method method : methods) {
                String staticMethodName = "static_" + method.getName();
                CtClass returnType = pool.get(method.getReturnType().getName());
                Class<?>[] paramTypes = method.getParameterTypes();
                CtClass[] ctParamTypes = new CtClass[paramTypes.length + 1];
                ctParamTypes[0] = pool.get("int"); // Dodajemy ID jako pierwszy parametr

                for (int i = 0; i < paramTypes.length; i++) {
                    ctParamTypes[i + 1] = pool.get(paramTypes[i].getName());
                }

                CtMethod staticMethod = new CtMethod(returnType, staticMethodName, ctParamTypes, cc);
                staticMethod.setModifiers(Modifier.STATIC | Modifier.PUBLIC); // Zapewnienie, że metoda jest publiczna i statyczna

                StringBuilder methodBody = new StringBuilder("{");
                if (returnType != CtClass.voidType) {
                    methodBody.append("return ");
                }
                methodBody.append("((" + className + ")instances.get(Integer.valueOf($1)))." + method.getName() + "(");
                for (int i = 1; i < ctParamTypes.length; i++) {
                    methodBody.append("$").append(i + 1);
                    if (i < ctParamTypes.length - 1) {
                        methodBody.append(", ");
                    }
                }
                methodBody.append("); }");
                staticMethod.setBody(methodBody.toString());
                cc.addMethod(staticMethod);
            }

            // Uzyskanie informacji o konstruktorach za pomocą refleksji
            Constructor[] constructors = Class.forName(className).getConstructors();
            for (Constructor constructor : constructors) {
                String factoryMethodName = "create" + cc.getSimpleName();
                CtClass[] ctParamTypes = toCtClasses(constructor.getParameterTypes(), pool);
                CtMethod factoryMethod = new CtMethod(pool.get("int"), factoryMethodName, ctParamTypes, cc);
                factoryMethod.setModifiers(Modifier.STATIC | Modifier.PUBLIC); // Zapewnienie, że metoda jest publiczna i statyczna

                StringBuilder constructorBody = new StringBuilder("{");
                constructorBody.append(className).append(" instance = new ").append(className).append("(");
                for (int i = 0; i < ctParamTypes.length; i++) {
                    constructorBody.append("$").append(i + 1);
                    if (i < ctParamTypes.length - 1) constructorBody.append(", ");
                }
                constructorBody.append("); ");
                constructorBody.append("instances.put(Integer.valueOf(instance.getId()), instance);");
                constructorBody.append("return instance.getId(); }");  // Zwracanie ID obiektu
                factoryMethod.setBody(constructorBody.toString());
                cc.addMethod(factoryMethod);
            }

            // Zapisz zmodyfikowaną klasę do pliku
            String outputDir = "./out";
            cc.writeFile(outputDir);
            System.out.println("Class " + className + " has been enhanced and saved.");

            // Ładowanie klasy do bazy danych Oracle
            String classFilePath = Paths.get(outputDir, className + ".class").toString();
            loadClassToOracle(classFilePath, dbUrl, username, password);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static CtClass[] toCtClasses(Class<?>[] classes, ClassPool pool) throws NotFoundException {
        CtClass[] ctClasses = new CtClass[classes.length];
        for (int i = 0; i < classes.length; i++) {
            ctClasses[i] = pool.get(classes[i].getName());
        }
        return ctClasses;
    }

    private static void addInstanceMethods(CtClass cc, ClassPool pool, String className) throws CannotCompileException, NotFoundException {
        CtClass ctClass = pool.get(className);

        // Method to add an instance to the map
        CtMethod addMethod = new CtMethod(CtClass.voidType, "addInstance", new CtClass[]{CtClass.intType, ctClass}, cc);
        addMethod.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
        addMethod.setBody("{ instances.put(Integer.valueOf($1), $2); }");
        cc.addMethod(addMethod);

        // Method to get an instance from the map
        CtMethod getMethod = new CtMethod(ctClass, "getInstance", new CtClass[]{CtClass.intType}, cc);
        getMethod.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
        getMethod.setBody("{ return (" + className + ")instances.get(Integer.valueOf($1)); }");
        cc.addMethod(getMethod);
    }

    private static void loadClassToOracle(String classFilePath, String dbUrl, String username, String password) {
        try {
            // Dodaj klasę do polecenia loadjava
            String command = "cmd /c loadjava -user " + username + "/" + password + "@" + dbUrl + " -resolve -force \"" + classFilePath + "\"";
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            processOutput(process);

            if (exitCode == 0) {
                System.out.println("Class has been successfully loaded into Oracle database.");
            } else {
                System.out.println("An error occurred while loading the class into the Oracle database. Exit code: " + exitCode);
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

    public static void main(String[] args) {
        // Przykład użycia z danymi połączenia
        String dbUrl = "localhost:1521:xe";
        String username = "c##kamil";
        String password = "mazur";
        enhanceClass("Book", dbUrl, username, password);
    }
}
