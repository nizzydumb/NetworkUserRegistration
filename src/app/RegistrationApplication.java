package app;

import javafx.application.Application;
import javafx.application.Platform;
import logging.AppLogger;
import repository.InMemoryRegistrationRepository;
import security.AuthService;
import security.AuthStorage;
import security.PasswordHasher;
import service.RegistrationService;
import ui.AuthWindow;
import ui.MainWindow;

public class RegistrationApplication extends Application {
    @Override
    public void init() {
        AppLogger.info("PROGRAM_STARTED");
    }

    @Override
    public void start(javafx.stage.Stage stage) {
        AuthService authService = new AuthService(new AuthStorage(), new PasswordHasher());
        AuthWindow authWindow = new AuthWindow(authService);
        if (!authWindow.show(stage)) {
            Platform.exit();
            return;
        }

        RegistrationService service = new RegistrationService(new InMemoryRegistrationRepository());
        MainWindow mainWindow = new MainWindow(service);
        mainWindow.show(stage);
    }

    @Override
    public void stop() {
        AppLogger.info("PROGRAM_ENDED");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
