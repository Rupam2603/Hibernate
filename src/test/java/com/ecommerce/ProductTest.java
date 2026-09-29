package com.ecommerce;

import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.query.ProductQueryService;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.ProductService;
import com.ecommerce.util.HibernateUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ProductTest {

    private ProductService productService;
    private CategoryService categoryService;
    private ProductQueryService queryService;

    @BeforeAll
    static void initHibernate() {
        try {
            HibernateUtil.setSessionFactory(HibernateUtil.buildCustomSessionFactory("hibernate-test.cfg.xml"));
        } catch (Exception ignored) {
        }
    }

    @BeforeEach
    void setUp() {
        productService = new ProductService();
        categoryService = new CategoryService();
        queryService = new ProductQueryService();
    }

    @Test
    void testCreateProduct() {
        Category cat = categoryService.createCategory("TechGadgets_" + System.currentTimeMillis(), "Modern technology");
        Product product = productService.createProduct("Smartphone X", new BigDecimal("49999.00"), 25, cat.getId());

        assertNotNull(product.getId());
        assertEquals("Smartphone X", product.getName());
        assertEquals(0, new BigDecimal("49999.00").compareTo(product.getPrice()));
        assertEquals(25, product.getStockQuantity());
        assertEquals(cat.getId(), product.getCategory().getId());
        assertFalse(product.isDeleted());
    }

    @Test
    void testFindProductById() {
        Category cat = categoryService.createCategory("AudioDevices_" + System.currentTimeMillis(), "Earphones and speakers");
        Product product = productService.createProduct("Wireless Earbuds", new BigDecimal("2499.00"), 50, cat.getId());

        Optional<Product> found = productService.getProductById(product.getId());
        assertTrue(found.isPresent());
        assertEquals("Wireless Earbuds", found.get().getName());
    }

    @Test
    void testUpdateProduct() {
        Category cat = categoryService.createCategory("Peripherals_" + System.currentTimeMillis(), "Keyboards and mice");
        Product product = productService.createProduct("Mechanical Keyboard", new BigDecimal("3500.00"), 15, cat.getId());

        Product updated = productService.updateProduct(
                product.getId(), "RGB Mechanical Keyboard", new BigDecimal("3999.00"), 20, cat.getId());

        assertEquals("RGB Mechanical Keyboard", updated.getName());
        assertEquals(0, new BigDecimal("3999.00").compareTo(updated.getPrice()));
        assertEquals(20, updated.getStockQuantity());
    }

    @Test
    void testFindProductsByCategory() {
        Category cat = categoryService.createCategory("Monitors_" + System.currentTimeMillis(), "Display panels");
        productService.createProduct("24-inch Monitor", new BigDecimal("12000.00"), 10, cat.getId());
        productService.createProduct("27-inch 4K Monitor", new BigDecimal("28000.00"), 8, cat.getId());

        List<Product> products = productService.getProductsByCategory(cat.getId());
        assertEquals(2, products.size());

        // Test NamedQuery execution
        List<Product> namedQueryProducts = queryService.findProductsByCategoryNamedQuery(cat.getId());
        assertEquals(2, namedQueryProducts.size());
    }

    @Test
    void testSoftDeleteProduct() {
        Category cat = categoryService.createCategory("Storage_" + System.currentTimeMillis(), "SSDs and HDDs");
        Product product = productService.createProduct("1TB NVMe SSD", new BigDecimal("6500.00"), 12, cat.getId());

        boolean softDeleted = productService.softDeleteProduct(product.getId());
        assertTrue(softDeleted);

        // Product should not be in active products list
        List<Product> activeProducts = productService.getAllActiveProducts();
        boolean foundInActive = activeProducts.stream().anyMatch(p -> p.getId().equals(product.getId()));
        assertFalse(foundInActive);

        // But still retrievable in raw query
        Optional<Product> retrieved = productService.getProductById(product.getId());
        assertTrue(retrieved.isPresent());
        assertTrue(retrieved.get().isDeleted());
    }
}
