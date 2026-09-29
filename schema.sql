-- =============================================================================
-- Hibernate ORM E-Commerce Management System Database Schema
-- Database: MySQL 8.x
-- =============================================================================

DROP DATABASE IF EXISTS hibernate_ecommerce;
CREATE DATABASE hibernate_ecommerce CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hibernate_ecommerce;

-- -----------------------------------------------------------------------------
-- Table: categories
-- -----------------------------------------------------------------------------
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    CONSTRAINT uk_category_name UNIQUE (name)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- Table: products
-- -----------------------------------------------------------------------------
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    category_id BIGINT NOT NULL,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id)
        REFERENCES categories(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_deleted ON products(deleted);

-- -----------------------------------------------------------------------------
-- Table: users
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT uk_user_email UNIQUE (email)
) ENGINE=InnoDB;

-- -----------------------------------------------------------------------------
-- Table: orders
-- -----------------------------------------------------------------------------
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_date DATETIME(6) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_orders_user ON orders(user_id);

-- -----------------------------------------------------------------------------
-- Table: order_details
-- -----------------------------------------------------------------------------
CREATE TABLE order_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    CONSTRAINT fk_order_details_order FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_order_details_product FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_order_details_order ON order_details(order_id);
CREATE INDEX idx_order_details_product ON order_details(product_id);

-- =============================================================================
-- Sample Seed Data
-- =============================================================================

-- Seed Categories
INSERT INTO categories (name, description) VALUES
('Electronics', 'Electronic devices, gadgets, and accessories'),
('Clothing', 'Men, women, and kids apparel'),
('Books', 'Educational, technical, and fictional books');

-- Seed Products
INSERT INTO products (name, price, stock_quantity, deleted, category_id) VALUES
('Laptop', 50000.00, 10, FALSE, 1),
('Smartphone', 25000.00, 15, FALSE, 1),
('Headphones', 2000.00, 30, FALSE, 1),
('T-Shirt', 650.00, 50, FALSE, 2),
('Java Programming Book', 800.00, 20, FALSE, 3);

-- Seed Users (Passwords are BCrypt hashed: 'AdminPass@2026' and 'SecretPass123')
INSERT INTO users (username, password, email, role) VALUES
('admin', '$2a$12$Zeq4q7046oDq8a3v658b2e1B3Xw9a/kF0ZlY54eX/OqM1k2r8V3f2', 'admin@ecommerce.com', 'ADMIN'),
('customer1', '$2a$12$L7RfZd7bF0KekR6n91fHl.nQ8wVz1P9wT2b9.6mR0Nq2O5V3b5sD6', 'customer1@example.com', 'CUSTOMER');
