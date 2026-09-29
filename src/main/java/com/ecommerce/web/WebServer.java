package com.ecommerce.web;

import com.ecommerce.entity.Category;
import com.ecommerce.entity.OrderDetails;
import com.ecommerce.entity.Orders;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Users;
import com.ecommerce.enumtype.UserRole;
import com.ecommerce.query.ProductQueryService;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.UsersService;
import com.ecommerce.util.HibernateUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Built-in lightweight HTTP Server serving a modern, interactive Web UI and REST API
 * for the Hibernate ORM E-Commerce Management System.
 * Operates using standard JDK HttpServer without requiring Spring Boot.
 */
public class WebServer {

    private static final int PORT = 8080;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final CategoryService categoryService = new CategoryService();
    private final ProductService productService = new ProductService();
    private final UsersService usersService = new UsersService();
    private final OrderService orderService = new OrderService();
    private final ProductQueryService queryService = new ProductQueryService();

    public static void main(String[] args) {
        new WebServer().start();
    }

    public void start() {
        try {
            seedInitialDataIfEmpty();

            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.setExecutor(Executors.newFixedThreadPool(10));

            // Static Web UI route
            server.createContext("/", new StaticPageHandler());

            // REST API routes
            server.createContext("/api/overview", new OverviewHandler());
            server.createContext("/api/categories", new CategoriesHandler());
            server.createContext("/api/products", new ProductsHandler());
            server.createContext("/api/users", new UsersHandler());
            server.createContext("/api/orders", new OrdersHandler());
            server.createContext("/api/order/create", new CreateOrderHandler());
            server.createContext("/api/queries/named", new NamedQueryHandler());
            server.createContext("/api/queries/criteria", new CriteriaQueryHandler());

            server.start();

            System.out.println("=================================================");
            System.out.println("  Hibernate E-Commerce Web Application Started! ");
            System.out.println("=================================================");
            System.out.printf("  Local Web UI URL: http://localhost:%d/%n", PORT);
            System.out.println("  Press Ctrl+C in this console to stop the server.");
            System.out.println("=================================================");

            // Try to open the browser automatically
            openBrowser("http://localhost:" + PORT + "/");

        } catch (Exception e) {
            System.err.println("Failed to start web server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void seedInitialDataIfEmpty() {
        if (categoryService.getAllCategories().isEmpty()) {
            System.out.println("Database is empty. Seeding initial demonstration data...");
            Category electronics = categoryService.createCategory("Electronics", "Electronic devices and accessories");
            Category clothing = categoryService.createCategory("Clothing", "Apparel and wearables");
            Category books = categoryService.createCategory("Books", "Printed and digital literature");

            Product laptop = productService.createProduct("Laptop", new BigDecimal("50000.00"), 10, electronics.getId());
            Product smartphone = productService.createProduct("Smartphone", new BigDecimal("25000.00"), 15, electronics.getId());
            Product headphones = productService.createProduct("Headphones", new BigDecimal("2000.00"), 30, electronics.getId());
            productService.createProduct("T-Shirt", new BigDecimal("650.00"), 50, clothing.getId());
            Product javaBook = productService.createProduct("Java Programming Book", new BigDecimal("800.00"), 20, books.getId());

            usersService.createUser("admin", "AdminPass@2026", "admin@ecommerce.com", UserRole.ADMIN);
            Users customer = usersService.createUser("customer1", "SecretPass123", "customer1@example.com", UserRole.CUSTOMER);

            // Create initial order
            List<OrderService.OrderItemRequest> items = Arrays.asList(
                    new OrderService.OrderItemRequest(laptop.getId(), 1),
                    new OrderService.OrderItemRequest(headphones.getId(), 2),
                    new OrderService.OrderItemRequest(javaBook.getId(), 1)
            );
            orderService.createOrder(customer.getId(), items);
            System.out.println("Initial demonstration data seeded successfully.");
        }
    }

    private static void openBrowser(String url) {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            }
        } catch (Exception ignored) {
        }
    }

    // Handlers
    private class OverviewHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            List<Orders> orders = orderService.getAllOrders();
            BigDecimal revenue = orders.stream()
                    .map(Orders::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            JsonObject json = new JsonObject();
            json.addProperty("categoriesCount", categoryService.getAllCategories().size());
            json.addProperty("productsCount", productService.getAllActiveProducts().size());
            json.addProperty("usersCount", usersService.getAllUsers().size());
            json.addProperty("ordersCount", orders.size());
            json.addProperty("totalRevenue", revenue.toPlainString());

            sendJson(exchange, 200, json.toString());
        }
    }

    private class CategoriesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            List<Category> categories = categoryService.getAllCategories();
            JsonArray arr = new JsonArray();
            for (Category c : categories) {
                JsonObject o = new JsonObject();
                o.addProperty("id", c.getId());
                o.addProperty("name", c.getName());
                o.addProperty("description", c.getDescription() != null ? c.getDescription() : "");
                arr.add(o);
            }
            sendJson(exchange, 200, arr.toString());
        }
    }

    private class ProductsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            List<Product> products = productService.getAllActiveProducts();
            JsonArray arr = new JsonArray();
            for (Product p : products) {
                JsonObject o = new JsonObject();
                o.addProperty("id", p.getId());
                o.addProperty("name", p.getName());
                o.addProperty("price", p.getPrice().toPlainString());
                o.addProperty("stockQuantity", p.getStockQuantity());
                o.addProperty("categoryId", p.getCategory().getId());
                o.addProperty("categoryName", p.getCategory().getName());
                arr.add(o);
            }
            sendJson(exchange, 200, arr.toString());
        }
    }

    private class UsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            List<Users> users = usersService.getAllUsers();
            JsonArray arr = new JsonArray();
            for (Users u : users) {
                JsonObject o = new JsonObject();
                o.addProperty("id", u.getId());
                o.addProperty("username", u.getUsername());
                o.addProperty("email", u.getEmail());
                o.addProperty("role", u.getRole().name());
                o.addProperty("passwordHash", u.getPassword());
                arr.add(o);
            }
            sendJson(exchange, 200, arr.toString());
        }
    }

    private class OrdersHandler implements HttpHandler {
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length >= 4) {
                // GET /api/orders/{id}
                try {
                    Long id = Long.parseLong(parts[3]);
                    Orders order = orderService.getOrderWithDetails(id);
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", order.getId());
                    obj.addProperty("orderDate", order.getOrderDate().format(fmt));
                    obj.addProperty("totalAmount", order.getTotalAmount().toPlainString());
                    obj.addProperty("customer", order.getUser().getUsername());
                    obj.addProperty("email", order.getUser().getEmail());

                    JsonArray detailsArr = new JsonArray();
                    for (OrderDetails d : order.getOrderDetails()) {
                        JsonObject dObj = new JsonObject();
                        dObj.addProperty("productId", d.getProduct().getId());
                        dObj.addProperty("productName", d.getProduct().getName());
                        dObj.addProperty("quantity", d.getQuantity());
                        dObj.addProperty("unitPrice", d.getUnitPrice().toPlainString());
                        dObj.addProperty("subtotal", d.getSubtotal().toPlainString());
                        detailsArr.add(dObj);
                    }
                    obj.add("items", detailsArr);
                    sendJson(exchange, 200, obj.toString());
                    return;
                } catch (Exception e) {
                    sendJson(exchange, 404, "{\"error\": \"Order not found\"}");
                    return;
                }
            }

            // List orders
            List<Orders> orders = orderService.getAllOrders();
            JsonArray arr = new JsonArray();
            for (Orders o : orders) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", o.getId());
                obj.addProperty("orderDate", o.getOrderDate().format(fmt));
                obj.addProperty("totalAmount", o.getTotalAmount().toPlainString());
                obj.addProperty("customer", o.getUser() != null ? o.getUser().getUsername() : "N/A");
                arr.add(obj);
            }
            sendJson(exchange, 200, arr.toString());
        }
    }

    private class CreateOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\": \"Method not allowed\"}");
                return;
            }

            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject req = JsonParser.parseString(body).getAsJsonObject();
                Long userId = req.get("userId").getAsLong();
                JsonArray itemsArr = req.getAsJsonArray("items");

                List<OrderService.OrderItemRequest> items = new ArrayList<>();
                for (JsonElement el : itemsArr) {
                    JsonObject itemObj = el.getAsJsonObject();
                    Long pId = itemObj.get("productId").getAsLong();
                    int qty = itemObj.get("quantity").getAsInt();
                    items.add(new OrderService.OrderItemRequest(pId, qty));
                }

                Orders created = orderService.createOrder(userId, items);
                Orders fetched = orderService.getOrderWithDetails(created.getId());

                JsonObject resp = new JsonObject();
                resp.addProperty("success", true);
                resp.addProperty("orderId", fetched.getId());
                resp.addProperty("totalAmount", fetched.getTotalAmount().toPlainString());
                resp.addProperty("customer", fetched.getUser().getUsername());
                resp.addProperty("date", fetched.getOrderDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

                sendJson(exchange, 201, resp.toString());
            } catch (Exception e) {
                JsonObject err = new JsonObject();
                err.addProperty("success", false);
                err.addProperty("error", e.getMessage());
                sendJson(exchange, 400, err.toString());
            }
        }
    }

    private class NamedQueryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            Map<String, String> params = parseQueryParams(exchange.getRequestURI());
            String catIdStr = params.get("categoryId");
            Long catId = catIdStr != null ? Long.parseLong(catIdStr) : 1L;

            List<Product> products = queryService.findProductsByCategoryNamedQuery(catId);
            JsonArray arr = new JsonArray();
            for (Product p : products) {
                JsonObject o = new JsonObject();
                o.addProperty("id", p.getId());
                o.addProperty("name", p.getName());
                o.addProperty("price", p.getPrice().toPlainString());
                o.addProperty("stockQuantity", p.getStockQuantity());
                arr.add(o);
            }
            sendJson(exchange, 200, arr.toString());
        }
    }

    private class CriteriaQueryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCors(exchange);
                return;
            }
            Map<String, String> params = parseQueryParams(exchange.getRequestURI());
            String priceStr = params.getOrDefault("minPrice", "1000.00");
            BigDecimal minPrice = new BigDecimal(priceStr);

            List<Product> products = queryService.findAvailableProductsAbovePrice(minPrice);
            JsonArray arr = new JsonArray();
            for (Product p : products) {
                JsonObject o = new JsonObject();
                o.addProperty("id", p.getId());
                o.addProperty("name", p.getName());
                o.addProperty("price", p.getPrice().toPlainString());
                o.addProperty("stockQuantity", p.getStockQuantity());
                arr.add(o);
            }
            sendJson(exchange, 200, arr.toString());
        }
    }

    private static Map<String, String> parseQueryParams(URI uri) {
        Map<String, String> map = new HashMap<>();
        String query = uri.getQuery();
        if (query == null) return map;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                map.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private static void sendCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static class StaticPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = getHtmlContent();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String getHtmlContent() {
            return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hibernate E-Commerce Management System</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #4f46e5;
            --primary-hover: #4338ca;
            --secondary: #0ea5e9;
            --bg: #f8fafc;
            --card-bg: #ffffff;
            --text-main: #0f172a;
            --text-muted: #64748b;
            --border: #e2e8f0;
            --success: #10b981;
            --warning: #f59e0b;
            --danger: #ef4444;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: 'Inter', sans-serif;
            background: var(--bg);
            color: var(--text-main);
            line-height: 1.5;
        }
        header {
            background: #ffffff;
            border-bottom: 1px solid var(--border);
            padding: 1rem 2rem;
            display: flex;
            justify-content: space-between;
            align-items: center;
            position: sticky;
            top: 0;
            z-index: 100;
        }
        .brand {
            display: flex;
            align-items: center;
            gap: 0.75rem;
        }
        .brand-badge {
            background: linear-gradient(135deg, #4f46e5, #0ea5e9);
            color: #fff;
            font-size: 1.1rem;
            font-weight: 700;
            padding: 0.4rem 0.8rem;
            border-radius: 8px;
        }
        .brand-title {
            font-size: 1.25rem;
            font-weight: 700;
            color: var(--text-main);
        }
        .nav-links {
            display: flex;
            gap: 0.5rem;
        }
        .nav-btn {
            background: none;
            border: 1px solid transparent;
            padding: 0.5rem 1rem;
            border-radius: 6px;
            font-weight: 500;
            cursor: pointer;
            color: var(--text-muted);
            transition: all 0.2s;
        }
        .nav-btn:hover {
            color: var(--primary);
            background: #eef2ff;
        }
        .nav-btn.active {
            color: var(--primary);
            background: #e0e7ff;
            border-color: #c7d2fe;
            font-weight: 600;
        }
        .container {
            max-width: 1200px;
            margin: 2rem auto;
            padding: 0 1.5rem;
        }
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 1.25rem;
            margin-bottom: 2rem;
        }
        .stat-card {
            background: var(--card-bg);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 1.25rem;
            box-shadow: 0 1px 3px rgba(0,0,0,0.05);
        }
        .stat-title {
            font-size: 0.875rem;
            color: var(--text-muted);
            font-weight: 500;
            text-transform: uppercase;
        }
        .stat-value {
            font-size: 1.875rem;
            font-weight: 700;
            color: var(--text-main);
            margin-top: 0.25rem;
        }
        .tab-content {
            display: none;
        }
        .tab-content.active {
            display: block;
        }
        .card {
            background: var(--card-bg);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 1.5rem;
            margin-bottom: 1.5rem;
            box-shadow: 0 1px 3px rgba(0,0,0,0.05);
        }
        .card-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 1.25rem;
            padding-bottom: 0.75rem;
            border-bottom: 1px solid var(--border);
        }
        .card-title {
            font-size: 1.25rem;
            font-weight: 600;
        }
        .products-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
            gap: 1.5rem;
        }
        .product-card {
            background: #fff;
            border: 1px solid var(--border);
            border-radius: 10px;
            padding: 1.25rem;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            transition: transform 0.2s, box-shadow 0.2s;
        }
        .product-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 16px rgba(0,0,0,0.06);
        }
        .product-category {
            font-size: 0.75rem;
            font-weight: 600;
            color: var(--primary);
            text-transform: uppercase;
            background: #e0e7ff;
            display: inline-block;
            padding: 0.2rem 0.5rem;
            border-radius: 4px;
            margin-bottom: 0.5rem;
        }
        .product-name {
            font-size: 1.1rem;
            font-weight: 600;
            margin-bottom: 0.5rem;
        }
        .product-meta {
            display: flex;
            justify-content: space-between;
            align-items: baseline;
            margin: 1rem 0;
        }
        .product-price {
            font-size: 1.35rem;
            font-weight: 700;
            color: var(--text-main);
        }
        .stock-badge {
            font-size: 0.85rem;
            padding: 0.2rem 0.5rem;
            border-radius: 6px;
            font-weight: 600;
        }
        .in-stock { background: #dcfce7; color: #15803d; }
        .low-stock { background: #fef9c3; color: #854d0e; }
        .out-of-stock { background: #fee2e2; color: #b91c1c; }
        .btn {
            background: var(--primary);
            color: #fff;
            border: none;
            padding: 0.6rem 1.2rem;
            border-radius: 6px;
            font-weight: 600;
            cursor: pointer;
            transition: background 0.2s;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
        }
        .btn:hover { background: var(--primary-hover); }
        .btn-sm { padding: 0.4rem 0.8rem; font-size: 0.875rem; }
        .btn-outline {
            background: transparent;
            border: 1px solid var(--border);
            color: var(--text-main);
        }
        .btn-outline:hover { background: #f1f5f9; }
        table {
            width: 100%;
            border-collapse: collapse;
            text-align: left;
        }
        th, td {
            padding: 0.75rem 1rem;
            border-bottom: 1px solid var(--border);
        }
        th {
            background: #f8fafc;
            font-size: 0.85rem;
            text-transform: uppercase;
            color: var(--text-muted);
            font-weight: 600;
        }
        tr:hover { background: #f8fafc; }
        .badge {
            padding: 0.25rem 0.6rem;
            border-radius: 9999px;
            font-size: 0.75rem;
            font-weight: 600;
        }
        .badge-admin { background: #ede9fe; color: #6d28d9; }
        .badge-customer { background: #e0f2fe; color: #0369a1; }
        .modal-overlay {
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(15, 23, 42, 0.6);
            display: none;
            justify-content: center;
            align-items: center;
            z-index: 1000;
        }
        .modal {
            background: #fff;
            border-radius: 12px;
            width: 90%;
            max-width: 550px;
            max-height: 90vh;
            overflow-y: auto;
            padding: 1.5rem;
            box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
        }
        .receipt-box {
            background: #f8fafc;
            border: 1px dashed var(--border);
            border-radius: 8px;
            padding: 1rem;
            font-family: monospace;
            margin: 1rem 0;
        }
        .form-group {
            margin-bottom: 1rem;
        }
        .form-group label {
            display: block;
            font-size: 0.875rem;
            font-weight: 600;
            margin-bottom: 0.35rem;
        }
        .form-control {
            width: 100%;
            padding: 0.6rem;
            border: 1px solid var(--border);
            border-radius: 6px;
            font-family: inherit;
        }
    </style>
</head>
<body>

<header>
    <div class="brand">
        <div class="brand-badge">H</div>
        <div>
            <div class="brand-title">Hibernate E-Commerce</div>
            <div style="font-size: 0.75rem; color: var(--text-muted);">JPA 3.1 &bull; Hibernate ORM 6.5 &bull; Transactions &bull; BCrypt</div>
        </div>
    </div>
    <nav class="nav-links">
        <button class="nav-btn active" onclick="switchTab('storefront')">Storefront</button>
        <button class="nav-btn" onclick="switchTab('orders')">Orders</button>
        <button class="nav-btn" onclick="switchTab('queries')">Hibernate Query Lab</button>
        <button class="nav-btn" onclick="switchTab('users')">Users & Security</button>
        <button class="nav-btn" onclick="switchTab('categories')">Categories</button>
    </nav>
</header>

<div class="container">
    <!-- Live Stats Grid -->
    <div class="stats-grid">
        <div class="stat-card">
            <div class="stat-title">Categories</div>
            <div class="stat-value" id="stat-categories">-</div>
        </div>
        <div class="stat-card">
            <div class="stat-title">Active Products</div>
            <div class="stat-value" id="stat-products">-</div>
        </div>
        <div class="stat-card">
            <div class="stat-title">Total Orders</div>
            <div class="stat-value" id="stat-orders">-</div>
        </div>
        <div class="stat-card">
            <div class="stat-title">Total Revenue</div>
            <div class="stat-value" id="stat-revenue">-</div>
        </div>
    </div>

    <!-- Storefront Tab -->
    <div id="tab-storefront" class="tab-content active">
        <div class="card">
            <div class="card-header">
                <div class="card-title">Product Catalog</div>
                <button class="btn btn-sm" onclick="openOrderModal()">+ Create Order</button>
            </div>
            <div class="products-grid" id="products-list">
                <!-- Loaded dynamically -->
            </div>
        </div>
    </div>

    <!-- Orders Tab -->
    <div id="tab-orders" class="tab-content">
        <div class="card">
            <div class="card-header">
                <div class="card-title">Customer Orders</div>
                <button class="btn btn-sm" onclick="openOrderModal()">+ New Order</button>
            </div>
            <table>
                <thead>
                    <tr>
                        <th>Order ID</th>
                        <th>Customer</th>
                        <th>Date</th>
                        <th>Total Amount</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody id="orders-table-body">
                    <!-- Loaded dynamically -->
                </tbody>
            </table>
        </div>
    </div>

    <!-- Hibernate Query Lab Tab -->
    <div id="tab-queries" class="tab-content">
        <div class="card">
            <div class="card-header">
                <div class="card-title">JPQL @NamedQuery &bull; Product.findByCategory</div>
            </div>
            <p style="color: var(--text-muted); margin-bottom: 1rem; font-size: 0.9rem;">
                Executes the predefined JPQL named query: <code>SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.deleted = false</code>
            </p>
            <div style="display: flex; gap: 1rem; margin-bottom: 1rem; align-items: center;">
                <label for="named-cat-select" style="font-weight: 600; font-size: 0.9rem;">Category:</label>
                <select id="named-cat-select" class="form-control" style="max-width: 250px;">
                    <!-- Categories -->
                </select>
                <button class="btn btn-sm" onclick="runNamedQuery()">Execute NamedQuery</button>
            </div>
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Product</th>
                        <th>Price</th>
                        <th>Stock</th>
                    </tr>
                </thead>
                <tbody id="named-query-body">
                    <tr><td colspan="4" style="text-align:center; color: var(--text-muted);">Select a category and click execute.</td></tr>
                </tbody>
            </table>
        </div>

        <div class="card">
            <div class="card-header">
                <div class="card-title">JPA CriteriaBuilder API &bull; Dynamic Filtering</div>
            </div>
            <p style="color: var(--text-muted); margin-bottom: 1rem; font-size: 0.9rem;">
                Executes dynamic type-safe Criteria Query: <code>price > :minPrice AND stockQuantity > 0 AND deleted = false</code>
            </p>
            <div style="display: flex; gap: 1rem; margin-bottom: 1rem; align-items: center;">
                <label for="min-price-input" style="font-weight: 600; font-size: 0.9rem;">Min Price (&#8377;):</label>
                <input type="number" id="min-price-input" class="form-control" value="1000" style="max-width: 150px;">
                <button class="btn btn-sm" onclick="runCriteriaQuery()">Execute CriteriaQuery</button>
            </div>
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Product</th>
                        <th>Price</th>
                        <th>Stock</th>
                    </tr>
                </thead>
                <tbody id="criteria-query-body">
                    <tr><td colspan="4" style="text-align:center; color: var(--text-muted);">Enter minimum price and click execute.</td></tr>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Users Tab -->
    <div id="tab-users" class="tab-content">
        <div class="card">
            <div class="card-header">
                <div class="card-title">Users &amp; BCrypt Password Security</div>
            </div>
            <p style="color: var(--text-muted); margin-bottom: 1rem; font-size: 0.9rem;">
                All passwords below are secured using 12-round BCrypt salt hashing before database persistence. Plaintext passwords are never saved.
            </p>
            <table>
                <thead>
                    <tr>
                        <th>User ID</th>
                        <th>Username</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Stored BCrypt Hash</th>
                    </tr>
                </thead>
                <tbody id="users-table-body">
                    <!-- Loaded dynamically -->
                </tbody>
            </table>
        </div>
    </div>

    <!-- Categories Tab -->
    <div id="tab-categories" class="tab-content">
        <div class="card">
            <div class="card-header">
                <div class="card-title">Product Categories</div>
            </div>
            <table>
                <thead>
                    <tr>
                        <th>Category ID</th>
                        <th>Name</th>
                        <th>Description</th>
                    </tr>
                </thead>
                <tbody id="categories-table-body">
                    <!-- Loaded dynamically -->
                </tbody>
            </table>
        </div>
    </div>
</div>

<!-- Create Order Modal -->
<div id="order-modal" class="modal-overlay">
    <div class="modal">
        <div class="card-header">
            <div class="card-title">Place E-Commerce Order</div>
            <button class="btn btn-sm btn-outline" onclick="closeOrderModal()">&times;</button>
        </div>
        <div class="form-group">
            <label for="order-user-select">Customer:</label>
            <select id="order-user-select" class="form-control">
                <!-- Users -->
            </select>
        </div>
        <div class="form-group">
            <label for="order-prod-select">Product:</label>
            <select id="order-prod-select" class="form-control">
                <!-- Products -->
            </select>
        </div>
        <div class="form-group">
            <label for="order-qty-input">Quantity:</label>
            <input type="number" id="order-qty-input" class="form-control" value="1" min="1">
        </div>
        <div id="order-error" style="color: var(--danger); font-size: 0.85rem; margin-bottom: 1rem; display: none;"></div>
        <div style="display: flex; justify-content: flex-end; gap: 0.5rem;">
            <button class="btn btn-outline" onclick="closeOrderModal()">Cancel</button>
            <button class="btn" onclick="submitOrder()">Submit Order</button>
        </div>
    </div>
</div>

<!-- Receipt Modal -->
<div id="receipt-modal" class="modal-overlay">
    <div class="modal">
        <div class="card-header">
            <div class="card-title">Order Details &bull; JOIN FETCH Graph</div>
            <button class="btn btn-sm btn-outline" onclick="closeReceiptModal()">&times;</button>
        </div>
        <div class="receipt-box" id="receipt-content">
            <!-- Receipt content -->
        </div>
        <div style="display: flex; justify-content: flex-end;">
            <button class="btn btn-sm" onclick="closeReceiptModal()">Close</button>
        </div>
    </div>
</div>

<script>
    let globalProducts = [];
    let globalCategories = [];
    let globalUsers = [];

    function switchTab(tabId) {
        document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
        document.querySelectorAll('.nav-btn').forEach(el => el.classList.remove('active'));
        document.getElementById('tab-' + tabId).classList.add('active');
        event.target.classList.add('active');
    }

    async function loadData() {
        try {
            // Load overview
            const ov = await fetch('/api/overview').then(r => r.json());
            document.getElementById('stat-categories').textContent = ov.categoriesCount;
            document.getElementById('stat-products').textContent = ov.productsCount;
            document.getElementById('stat-orders').textContent = ov.ordersCount;
            document.getElementById('stat-revenue').textContent = '₹' + ov.totalRevenue;

            // Load products
            globalProducts = await fetch('/api/products').then(r => r.json());
            renderProducts(globalProducts);

            // Load categories
            globalCategories = await fetch('/api/categories').then(r => r.json());
            renderCategories(globalCategories);

            // Load users
            globalUsers = await fetch('/api/users').then(r => r.json());
            renderUsers(globalUsers);

            // Load orders
            const orders = await fetch('/api/orders').then(r => r.json());
            renderOrders(orders);

            populateDropdowns();
        } catch (e) {
            console.error(e);
        }
    }

    function renderProducts(products) {
        const grid = document.getElementById('products-list');
        grid.innerHTML = '';
        products.forEach(p => {
            const stockClass = p.stockQuantity > 10 ? 'in-stock' : (p.stockQuantity > 0 ? 'low-stock' : 'out-of-stock');
            const stockText = p.stockQuantity > 0 ? `${p.stockQuantity} in stock` : 'Out of stock';

            const card = document.createElement('div');
            card.className = 'product-card';
            card.innerHTML = `
                <div>
                    <span class="product-category">${p.categoryName}</span>
                    <div class="product-name">${p.name}</div>
                </div>
                <div>
                    <div class="product-meta">
                        <div class="product-price">&#8377;${p.price}</div>
                        <span class="stock-badge ${stockClass}">${stockText}</span>
                    </div>
                    <button class="btn btn-sm" style="width: 100%;" onclick="quickOrder(${p.id})">Order Product</button>
                </div>
            `;
            grid.appendChild(card);
        });
    }

    function renderCategories(categories) {
        const tbody = document.getElementById('categories-table-body');
        tbody.innerHTML = '';
        categories.forEach(c => {
            tbody.innerHTML += `
                <tr>
                    <td><strong>#${c.id}</strong></td>
                    <td>${c.name}</td>
                    <td>${c.description}</td>
                </tr>
            `;
        });
    }

    function renderUsers(users) {
        const tbody = document.getElementById('users-table-body');
        tbody.innerHTML = '';
        users.forEach(u => {
            const badgeClass = u.role === 'ADMIN' ? 'badge-admin' : 'badge-customer';
            tbody.innerHTML += `
                <tr>
                    <td>#${u.id}</td>
                    <td><strong>${u.username}</strong></td>
                    <td>${u.email}</td>
                    <td><span class="badge ${badgeClass}">${u.role}</span></td>
                    <td><code style="font-size: 0.75rem;">${u.passwordHash}</code></td>
                </tr>
            `;
        });
    }

    function renderOrders(orders) {
        const tbody = document.getElementById('orders-table-body');
        tbody.innerHTML = '';
        orders.forEach(o => {
            tbody.innerHTML += `
                <tr>
                    <td><strong>Order #${o.id}</strong></td>
                    <td>${o.customer}</td>
                    <td>${o.orderDate}</td>
                    <td><strong>&#8377;${o.totalAmount}</strong></td>
                    <td>
                        <button class="btn btn-sm btn-outline" onclick="viewOrderDetails(${o.id})">View Graph</button>
                    </td>
                </tr>
            `;
        });
    }

    function populateDropdowns() {
        const catSelect = document.getElementById('named-cat-select');
        catSelect.innerHTML = '';
        globalCategories.forEach(c => {
            catSelect.innerHTML += `<option value="${c.id}">${c.name}</option>`;
        });

        const userSelect = document.getElementById('order-user-select');
        userSelect.innerHTML = '';
        globalUsers.forEach(u => {
            userSelect.innerHTML += `<option value="${u.id}">${u.username} (${u.role})</option>`;
        });

        const prodSelect = document.getElementById('order-prod-select');
        prodSelect.innerHTML = '';
        globalProducts.forEach(p => {
            prodSelect.innerHTML += `<option value="${p.id}">${p.name} (&#8377;${p.price}, Stock: ${p.stockQuantity})</option>`;
        });
    }

    async function viewOrderDetails(orderId) {
        try {
            const res = await fetch('/api/orders/' + orderId);
            const data = await res.json();
            const box = document.getElementById('receipt-content');
            box.innerHTML = `
========== ORDER RECEIPT ==========
Order ID:   #${data.id}
Customer:   ${data.customer} (${data.email})
Date:       ${data.orderDate}

Products:
-----------------------------------
${data.items.map(it => `${it.productName}\n  Quantity: ${it.quantity} x &#8377;${it.unitPrice} = &#8377;${it.subtotal}`).join('\n\n')}
-----------------------------------
Total Amount: &#8377;${data.totalAmount}
===================================
            `;
            document.getElementById('receipt-modal').style.display = 'flex';
        } catch (e) {
            alert('Failed to load order details');
        }
    }

    async function runNamedQuery() {
        const catId = document.getElementById('named-cat-select').value;
        const res = await fetch('/api/queries/named?categoryId=' + catId);
        const products = await res.json();
        const tbody = document.getElementById('named-query-body');
        tbody.innerHTML = '';
        if (products.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">No products found in this category.</td></tr>';
            return;
        }
        products.forEach(p => {
            tbody.innerHTML += `
                <tr>
                    <td>#${p.id}</td>
                    <td><strong>${p.name}</strong></td>
                    <td>&#8377;${p.price}</td>
                    <td>${p.stockQuantity}</td>
                </tr>
            `;
        });
    }

    async function runCriteriaQuery() {
        const minPrice = document.getElementById('min-price-input').value;
        const res = await fetch('/api/queries/criteria?minPrice=' + minPrice);
        const products = await res.json();
        const tbody = document.getElementById('criteria-query-body');
        tbody.innerHTML = '';
        if (products.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">No available products found above this price.</td></tr>';
            return;
        }
        products.forEach(p => {
            tbody.innerHTML += `
                <tr>
                    <td>#${p.id}</td>
                    <td><strong>${p.name}</strong></td>
                    <td>&#8377;${p.price}</td>
                    <td>${p.stockQuantity}</td>
                </tr>
            `;
        });
    }

    function quickOrder(productId) {
        document.getElementById('order-prod-select').value = productId;
        openOrderModal();
    }

    function openOrderModal() {
        document.getElementById('order-error').style.display = 'none';
        document.getElementById('order-modal').style.display = 'flex';
    }

    function closeOrderModal() {
        document.getElementById('order-modal').style.display = 'none';
    }

    function closeReceiptModal() {
        document.getElementById('receipt-modal').style.display = 'none';
    }

    async function submitOrder() {
        const userId = document.getElementById('order-user-select').value;
        const productId = document.getElementById('order-prod-select').value;
        const quantity = parseInt(document.getElementById('order-qty-input').value, 10);

        const payload = {
            userId: parseInt(userId, 10),
            items: [{ productId: parseInt(productId, 10), quantity: quantity }]
        };

        const errorDiv = document.getElementById('order-error');
        errorDiv.style.display = 'none';

        try {
            const res = await fetch('/api/order/create', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (data.success) {
                closeOrderModal();
                await loadData();
                viewOrderDetails(data.orderId);
            } else {
                errorDiv.textContent = data.error || 'Failed to place order';
                errorDiv.style.display = 'block';
            }
        } catch (e) {
            errorDiv.textContent = 'Network or server error';
            errorDiv.style.display = 'block';
        }
    }

    // Initial fetch on page load
    loadData();
</script>
</body>
</html>
""";
        }
    }
}
