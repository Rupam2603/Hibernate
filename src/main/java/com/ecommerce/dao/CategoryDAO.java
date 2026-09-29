package com.ecommerce.dao;

import com.ecommerce.entity.Category;
import com.ecommerce.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Category entity operations.
 */
public class CategoryDAO {

    public Category save(Category category) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            session.persist(category);
            transaction.commit();
            return category;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (Exception rbEx) {
                    // Suppress rollback exception to rethrow original cause
                }
            }
            throw e;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public Category save(Session session, Category category) {
        session.persist(category);
        return category;
    }

    public Optional<Category> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Category.class, id));
        }
    }

    public Optional<Category> findById(Session session, Long id) {
        return Optional.ofNullable(session.get(Category.class, id));
    }

    public List<Category> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Category", Category.class).list();
        }
    }

    public Category update(Category category) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            Category merged = session.merge(category);
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

    public Category update(Session session, Category category) {
        return session.merge(category);
    }

    public boolean delete(Long id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            Category category = session.get(Category.class, id);
            if (category != null) {
                session.remove(category);
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

    public boolean delete(Session session, Long id) {
        Category category = session.get(Category.class, id);
        if (category != null) {
            session.remove(category);
            return true;
        }
        return false;
    }
}
