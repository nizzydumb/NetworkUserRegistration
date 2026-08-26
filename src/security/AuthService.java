package security;

import logging.AppLogger;

import java.util.Arrays;

public class AuthService {
    private final AuthStorage storage;
    private final PasswordHasher passwordHasher;

    public AuthService(AuthStorage storage, PasswordHasher passwordHasher) {
        this.storage = storage;
        this.passwordHasher = passwordHasher;
    }

    public boolean setupRequired() {
        if (!storage.credentialsFileExists()) {
            return true;
        }

        try {
            storage.load();
            return false;
        } catch (RuntimeException exception) {
            AppLogger.warning("AUTH_CONFIGURATION_INVALID reason=\"" + safe(exception.getMessage()) + "\"");
            return true;
        }
    }

    public String authFilePath() {
        return storage.authFile().toString();
    }

    public void createAdmin(String username, char[] password, char[] confirmation) {
        try {
            validateAdminCredentials(username, password, confirmation);

            String salt = passwordHasher.newSalt();
            String hash = passwordHasher.hash(password, salt, PasswordHasher.ITERATIONS, PasswordHasher.ALGORITHM);
            storage.save(new UserCredentials(
                    username.trim(),
                    hash,
                    salt,
                    PasswordHasher.ITERATIONS,
                    PasswordHasher.ALGORITHM
            ));
            AppLogger.info("ADMIN_ACCOUNT_CREATED username=\"" + safe(username.trim()) + "\"");
        } catch (RuntimeException exception) {
            AppLogger.warning("ADMIN_ACCOUNT_CREATION_FAILED username=\"" + safe(username) + "\" reason=\""
                    + safe(exception.getMessage()) + "\"");
            throw exception;
        } finally {
            clear(password);
            clear(confirmation);
        }
    }

    public String validateAdminCredentialsMessage(String username, char[] password, char[] confirmation) {
        try {
            validateAdminCredentials(username, password, confirmation);
            return "";
        } catch (IllegalArgumentException exception) {
            return exception.getMessage();
        } finally {
            clear(password);
            clear(confirmation);
        }
    }

    public boolean login(String username, char[] password) {
        try {
            UserCredentials credentials = storage.load();
            String normalizedUsername = username == null ? "" : username.trim();
            boolean authenticated = credentials.username().equals(normalizedUsername)
                    && passwordHasher.matches(password, credentials);
            AppLogger.info("AUTHENTICATION_" + (authenticated ? "SUCCEEDED" : "FAILED")
                    + " username=\"" + safe(normalizedUsername) + "\"");
            return authenticated;
        } catch (RuntimeException exception) {
            AppLogger.error("AUTHENTICATION_ERROR username=\"" + safe(username) + "\"", exception);
            throw exception;
        } finally {
            clear(password);
        }
    }

    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
    }

    private void validateAdminCredentials(String username, char[] password, char[] confirmation) {
        validateUsername(username);
        validatePassword(password, confirmation);
    }

    private void validatePassword(char[] password, char[] confirmation) {
        if (!Arrays.equals(password, confirmation)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (password.length < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;

        for (char character : password) {
            hasUppercase = hasUppercase || Character.isUpperCase(character);
            hasLowercase = hasLowercase || Character.isLowerCase(character);
            hasDigit = hasDigit || Character.isDigit(character);
        }

        if (!hasUppercase || !hasLowercase || !hasDigit) {
            throw new IllegalArgumentException("Password must contain uppercase, lowercase, and digit characters.");
        }
    }

    private void clear(char[] value) {
        if (value != null) {
            Arrays.fill(value, '\0');
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').replace('"', '\'');
    }
}
