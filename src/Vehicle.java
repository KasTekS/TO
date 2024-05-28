public class Vehicle {
    private int id;
    private String type;

    public Vehicle(int id, String type) {
        this.id = id;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void displayType() {
        System.out.println("Type: " + type);
    }
}
