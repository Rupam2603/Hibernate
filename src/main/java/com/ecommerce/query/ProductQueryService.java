package com.ecommerce.query;

import com.ecommerce.entity.Product;
import com.ecommerce.util.HibernateUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service demonstrating JPQL Named Queries, JPA CriteriaBuilder API, and Pagination.
 */
public class ProductQueryService {

    /**
     * Demonstrates execution of a JPA @NamedQuery defined on the Product entity.
     * Fetches non-deleted products belonging to a given category.
     *
     * @param categoryId the Category identifier
     * @return list of matching Product entities
     */
    public List<Product> findProductsByCategoryNamedQuery(Long categoryId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createNamedQuery("Product.findByCategory", Product.class)
                    .setParameter("categoryId", categoryId)
                    .getResultList();
        }
    }

    /**
     * Demonstrates dynamic querying using the JPA Criteria API (CriteriaBuilder, CriteriaQuery, Root, Predicate).
     * Finds active products where price > specified amount AND stockQuantity > 0.
     *
     * @param minPrice minimum price threshold
     * @return list of matching available products
     */
    public List<Product> findAvailableProductsAbovePrice(BigDecimal minPrice) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Product> cq = cb.createQuery(Product.class);
            Root<Product> root = cq.from(Product.class);

            // Predicates: price > minPrice AND stockQuantity > 0 AND deleted = false
            Predicate pricePredicate = cb.greaterThan(root.get("price"), minPrice);
            Predicate stockPredicate = cb.greaterThan(root.get("stockQuantity"), 0);
            Predicate activePredicate = cb.isFalse(root.get("deleted"));

            cq.select(root).where(cb.and(pricePredicate, stockPredicate, activePredicate));
            cq.orderBy(cb.asc(root.get("price")));

            return session.createQuery(cq).getResultList();
        }
    }

    /**
     * Demonstrates database pagination using query.setFirstResult and query.setMaxResults.
     *
     * @param page zero-based page index (0 = first page)
     * @param pageSize number of records per page
     * @return list of products for the requested page
     */
    public List<Product> findProductsPaged(int page, int pageSize) {
        if (page < 0) page = 0;
        if (pageSize <= 0) pageSize = 10;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Product p WHERE p.deleted = false ORDER BY p.id ASC", Product.class)
                    .setFirstResult(page * pageSize)
                    .setMaxResults(pageSize)
                    .getResultList();
        }
    }
}
