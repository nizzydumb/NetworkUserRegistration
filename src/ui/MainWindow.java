package ui;

import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Network;
import model.NetworkUser;
import service.RegistrationService;

import java.net.URL;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MainWindow {
    private final RegistrationService service;
    private final ObservableList<Network> networks = FXCollections.observableArrayList();
    private final ObservableList<NetworkUser> visibleUsers = FXCollections.observableArrayList();

    private ListView<Network> networkList;
    private TableView<NetworkUser> userTable;
    private Button addUserButton;
    private Button editNetworkButton;
    private Button editUserButton;
    private Circle syncStatusLed;
    private Label syncStatusLabel;

    public MainWindow(RegistrationService service) {
        this.service = service;
    }

    public void show(Stage stage) {
        networks.setAll(service.getNetworks());

        BorderPane root = new BorderPane();
        root.setTop(createHeader());
        root.setCenter(createBody(stage));
        root.setBottom(createSyncStatusBar());
        root.getStyleClass().add("app-root");

        Scene scene = new Scene(root, 1000, 620);
        URL stylesheet = MainWindow.class.getResource("app.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle("Network User Registration");
        setWindowIcon(stage);
        stage.setMinWidth(850);
        stage.setMinHeight(520);
        stage.setScene(scene);
        stage.show();

        if (!networks.isEmpty()) {
            networkList.getSelectionModel().selectFirst();
        }

        startSyncStatusDemo();
    }

    private void setWindowIcon(Stage stage) {
        int[] iconSizes = {16, 24, 32, 48, 64, 128, 256};
        for (int size : iconSizes) {
            URL iconUrl = MainWindow.class.getResource("/assets/app-icon-" + size + ".png");
            if (iconUrl != null) {
                stage.getIcons().add(new Image(iconUrl.toExternalForm()));
            }
        }
    }

    private HBox createHeader() {
        Label title = new Label("Network User Registration");
        title.getStyleClass().add("header-title");

        Label subtitle = new Label("Manage networks and their registered users");
        subtitle.getStyleClass().add("header-subtitle");

        VBox text = new VBox(2, title, subtitle);

        HBox header = new HBox(text);
        header.setPadding(new Insets(18, 22, 16, 22));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("app-header");
        return header;
    }

    private SplitPane createBody(Stage owner) {
        VBox networksPanel = createNetworksPanel(owner);
        VBox usersPanel = createUsersPanel(owner);

        SplitPane splitPane = new SplitPane(networksPanel, usersPanel);
        splitPane.setDividerPositions(0.36);
        splitPane.setPadding(new Insets(18));
        splitPane.getStyleClass().add("content-split");
        return splitPane;
    }

    private HBox createSyncStatusBar() {
        syncStatusLed = new Circle(5);
        syncStatusLed.getStyleClass().add("sync-led");
        syncStatusLabel = new Label();
        syncStatusLabel.getStyleClass().add("sync-label");

        HBox syncTab = new HBox(7, syncStatusLed, syncStatusLabel);
        syncTab.setAlignment(Pos.CENTER_LEFT);
        syncTab.setPadding(new Insets(5, 10, 5, 10));
        syncTab.getStyleClass().add("sync-tab");

        HBox statusBar = new HBox(syncTab);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(0, 18, 12, 18));
        statusBar.getStyleClass().add("status-bar");

        setSyncStatus(SyncStatus.ERROR);
        return statusBar;
    }

    private void startSyncStatusDemo() {
        PauseTransition switchToInit = new PauseTransition(Duration.seconds(5));
        switchToInit.setOnFinished(event -> setSyncStatus(SyncStatus.INITIALIZING));

        PauseTransition switchToOk = new PauseTransition(Duration.seconds(5));
        switchToOk.setOnFinished(event -> setSyncStatus(SyncStatus.OK));

        new SequentialTransition(switchToInit, switchToOk).play();
    }

    private void setSyncStatus(SyncStatus status) {
        syncStatusLed.setFill(status.color);
        syncStatusLabel.setText(status.label);
    }

    private VBox createNetworksPanel(Stage owner) {
        Label title = new Label("Networks");
        title.getStyleClass().add("panel-title");

        Button addNetworkButton = new Button("Add Network");
        addNetworkButton.getStyleClass().add("primary-button");
        addNetworkButton.setOnAction(event -> showAddNetworkDialog(owner));

        editNetworkButton = new Button("Edit Selected");
        editNetworkButton.getStyleClass().add("secondary-button");
        editNetworkButton.setDisable(true);
        editNetworkButton.setOnAction(event -> showEditNetworkDialog(owner));

        networkList = new ListView<>(networks);
        networkList.getStyleClass().add("material-list");
        networkList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Network network, boolean empty) {
                super.updateItem(network, empty);
                if (empty || network == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                    return;
                }

                Label name = new Label(network.name());
                name.getStyleClass().add("row-title");

                Label description = new Label(network.description());
                description.getStyleClass().add("row-description");

                VBox text = new VBox(3, name, description);
                HBox.setHgrow(text, Priority.ALWAYS);

                Button editButton = createEditIconButton();
                editButton.getStyleClass().addAll("row-action-button", "row-edit-button");
                editButton.setOnAction(event -> {
                    event.consume();
                    networkList.getSelectionModel().select(network);
                    showEditNetworkDialog((Stage) networkList.getScene().getWindow());
                });

                Button deleteButton = createDeleteIconButton();
                deleteButton.getStyleClass().addAll("row-action-button", "row-delete-button");
                deleteButton.setOnAction(event -> {
                    event.consume();
                    networkList.getSelectionModel().select(network);
                    deleteSelectedNetwork();
                });

                HBox rowActions = new HBox(4, editButton, deleteButton);
                rowActions.setAlignment(Pos.CENTER_RIGHT);

                HBox row = new HBox(12, text, rowActions);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setMaxWidth(Double.MAX_VALUE);
                row.prefWidthProperty().bind(networkList.widthProperty().subtract(34));
                row.getStyleClass().add("network-row-content");

                setText(null);
                setGraphic(row);
                setContextMenu(createNetworkContextMenu(network));
            }
        });
        networkList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && networkList.getSelectionModel().getSelectedItem() != null) {
                showEditNetworkDialog(owner);
            }
        });
        networkList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                deleteSelectedNetwork();
            }
        });
        networkList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selectedNetwork) -> {
            visibleUsers.setAll(service.getUsers(selectedNetwork));
            addUserButton.setDisable(selectedNetwork == null);
            editNetworkButton.setDisable(selectedNetwork == null);
            editUserButton.setDisable(true);
        });

        VBox.setVgrow(networkList, Priority.ALWAYS);

        HBox actions = new HBox(8, addNetworkButton, editNetworkButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("panel-actions");

        VBox panel = new VBox(10, title, networkList, actions);
        panel.setPadding(new Insets(14));
        panel.getStyleClass().add("surface-panel");
        return panel;
    }

    private VBox createUsersPanel(Stage owner) {
        Label title = new Label("Users");
        title.getStyleClass().add("panel-title");

        userTable = new TableView<>(visibleUsers);
        userTable.getStyleClass().add("material-table");
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);

        TableColumn<NetworkUser, String> idColumn = createColumn("ID", NetworkUser::hexId, Pos.CENTER_LEFT);
        idColumn.setMinWidth(120);
        idColumn.setPrefWidth(132);
        idColumn.setMaxWidth(150);
        idColumn.setResizable(false);
        idColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<NetworkUser, String> descriptionColumn = createColumn("Description", NetworkUser::description, Pos.CENTER_LEFT);
        descriptionColumn.setMinWidth(140);
        descriptionColumn.setPrefWidth(360);
        descriptionColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<NetworkUser, Void> actionsColumn = createActionsColumn();
        actionsColumn.setMinWidth(112);
        actionsColumn.setPrefWidth(112);
        actionsColumn.setMaxWidth(112);
        actionsColumn.setResizable(false);

        userTable.getColumns().add(idColumn);
        userTable.getColumns().add(descriptionColumn);
        userTable.getColumns().add(actionsColumn);
        userTable.setRowFactory(table -> {
            TableRow<NetworkUser> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    userTable.getSelectionModel().select(row.getItem());
                    showEditUserDialog(owner);
                }
            });
            row.itemProperty().addListener((obs, oldUser, user) ->
                    row.setContextMenu(user == null ? null : createUserContextMenu(user)));
            return row;
        });
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selectedUser) ->
        {
            editUserButton.setDisable(selectedUser == null);
        });
        userTable.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                deleteSelectedUser();
            }
        });

        addUserButton = new Button("Add User");
        addUserButton.getStyleClass().add("primary-button");
        addUserButton.setDisable(true);
        addUserButton.setOnAction(event -> showAddUserDialog(owner));

        editUserButton = new Button("Edit Selected");
        editUserButton.getStyleClass().add("secondary-button");
        editUserButton.setDisable(true);
        editUserButton.setOnAction(event -> showEditUserDialog(owner));

        VBox.setVgrow(userTable, Priority.ALWAYS);

        HBox actions = new HBox(8, addUserButton, editUserButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("panel-actions");

        VBox panel = new VBox(10, title, userTable, actions);
        panel.setPadding(new Insets(14));
        panel.getStyleClass().add("surface-panel");
        return panel;
    }

    private TableColumn<NetworkUser, String> createColumn(String title, UserValueProvider valueProvider, Pos headerAlignment) {
        TableColumn<NetworkUser, String> column = new TableColumn<>();
        column.setCellValueFactory(cell -> new SimpleStringProperty(valueProvider.get(cell.getValue())));

        Label header = new Label(title);
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(headerAlignment);
        header.getStyleClass().add("table-header-label");
        header.prefWidthProperty().bind(column.widthProperty().subtract(24));
        column.setGraphic(header);
        return column;
    }

    private TableColumn<NetworkUser, Void> createActionsColumn() {
        TableColumn<NetworkUser, Void> column = new TableColumn<>();

        Label header = new Label("Actions");
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-alignment: CENTER;");
        header.getStyleClass().add("table-header-label");
        header.prefWidthProperty().bind(column.widthProperty().subtract(24));
        column.setGraphic(header);
        column.setStyle("-fx-alignment: CENTER;");

        column.setCellFactory(tableColumn -> new TableCell<>() {
            private final Button editButton = createEditIconButton();
            private final Button deleteButton = createDeleteIconButton();
            private final HBox actions = new HBox(4, editButton, deleteButton);

            {
                actions.setAlignment(Pos.CENTER);
                actions.setMaxWidth(Double.MAX_VALUE);
                setAlignment(Pos.CENTER);
                setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
                getStyleClass().add("actions-cell");
                editButton.getStyleClass().addAll("row-action-button", "row-edit-button");
                editButton.setOnAction(event -> {
                    event.consume();
                    NetworkUser user = getTableRow().getItem();
                    if (user != null) {
                        userTable.getSelectionModel().select(user);
                        showEditUserDialog((Stage) userTable.getScene().getWindow());
                    }
                });

                deleteButton.getStyleClass().addAll("row-action-button", "row-delete-button");
                deleteButton.setOnAction(event -> {
                    event.consume();
                    NetworkUser user = getTableRow().getItem();
                    if (user != null) {
                        userTable.getSelectionModel().select(user);
                        deleteSelectedUser();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });

        return column;
    }

    private Button createEditIconButton() {
        SVGPath icon = new SVGPath();
        icon.setContent("M4 20h4l10.5-10.5-4-4L4 16v4 M13.5 6.5l4 4");
        icon.getStyleClass().add("edit-icon");

        Button button = new Button();
        button.setGraphic(icon);
        button.setFocusTraversable(false);
        button.setAccessibleText("Edit");
        return button;
    }

    private Button createDeleteIconButton() {
        SVGPath icon = new SVGPath();
        icon.setContent("M3 6h18 M8 6V4h8v2 M6 6l1 14h10l1-14 M10 10v7 M14 10v7");
        icon.getStyleClass().add("delete-icon");

        Button button = new Button();
        button.setGraphic(icon);
        button.setFocusTraversable(false);
        button.setAccessibleText("Delete");
        return button;
    }

    private ContextMenu createNetworkContextMenu(Network network) {
        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(event -> {
            networkList.getSelectionModel().select(network);
            showEditNetworkDialog((Stage) networkList.getScene().getWindow());
        });

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.getStyleClass().add("danger-menu-item");
        deleteItem.setOnAction(event -> {
            networkList.getSelectionModel().select(network);
            deleteSelectedNetwork();
        });

        ContextMenu menu = new ContextMenu(editItem, deleteItem);
        menu.getStyleClass().add("material-context-menu");
        return menu;
    }

    private ContextMenu createUserContextMenu(NetworkUser user) {
        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(event -> {
            userTable.getSelectionModel().select(user);
            showEditUserDialog((Stage) userTable.getScene().getWindow());
        });

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.getStyleClass().add("danger-menu-item");
        deleteItem.setOnAction(event -> {
            userTable.getSelectionModel().select(user);
            deleteSelectedUser();
        });

        ContextMenu menu = new ContextMenu(editItem, deleteItem);
        menu.getStyleClass().add("material-context-menu");
        return menu;
    }

    private void deleteSelectedNetwork() {
        Network selectedNetwork = networkList.getSelectionModel().getSelectedItem();
        if (selectedNetwork == null) {
            return;
        }

        if (!confirmDelete("Delete Network", "Delete " + selectedNetwork.name() + " and all its users?")) {
            return;
        }

        service.deleteNetwork(selectedNetwork);
        networks.setAll(service.getNetworks());
        visibleUsers.clear();
        if (!networks.isEmpty()) {
            networkList.getSelectionModel().selectFirst();
        }
    }

    private void deleteSelectedUser() {
        Network selectedNetwork = networkList.getSelectionModel().getSelectedItem();
        NetworkUser selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedNetwork == null || selectedUser == null) {
            return;
        }

        if (!confirmDelete("Delete User", "Delete user " + selectedUser.hexId() + "?")) {
            return;
        }

        service.deleteUser(selectedUser);
        visibleUsers.setAll(service.getUsers(selectedNetwork));
    }

    private void showAddNetworkDialog(Stage owner) {
        TextField nameField = new TextField();
        nameField.setPromptText("Office LAN");

        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Corporate office devices");

        TextField addressRangeField = new TextField();
        addressRangeField.setPromptText("192.168.1.0/24");

        TextField locationField = new TextField();
        locationField.setPromptText("Main office");
        setFormFieldWidths(nameField, descriptionField, addressRangeField, locationField);

        Dialog<NetworkFormData> dialog = createFormDialog(owner, "Add Network");
        GridPane form = createFormGrid();
        form.addRow(0, new Label("Name"), nameField);
        form.addRow(1, new Label("Description"), descriptionField);
        form.addRow(2, new Label("Address range"), addressRangeField);
        form.addRow(3, new Label("Location"), locationField);
        dialog.getDialogPane().setContent(form);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            NetworkFormData formData = new NetworkFormData(
                    nameField.getText(),
                    descriptionField.getText(),
                    addressRangeField.getText(),
                    locationField.getText()
            );
            runDialogButtonTask(dialog, okButton, "Creating...", () -> {
                simulateRemoteSyncDelay();
                return service.addNetwork(formData.name, formData.description, formData.addressRange, formData.location);
            }, network -> {
                dialog.setResult(formData);
                dialog.close();
                networks.setAll(service.getNetworks());
                networkList.getSelectionModel().select(network);
            });
        });

        dialog.showAndWait();
    }

    private void showEditNetworkDialog(Stage owner) {
        Network selectedNetwork = networkList.getSelectionModel().getSelectedItem();
        if (selectedNetwork == null) {
            return;
        }

        TextField idField = lockedTextField(selectedNetwork.id().toString());
        TextField nameField = new TextField(selectedNetwork.name());
        TextField descriptionField = new TextField(selectedNetwork.description());
        TextField addressRangeField = new TextField(selectedNetwork.addressRange());
        TextField locationField = new TextField(selectedNetwork.location());
        setFormFieldWidths(idField, nameField, descriptionField, addressRangeField, locationField);

        Dialog<Network> dialog = createFormDialog(owner, "Network Parameters");
        GridPane form = createFormGrid();
        form.addRow(0, new Label("ID"), idField);
        form.addRow(1, new Label("Name"), nameField);
        form.addRow(2, new Label("Description"), descriptionField);
        form.addRow(3, new Label("Address range"), addressRangeField);
        form.addRow(4, new Label("Location"), locationField);
        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            try {
                return service.updateNetwork(
                        selectedNetwork,
                        nameField.getText(),
                        descriptionField.getText(),
                        addressRangeField.getText(),
                        locationField.getText()
                );
            } catch (IllegalArgumentException exception) {
                showValidationError(exception.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(updatedNetwork -> {
            networks.setAll(service.getNetworks());
            networkList.getSelectionModel().select(updatedNetwork);
        });
    }

    private void showAddUserDialog(Stage owner) {
        Network selectedNetwork = networkList.getSelectionModel().getSelectedItem();
        if (selectedNetwork == null) {
            return;
        }

        TextField networkField = lockedTextField(selectedNetwork.name());

        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Help desk workstation");

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Aigerim Sadykova");

        TextField loginField = new TextField();
        loginField.setPromptText("asadykova");

        TextField ipAddressField = new TextField();
        ipAddressField.setPromptText("192.168.1.25");

        TextField roleField = new TextField();
        roleField.setPromptText("Operator");
        setFormFieldWidths(networkField, descriptionField, fullNameField, loginField, ipAddressField, roleField);

        Dialog<UserFormData> dialog = createFormDialog(owner, "Add User to " + selectedNetwork.name());
        GridPane form = createFormGrid();
        form.addRow(0, new Label("Network"), networkField);
        form.addRow(1, new Label("Description"), descriptionField);
        form.addRow(2, new Label("Full name"), fullNameField);
        form.addRow(3, new Label("Login"), loginField);
        form.addRow(4, new Label("IP address"), ipAddressField);
        form.addRow(5, new Label("Role"), roleField);
        dialog.getDialogPane().setContent(form);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            UserFormData formData = new UserFormData(
                    descriptionField.getText(),
                    fullNameField.getText(),
                    loginField.getText(),
                    ipAddressField.getText(),
                    roleField.getText()
            );
            runDialogButtonTask(dialog, okButton, "Creating...", () -> {
                simulateRemoteSyncDelay();
                return service.addUser(
                        selectedNetwork,
                        formData.description,
                        formData.fullName,
                        formData.login,
                        formData.ipAddress,
                        formData.role
                );
            }, user -> {
                dialog.setResult(formData);
                dialog.close();
                visibleUsers.setAll(service.getUsers(selectedNetwork));
            });
        });

        dialog.showAndWait();
    }

    private void showEditUserDialog(Stage owner) {
        Network selectedNetwork = networkList.getSelectionModel().getSelectedItem();
        NetworkUser selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedNetwork == null || selectedUser == null) {
            return;
        }

        TextField idField = lockedTextField(selectedUser.hexId());
        TextField networkField = lockedTextField(selectedNetwork.name());
        TextField descriptionField = new TextField(selectedUser.description());
        TextField fullNameField = new TextField(selectedUser.fullName());
        TextField loginField = new TextField(selectedUser.login());
        TextField ipAddressField = new TextField(selectedUser.ipAddress());
        TextField roleField = new TextField(selectedUser.role());
        setFormFieldWidths(idField, networkField, descriptionField, fullNameField, loginField, ipAddressField, roleField);

        Dialog<NetworkUser> dialog = createFormDialog(owner, "User Parameters");
        GridPane form = createFormGrid();
        form.addRow(0, new Label("ID"), idField);
        form.addRow(1, new Label("Network"), networkField);
        form.addRow(2, new Label("Description"), descriptionField);
        form.addRow(3, new Label("Full name"), fullNameField);
        form.addRow(4, new Label("Login"), loginField);
        form.addRow(5, new Label("IP address"), ipAddressField);
        form.addRow(6, new Label("Role"), roleField);
        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            try {
                return service.updateUser(
                        selectedUser,
                        descriptionField.getText(),
                        fullNameField.getText(),
                        loginField.getText(),
                        ipAddressField.getText(),
                        roleField.getText()
                );
            } catch (IllegalArgumentException exception) {
                showValidationError(exception.getMessage());
                return null;
            }
        });

        dialog.showAndWait().ifPresent(user -> {
            visibleUsers.setAll(service.getUsers(selectedNetwork));
            userTable.getSelectionModel().select(user);
        });
    }

    private <T> Dialog<T> createFormDialog(Stage owner, String title) {
        Dialog<T> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        URL stylesheet = MainWindow.class.getResource("app.css");
        if (stylesheet != null) {
            dialog.getDialogPane().getStylesheets().add(stylesheet.toExternalForm());
        }
        dialog.getDialogPane().getStyleClass().add("material-dialog");
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("primary-button");
        okButton.setMinWidth(150);
        okButton.setPrefWidth(150);
        okButton.setGraphicTextGap(8);

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setMinWidth(120);
        cancelButton.setPrefWidth(120);
        return dialog;
    }

    private GridPane createFormGrid() {
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.getStyleClass().add("form-grid");

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(130);

        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        fieldColumn.setMinWidth(320);

        form.getColumnConstraints().addAll(labelColumn, fieldColumn);
        return form;
    }

    private void setFormFieldWidths(TextField... fields) {
        for (TextField field : fields) {
            field.setMinWidth(320);
            field.setPrefWidth(360);
        }
    }

    private TextField lockedTextField(String value) {
        TextField field = new TextField(value);
        field.setEditable(false);
        field.getStyleClass().add("locked-field");
        return field;
    }

    private <T> void runDialogButtonTask(Dialog<?> dialog, Button okButton, String loadingText, Supplier<T> action, Consumer<T> onSuccess) {
        DialogPane dialogPane = dialog.getDialogPane();
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        String originalText = okButton.getText();
        javafx.scene.Node originalGraphic = okButton.getGraphic();

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(15, 15);
        progressIndicator.setMinSize(15, 15);
        progressIndicator.getStyleClass().add("button-progress");

        dialogPane.getContent().setDisable(true);
        okButton.setText(loadingText);
        okButton.setGraphic(progressIndicator);
        okButton.setGraphicTextGap(8);
        okButton.setMinWidth(150);
        okButton.setPrefWidth(150);
        okButton.getStyleClass().add("loading-button");
        okButton.setDisable(true);
        cancelButton.setDisable(true);

        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return action.get();
            }
        };

        task.setOnSucceeded(event -> {
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            dialogPane.getContent().setDisable(false);
            okButton.setText(originalText);
            okButton.setGraphic(originalGraphic);
            okButton.getStyleClass().remove("loading-button");
            okButton.setDisable(false);
            cancelButton.setDisable(false);
            Throwable exception = task.getException();
            showValidationError(exception == null ? "Operation failed." : exception.getMessage());
        });

        Thread worker = new Thread(task, "registration-sync-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void simulateRemoteSyncDelay() {
        try {
            Thread.sleep(900);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Sync was interrupted.");
        }
    }

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setHeaderText("Check the form");
        alert.showAndWait();
    }

    private boolean confirmDelete(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        URL stylesheet = MainWindow.class.getResource("app.css");
        if (stylesheet != null) {
            alert.getDialogPane().getStylesheets().add(stylesheet.toExternalForm());
        }
        alert.getDialogPane().getStyleClass().add("material-dialog");
        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Delete");
        okButton.getStyleClass().add("danger-button");
        Button cancelButton = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("secondary-button");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    @FunctionalInterface
    private interface UserValueProvider {
        String get(NetworkUser user);
    }

    private record NetworkFormData(String name, String description, String addressRange, String location) {
    }

    private record UserFormData(String description, String fullName, String login, String ipAddress, String role) {
    }

    private enum SyncStatus {
        ERROR("Sync error", Color.web("#d93025")),
        INITIALIZING("Sync initializing", Color.web("#f9ab00")),
        OK("Sync OK", Color.web("#188038"));

        private final String label;
        private final Color color;

        SyncStatus(String label, Color color) {
            this.label = label;
            this.color = color;
        }
    }
}
