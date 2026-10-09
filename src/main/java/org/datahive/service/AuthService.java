package org.datahive.service;

import org.datahive.dao.UserDao;
import org.datahive.model.User;
import org.datahive.security.PasswordHasher;

import java.sql.SQLException;
import java.util.Optional;

public final class AuthService {
    private final UserDao userDao;

    public AuthService() {
        this(new UserDao());
    }

    AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public Optional<User> authenticate(String email, String password) throws SQLException {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        return userDao.findForLogin(email.trim())
                .filter(record -> record.user().isActive())
                .filter(record -> PasswordHasher.verify(password, record.passwordHash()))
                .map(UserDao.AuthenticatedUser::user);
    }
}
