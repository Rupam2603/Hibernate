package com.ecommerce.dao;

import com.ecommerce.entity.OrderDetails;
import com.ecommerce.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for OrderDetails entity operations.
 */
public class OrderDetailsDAO {

    public OrderDetails save(OrderDetails orderDetails) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            session.persist(orderDetails);
            transaction.commit();
            return orderDetails;
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

    public OrderDetails save(Session session, OrderDetails orderDetails) {
        session.persist(orderDetails);
        return orderDetails;
    }

    public Optional<OrderDetails> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(OrderDetails.class, id));
        }
    }

    public Optional<OrderDetails> findById(Session session, Long id) {
        return Optional.ofNullable(session.get(OrderDetails.class, id));
    }

    public List<OrderDetails> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM OrderDetails", OrderDetails.class).list();
        }
    }

    public OrderDetails update(OrderDetails orderDetails) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            OrderDetails merged = session.merge(orderDetails);
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

    public OrderDetails update(Session session, OrderDetails orderDetails) {
        return session.merge(orderDetails);
    }

    public boolean delete(Long id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            OrderDetails orderDetails = session.get(OrderDetails.class, id);
            if (orderDetails != null) {
                session.remove(orderDetails);
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
