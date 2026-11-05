import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class FarmerMarketServer {

    static final String FARMER_FILE = "farmers.txt";
    static final String PRODUCT_FILE = "products.txt";

    static String loggedInPhone = "";

    static String readForm(HttpExchange e) throws IOException {
        return new String(e.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    static String value(String data, String key) {
        for (String p : data.split("&")) {
            String[] a = p.split("=", 2);
            if (a.length == 2 && a[0].equals(key))
                return URLDecoder.decode(a[1], StandardCharsets.UTF_8);
        }
        return "";
    }

    static void save(String file, String text) throws IOException {
        Files.writeString(Path.of(file), text + "\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    static String read(String file) throws IOException {
        if (!Files.exists(Path.of(file)))
            return "<p>No information available.</p>";

        StringBuilder s = new StringBuilder();

        for (String line : Files.readAllLines(Path.of(file))) {
            s.append("<p>").append(line).append("</p>");
        }

        return s.toString();
    }

    static void respond(HttpExchange e, String html) throws IOException {
        byte[] b = html.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        e.sendResponseHeaders(200, b.length);

        try (OutputStream out = e.getResponseBody()) {
            out.write(b);
        }
    }

    static String page(String title, String body) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="UTF-8">
        <title>%s</title>
        <style>
        body{font-family:Arial;background:#e8f5e9;text-align:center;padding:25px}
        h1{color:#16803c}
        section{background:white;padding:20px;margin:20px auto;max-width:450px;border-radius:12px}
        input,button{padding:12px;margin:8px;width:80%%}
        button{background:green;color:white;border:0;cursor:pointer}
        a{color:green;margin:10px}
        </style>
        </head>
        <body>
        <h1>🌾 Local Farmer Market</h1>
        <nav>
        <a href="/">Home</a>
        <a href="/farmers">Registered Farmers</a>
        <a href="/products">Available Products</a>
        </nav>
        %s
        </body>
        </html>
        """.formatted(title, body);
    }

    public static void main(String[] args) throws Exception {

        int port = Integer.parseInt(
    System.getenv().getOrDefault("PORT", "8088")
);

HttpServer server = HttpServer.create(
    new InetSocketAddress(port), 0
);

        server.createContext("/", e -> {
            String body = """
            <section>
            <h2>Farmer Registration</h2>
            <form action="/register" method="post">
            <input name="name" placeholder="Farmer Name" required>
            <input name="phone" placeholder="Phone Number" required>
            <input name="password" type="password" placeholder="Enter Password" required>
            <input name="village" placeholder="Village" required>
            <button>Register Farmer</button>
            </form>
            </section>
            <section>
<h2>Farmer Login</h2>

<form action="/login" method="post">
<input name="Phone" placeholder="Phone Number" required>
<input name="password" type="password" placeholder="Enter Password" required>
<button>Login</button>
</form>

</section>
            <section>
            <h2>Add Product</h2>
            <form action="/add" method="post">
            <input name="farmer" placeholder="Farmer Name" required>
            <input name="product" placeholder="Product Name" required>
            <input name="price" type="number" step="0.01" placeholder="Price per Kg" required>
            <input name="quantity" type="number" step="0.01" placeholder="Quantity Kg" required>
            <button>Add Product</button>
            </form>
            </section>

            <section>
            <h2>Search Product</h2>
            <form action="/search" method="get">
            <input name="q" placeholder="Enter product name">
            <button>Search</button>
            </form>
            </section>
            """;

            respond(e, page("Home", body));
        });

        server.createContext("/register", e -> {
            String d = readForm(e);
            save(FARMER_FILE, "Name: " + value(d,"name")
        + " | Phone: " + value(d,"Phone")
        + " | Village: " + value(d,"village")
        + " | Password: " + value(d,"password"));

            respond(e, page("Registration",
                    "<h2>Registration Successful!</h2><a href='/'>Back Home</a>"));
        });
        server.createContext("/login", e -> {
    String d = readForm(e);

    String Phone = value(d, "Phone");
    String password = value(d, "password");

    boolean found = false;

    if (Files.exists(Path.of(FARMER_FILE))) {
        for (String line : Files.readAllLines(Path.of(FARMER_FILE))) {
            if (line.contains("Phone: " + Phone + " |")
                    && line.contains("Password: " + password)) {
                found = true;
                break;
            }
        }
    }

    if (found) {
        loggedInPhone = Phone;
        respond(e, page("Farmer Login",
                "<h2>Login Successful!</h2>"
                + "<p>Welcome to Farmer Market</p>"
                + "<a href='/'>Back Home</a>"));
    } else {
        respond(e, page("Login Failed",
                "<h2>Invalid Phone Number or Password</h2>"
                + "<a href='/'>Try Again</a>"));
    }
});

        server.createContext("/add", e -> {

            if (loggedInPhone.isEmpty()) {
    respond(e, page("Login Required",
            "<h2>Please Login First!</h2>"
            + "<a href='/'>Go to Login</a>"));
    return;
}
            String d = readForm(e);
            save(PRODUCT_FILE, "Farmer: " + value(d,"farmer")
                    + " | Product: " + value(d,"product")
                    + " | Price: ₹" + value(d,"price")
                    + "/Kg | Quantity: " + value(d,"quantity") + " Kg");

            respond(e, page("Product Added",
                    "<h2>Product Added Successfully!</h2><a href='/'>Back Home</a>"));
        });

        server.createContext("/farmers", e -> {
            respond(e, page("Registered Farmers", read(FARMER_FILE)));
        });

        server.createContext("/products", e -> {
            respond(e, page("Available Products", read(PRODUCT_FILE)));
        });

        server.createContext("/search", e -> {
            String q = value(e.getRequestURI().getRawQuery(), "q").toLowerCase();
            StringBuilder result = new StringBuilder();

            if (Files.exists(Path.of(PRODUCT_FILE))) {
                for (String line : Files.readAllLines(Path.of(PRODUCT_FILE))) {
                    if (line.toLowerCase().contains(q))
                        result.append("<p>").append(line).append("</p>");
                }
            }

            if (result.length() == 0)
                result.append("<p>Product not found.</p>");

            respond(e, page("Search Results", result.toString()));
        });

        server.start();

        System.out.println("Farmer Market Server Started!");
        System.out.println("Open http://localhost:8088");
    }
}