package com.ecommerce;

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

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Main application class running a comprehensive console demonstration of the
 * Hibernate ORM E-Commerce Management System.
 */
public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && (args[0].equalsIgnoreCase("web") || args[0].equalsIgnoreCase("--web"))) {
            com.ecommerce.web.WebServer.main(args);
            return;
        }

        System.out.println("========================================");
        System.out.println("   Hibernate E-Commerce Application    ");
        System.out.println("========================================");

        CategoryService categoryService = new CategoryService();
        ProductService productService = new ProductService();
        UsersService usersService = new UsersService();
        OrderService orderService = new OrderService();
        ProductQueryService queryService = new ProductQueryService();

        try {
            // Step 1: Create Categories
            System.out.println("\n1. Creating categories...");
            Category electronics = categoryService.createCategory("Electronics", "Electronic devices and accessories");
            Category clothing = categoryService.createCategory("Clothing", "Apparel, garments, and wearables");
            Category books = categoryService.createCategory("Books", "Printed and digital literature");
            System.out.printf("   Created: %s (ID: %d), %s (ID: %d), %s (ID: %d)%n",
                    electronics.getName(), electronics.getId(),
                    clothing.getName(), clothing.getId(),
                    books.getName(), books.getId());

            // Step 2: Create Products
            System.out.println("\n2. Creating products...");
            Product laptop = productService.createProduct("Laptop", new BigDecimal("50000.00"), 10, electronics.getId());
            Product smartphone = productService.createProduct("Smartphone", new BigDecimal("25000.00"), 15, electronics.getId());
            Product headphones = productService.createProduct("Headphones", new BigDecimal("2000.00"), 30, electronics.getId());
            Product tshirt = productService.createProduct("T-Shirt", new BigDecimal("650.00"), 50, clothing.getId());
            Product javaBook = productService.createProduct("Java Programming Book", new BigDecimal("800.00"), 20, books.getId());
            System.out.printf("   Created: %s (Stock: %d, ₹%s)%n", laptop.getName(), laptop.getStockQuantity(), laptop.getPrice());
            System.out.printf("   Created: %s (Stock: %d, ₹%s)%n", smartphone.getName(), smartphone.getStockQuantity(), smartphone.getPrice());
            System.out.printf("   Created: %s (Stock: %d, ₹%s)%n", headphones.getName(), headphones.getStockQuantity(), headphones.getPrice());
            System.out.printf("   Created: %s (Stock: %d, ₹%s)%n", tshirt.getName(), tshirt.getStockQuantity(), tshirt.getPrice());
            System.out.printf("   Created: %s (Stock: %d, ₹%s)%n", javaBook.getName(), javaBook.getStockQuantity(), javaBook.getPrice());

            // Step 3: Create Users with BCrypt Hashed Passwords
            System.out.println("\n3. Creating users with BCrypt password hashing...");
            Users admin = usersService.createUser("admin", "AdminPass@2026", "admin@ecommerce.com", UserRole.ADMIN);
            Users customer = usersService.createUser("customer1", "SecretPass123", "customer1@example.com", UserRole.CUSTOMER);
            System.out.printf("   Created User: %s [Role: %s, Hash: %s]%n",
                    admin.getUsername(), admin.getRole(), admin.getPassword().substring(0, 20) + "...");
            System.out.printf("   Created User: %s [Role: %s, Hash: %s]%n",
                    customer.getUsername(), customer.getRole(), customer.getPassword().substring(0, 20) + "...");

            // Verify BCrypt password authentication
            boolean authSuccess = usersService.authenticateUser("customer1", "SecretPass123");
            boolean authFail = usersService.authenticateUser("customer1", "WrongPass");
            System.out.printf("   BCrypt authentication test -> Valid password: %b, Invalid password: %b%n",
                    authSuccess, authFail);

            // Step 4 & 5: Create Order containing multiple OrderDetails and calculate total
            System.out.println("\n4. Creating order with multiple OrderDetails...");
            System.out.println("5. Calculating order total using BigDecimal arithmetic...");
            List<OrderService.OrderItemRequest> items = Arrays.asList(
                    new OrderService.OrderItemRequest(laptop.getId(), 1),      // 50000 * 1 = 50000
                    new OrderService.OrderItemRequest(headphones.getId(), 2),  // 2000 * 2 = 4000
                    new OrderService.OrderItemRequest(javaBook.getId(), 1)     // 800 * 1 = 800
            );

            Orders createdOrder = orderService.createOrder(customer.getId(), items);
            System.out.printf("   Order #%d placed successfully. Total Amount: ₹%s%n",
                    createdOrder.getId(), createdOrder.getTotalAmount());

            // Step 6: Updating product stock and validating deduction
            System.out.println("\n6. Updating product stock (validating inventory deductions)...");
            Product updatedLaptop = productService.getProductById(laptop.getId()).orElseThrow();
            Product updatedHeadphones = productService.getProductById(headphones.getId()).orElseThrow();
            Product updatedJavaBook = productService.getProductById(javaBook.getId()).orElseThrow();
            System.out.printf("   Laptop Stock: %d -> %d (deducted 1)%n", laptop.getStockQuantity(), updatedLaptop.getStockQuantity());
            System.out.printf("   Headphones Stock: %d -> %d (deducted 2)%n", headphones.getStockQuantity(), updatedHeadphones.getStockQuantity());
            System.out.printf("   Java Book Stock: %d -> %d (deducted 1)%n", javaBook.getStockQuantity(), updatedJavaBook.getStockQuantity());

            // Step 7 & 8: Fetching order with Customer and Products (JOIN FETCH)
            System.out.println("\n7. Fetching order with customer (JOIN FETCH)...");
            System.out.println("8. Fetching order products (displaying full relationship tree)...");
            Orders fetchedOrder = orderService.getOrderWithDetails(createdOrder.getId());

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            System.out.println("\n========== ORDER ==========");
            System.out.println("Order ID: " + fetchedOrder.getId());
            System.out.println("Customer: " + fetchedOrder.getUser().getUsername());
            System.out.println("Email: " + fetchedOrder.getUser().getEmail());
            System.out.println("Date: " + fetchedOrder.getOrderDate().format(dateFormatter));
            System.out.println("\nProducts:");
            System.out.println("--------------------------");
            for (OrderDetails detail : fetchedOrder.getOrderDetails()) {
                System.out.println(detail.getProduct().getName());
                System.out.println("Quantity: " + detail.getQuantity());
                System.out.println("Unit Price: ₹" + detail.getUnitPrice().toPlainString());
                System.out.println();
            }
            System.out.println("--------------------------");
            System.out.println("Total Amount: ₹" + fetchedOrder.getTotalAmount().toPlainString());
            System.out.println("==========================");

            // Step 9: Running Named Query
            System.out.println("\n9. Running Named Query ('Product.findByCategory')...");
            List<Product> electronicsProducts = queryService.findProductsByCategoryNamedQuery(electronics.getId());
            System.out.printf("   Found %d products in category 'Electronics':%n", electronicsProducts.size());
            for (Product p : electronicsProducts) {
                System.out.printf("   - %s (₹%s, Stock: %d)%n", p.getName(), p.getPrice(), p.getStockQuantity());
            }

            // Step 10: Running Criteria Query & Pagination
            System.out.println("\n10. Running Criteria Query (price > 1000 AND stock > 0)...");
            List<Product> premiumProducts = queryService.findAvailableProductsAbovePrice(new BigDecimal("1000.00"));
            System.out.printf("   Found %d products above ₹1000 with available stock:%n", premiumProducts.size());
            for (Product p : premiumProducts) {
                System.out.printf("   - %s (₹%s, Stock: %d)%n", p.getName(), p.getPrice(), p.getStockQuantity());
            }

            System.out.println("\n   Bonus: Running Pagination Query (Page 0, Size 3)...");
            List<Product> pagedProducts = queryService.findProductsPaged(0, 3);
            for (int i = 0; i < pagedProducts.size(); i++) {
                System.out.printf("   [%d] %s (₹%s)%n", i + 1, pagedProducts.get(i).getName(), pagedProducts.get(i).getPrice());
            }

            System.out.println("\nApplication completed successfully.");
        } catch (Exception e) {
            System.err.println("Error executing application demo: " + e.getMessage());
            e.printStackTrace();
        } finally {
            HibernateUtil.shutdown();
        }
    }
}
