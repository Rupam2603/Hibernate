package com.ecommerce.service;

import com.ecommerce.dao.OrdersDAO;
import com.ecommerce.entity.OrderDetails;
import com.ecommerce.entity.Orders;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Users;
import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.exception.OrderNotFoundException;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.exception.UserNotFoundException;
import com.ecommerce.exception.ValidationException;
import com.ecommerce.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing transactional order creation, stock deductions, monetary calculations, and retrievals.
 */
public class OrderService {

    private final OrdersDAO ordersDAO;

    public OrderService() {
        this.ordersDAO = new OrdersDAO();
    }

    public OrderService(OrdersDAO ordersDAO) {
        this.ordersDAO = ordersDAO;
    }

    /**
     * DTO representing an item in an order creation request.
     */
    public static class OrderItemRequest {
        private final Long productId;
        private final int quantity;

        public OrderItemRequest(Long productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() {
            return productId;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    /**
     * Atomically creates an Order containing multiple OrderDetails, calculates the exact
     * monetary total using BigDecimal, and decrements stock quantity for each product.
     * Rolls back the entire transaction if stock is insufficient or if validation fails.
     *
     * @param userId the ID of the ordering customer
     * @param itemRequests list of products and quantities to purchase
     * @return the fully persisted Orders entity
     */
    public Orders createOrder(Long userId, List<OrderItemRequest> itemRequests) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new ValidationException("Order must contain at least one item");
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();

            // 1. Fetch and validate User
            Users user = session.get(Users.class, userId);
            if (user == null) {
                throw new UserNotFoundException("User not found with ID: " + userId);
            }

            // 2. Initialize Order
            Orders order = new Orders();
            order.setOrderDate(LocalDateTime.now());
            order.setUser(user);

            BigDecimal totalAmount = BigDecimal.ZERO;
            List<OrderDetails> detailsList = new ArrayList<>();

            // 3. Process each item, validate stock, and calculate subtotal
            for (OrderItemRequest req : itemRequests) {
                if (req.getProductId() == null) {
                    throw new ValidationException("Product ID cannot be null in order item");
                }
                if (req.getQuantity() <= 0) {
                    throw new ValidationException("Quantity must be greater than zero for product ID: " + req.getProductId());
                }

                Product product = session.get(Product.class, req.getProductId());
                if (product == null) {
                    throw new ProductNotFoundException("Product not found with ID: " + req.getProductId());
                }
                if (product.isDeleted()) {
                    throw new ValidationException("Cannot order discontinued product: " + product.getName());
                }

                // Check stock availability
                if (product.getStockQuantity() < req.getQuantity()) {
                    throw new InsufficientStockException(
                            String.format("Insufficient stock for product '%s' (ID: %d). Available: %d, Requested: %d",
                                    product.getName(), product.getId(), product.getStockQuantity(), req.getQuantity())
                    );
                }

                // Decrement stock atomically
                product.setStockQuantity(product.getStockQuantity() - req.getQuantity());
                session.merge(product);

                // Compute subtotal: unitPrice * quantity
                BigDecimal unitPrice = product.getPrice();
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(req.getQuantity()));
                totalAmount = totalAmount.add(subtotal);

                // Create OrderDetails entity
                OrderDetails orderDetail = new OrderDetails(req.getQuantity(), unitPrice, order, product);
                detailsList.add(orderDetail);
            }

            // 4. Set calculated total amount and order details
            order.setTotalAmount(totalAmount);
            order.setOrderDetails(detailsList);

            // 5. Persist order (cascade will persist order details)
            session.persist(order);

            transaction.commit();
            return order;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (Exception rbEx) {
                    // Suppress secondary rollback failure
                }
            }
            throw e;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    /**
     * Fetches an order by ID along with its Customer, OrderDetails, and Products eagerly loaded
     * to eliminate LazyInitializationException and N+1 queries.
     *
     * @param orderId the primary key of the order
     * @return the fully initialized Orders graph
     */
    public Orders getOrderWithDetails(Long orderId) {
        if (orderId == null) {
            throw new ValidationException("Order ID cannot be null");
        }
        return ordersDAO.findOrderWithDetails(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
    }

    public Optional<Orders> getOrderById(Long id) {
        if (id == null) {
            throw new ValidationException("Order ID cannot be null");
        }
        return ordersDAO.findById(id);
    }

    public List<Orders> getAllOrders() {
        return ordersDAO.findAll();
    }

    public List<Orders> getOrdersByUser(Long userId) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }
        return ordersDAO.findOrdersByUser(userId);
    }

    public boolean deleteOrder(Long id) {
        if (id == null) {
            throw new ValidationException("Order ID cannot be null");
        }
        return ordersDAO.delete(id);
    }
}
