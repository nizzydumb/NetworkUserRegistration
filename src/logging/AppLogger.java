package logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Central application audit logger backed by an append-only log file. */
public final class AppLogger {
    private static final Logger LOGGER = createLogger();

    private AppLogger() {
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warning(String message) {
        LOGGER.warning(message);
    }

    public static void error(String message, Throwable exception) {
        LOGGER.log(Level.SEVERE, message, exception);
    }

    private static Logger createLogger() {
        Logger logger = Logger.getLogger("NetworkUserRegistration");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        Path logDirectory = Paths.get(System.getProperty("user.dir"), "logs");
        try {
            Files.createDirectories(logDirectory);
            FileHandler fileHandler = new FileHandler(
                    logDirectory.resolve("application.log").toString(),
                    true
            );
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException | SecurityException exception) {
            System.err.println("Could not initialize application logging: " + exception.getMessage());
        }
        return logger;
    }
}
