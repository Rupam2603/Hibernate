package com.ecommerce.util;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to configure and manage the Hibernate SessionFactory singleton.
 */
public class HibernateUtil {

    private static final Logger logger = LoggerFactory.getLogger(HibernateUtil.class);
    private static SessionFactory sessionFactory = buildSessionFactory();

    /**
     * Builds the standard SessionFactory using hibernate.cfg.xml.
     * Supports environment variable / system property overrides for database credentials:
     * - DB_URL / db.url
     * - DB_USER / db.user
     * - DB_PASSWORD / db.password
     */
    private static SessionFactory buildSessionFactory() {
        try {
            StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
                    .configure("hibernate.cfg.xml");

            // Apply environment or system property overrides if provided
            String dbUrl = System.getProperty("db.url", System.getenv("DB_URL"));
            if (dbUrl != null && !dbUrl.trim().isEmpty()) {
                registryBuilder.applySetting("hibernate.connection.url", dbUrl);
            }

            String dbUser = System.getProperty("db.user", System.getenv("DB_USER"));
            if (dbUser != null && !dbUser.trim().isEmpty()) {
                registryBuilder.applySetting("hibernate.connection.username", dbUser);
            }

            String dbPassword = System.getProperty("db.password", System.getenv("DB_PASSWORD"));
            if (dbPassword != null) {
                registryBuilder.applySetting("hibernate.connection.password", dbPassword);
            }

            StandardServiceRegistry registry = registryBuilder.build();
            MetadataSources sources = new MetadataSources(registry);
            Metadata metadata = sources.getMetadataBuilder().build();

            return metadata.getSessionFactoryBuilder().build();
        } catch (Exception ex) {
            logger.error("Initial SessionFactory creation failed: {}", ex.getMessage());
            // Fallback to in-memory MySQL-compatible SessionFactory if external MySQL is unreachable
            try {
                logger.info("External MySQL is unreachable. Falling back to in-memory H2 MySQL-compatible SessionFactory...");
                return buildCustomSessionFactory("hibernate-fallback.cfg.xml");
            } catch (Exception fallbackEx) {
                logger.error("Fallback SessionFactory creation also failed.", fallbackEx);
                throw new ExceptionInInitializerError(ex);
            }
        }
    }

    /**
     * Builds a SessionFactory from a specific configuration file (e.g. for testing).
     *
     * @param configFile the resource path to the hibernate configuration xml file
     * @return a new SessionFactory instance
     */
    public static SessionFactory buildCustomSessionFactory(String configFile) {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure(configFile)
                .build();
        MetadataSources sources = new MetadataSources(registry);
        Metadata metadata = sources.getMetadataBuilder().build();
        return metadata.getSessionFactoryBuilder().build();
    }

    /**
     * Replaces the active session factory (e.g., in test setup).
     *
     * @param customFactory the custom SessionFactory to set
     */
    public static synchronized void setSessionFactory(SessionFactory customFactory) {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
        sessionFactory = customFactory;
    }

    /**
     * Returns the singleton SessionFactory instance.
     *
     * @return the active SessionFactory
     */
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            sessionFactory = buildSessionFactory();
        }
        return sessionFactory;
    }

    /**
     * Closes the active SessionFactory and releases all resources.
     */
    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
        try {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Throwable ignored) {
        }
    }
}
