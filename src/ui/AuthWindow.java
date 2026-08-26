package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import security.AuthService;

import java.net.URL;
import java.util.Arrays;

public class AuthWindow {
    private final AuthService authService;

    public AuthWindow(AuthService authService) {
        this.authService = authService;
    }

    public boolean show(Stage owner) {
        return authService.setupRequired() ? showSetup(owner) : showLogin(owner);
    }

    private boolean showSetup(Stage owner) {
        Stage stage = createStage(owner, "Create Admin Account");

        TextField usernameField = new TextField();
        usernameField.setPromptText("admin");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        PasswordField confirmationField = new PasswordField();
        confirmationField.setPromptText("Confirm password");

        GridPane form = createForm();
        form.addRow(0, new Label("Username"), usernameField);
        form.addRow(1, new Label("Password"), passwordField);
        form.addRow(2, new Label("Confirm"), confirmationField);

        Label title = new Label("Create Admin Account");
        title.getStyleClass().add("auth-title");

        Label hint = new Label("First launch detected. Credentials will be stored in " + authService.authFilePath());
        hint.setWrapText(true);
        hint.getStyleClass().add("auth-hint");

        Label validationMessage = new Label();
        validationMessage.setWrapText(true);
        validationMessage.getStyleClass().add("auth-validation");

        Button createButton = new Button("Create Account");
        createButton.getStyleClass().add("primary-button");
        createButton.setMinWidth(150);
        createButton.setDisable(true);

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setMinWidth(110);

        final boolean[] authenticated = {false};
        Runnable updateValidation = () -> updateSetupValidation(
                usernameField,
                passwordField,
                confirmationField,
                validationMessage,
                createButton
        );
        usernameField.textProperty().addListener((obs, oldValue, newValue) -> updateValidation.run());
        passwordField.textProperty().addListener((obs, oldValue, newValue) -> updateValidation.run());
        confirmationField.textProperty().addListener((obs, oldValue, newValue) -> updateValidation.run());
        updateValidation.run();

        createButton.setOnAction(event -> {
            try {
                authService.createAdmin(
                        usernameField.getText(),
                        passwordField.getText().toCharArray(),
                        confirmationField.getText().toCharArray()
                );
                authenticated[0] = true;
                stage.close();
            } catch (RuntimeException exception) {
                validationMessage.setText(exception.getMessage());
                validationMessage.getStyleClass().remove("auth-validation-ok");
                validationMessage.getStyleClass().add("auth-validation-error");
            } finally {
                passwordField.clear();
                confirmationField.clear();
            }
        });
        cancelButton.setOnAction(event -> stage.close());

        stage.setScene(createScene(title, hint, validationMessage, form, createButton, cancelButton));
        stage.showAndWait();
        return authenticated[0];
    }

    private boolean showLogin(Stage owner) {
        Stage stage = createStage(owner, "Login");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        GridPane form = createForm();
        form.addRow(0, new Label("Username"), usernameField);
        form.addRow(1, new Label("Password"), passwordField);

        Label title = new Label("Login");
        title.getStyleClass().add("auth-title");

        Label hint = new Label("Enter local administrator credentials.");
        hint.getStyleClass().add("auth-hint");

        Label validationMessage = new Label();
        validationMessage.setWrapText(true);
        validationMessage.getStyleClass().add("auth-validation");

        Button loginButton = new Button("Login");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMinWidth(150);

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setMinWidth(110);

        final boolean[] authenticated = {false};
        loginButton.setOnAction(event -> {
            if (authService.login(usernameField.getText(), passwordField.getText().toCharArray())) {
                authenticated[0] = true;
                stage.close();
            } else {
                validationMessage.setText("Username or password is incorrect.");
                validationMessage.getStyleClass().remove("auth-validation-ok");
                validationMessage.getStyleClass().add("auth-validation-error");
                passwordField.clear();
            }
        });
        cancelButton.setOnAction(event -> stage.close());

        stage.setScene(createScene(title, hint, validationMessage, form, loginButton, cancelButton));
        validationMessage.setText("");
        stage.showAndWait();
        return authenticated[0];
    }

    private Stage createStage(Stage owner, String title) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        WindowIcons.apply(stage);
        return stage;
    }

    private Scene createScene(Label title, Label hint, Label validationMessage, GridPane form, Button primaryButton, Button secondaryButton) {
        HBox actions = new HBox(10, secondaryButton, primaryButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, title, hint, validationMessage, form, actions);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("auth-root");

        Scene scene = new Scene(root, 480, 340);
        URL stylesheet = AuthWindow.class.getResource("app.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        return scene;
    }

    private void updateSetupValidation(
            TextField usernameField,
            PasswordField passwordField,
            PasswordField confirmationField,
            Label validationMessage,
            Button createButton
    ) {
        char[] password = passwordField.getText().toCharArray();
        char[] confirmation = confirmationField.getText().toCharArray();
        String message = authService.validateAdminCredentialsMessage(usernameField.getText(), password, confirmation);

        if (message.isEmpty()) {
            validationMessage.setText("Password requirements are satisfied.");
            validationMessage.getStyleClass().remove("auth-validation-error");
            validationMessage.getStyleClass().add("auth-validation-ok");
            createButton.setDisable(false);
        } else {
            validationMessage.setText(message);
            validationMessage.getStyleClass().remove("auth-validation-ok");
            validationMessage.getStyleClass().add("auth-validation-error");
            createButton.setDisable(true);
        }

        Arrays.fill(password, '\0');
        Arrays.fill(confirmation, '\0');
    }

    private GridPane createForm() {
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);

        javafx.scene.layout.ColumnConstraints labelColumn = new javafx.scene.layout.ColumnConstraints();
        labelColumn.setMinWidth(100);

        javafx.scene.layout.ColumnConstraints fieldColumn = new javafx.scene.layout.ColumnConstraints();
        fieldColumn.setMinWidth(280);
        fieldColumn.setHgrow(Priority.ALWAYS);

        form.getColumnConstraints().addAll(labelColumn, fieldColumn);
        return form;
    }

    private static class WindowIcons {
        private static void apply(Stage stage) {
            int[] iconSizes = {16, 24, 32, 48, 64, 128, 256};
            for (int size : iconSizes) {
                URL iconUrl = AuthWindow.class.getResource("/assets/app-icon-" + size + ".png");
                if (iconUrl != null) {
                    stage.getIcons().add(new Image(iconUrl.toExternalForm()));
                }
            }
        }
    }
}
