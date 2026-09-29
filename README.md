# Hibernate ORM E-Commerce Management System

A complete, standalone Java Maven application implementing an e-commerce management system using **Hibernate ORM 6.x** and **Jakarta Persistence API (JPA)**.

This project demonstrates core ORM concepts, entity relationships, CRUD operations, atomic transaction management, BCrypt password hashing, JPQL Named Queries, the JPA Criteria API, pagination, and soft deletion.

---

## 🛠 Technologies Used

- **Language:** Java 17+ (SE Runtime)
- **Build Tool:** Apache Maven
- **ORM / JPA Provider:** Hibernate ORM 6.5.x & Jakarta Persistence API 3.1
- **Database:** MySQL 8.x (with H2 in-memory mode for isolated test execution and standalone zero-dependency evaluation)
- **Security:** jBCrypt 0.4 (Blowfish password hashing)
- **Testing:** JUnit 5 (Jupiter 5.10.x)
- **Logging:** SLF4J Simple Logger

---

## ✨ Features

- **Entity Modeling & JPA Annotations:**
  - Standard JPA mappings (`@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@Enumerated`, `@ManyToOne`, `@OneToMany`, `@JoinColumn`).
- **Entity Relationships:**
  - `Category` (1) ── (N) `Product`
  - `Users` (1) ── (N) `Orders`
  - `Orders` (1) ── (N) `OrderDetails`
  - `Product` (1) ── (N) `OrderDetails`
- **Session & Transaction Management:**
  - Singleton `HibernateUtil` managing Hibernate `SessionFactory`.
  - Atomic transaction handling with explicit commit and rollback upon error.
- **Order Processing & Inventory Management:**
  - Monetary calculations using precise `BigDecimal` arithmetic.
  - Automatic validation and stock deduction during order creation.
  - Rollback on insufficient stock (`InsufficientStockException`).
- **High-Performance Fetching:**
  - `JOIN FETCH` queries to eliminate the N+1 select problem and avoid `LazyInitializationException`.
- **Security:**
  - Passwords hashed using BCrypt before persistence; verification without storing plain text.
- **Advanced Querying (Bonus Features):**
  - **JPQL Named Queries:** `@NamedQuery` defined on `Product` to query active products by category.
  - **JPA Criteria API:** Dynamic query building using `CriteriaBuilder`, `CriteriaQuery`, `Root`, and `Predicate`.
  - **Pagination:** Paged queries using `setFirstResult` and `setMaxResults`.
  - **Soft Deletion:** Products feature a `deleted` boolean flag for logical deletion.

---

## 📂 Project Structure

```text
hibernate-ecommerce/
│
├── pom.xml                                   # Maven build configuration
├── README.md                                 # Project documentation
├── schema.sql                                # Complete MySQL DDL & Seed script
├── .gitignore                                # Git ignore rules
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── ecommerce/
    │   │           ├── entity/
    │   │           │   ├── Category.java      # Category entity (@OneToMany Products)
    │   │           │   ├── Product.java       # Product entity (@ManyToOne Category, Named Queries)
    │   │           │   ├── Users.java         # Users entity (@OneToMany Orders, BCrypt passwords)
    │   │           │   ├── Orders.java        # Orders entity (@ManyToOne User, @OneToMany Details)
    │   │           │   └── OrderDetails.java  # Order line item entity
    │   │           │
    │   │           ├── enumtype/
    │   │           │   └── UserRole.java      # ADMIN, CUSTOMER enum
    │   │           │
    │   │           ├── exception/
    │   │           │   ├── InsufficientStockException.java
    │   │           │   ├── OrderNotFoundException.java
    │   │           │   ├── ProductNotFoundException.java
    │   │           │   ├── UserNotFoundException.java
    │   │           │   └── ValidationException.java
    │   │           │
    │   │           ├── util/
    │   │           │   ├── HibernateUtil.java # SessionFactory lifecycle manager
    │   │           │   └── PasswordUtil.java  # BCrypt hashing & checking
    │   │           │
    │   │           ├── dao/
    │   │           │   ├── CategoryDAO.java
    │   │           │   ├── ProductDAO.java
    │   │           │   ├── UsersDAO.java
    │   │           │   ├── OrdersDAO.java
    │   │           │   └── OrderDetailsDAO.java
    │   │           │
    │   │           ├── service/
    │   │           │   ├── CategoryService.java
    │   │           │   ├── ProductService.java
    │   │           │   ├── UsersService.java
    │   │           │   └── OrderService.java
    │   │           │
    │   │           ├── query/
    │   │           │   └── ProductQueryService.java # Named Queries, Criteria API, Pagination
    │   │           │
    │   │           └── Main.java              # Console demo runner
    │   │
    │   └── resources/
    │       └── hibernate.cfg.xml              # MySQL database configuration
    │
    └── test/
        ├── java/
        │   └── com/
        │       └── ecommerce/
        │           ├── CategoryTest.java      # Category CRUD tests
        │           ├── ProductTest.java       # Product CRUD & query tests
        │           ├── UsersTest.java         # Users CRUD & BCrypt tests
        │           └── OrderTest.java         # Order creation, details, stock, rollback tests
        └── resources/
            └── hibernate-test.cfg.xml         # Isolated test database configuration
```

---

## 🗄 Entity Relationship Diagram

```text
  +------------------+             +-----------------------+
  |    CATEGORIES    | 1         * |       PRODUCTS        |
  +------------------+-------------+-----------------------+
  | PK  id           |             | PK  id                |
  |     name         |             |     name              |
  |     description  |             |     price             |
  +------------------+             |     stock_quantity    |
                                   |     deleted           |
                                   | FK  category_id       |
                                   +-----------+-----------+
                                               | 1
                                               |
                                               | *
  +------------------+             +-----------+-----------+
  |      USERS       | 1         * |     ORDER_DETAILS     |
  +------------------+-------------+-----------------------+
  | PK  id           |             | PK  id                |
  |     username     |             |     quantity          |
  |     password     |             |     unit_price        |
  |     email        |             | FK  order_id          |
  |     role         |             | FK  product_id        |
  +--------+---------+             +-----------+-----------+
           | 1                                 | *
           |                                   |
           | *                                 | 1
  +--------+---------+                         |
  |      ORDERS      |-------------------------+
  +------------------+
  | PK  id           |
  |     order_date   |
  |     total_amount |
  | FK  user_id      |
  +------------------+
```

---

## 🚀 Setup & Execution

### 1. Prerequisites
- Java 17 or higher (`java -version`)
- Apache Maven 3.8+ (`mvn -version`)
- MySQL Server 8.x (optional for tests, which run in-memory)

### 2. MySQL Database Setup (Optional for Demo)
If running against a live MySQL server:
```sql
mysql -u root -p < schema.sql
```
Or create the database manually:
```sql
CREATE DATABASE hibernate_ecommerce;
```

Configure your credentials in `src/main/resources/hibernate.cfg.xml`:
```xml
<property name="hibernate.connection.url">jdbc:mysql://localhost:3306/hibernate_ecommerce</property>
<property name="hibernate.connection.username">root</property>
<property name="hibernate.connection.password">YOUR_PASSWORD</property>
```
*Tip: You can also pass credentials dynamically via system properties or environment variables:*
```bash
mvn exec:java -Ddb.user=root -Ddb.password=mySecretPassword
```

### 3. Run Automated Tests
Execute the complete JUnit 5 test suite:
```bash
mvn clean test
```
All tests run against an isolated in-memory test database, guaranteeing zero external dependencies and zero risk to live databases.

### 4. Run Console Demonstration
Build and run the console demonstration:
```bash
mvn compile exec:java
```

---

## 🖥 Sample Output

```text
========================================
   Hibernate E-Commerce Application    
========================================

1. Creating categories...
   Created: Electronics (ID: 1), Clothing (ID: 2), Books (ID: 3)

2. Creating products...
   Created: Laptop (Stock: 10, ₹50000.00)
   Created: Smartphone (Stock: 15, ₹25000.00)
   Created: Headphones (Stock: 30, ₹2000.00)
   Created: T-Shirt (Stock: 50, ₹650.00)
   Created: Java Programming Book (Stock: 20, ₹800.00)

3. Creating users with BCrypt password hashing...
   Created User: admin [Role: ADMIN, Hash: $2a$12$Zeq4q7046oDq8a3v...]
   Created User: customer1 [Role: CUSTOMER, Hash: $2a$12$L7RfZd7bF0KekR6n...]
   BCrypt authentication test -> Valid password: true, Invalid password: false

4. Creating order with multiple OrderDetails...
5. Calculating order total using BigDecimal arithmetic...
   Order #1 placed successfully. Total Amount: ₹54800.00

6. Updating product stock (validating inventory deductions)...
   Laptop Stock: 10 -> 9 (deducted 1)
   Headphones Stock: 30 -> 28 (deducted 2)
   Java Book Stock: 20 -> 19 (deducted 1)

7. Fetching order with customer (JOIN FETCH)...
8. Fetching order products (displaying full relationship tree)...

========== ORDER ==========
Order ID: 1
Customer: customer1
Email: customer1@example.com
Date: 2026-09-29 17:40

Products:
--------------------------
Laptop
Quantity: 1
Unit Price: ₹50000.00

Headphones
Quantity: 2
Unit Price: ₹2000.00

Java Programming Book
Quantity: 1
Unit Price: ₹800.00

--------------------------
Total Amount: ₹54800.00
==========================

9. Running Named Query ('Product.findByCategory')...
   Found 3 products in category 'Electronics':
   - Laptop (₹50000.00, Stock: 9)
   - Smartphone (₹25000.00, Stock: 15)
   - Headphones (₹2000.00, Stock: 28)

10. Running Criteria Query (price > 1000 AND stock > 0)...
   Found 3 products above ₹1000 with available stock:
   - Headphones (₹2000.00, Stock: 28)
   - Smartphone (₹25000.00, Stock: 15)
   - Laptop (₹50000.00, Stock: 9)

   Bonus: Running Pagination Query (Page 0, Size 3)...
   [1] Laptop (₹50000.00)
   [2] Smartphone (₹25000.00)
   [3] Headphones (₹2000.00)

Application completed successfully.
```
