package com.ecommerce.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Orders entity representing customer orders.
 * Maps to the 'orders' table in the database.
 */
@Entity
@Table(name = "orders")
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<OrderDetails> orderDetails = new ArrayList<>();

    // Default no-argument constructor required by JPA
    public Orders() {
    }

    // Parameterized constructor without id
    public Orders(LocalDateTime orderDate, BigDecimal totalAmount, Users user) {
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.user = user;
    }

    // Full constructor
    public Orders(Long id, LocalDateTime orderDate, BigDecimal totalAmount, Users user) {
        this.id = id;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
        this.user = user;
    }

    // Bidirectional helper methods
    public void addOrderDetail(OrderDetails detail) {
        orderDetails.add(detail);
        detail.setOrder(this);
    }

    public void removeOrderDetail(OrderDetails detail) {
        orderDetails.remove(detail);
        detail.setOrder(null);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public List<OrderDetails> getOrderDetails() {
        return orderDetails;
    }

    public void setOrderDetails(List<OrderDetails> orderDetails) {
        this.orderDetails = orderDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Orders orders = (Orders) o;
        return Objects.equals(id, orders.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // Safe toString() without referencing recursive collections
    @Override
    public String toString() {
        return "Orders{" +
                "id=" + id +
                ", orderDate=" + orderDate +
                ", totalAmount=" + totalAmount +
                ", userId=" + (user != null ? user.getId() : null) +
                '}';
    }
}
