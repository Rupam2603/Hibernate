package com.ecommerce.service;

import com.ecommerce.dao.UsersDAO;
import com.ecommerce.entity.Users;
import com.ecommerce.enumtype.UserRole;
import com.ecommerce.exception.UserNotFoundException;
import com.ecommerce.exception.ValidationException;
import com.ecommerce.util.PasswordUtil;

import java.util.List;
import java.util.Optional;

/**
 * Service handling business logic, BCrypt security, and validations for Users entity.
 */
public class UsersService {

    private final UsersDAO usersDAO;

    public UsersService() {
        this.usersDAO = new UsersDAO();
    }

    public UsersService(UsersDAO usersDAO) {
        this.usersDAO = usersDAO;
    }

    public Users createUser(String username, String plainPassword, String email, UserRole role) {
        validateUserData(username, plainPassword, email);
        if (role == null) {
            role = UserRole.CUSTOMER;
        }

        // Hash the password using BCrypt - NEVER store plain text
        String hashedPassword = PasswordUtil.hashPassword(plainPassword);

        Users user = new Users(username.trim(), hashedPassword, email.trim(), role);
        return usersDAO.save(user);
    }

    public Optional<Users> getUserById(Long id) {
        if (id == null) {
            throw new ValidationException("User ID cannot be null");
        }
        return usersDAO.findById(id);
    }

    public Optional<Users> getUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be null or empty");
        }
        return usersDAO.findByUsername(username.trim());
    }

    public List<Users> getAllUsers() {
        return usersDAO.findAll();
    }

    /**
     * Authenticates a user by validating the provided plain password against the stored BCrypt hash.
     *
     * @param username the username
     * @param plainPassword the plain text password attempt
     * @return true if credentials are valid, false otherwise
     */
    public boolean authenticateUser(String username, String plainPassword) {
        if (username == null || plainPassword == null) {
            return false;
        }
        return usersDAO.findByUsername(username)
                .map(user -> PasswordUtil.checkPassword(plainPassword, user.getPassword()))
                .orElse(false);
    }

    public Users updateUser(Long id, String plainPasswordOrNull, String email, UserRole role) {
        if (id == null) {
            throw new ValidationException("User ID cannot be null");
        }
        Users user = usersDAO.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));

        if (email != null && !email.trim().isEmpty()) {
            user.setEmail(email.trim());
        }
        if (role != null) {
            user.setRole(role);
        }
        if (plainPasswordOrNull != null && !plainPasswordOrNull.trim().isEmpty()) {
            user.setPassword(PasswordUtil.hashPassword(plainPasswordOrNull));
        }

        return usersDAO.update(user);
    }

    public boolean deleteUser(Long id) {
        if (id == null) {
            throw new ValidationException("User ID cannot be null");
        }
        return usersDAO.delete(id);
    }

    private void validateUserData(String username, String password, String email) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("Password cannot be null or empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email cannot be null or empty");
        }
    }
}
