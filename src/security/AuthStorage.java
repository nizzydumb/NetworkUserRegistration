package security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class AuthStorage {
    private static final String APP_FOLDER = "NetworkUserRegistration";
    private static final String AUTH_FILE = "auth.properties";

    private final Path authFile;

    public AuthStorage() {
        this(resolveDefaultAuthFile());
    }

    public AuthStorage(Path authFile) {
        this.authFile = authFile;
    }

    public Path authFile() {
        return authFile;
    }

    public boolean credentialsFileExists() {
        return Files.exists(authFile);
    }

    public UserCredentials load() {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(authFile)) {
            properties.load(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read authentication file.", exception);
        }

        String username = require(properties, "username");
        String passwordHash = require(properties, "passwordHash");
        String passwordSalt = require(properties, "passwordSalt");
        int iterations = Integer.parseInt(require(properties, "iterations"));
        String algorithm = require(properties, "algorithm");

        return new UserCredentials(username, passwordHash, passwordSalt, iterations, algorithm);
    }

    public void save(UserCredentials credentials) {
        Properties properties = new Properties();
        properties.setProperty("username", credentials.username());
        properties.setProperty("passwordHash", credentials.passwordHash());
        properties.setProperty("passwordSalt", credentials.passwordSalt());
        properties.setProperty("iterations", Integer.toString(credentials.iterations()));
        properties.setProperty("algorithm", credentials.algorithm());

        try {
            Files.createDirectories(authFile.getParent());
            try (OutputStream outputStream = Files.newOutputStream(authFile)) {
                properties.store(outputStream, "Network User Registration authentication");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save authentication file.", exception);
        }
    }

    private static Path resolveDefaultAuthFile() {
        String appData = System.getenv("APPDATA");
        Path baseDir = appData == null || appData.isBlank()
                ? Paths.get(System.getProperty("user.home"), "." + APP_FOLDER)
                : Paths.get(appData, APP_FOLDER);
        return baseDir.resolve(AUTH_FILE);
    }

    private String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Authentication file is missing " + key + ".");
        }
        return value;
    }
}
