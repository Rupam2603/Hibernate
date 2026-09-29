package com.ecommerce;

import com.ecommerce.entity.Category;
import com.ecommerce.service.CategoryService;
import com.ecommerce.util.HibernateUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CategoryTest {

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
        categoryService = new CategoryService();
    }

    @Test
    void testCreateCategory() {
        String catName = "Electronics_" + System.currentTimeMillis();
        Category category = categoryService.createCategory(catName, "Gadgets and tech devices");
        assertNotNull(category.getId());
        assertEquals(catName, category.getName());
        assertEquals("Gadgets and tech devices", category.getDescription());
    }

    @Test
    void testFindCategoryById() {
        String catName = "Books_" + System.currentTimeMillis();
        Category created = categoryService.createCategory(catName, "Fiction and non-fiction");
        Optional<Category> found = categoryService.getCategoryById(created.getId());

        assertTrue(found.isPresent());
        assertEquals(catName, found.get().getName());
    }

    @Test
    void testUpdateCategory() {
        String catName = "Home_" + System.currentTimeMillis();
        String updatedName = "Home_Kitchen_" + System.currentTimeMillis();
        Category created = categoryService.createCategory(catName, "Home decor");
        Category updated = categoryService.updateCategory(created.getId(), updatedName, "Updated description");

        assertEquals(updatedName, updated.getName());
        assertEquals("Updated description", updated.getDescription());

        Optional<Category> retrieved = categoryService.getCategoryById(created.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(updatedName, retrieved.get().getName());
    }

    @Test
    void testDeleteCategory() {
        String catName = "Automotive_" + System.currentTimeMillis();
        Category created = categoryService.createCategory(catName, "Vehicle parts");
        Long id = created.getId();

        boolean deleted = categoryService.deleteCategory(id);
        assertTrue(deleted);

        Optional<Category> found = categoryService.getCategoryById(id);
        assertFalse(found.isPresent());
    }

    @Test
    void testFindAllCategories() {
        String catName1 = "Sports_" + System.currentTimeMillis();
        String catName2 = "Fashion_" + System.currentTimeMillis();
        categoryService.createCategory(catName1, "Athletics gear");
        categoryService.createCategory(catName2, "Clothing and shoes");

        List<Category> all = categoryService.getAllCategories();
        assertTrue(all.size() >= 2);
    }
}
