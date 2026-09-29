package com.ecommerce.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for BCrypt password hashing and verification.
 * Guarantees that plain text passwords are never stored in the database.
 */
public final class PasswordUtil {

    private PasswordUtil() {
        // Utility class private constructor
    }

    /**
     * Hashes a plain text password using BCrypt with a secure salt.
     *
     * @param plainPassword the plain text password to hash
     * @return the resulting BCrypt hash string
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    /**
     * Verifies a plain text password against a stored BCrypt hash.
     *
     * @param plainPassword the plain text candidate password
     * @param hashedPassword the stored BCrypt hash
     * @return true if password matches, false otherwise
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
