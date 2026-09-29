package com.ecommerce.service;

import com.ecommerce.dao.CategoryDAO;
import com.ecommerce.dao.ProductDAO;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.exception.ValidationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service handling business logic and validations for Product entity.
 */
public class ProductService {

    private final ProductDAO productDAO;
    private final CategoryDAO categoryDAO;

    public ProductService() {
        this.productDAO = new ProductDAO();
        this.categoryDAO = new CategoryDAO();
    }

    public ProductService(ProductDAO productDAO, CategoryDAO categoryDAO) {
        this.productDAO = productDAO;
        this.categoryDAO = categoryDAO;
    }

    public Product createProduct(String name, BigDecimal price, Integer stockQuantity, Long categoryId) {
        validateProductData(name, price, stockQuantity);
        if (categoryId == null) {
            throw new ValidationException("Category ID cannot be null");
        }

        Category category = categoryDAO.findById(categoryId)
                .orElseThrow(() -> new ValidationException("Category not found with ID: " + categoryId));

        Product product = new Product(name.trim(), price, stockQuantity, category);
        return productDAO.save(product);
    }

    public Optional<Product> getProductById(Long id) {
        if (id == null) {
            throw new ValidationException("Product ID cannot be null");
        }
        return productDAO.findById(id);
    }

    public List<Product> getAllActiveProducts() {
        return productDAO.findAll();
    }

    public List<Product> getAllProducts() {
        return productDAO.findAllIncludingDeleted();
    }

    public List<Product> getProductsByCategory(Long categoryId) {
        if (categoryId == null) {
            throw new ValidationException("Category ID cannot be null");
        }
        return productDAO.findByCategory(categoryId);
    }

    public Product updateProduct(Long id, String name, BigDecimal price, Integer stockQuantity, Long categoryId) {
        if (id == null) {
            throw new ValidationException("Product ID cannot be null");
        }
        validateProductData(name, price, stockQuantity);

        Product product = productDAO.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));

        if (categoryId != null) {
            Category category = categoryDAO.findById(categoryId)
                    .orElseThrow(() -> new ValidationException("Category not found with ID: " + categoryId));
            product.setCategory(category);
        }

        product.setName(name.trim());
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);

        return productDAO.update(product);
    }

    public boolean deleteProduct(Long id) {
        if (id == null) {
            throw new ValidationException("Product ID cannot be null");
        }
        return productDAO.delete(id);
    }

    public boolean softDeleteProduct(Long id) {
        if (id == null) {
            throw new ValidationException("Product ID cannot be null");
        }
        return productDAO.softDelete(id);
    }

    private void validateProductData(String name, BigDecimal price, Integer stockQuantity) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Product name cannot be null or empty");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Product price must be non-null and greater than or equal to 0");
        }
        if (stockQuantity == null || stockQuantity < 0) {
            throw new ValidationException("Stock quantity must be non-null and greater than or equal to 0");
        }
    }
}
