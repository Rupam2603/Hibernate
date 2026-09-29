package com.ecommerce.dao;

import com.ecommerce.entity.Orders;
import com.ecommerce.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Orders entity operations.
 */
public class OrdersDAO {

    public Orders save(Orders order) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            session.persist(order);
            transaction.commit();
            return order;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (Exception rbEx) {
                    // Suppress
                }
            }
            throw e;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public Orders save(Session session, Orders order) {
        session.persist(order);
        return order;
    }

    public Optional<Orders> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Orders.class, id));
        }
    }

    public Optional<Orders> findById(Session session, Long id) {
        return Optional.ofNullable(session.get(Orders.class, id));
    }

    /**
     * Eagerly loads an order along with its associated User, OrderDetails, and Products
     * using JOIN FETCH to completely eliminate the N+1 select problem and avoid LazyInitializationException.
     *
     * @param orderId the primary key of the order
     * @return Optional containing the fully initialized Orders graph if found
     */
    public Optional<Orders> findOrderWithDetails(Long orderId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT DISTINCT o FROM Orders o " +
                    "JOIN FETCH o.user " +
                    "JOIN FETCH o.orderDetails od " +
                    "JOIN FETCH od.product " +
                    "WHERE o.id = :orderId";
            return session.createQuery(hql, Orders.class)
                    .setParameter("orderId", orderId)
                    .uniqueResultOptional();
        }
    }

    public Optional<Orders> findOrderWithDetails(Session session, Long orderId) {
        String hql = "SELECT DISTINCT o FROM Orders o " +
                "JOIN FETCH o.user " +
                "JOIN FETCH o.orderDetails od " +
                "JOIN FETCH od.product " +
                "WHERE o.id = :orderId";
        return session.createQuery(hql, Orders.class)
                .setParameter("orderId", orderId)
                .uniqueResultOptional();
    }

    public List<Orders> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT o FROM Orders o JOIN FETCH o.user ORDER BY o.id DESC", Orders.class).list();
        }
    }

    public List<Orders> findOrdersByUser(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Orders o WHERE o.user.id = :uid", Orders.class)
                    .setParameter("uid", userId)
                    .list();
        }
    }

    public Orders update(Orders order) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            Orders merged = session.merge(order);
            transaction.commit();
            return merged;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (Exception rbEx) {
                    // Suppress
                }
            }
            throw e;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public Orders update(Session session, Orders order) {
        return session.merge(order);
    }

    public boolean delete(Long id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            Orders order = session.get(Orders.class, id);
            if (order != null) {
                session.remove(order);
                transaction.commit();
                return true;
            }
            transaction.commit();
            return false;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (Exception rbEx) {
                    // Suppress
                }
            }
            throw e;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
