package com.ecommerce.service;

import com.ecommerce.dao.CategoryDAO;
import com.ecommerce.entity.Category;
import com.ecommerce.exception.ValidationException;

import java.util.List;
import java.util.Optional;

/**
 * Service handling business logic and validations for Category entity.
 */
public class CategoryService {

    private final CategoryDAO categoryDAO;

    public CategoryService() {
        this.categoryDAO = new CategoryDAO();
    }

    public CategoryService(CategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO;
    }

    public Category createCategory(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Category name cannot be null or empty");
        }
        Category category = new Category(name.trim(), description);
        return categoryDAO.save(category);
    }

    public Optional<Category> getCategoryById(Long id) {
        if (id == null) {
            throw new ValidationException("Category ID cannot be null");
        }
        return categoryDAO.findById(id);
    }

    public List<Category> getAllCategories() {
        return categoryDAO.findAll();
    }

    public Category updateCategory(Long id, String name, String description) {
        if (id == null) {
            throw new ValidationException("Category ID cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Category name cannot be null or empty");
        }
        Category category = categoryDAO.findById(id)
                .orElseThrow(() -> new ValidationException("Category not found with ID: " + id));

        category.setName(name.trim());
        category.setDescription(description);
        return categoryDAO.update(category);
    }

    public boolean deleteCategory(Long id) {
        if (id == null) {
            throw new ValidationException("Category ID cannot be null");
        }
        return categoryDAO.delete(id);
    }
}
