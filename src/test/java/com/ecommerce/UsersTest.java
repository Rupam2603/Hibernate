package com.ecommerce;

import com.ecommerce.entity.Users;
import com.ecommerce.enumtype.UserRole;
import com.ecommerce.service.UsersService;
import com.ecommerce.util.HibernateUtil;
import com.ecommerce.util.PasswordUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UsersTest {

    private UsersService usersService;

    @BeforeAll
    static void initHibernate() {
        try {
            HibernateUtil.setSessionFactory(HibernateUtil.buildCustomSessionFactory("hibernate-test.cfg.xml"));
        } catch (Exception ignored) {
        }
    }

    @BeforeEach
    void setUp() {
        usersService = new UsersService();
    }

    @Test
    void testCreateUserAndBCryptHashing() {
        long ts = System.currentTimeMillis();
        String plainPassword = "SuperSecurePassword#2026";
        Users user = usersService.createUser("user_" + ts, plainPassword, "user_" + ts + "@example.com", UserRole.CUSTOMER);

        assertNotNull(user.getId());
        assertEquals("user_" + ts, user.getUsername());
        assertEquals("user_" + ts + "@example.com", user.getEmail());
        assertEquals(UserRole.CUSTOMER, user.getRole());

        // Password MUST NOT be stored in plain text
        assertNotEquals(plainPassword, user.getPassword());
        assertTrue(user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$") || user.getPassword().startsWith("$2y$"));

        // BCrypt verification
        assertTrue(PasswordUtil.checkPassword(plainPassword, user.getPassword()));
        assertFalse(PasswordUtil.checkPassword("WrongPassword", user.getPassword()));
    }

    @Test
    void testAuthenticateUser() {
        long ts = System.currentTimeMillis();
        usersService.createUser("auth_" + ts, "MyPassword123", "auth_" + ts + "@example.com", UserRole.ADMIN);

        assertTrue(usersService.authenticateUser("auth_" + ts, "MyPassword123"));
        assertFalse(usersService.authenticateUser("auth_" + ts, "BadPassword"));
        assertFalse(usersService.authenticateUser("nonexistent_" + ts, "MyPassword123"));
    }

    @Test
    void testFindUserByUsername() {
        long ts = System.currentTimeMillis();
        usersService.createUser("findme_" + ts, "Secret999", "findme_" + ts + "@example.com", UserRole.CUSTOMER);

        Optional<Users> found = usersService.getUserByUsername("findme_" + ts);
        assertTrue(found.isPresent());
        assertEquals("findme_" + ts + "@example.com", found.get().getEmail());
    }

    @Test
    void testUpdateUser() {
        long ts = System.currentTimeMillis();
        Users created = usersService.createUser("update_" + ts, "PassInitial1", "update_" + ts + "@example.com", UserRole.CUSTOMER);
        Users updated = usersService.updateUser(created.getId(), "NewPassword456", "new_" + ts + "@example.com", UserRole.ADMIN);

        assertEquals("new_" + ts + "@example.com", updated.getEmail());
        assertEquals(UserRole.ADMIN, updated.getRole());
        assertTrue(PasswordUtil.checkPassword("NewPassword456", updated.getPassword()));
    }

    @Test
    void testDeleteUser() {
        long ts = System.currentTimeMillis();
        Users created = usersService.createUser("del_" + ts, "PassDelete1", "del_" + ts + "@example.com", UserRole.CUSTOMER);
        Long id = created.getId();

        boolean deleted = usersService.deleteUser(id);
        assertTrue(deleted);

        Optional<Users> found = usersService.getUserById(id);
        assertFalse(found.isPresent());
    }
}
