package com.ecommerce.dao;

import com.ecommerce.entity.Product;
import com.ecommerce.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Product entity operations.
 */
public class ProductDAO {

    public Product save(Product product) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            session.persist(product);
            transaction.commit();
            return product;
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

    public Product save(Session session, Product product) {
        session.persist(product);
        return product;
    }

    public Optional<Product> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Product.class, id));
        }
    }

    public Optional<Product> findById(Session session, Long id) {
        return Optional.ofNullable(session.get(Product.class, id));
    }

    public List<Product> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Product p WHERE p.deleted = false", Product.class).list();
        }
    }

    public List<Product> findAllIncludingDeleted() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Product", Product.class).list();
        }
    }

    public List<Product> findByCategory(Long categoryId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Product p WHERE p.category.id = :catId AND p.deleted = false", Product.class)
                    .setParameter("catId", categoryId)
                    .list();
        }
    }

    public Product update(Product product) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            Product merged = session.merge(product);
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

    public Product update(Session session, Product product) {
        return session.merge(product);
    }

    public boolean delete(Long id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            Product product = session.get(Product.class, id);
            if (product != null) {
                session.remove(product);
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

    public boolean softDelete(Long id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            Product product = session.get(Product.class, id);
            if (product != null) {
                product.setDeleted(true);
                session.merge(product);
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
