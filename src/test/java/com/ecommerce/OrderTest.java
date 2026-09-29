package com.ecommerce;

import com.ecommerce.entity.Category;
import com.ecommerce.entity.OrderDetails;
import com.ecommerce.entity.Orders;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Users;
import com.ecommerce.enumtype.UserRole;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.UsersService;
import com.ecommerce.util.HibernateUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OrderTest {

    private OrderService orderService;
    private ProductService productService;
    private UsersService usersService;
    private CategoryService categoryService;

    @BeforeAll
    static void initHibernate() {
        try {
            HibernateUtil.setSessionFactory(HibernateUtil.buildCustomSessionFactory("hibernate-test.cfg.xml"));
        } catch (Exception ignored) {
        }
    }

    @BeforeEach
    void setUp() {
        orderService = new OrderService();
        productService = new ProductService();
        usersService = new UsersService();
        categoryService = new CategoryService();
    }

    @Test
    void testCreateOrderWithMultipleOrderDetailsAndTotalAmount() {
        long ts = System.currentTimeMillis();
        Category cat = categoryService.createCategory("Electronics_" + ts, "Electronic items");
        Product p1 = productService.createProduct("Gaming Mouse", new BigDecimal("1500.00"), 20, cat.getId());
        Product p2 = productService.createProduct("Mousepad XXL", new BigDecimal("500.00"), 40, cat.getId());
        Users user = usersService.createUser("orderuser_" + ts, "Pass123!", "orderuser_" + ts + "@example.com", UserRole.CUSTOMER);

        // Order 2 Gaming Mice (2 * 1500 = 3000) and 3 Mousepads (3 * 500 = 1500) -> Total = 4500
        List<OrderService.OrderItemRequest> items = Arrays.asList(
                new OrderService.OrderItemRequest(p1.getId(), 2),
                new OrderService.OrderItemRequest(p2.getId(), 3)
        );

        Orders order = orderService.createOrder(user.getId(), items);

        assertNotNull(order.getId());
        assertEquals(0, new BigDecimal("4500.00").compareTo(order.getTotalAmount()));
        assertEquals(user.getId(), order.getUser().getId());
        assertEquals(2, order.getOrderDetails().size());

        // Validate inventory stock deductions
        Product updatedP1 = productService.getProductById(p1.getId()).orElseThrow();
        Product updatedP2 = productService.getProductById(p2.getId()).orElseThrow();
        assertEquals(18, updatedP1.getStockQuantity());
        assertEquals(37, updatedP2.getStockQuantity());

        // Test eager retrieval via findOrderWithDetails
        Orders fetched = orderService.getOrderWithDetails(order.getId());
        assertNotNull(fetched);
        assertEquals("orderuser_" + ts, fetched.getUser().getUsername());
        assertEquals(2, fetched.getOrderDetails().size());

        for (OrderDetails detail : fetched.getOrderDetails()) {
            assertNotNull(detail.getProduct());
            assertNotNull(detail.getProduct().getName());
        }
    }

    @Test
    void testInsufficientStockThrowsExceptionAndRollsBack() {
        long ts = System.currentTimeMillis();
        Category cat = categoryService.createCategory("RareCat_" + ts, "Rare items");
        Product p = productService.createProduct("Rare Collectible", new BigDecimal("10000.00"), 2, cat.getId());
        Users user = usersService.createUser("buyer2_" + ts, "Pass123!", "buyer2_" + ts + "@example.com", UserRole.CUSTOMER);

        // Attempt to order 5 when only 2 are available
        List<OrderService.OrderItemRequest> items = List.of(
                new OrderService.OrderItemRequest(p.getId(), 5)
        );

        assertThrows(InsufficientStockException.class, () -> {
            orderService.createOrder(user.getId(), items);
        });

        // Verify stock was NOT deducted due to transaction rollback
        Product unchangedProduct = productService.getProductById(p.getId()).orElseThrow();
        assertEquals(2, unchangedProduct.getStockQuantity());
    }

    @Test
    void testGetOrdersByUser() {
        long ts = System.currentTimeMillis();
        Category cat = categoryService.createCategory("Stationery_" + ts, "Office supplies");
        Product p = productService.createProduct("Notebook", new BigDecimal("100.00"), 50, cat.getId());
        Users user = usersService.createUser("buyer3_" + ts, "Pass123!", "buyer3_" + ts + "@example.com", UserRole.CUSTOMER);

        orderService.createOrder(user.getId(), List.of(new OrderService.OrderItemRequest(p.getId(), 1)));
        orderService.createOrder(user.getId(), List.of(new OrderService.OrderItemRequest(p.getId(), 2)));

        List<Orders> userOrders = orderService.getOrdersByUser(user.getId());
        assertEquals(2, userOrders.size());
    }
}
