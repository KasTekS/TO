import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@interface FieldName {
    String value();
}

class Vehicle {
    private String type;

    public Vehicle( String type) {
        this.type = type;
    }

    public void displayType() {
        System.out.println("Type: " + type);
    }
}

public class Car extends Vehicle {
    private String brand;
    private String model;
    private int year;
    private double price;

    public Car(@FieldName("brand") String brand, @FieldName("model") String model, @FieldName("year") int year, @FieldName("price") double price) {
        super("Car");
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.price = price;
    }

    public void displayInfo() {
        super.displayType();
        System.out.println("Brand: " + brand);
        System.out.println("Model: " + model);
        System.out.println("Year: " + year);
        System.out.println("Price: $" + price);
    }
}

class SportsCar extends Car {
    private boolean turbo;


    public SportsCar(@FieldName("brand") String brand, @FieldName("model") String model, @FieldName("year") int year,
                     @FieldName("price") double price, @FieldName("turbo") boolean turbo) {
        super(brand, model, year, price);
        this.turbo = turbo;
    }

    public void displayTurbo() {
        System.out.println("Turbocharged: " + turbo);
    }
}
