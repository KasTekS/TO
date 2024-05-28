public class Book {

    // Private member variables
    private int id;
    private String title;
    private String author;
    private int publicationYear;
    private String genre;
    private double price;

    // Constructor
    public Book(int id, String title, String author, int publicationYear, String genre, double price) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.publicationYear = publicationYear;
        this.genre = genre;
        this.price = price;
    }

    // Getter and Setter methods
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    // Method to display book information
    public void displayInfo() {
        System.out.println("ID: " + id);
        System.out.println("Title: " + title);
        System.out.println("Author: " + author);
        System.out.println("Publication Year: " + publicationYear);
        System.out.println("Genre: " + genre);
        System.out.println("Price: $" + price);
    }

    // Method to calculate discounted price
    public double calculateDiscountedPrice(double discountRate) {
        return price - (price * discountRate / 100);
    }

    // Method to update book details
    public void updateDetails(String title, String author, int publicationYear, String genre, double price) {
        this.title = title;
        this.author = author;
        this.publicationYear = publicationYear;
        this.genre = genre;
        this.price = price;
    }
}
