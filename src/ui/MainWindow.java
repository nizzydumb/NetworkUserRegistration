package ui;

import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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
import javafx.scene.control.ComboBox;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Network;
import model.NetworkUser;
import logging.AppLogger;
import service.RegistrationService;
import storage.JnaPhysicalDriveService;
import storage.PhysicalDriveInfo;
import storage.RawByteWriteRequest;

import java.net.URL;
import java.util.HexFormat;
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
    private boolean physicalDriveRefreshRunning;

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

        Label physicalDriveLabel = new Label("PHYSICAL DRIVE");
        physicalDriveLabel.getStyleClass().add("drive-selector-label");
        ComboBox<PhysicalDriveInfo> physicalDriveBox = new ComboBox<>();
        physicalDriveBox.setPromptText("Select a physical drive");
        physicalDriveBox.setPrefWidth(350);
        physicalDriveBox.setMaxWidth(350);
        physicalDriveBox.getStyleClass().add("drive-selector");
        physicalDriveBox.setCellFactory(list -> createPhysicalDriveCell(false));
        physicalDriveBox.setButtonCell(createPhysicalDriveCell(true));
        JnaPhysicalDriveService driveService = new JnaPhysicalDriveService();
        loadPhysicalDrives(physicalDriveBox, driveService, null);
        physicalDriveBox.setOnShowing(event -> {
            PhysicalDriveInfo selected = physicalDriveBox.getValue();
            loadPhysicalDrives(physicalDriveBox, driveService, selected == null ? null : selected.number());
        });
        Timeline selectedDriveRefresh = new Timeline(new KeyFrame(
                Duration.seconds(2), event -> refreshSelectedPhysicalDrive(physicalDriveBox, driveService)
        ));
        selectedDriveRefresh.setCycleCount(Timeline.INDEFINITE);
        selectedDriveRefresh.play();
        VBox driveSelector = new VBox(5, physicalDriveLabel, physicalDriveBox);
        driveSelector.getStyleClass().add("drive-selector-group");
        driveSelector.setAlignment(Pos.CENTER_RIGHT);
        physicalDriveLabel.setMaxWidth(Double.MAX_VALUE);
        physicalDriveLabel.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(18, text, driveSelector);
        HBox.setHgrow(text, Priority.ALWAYS);
        header.setPadding(new Insets(18, 22, 16, 22));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("app-header");
        return header;
    }

    private void loadPhysicalDrives(ComboBox<PhysicalDriveInfo> driveBox,
                                    JnaPhysicalDriveService driveService,
                                    Integer selectedNumber) {
        try {
            driveBox.getItems().setAll(driveService.listPhysicalDrives());
            driveBox.getSelectionModel().clearSelection();
            if (selectedNumber != null) {
                driveBox.getItems().stream()
                        .filter(drive -> drive.number() == selectedNumber)
                        .findFirst()
                        .ifPresent(driveBox.getSelectionModel()::select);
            }
        } catch (Throwable exception) {
            driveBox.setPromptText("Drive scan failed - see application.log");
            AppLogger.error("Physical drive inventory failed", exception);
        }
    }

    private void refreshSelectedPhysicalDrive(ComboBox<PhysicalDriveInfo> driveBox,
                                              JnaPhysicalDriveService driveService) {
        PhysicalDriveInfo selected = driveBox.getValue();
        if (selected == null || driveBox.isShowing() || physicalDriveRefreshRunning) return;
        physicalDriveRefreshRunning = true;
        Task<PhysicalDriveInfo> refreshTask = new Task<>() {
            @Override
            protected PhysicalDriveInfo call() {
                return driveService.queryPhysicalDrive(selected.number());
            }
        };
        refreshTask.setOnSucceeded(event -> {
            physicalDriveRefreshRunning = false;
            PhysicalDriveInfo refreshed = refreshTask.getValue();
            if (refreshed == null) {
                driveBox.getItems().remove(selected);
                driveBox.getSelectionModel().clearSelection();
                return;
            }
            int index = driveBox.getItems().indexOf(selected);
            if (index >= 0 && !refreshed.equals(selected)) {
                driveBox.getItems().set(index, refreshed);
                driveBox.getSelectionModel().select(refreshed);
            }
        });
        refreshTask.setOnFailed(event -> {
            physicalDriveRefreshRunning = false;
            AppLogger.error("Selected physical drive state refresh failed", refreshTask.getException());
        });
        Thread worker = new Thread(refreshTask, "physical-drive-state-refresh");
        worker.setDaemon(true);
        worker.start();
    }

    private ListCell<PhysicalDriveInfo> createPhysicalDriveCell(boolean compact) {
        return new ListCell<>() {
            @Override
            protected void updateItem(PhysicalDriveInfo drive, boolean empty) {
                super.updateItem(drive, empty);
                if (!getStyleClass().contains("drive-cell")) getStyleClass().add("drive-cell");
                if (empty || drive == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Circle stateDot = new Circle(4);
                stateDot.getStyleClass().add(drive.systemDisk() ? "drive-dot-system"
                        : (drive.mounted() ? "drive-dot-mounted" : "drive-dot-unmounted"));

                Label diskName = new Label("Disk " + drive.number());
                diskName.getStyleClass().add("drive-disk-number");
                diskName.setMinWidth(58);
                diskName.setPrefWidth(58);

                if (compact) {
                    Label summary = new Label((drive.mounted() ? "Mounted" : "Not mounted")
                            + "  ·  " + drive.sizeLabel() + "  ·  " + drive.model());
                    summary.getStyleClass().add("drive-summary");
                    summary.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(summary, Priority.ALWAYS);
                    HBox content = new HBox(9, stateDot, diskName, summary);
                    content.setAlignment(Pos.CENTER_LEFT);
                    content.setMinWidth(0);
                    setText(null);
                    setGraphic(content);
                    return;
                }

                Label name = new Label(drive.model());
                name.getStyleClass().add("drive-name");
                name.setMaxWidth(Double.MAX_VALUE);

                Label details = new Label(drive.devicePath() + "  ·  " + drive.sizeLabel()
                        + "  ·  " + (drive.offline() ? "OFFLINE" : "ONLINE"));
                details.getStyleClass().add("drive-details");

                String statusText = drive.systemDisk() ? "SYSTEM" : (drive.mounted() ? "MOUNTED" : "UNMOUNTED");
                Label status = new Label(statusText);
                status.getStyleClass().addAll("drive-status",
                        drive.systemDisk() ? "drive-status-system"
                                : (drive.mounted() ? "drive-status-mounted" : "drive-status-unmounted"));

                HBox titleRow = new HBox(8, stateDot, diskName, name);
                titleRow.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(name, Priority.ALWAYS);
                HBox detailRow = new HBox(8, details, status);
                detailRow.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(details, Priority.ALWAYS);
                VBox content = new VBox(3, titleRow, detailRow);
                content.setMinWidth(0);
                setText(null);
                setGraphic(content);
            }
        };
    }

    private void showRawDriveDialog(Stage owner) {
        JnaPhysicalDriveService driveService;
        try {
            driveService = new JnaPhysicalDriveService();
        } catch (Throwable exception) {
            AppLogger.error("Could not load native raw-drive library", exception);
            showOperationError("Could not load lib/native/rawdrive/rawdrive.dll: " + exception.getMessage());
            return;
        }

        ComboBox<PhysicalDriveInfo> driveBox = new ComboBox<>();
        driveBox.setMaxWidth(Double.MAX_VALUE);
        TextField offsetField = new TextField("0");
        offsetField.setPromptText("Byte offset (decimal or 0x hexadecimal)");
        TextField bytesField = new TextField("DE AD BE EF");
        bytesField.setPromptText("Hex bytes, for example DE AD BE EF");
        Button refreshButton = new Button("Refresh drives");
        refreshButton.getStyleClass().add("secondary-button");
        Label elevation = new Label(driveService.isElevated()
                ? "Administrator: yes" : "Administrator: no — restart IntelliJ as Administrator");
        Label policy = new Label("All disks are listed. Writes require an offline, non-system disk and read-back verification.");
        policy.setWrapText(true);

        ButtonType writeType = new ButtonType("Write and verify", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Physical Drive Byte Writer");
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(writeType, ButtonType.CANCEL);
        GridPane form = createFormGrid();
        form.addRow(0, new Label("Physical drive"), driveBox);
        form.addRow(1, new Label("Byte offset"), offsetField);
        form.addRow(2, new Label("Bytes (hex)"), bytesField);
        form.addRow(3, new Label("Elevation"), elevation);
        form.add(refreshButton, 1, 4);
        form.add(policy, 0, 5, 2, 1);
        dialog.getDialogPane().setContent(form);
        URL stylesheet = MainWindow.class.getResource("app.css");
        if (stylesheet != null) dialog.getDialogPane().getStylesheets().add(stylesheet.toExternalForm());

        try {
            driveBox.getItems().setAll(driveService.listPhysicalDrives());
            if (!driveBox.getItems().isEmpty()) driveBox.getSelectionModel().selectFirst();
        } catch (RuntimeException exception) {
            AppLogger.error("Physical drive inventory failed", exception);
            showOperationError(exception.getMessage());
        }
        refreshButton.setOnAction(event -> {
            Integer selectedNumber = driveBox.getValue() == null ? null : driveBox.getValue().number();
            try {
                driveBox.getItems().setAll(driveService.listPhysicalDrives());
                if (selectedNumber != null) {
                    driveBox.getItems().stream().filter(drive -> drive.number() == selectedNumber)
                            .findFirst().ifPresent(driveBox.getSelectionModel()::select);
                }
                if (driveBox.getValue() == null && !driveBox.getItems().isEmpty())
                    driveBox.getSelectionModel().selectFirst();
            } catch (RuntimeException exception) {
                AppLogger.error("Physical drive inventory refresh failed", exception);
                showOperationError(exception.getMessage());
            }
        });

        Button writeButton = (Button) dialog.getDialogPane().lookupButton(writeType);
        BooleanBinding invalid = Bindings.createBooleanBinding(() -> {
            PhysicalDriveInfo drive = driveBox.getValue();
            return !driveService.isElevated() || drive == null || !drive.writableByPolicy()
                    || !validOffset(offsetField.getText()) || !validHexBytes(bytesField.getText());
        }, driveBox.valueProperty(), offsetField.textProperty(), bytesField.textProperty());
        writeButton.disableProperty().bind(invalid);
        writeButton.getStyleClass().add("danger-button");
        writeButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            PhysicalDriveInfo drive = driveBox.getValue();
            long offset = parseOffset(offsetField.getText());
            byte[] bytes = parseHexBytes(bytesField.getText());
            if (!confirmPhysicalWrite(owner, drive, offset, bytes.length)) return;
            try {
                driveService.writePhysical(new RawByteWriteRequest(drive.number(), offset, bytes));
                AppLogger.info("Physical drive write verified: drive=" + drive.number()
                        + ", offset=" + offset + ", length=" + bytes.length);
                dialog.close();
                Alert success = new Alert(Alert.AlertType.INFORMATION,
                        "The bytes were written and verified by reading them back.", ButtonType.OK);
                success.setHeaderText("Physical drive write verified");
                success.initOwner(owner);
                success.showAndWait();
            } catch (RuntimeException exception) {
                AppLogger.error("Physical drive write failed: drive=" + drive.number(), exception);
                showOperationError(exception.getMessage());
            }
        });
        dialog.showAndWait();
    }

    private boolean confirmPhysicalWrite(Stage owner, PhysicalDriveInfo drive, long offset, int length) {
        String token = "WRITE PHYSICALDRIVE" + drive.number();
        TextField confirmation = new TextField();
        confirmation.setPromptText(token);
        Label warning = new Label("This permanently overwrites " + length + " byte(s) at offset " + offset
                + " on " + drive.devicePath() + ". Type “" + token + "” to continue.");
        warning.setWrapText(true);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Confirm permanent raw-disk write");
        dialog.initOwner(owner);
        ButtonType confirmType = new ButtonType("Permanently write", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(new VBox(10, warning, confirmation));
        Button confirm = (Button) dialog.getDialogPane().lookupButton(confirmType);
        confirm.getStyleClass().add("danger-button");
        confirm.disableProperty().bind(confirmation.textProperty().isNotEqualTo(token));
        return dialog.showAndWait().orElse(ButtonType.CANCEL) == confirmType;
    }

    private boolean validOffset(String text) {
        try { return parseOffset(text) >= 0; } catch (RuntimeException exception) { return false; }
    }

    private long parseOffset(String text) {
        String value = text == null ? "" : text.strip();
        return value.startsWith("0x") || value.startsWith("0X")
                ? Long.parseUnsignedLong(value.substring(2), 16) : Long.parseLong(value);
    }

    private boolean validHexBytes(String text) {
        try { return parseHexBytes(text).length <= JnaPhysicalDriveService.MAX_WRITE_BYTES; }
        catch (RuntimeException exception) { return false; }
    }

    private byte[] parseHexBytes(String text) {
        String normalized = text == null ? "" : text.replaceAll("(?i)0x", "").replaceAll("[\\s,;:_-]", "");
        if (normalized.isEmpty() || (normalized.length() & 1) != 0) throw new IllegalArgumentException("Invalid hex bytes.");
        return HexFormat.of().parseHex(normalized);
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
                text.setMinWidth(0);
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
                row.setMinWidth(0);
                row.setMaxWidth(Double.MAX_VALUE);
                row.prefWidthProperty().bind(networkList.widthProperty().subtract(58));
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
        bindRequiredFields(dialog, okButton, nameField, descriptionField, addressRangeField);
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

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        bindRequiredFields(dialog, okButton, nameField, descriptionField, addressRangeField);

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            return service.updateNetwork(
                    selectedNetwork,
                    nameField.getText(),
                    descriptionField.getText(),
                    addressRangeField.getText(),
                    locationField.getText()
            );
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
        bindRequiredFields(dialog, okButton, descriptionField, fullNameField, loginField);
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

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        bindRequiredFields(dialog, okButton, descriptionField, fullNameField, loginField);

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            return service.updateUser(
                    selectedUser,
                    descriptionField.getText(),
                    fullNameField.getText(),
                    loginField.getText(),
                    ipAddressField.getText(),
                    roleField.getText()
            );
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

    private void bindRequiredFields(Dialog<?> dialog, Button submitButton, TextField... requiredFields) {
        Observable[] dependencies = new Observable[requiredFields.length];
        for (int index = 0; index < requiredFields.length; index++) {
            dependencies[index] = requiredFields[index].textProperty();
        }

        BooleanBinding invalid = Bindings.createBooleanBinding(
                () -> {
                    for (TextField field : requiredFields) {
                        if (field.getText() == null || field.getText().isBlank()) {
                            return true;
                        }
                    }
                    return false;
                },
                dependencies
        );
        submitButton.disableProperty().bind(
                invalid.or(dialog.getDialogPane().getContent().disableProperty())
        );
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
            cancelButton.setDisable(false);
            Throwable exception = task.getException();
            showOperationError(exception == null ? "Operation failed." : exception.getMessage());
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

    private void showOperationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Operation Error");
        alert.setHeaderText("The operation could not be completed");

        SVGPath cross = new SVGPath();
        cross.setContent("M6 6l12 12 M18 6L6 18");
        cross.getStyleClass().add("operation-error-icon-mark");
        StackPane icon = new StackPane(cross);
        icon.getStyleClass().add("operation-error-icon");
        alert.setGraphic(icon);

        Label messageLabel = new Label(
                message == null || message.isBlank() ? "An unexpected error occurred." : message
        );
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(430);
        messageLabel.getStyleClass().add("operation-error-message");

        Label hintLabel = new Label("You can dismiss this message and try the operation again.");
        hintLabel.setWrapText(true);
        hintLabel.getStyleClass().add("operation-error-hint");

        VBox content = new VBox(8, messageLabel, hintLabel);
        content.getStyleClass().add("operation-error-content");
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().getStyleClass().addAll("material-dialog", "operation-error-dialog");
        URL stylesheet = MainWindow.class.getResource("app.css");
        if (stylesheet != null) alert.getDialogPane().getStylesheets().add(stylesheet.toExternalForm());
        alert.getDialogPane().setMinWidth(500);

        Button dismissButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        dismissButton.setText("Dismiss");
        dismissButton.getStyleClass().add("operation-error-dismiss");
        if (userTable != null && userTable.getScene() != null) {
            alert.initOwner(userTable.getScene().getWindow());
        }
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
