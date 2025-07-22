package com.chatapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class App extends Application {
    private ConnectionHandler connectionHandler;
    private Chatbot chatbot;
    private boolean isServer = false;
    private String username = "User";
    private Image profileImage;
    private boolean isDarkMode = true;
    private boolean showReadReceipts = true;
    private boolean enableNotifications = true;
    private AtomicBoolean isTyping = new AtomicBoolean(false);
    private TextField messageField;
    private TextField hostField;
    private TextField portField;
    private Button connectButton;
    private Button serverButton;
    private Button sendButton;
    private Button fileButton;
    private Button emojiButton;
    private Button saveButton;
    private Button themeButton;
    private Button settingsButton;
    private Button disconnectButton;
    private ImageView profileImageView;
    private Label usernameLabel;
    private Label statusLabel;
    private Label typingLabel;
    private VBox messageArea;
    private ScrollPane scrollPane;
    private StringBuilder chatHistory = new StringBuilder();
    private Timeline typingTimeline;

    private Circle statusIndicator;
    private Label statusText;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("P2P Chat Application");
        try {
            profileImage = new Image(getClass().getResourceAsStream("/images/default_profile.png"));
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/chat_icon.png")));
        } catch (Exception e) {
            // Use a default colored circle if images can't be loaded
            profileImage = null;
        }
        BorderPane root = new BorderPane();
        root.getStyleClass().add(isDarkMode ? "dark-container" : "light-container");
        HBox header = createHeader();
        VBox topSection = new VBox(5);
        topSection.getChildren().addAll(header, createConnectionArea());
        root.setTop(topSection);
        messageArea = new VBox(10);
        messageArea.setPadding(new Insets(10));
        styleVBox(messageArea);
        scrollPane = new ScrollPane(messageArea);
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setFitToWidth(true);
        root.setCenter(scrollPane);
        chatbot = new Chatbot(this);
        HBox inputArea = createInputArea();
        HBox statusBar = createStatusBar();
        VBox bottomSection = new VBox(5);
        bottomSection.getChildren().addAll(inputArea, statusBar);
        root.setBottom(bottomSection);
        applyTheme(root);
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets()
                .add(getClass().getResource("/com/chatapp/CSS.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
        setupEventHandlers();
        addSystemMessage("Welcome to P2P Chat! Connect to a peer or start a server to begin chatting.");
        Platform.runLater(() -> {
            try {
                Thread.sleep(500);
                addChatbotMessage("Hello! I'm ChatBot, your assistant. Type '@bot' to talk to me!");
            } catch (InterruptedException e) {
                // Ignore
            }
        });
        primaryStage.setOnCloseRequest(event -> {
            if (connectionHandler != null) {
                connectionHandler.closeConnection();
            }
        });
    }

    public void addChatbotMessage(String message) {
        Platform.runLater(() -> {
            HBox messageBox = new HBox(10);
            messageBox.setAlignment(Pos.CENTER_LEFT);
            Circle botAvatar = new Circle(15);
            botAvatar.setFill(Color.web("#6C63FF"));
            Text botInitial = new Text("B");
            botInitial.setFill(Color.WHITE);
            botInitial.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            StackPane botIconPane = new StackPane(botAvatar, botInitial);
            VBox messageContent = new VBox(3);
            messageContent.setPadding(new Insets(8, 12, 8, 12));
            messageContent.getStyleClass().addAll("message-bubble", "bot-message");
            Text messageText = new Text(message);
            messageText.setFill(Color.WHITE);
            messageText.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
            Text timeText = new Text(getCurrentTime());
            timeText.setFill(Color.rgb(255, 255, 255, 0.7));
            timeText.setFont(Font.font("Segoe UI", 9));
            messageContent.getChildren().addAll(messageText, timeText);
            DropShadow shadow = new DropShadow();
            shadow.setRadius(5.0);
            shadow.setOffsetX(2.0);
            shadow.setOffsetY(2.0);
            shadow.setColor(Color.rgb(0, 0, 0, 0.25));
            messageContent.setEffect(shadow);
            messageBox.getChildren().addAll(botIconPane, messageContent);
            messageArea.getChildren().add(messageBox);
            appendToChatHistory("ChatBot: " + message);
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            scrollPane.setVvalue(1.0);
        });
    }

    public void addSystemMessage(String message) {
        Platform.runLater(() -> {
            HBox messageBox = new HBox(10);
            messageBox.setAlignment(Pos.CENTER);
            VBox messageContent = new VBox(3);
            messageContent.setPadding(new Insets(8, 15, 8, 15));
            messageContent.getStyleClass().addAll("message-bubble", "system-message");
            Text messageText = new Text(message);
            messageText.setFill(Color.WHITE);
            messageText.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
            HBox contentBox = new HBox(8);
            contentBox.setAlignment(Pos.CENTER);
            Circle infoCircle = new Circle(5, Color.WHITE);
            Text infoText = new Text("i");
            infoText.setFill(Color.web("#6B7280"));
            infoText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            StackPane infoIcon = new StackPane(infoCircle, infoText);
            contentBox.getChildren().addAll(infoIcon, messageText);
            messageContent.getChildren().add(contentBox);
            DropShadow shadow = new DropShadow();
            shadow.setRadius(4.0);
            shadow.setOffsetX(1.0);
            shadow.setOffsetY(1.0);
            shadow.setColor(Color.rgb(0, 0, 0, 0.2));
            messageContent.setEffect(shadow);
            messageBox.getChildren().add(messageContent);
            messageArea.getChildren().add(messageBox);
            appendToChatHistory("System: " + message);
            scrollToBottom();
        });
    }

    public void addFileReceivedMessage(String filename, File file) {
        Platform.runLater(() -> {
            HBox messageBox = new HBox(10);
            messageBox.setAlignment(Pos.CENTER_LEFT);
            ImageView smallProfilePic;
            if (profileImage != null) {
                smallProfilePic = new ImageView(profileImage);
            } else {
                smallProfilePic = new ImageView();
            }
            smallProfilePic.setFitHeight(30);
            smallProfilePic.setFitWidth(30);
            Circle clip = new Circle(15);
            smallProfilePic.setClip(clip);
            VBox messageContent = new VBox(5);
            messageContent.setPadding(new Insets(12, 15, 12, 15));
            messageContent.setBackground(new Background(new BackgroundFill(
                    Color.web("#EC4899"), new CornerRadii(0, 18, 18, 18, false), Insets.EMPTY)));
            ImageView fileIcon = createIcon("/icons/file_download.png");
            StackPane fileIconPane = new StackPane();
            Circle fileIconBg = new Circle(15, Color.WHITE);
            fileIcon.setFitHeight(20);
            fileIcon.setFitWidth(20);
            fileIconPane.getChildren().addAll(fileIconBg, fileIcon);
            String fileSize = String.format("%.1f KB", file.length() / 1024.0);
            Hyperlink fileLink = new Hyperlink(filename);
            fileLink.setTextFill(Color.WHITE);
            fileLink.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
            fileLink.setOnAction(e -> {
                try {
                    // Open the file with default system application
                    java.awt.Desktop.getDesktop().open(file);
                } catch (Exception ex) {
                    showAlert("File Error", "Could not open file: " + ex.getMessage());
                }
            });
            Label fileSizeLabel = new Label(fileSize);
            fileSizeLabel.setTextFill(Color.rgb(255, 255, 255, 0.8));
            fileSizeLabel.setFont(Font.font("Segoe UI", 11));
            VBox fileLinkBox = new VBox(3);
            fileLinkBox.getChildren().addAll(fileLink, fileSizeLabel);
            HBox fileRow = new HBox(12);
            fileRow.setAlignment(Pos.CENTER_LEFT);
            fileRow.getChildren().addAll(fileIconPane, fileLinkBox);
            Text timeText = new Text(getCurrentTime());
            timeText.setFill(Color.rgb(255, 255, 255, 0.7));
            timeText.setFont(Font.font("Segoe UI", 9));
            messageContent.getChildren().addAll(fileRow, timeText);
            DropShadow shadow = new DropShadow();
            shadow.setRadius(5.0);
            shadow.setOffsetX(2.0);
            shadow.setOffsetY(2.0);
            shadow.setColor(Color.rgb(0, 0, 0, 0.25));
            messageContent.setEffect(shadow);
            messageBox.getChildren().addAll(smallProfilePic, messageContent);
            messageArea.getChildren().add(messageBox);
            appendToChatHistory("Peer: Sent file " + filename);
            scrollToBottom();
        });
    }

    public void updateConnectionStatus(String status) {
        Platform.runLater(() -> {
            statusLabel.setText(status);
            if (status.contains("Connected") || status.contains("Client Connected")) {
                statusLabel.setTextFill(Color.GREEN);
                addSystemMessage("Connection established successfully");
                toggleConnectionControls(true);
                updateStatusIndicator(true);
            } else if (status.contains("Listening")) {
                statusLabel.setTextFill(Color.BLUE);
                addSystemMessage("Server started. Waiting for connections...");
                toggleConnectionControls(true);
            } else if (status.contains("Connecting")) {
                statusLabel.setTextFill(Color.ORANGE);
            } else {
                statusLabel.setTextFill(Color.RED);
                toggleConnectionControls(false);
                updateStatusIndicator(false);
            }
        });
    }

    private void toggleConnectionControls(boolean connected) {
        Platform.runLater(() -> {
            hostField.setDisable(connected);
            portField.setDisable(connected);
            connectButton.setDisable(connected);
            serverButton.setDisable(connected);
            disconnectButton.setDisable(!connected);
        });
    }

    private void appendToChatHistory(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = sdf.format(new Date());
        chatHistory.append("[").append(timestamp).append("] ").append(message).append("\n");
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return sdf.format(new Date());
    }

    private void showDesktopNotification(String title, String message) {
        // In a real application, you would implement desktop notifications
        // using Java AWT TrayIcon or platform-specific libraries
    }

    private void applyTheme(BorderPane root) {
        if (root == null)
            return;
        if (isDarkMode) {
            root.setStyle("-fx-background-color: #111827;");
            if (scrollPane != null)
                scrollPane.setStyle("-fx-background: #111827; -fx-background-color: #111827;");
            if (messageArea != null) {
                for (javafx.scene.Node node : messageArea.getChildren()) {
                    if (node instanceof Region && ((Region) node).getBackground() != null &&
                            !((Region) node).getBackground().getFills().isEmpty()) {
                        continue;
                    }
                    node.setStyle("-fx-background-color: #111827;");
                }
            }
            if (messageField != null)
                messageField.setStyle(
                        "-fx-background-color: #374151; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 12 8 12;");
            if (hostField != null)
                hostField.setStyle(
                        "-fx-background-color: #374151; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 12 8 12;");
            if (portField != null)
                portField.setStyle(
                        "-fx-background-color: #374151; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 12 8 12;");
        } else {
            root.setStyle("-fx-background-color: #F9FAFB;");
            if (scrollPane != null)
                scrollPane.setStyle("-fx-background: #F9FAFB; -fx-background-color: #F9FAFB;");
            if (messageArea != null)
                messageArea.setStyle("-fx-background-color: #F9FAFB;");
            if (messageArea != null) {
                for (javafx.scene.Node node : messageArea.getChildren()) {
                    if (node instanceof Region) {
                        Region region = (Region) node;
                        Background bg = region.getBackground();
                        if (bg != null && !bg.getFills().isEmpty()) {
                            continue;
                        }
                        region.setStyle("-fx-background-color: #F9FAFB;");
                    }
                }
            }
            if (messageField != null)
                messageField.setStyle(
                        "-fx-background-color: white; -fx-text-fill: #1F2937; -fx-background-radius: 20; -fx-padding: 8 12 8 12;");
            if (hostField != null)
                hostField.setStyle(
                        "-fx-background-color: white; -fx-text-fill: #1F2937; -fx-background-radius: 20; -fx-padding: 8 12 8 12;");
            if (portField != null)
                portField.setStyle(
                        "-fx-background-color: white; -fx-text-fill: #1F2937; -fx-background-radius: 20; -fx-padding: 8 12 8 12;");
        }
        if (scrollPane != null && scrollPane.getContent() != null) {
            scrollPane.getContent().setStyle("-fx-background-color: " + (isDarkMode ? "#111827" : "#F9FAFB") + ";");
        }
        FadeTransition ft = new FadeTransition(Duration.millis(300), root);
        ft.setFromValue(0.8);
        ft.setToValue(1.0);
        ft.play();
    }

    private void styleTextField(TextField textField) {
        textField.getStyleClass().add("text-field");
        if (isDarkMode) {
            textField.getStyleClass().add("dark-text-field");
        } else {
            textField.getStyleClass().add("light-text-field");
        }
    }

    private void styleButton(Button button, Color backgroundColor, Color textColor) {
        String baseStyle = String.format(
                "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 20; -fx-font-weight: bold; -fx-padding: 8 15 8 15;",
                toHexString(backgroundColor),
                toHexString(textColor));
        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(150), button);
            ft.setFromValue(1.0);
            ft.setToValue(0.8);
            ft.play();
            button.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 20; -fx-font-weight: bold; -fx-padding: 8 15 8 15;",
                    toHexString(backgroundColor.brighter()),
                    toHexString(textColor)));
        });
        button.setOnMouseExited(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(150), button);
            ft.setFromValue(0.8);
            ft.setToValue(1.0);
            ft.play();
            button.setStyle(baseStyle);
        });
        button.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
            st.setToX(0.95);
            st.setToY(0.95);
            st.play();
            button.setStyle(String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 20; -fx-font-weight: bold; -fx-padding: 8 15 8 15;",
                    toHexString(backgroundColor.darker()),
                    toHexString(textColor)));
        });
        button.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            if (button.isHover()) {
                button.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: %s; -fx-background-radius: 20; -fx-font-weight: bold; -fx-padding: 8 15 8 15;",
                        toHexString(backgroundColor.brighter()),
                        toHexString(textColor)));
            } else {
                button.setStyle(baseStyle);
            }
        });
    }

    private void styleVBox(VBox vbox) {
        
        vbox.getStyleClass().add(isDarkMode ? "dark-container" : "light-container");
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-background-color: "
                    + (isDarkMode ? "#212121; -fx-text-fill: white;" : "#f5f5f5; -fx-text-fill: black;"));
            alert.showAndWait();
        });
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setPadding(new Insets(15));
        header.setAlignment(Pos.CENTER);
        Stop[] stops = new Stop[] {
                new Stop(0, Color.web("#4568dc")),
                new Stop(1, Color.web("#b06ab3"))
        };
        LinearGradient gradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops);
        header.setBackground(
                new Background(new BackgroundFill(gradient, new CornerRadii(0, 0, 20, 20, false), Insets.EMPTY)));
        StackPane profileContainer = new StackPane();
        profileImageView = new ImageView();
        profileImageView.setFitHeight(50);
        profileImageView.setFitWidth(50);
        profileImageView.setPreserveRatio(true); // Ensure image fills the entire area
        if (profileImage != null) {
            profileImageView.setImage(profileImage);
            // Create circular clip - centered properly
            Circle clip = new Circle(25); // radius = width/2
            clip.setCenterX(25); // center X = width/2
            clip.setCenterY(25); // center Y = height/2
            profileImageView.setClip(clip);
        } else {
            Circle circle = new Circle(25, Color.DODGERBLUE);
            profileContainer.getChildren().add(circle);
        }
        Circle clip = new Circle(25);
        clip.setCenterX(25);
        clip.setCenterY(25);
        profileImageView.setClip(clip);
        profileContainer.getChildren().add(profileImageView);
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(5.0);
        dropShadow.setOffsetX(3.0);
        dropShadow.setOffsetY(3.0);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.3));
        profileContainer.setEffect(dropShadow);
        usernameLabel = new Label(username);
        usernameLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        usernameLabel.setTextFill(Color.WHITE);
    
        statusIndicator = new Circle(5);
        statusIndicator.setFill(Color.RED); // Start with offline (red)
        statusText = new Label("Offline");
        statusText.setTextFill(Color.WHITE);
        statusText.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 12));

        VBox userInfo = new VBox(5);
        userInfo.setAlignment(Pos.CENTER_LEFT);
        userInfo.getChildren().addAll(usernameLabel, new HBox(5, statusIndicator, statusText));
        Label appTitle = new Label("P2P Chat");
        appTitle.setFont(Font.font("Verdana", FontWeight.BOLD, 22));
        appTitle.setTextFill(Color.WHITE);
        Reflection reflection = new Reflection();
        reflection.setFraction(0.5);
        appTitle.setEffect(reflection);
        header.getChildren().addAll(profileContainer, userInfo);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().add(spacer);
        header.getChildren().add(appTitle);
        return header;
    }

    private void updateStatusIndicator(boolean isOnline) {
        Platform.runLater(() -> {
            if (isOnline) {
                statusIndicator.setFill(Color.LIME);
                statusText.setText("Online");
            } else {
                statusIndicator.setFill(Color.RED);
                statusText.setText("Offline");
            }

            // Add a subtle animation
            FadeTransition ft = new FadeTransition(Duration.millis(300), statusIndicator);
            ft.setFromValue(0.5);
            ft.setToValue(1.0);
            ft.play();
        });
    }

    private HBox createConnectionArea() {
        HBox connectionArea = new HBox(10);
        connectionArea.setPadding(new Insets(12, 15, 12, 15));
        connectionArea.setAlignment(Pos.CENTER);
        Stop[] stops = new Stop[] {
                new Stop(0, isDarkMode ? Color.web("#1F2937") : Color.web("#F3F4F6")),
                new Stop(1, isDarkMode ? Color.web("#111827") : Color.web("#E5E7EB"))
        };
        LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
        connectionArea.setBackground(new Background(new BackgroundFill(gradient, new CornerRadii(15), Insets.EMPTY)));
        connectionArea.setBorder(new Border(new BorderStroke(
                isDarkMode ? Color.web("#374151") : Color.web("#D1D5DB"),
                BorderStrokeStyle.SOLID,
                new CornerRadii(15),
                new BorderWidths(1))));
        Label hostLabel = new Label("Host:");
        hostLabel.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 13));
        hostLabel.setTextFill(isDarkMode ? Color.web("#D1D5DB") : Color.web("#4B5563"));
        hostField = new TextField("localhost");
        hostField.setPromptText("Host");
        hostField.setPrefWidth(200);
        styleTextField(hostField);
        Label portLabel = new Label("Port:");
        portLabel.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 13));
        portLabel.setTextFill(isDarkMode ? Color.web("#D1D5DB") : Color.web("#4B5563"));
        portField = new TextField("1501");
        portField.setPromptText("Port");
        portField.setPrefWidth(80);
        styleTextField(portField);
        connectButton = new Button("Connect");
        connectButton.getStyleClass().addAll("button", "primary-button");
        styleButton(connectButton, Color.web("#3B82F6"), Color.WHITE);
        serverButton = new Button("Start Server");
        styleButton(serverButton, Color.web("#10B981"), Color.WHITE);
        connectButton.getStyleClass().addAll("button", "secondary-button");
        disconnectButton = new Button("Disconnect");
        styleButton(disconnectButton, Color.web("#EF4444"), Color.WHITE);
        disconnectButton.getStyleClass().addAll("button", "secondary-button");
        disconnectButton.setDisable(true);
        connectionArea.getChildren().addAll(
                hostLabel, hostField,
                portLabel, portField,
                connectButton, serverButton, disconnectButton);
        return connectionArea;
    }

    private HBox createInputArea() {
        HBox inputArea = new HBox(10);
        inputArea.setPadding(new Insets(12, 15, 12, 15));
        inputArea.setAlignment(Pos.CENTER);
        inputArea.setBackground(new Background(new BackgroundFill(
                isDarkMode ? Color.web("#1F2937") : Color.web("#F9FAFB"),
                CornerRadii.EMPTY,
                Insets.EMPTY)));
        inputArea.setBorder(new Border(new BorderStroke(
                isDarkMode ? Color.web("#374151") : Color.web("#E5E7EB"),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(1, 0, 0, 0))));
        DropShadow shadow = new DropShadow();
        shadow.setRadius(5.0);
        shadow.setOffsetY(-2.0);
        shadow.setColor(Color.rgb(0, 0, 0, 0.1));
        inputArea.setEffect(shadow);
        messageField = new TextField();
        messageField.setPromptText("Type your message here...");
        HBox.setHgrow(messageField, Priority.ALWAYS);
        styleTextField(messageField);
        sendButton = new Button("Send");
        ImageView sendIcon = createIcon("/icons/send.png");
        sendButton.setGraphic(sendIcon);
        sendButton.getStyleClass().addAll("button", "primary-button");
        styleButton(sendButton, Color.web("#3B82F6"), Color.WHITE);
        fileButton = createCircularButton(createIcon("/icons/fileshare.png"), Color.web("#E8ECEF"));
        fileButton.setTooltip(new Tooltip("Send File"));
        fileButton.getStyleClass().addAll("button", "accent-button");
        emojiButton = createCircularButton(createIcon("/icons/emojishare.png"), Color.web("#E8ECEF"));
        emojiButton.setTooltip(new Tooltip("Insert Emoji"));
        emojiButton.getStyleClass().addAll("button", "accent-button");
        saveButton = createCircularButton(createIcon("/icons/savechathistory.png"), Color.web("#E8ECEF"));
        saveButton.setTooltip(new Tooltip("Save Chat History"));
        themeButton = createCircularButton(
                createIcon(isDarkMode ? "/icons/lightmodeicon.png" : "/icons/darkmodeicon.png"),
                Color.web("E8ECEF"));
        themeButton.setTooltip(new Tooltip(isDarkMode ? "Switch to Light Mode" : "Switch to Dark Mode"));
        settingsButton = createCircularButton(createIcon("/icons/settingicon.png"), Color.web("#E8ECEF"));
        settingsButton.setTooltip(new Tooltip("Settings"));
        inputArea.getChildren().addAll(
                messageField, sendButton, fileButton,
                emojiButton, saveButton, themeButton, settingsButton);
        return inputArea;
    }

    private Button createCircularButton(ImageView icon, Color color) {
        Button button = new Button();
        button.setGraphic(icon);
        button.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 50%%; -fx-padding: 8;",
                toHexString(color)));
        button.setMinSize(36, 36);
        button.setMaxSize(36, 36);
        button.setOnMouseEntered(e -> button.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 50%%; -fx-padding: 8;",
                toHexString(color.brighter()))));
        button.setOnMouseExited(e -> button.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 50%%; -fx-padding: 8;",
                toHexString(color))));
        button.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
            st.setToX(0.85);
            st.setToY(0.85);
            st.play();
            button.setStyle(String.format(
                    "-fx-background-color: %s; -fx-background-radius: 50%%; -fx-padding: 8;",
                    toHexString(color.darker())));
        });
        button.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            if (button.isHover()) {
                button.setStyle(String.format(
                        "-fx-background-color: %s; -fx-background-radius: 50%%; -fx-padding: 8;",
                        toHexString(color.brighter())));
            } else {
                button.setStyle(String.format(
                        "-fx-background-color: %s; -fx-background-radius: 50%%; -fx-padding: 8;",
                        toHexString(color)));
            }
        });
        return button;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(15);
        statusBar.setPadding(new Insets(8, 15, 8, 15));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        Stop[] stops = new Stop[] {
                new Stop(0, isDarkMode ? Color.web("#111827") : Color.web("#F9FAFB")),
                new Stop(1, isDarkMode ? Color.web("#1F2937") : Color.web("#F3F4F6"))
        };
        LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
        statusBar.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));
        statusBar.setBorder(new Border(new BorderStroke(
                isDarkMode ? Color.web("#374151") : Color.web("#E5E7EB"),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(1, 0, 0, 0))));
        statusLabel = new Label("Not Connected");
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 12));
        statusLabel.setTextFill(Color.web("#EF4444"));
        typingLabel = new Label("");
        typingLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        typingLabel.setTextFill(Color.web("#9CA3AF"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox timeBox = new HBox(5);
        timeBox.setAlignment(Pos.CENTER);
        Circle clockCircle = new Circle(8, isDarkMode ? Color.web("#9CA3AF") : Color.web("#6B7280"));
        Text clockText = new Text("⏱");
        clockText.setFill(isDarkMode ? Color.web("#1F2937") : Color.WHITE);
        clockText.setFont(Font.font("Segoe UI", 10));
        StackPane clockIcon = new StackPane(clockCircle, clockText);
        Label timeLabel = new Label();
        timeLabel.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 12));
        timeLabel.setTextFill(isDarkMode ? Color.web("#D1D5DB") : Color.web("#4B5563"));
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        timeLabel.setText(sdf.format(new Date()));
        timeBox.getChildren().addAll(clockIcon, timeLabel);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    timeLabel.setText(sdf.format(new Date()));
                }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        statusBar.getChildren().addAll(statusLabel, typingLabel, spacer, timeBox);
        return statusBar;
    }

    private ImageView createIcon(String resourcePath) {
        try {
            Image image = new Image(getClass().getResourceAsStream(resourcePath));
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(20);
            imageView.setFitWidth(20);
            return imageView;
        } catch (Exception e) {
            return new ImageView();
        }
    }

    private void setupEventHandlers() {
        connectButton.setOnAction(event -> {
            try {
                int port = Integer.parseInt(portField.getText());
                String host = hostField.getText();
                isServer = false;
                connectionHandler = new ConnectionHandler(host, port, this);
                connectionHandler.startConnection();
                updateConnectionStatus("Connecting to " + host + ":" + port + "...");
            } catch (NumberFormatException e) {
                showAlert("Invalid Port", "Please enter a valid port number.");
            } catch (Exception e) {
                showAlert("Connection Error", "Could not connect: " + e.getMessage());
            }
        });
        serverButton.setOnAction(event -> {
            try {
                int port = Integer.parseInt(portField.getText());
                isServer = true;
                connectionHandler = new ConnectionHandler(port, this);
                connectionHandler.startServer();
                updateConnectionStatus("Listening on port " + port + "...");
            } catch (NumberFormatException e) {
                showAlert("Invalid Port", "Please enter a valid port number.");
            } catch (Exception e) {
                showAlert("Server Error", "Could not start server: " + e.getMessage());
            }
        });
        disconnectButton.setOnAction(event -> {
            if (connectionHandler != null) {
                connectionHandler.closeConnection();
                connectionHandler = null;
            }
            updateConnectionStatus("Disconnected");
        });

        sendButton.setOnAction(e -> {
            String message = messageField.getText().trim();
            if (!message.isEmpty()) {
                messageField.clear();
                addSentMessage(message);
                System.out.println("DEBUG: About to process message with chatbot: " + message);
                boolean processedByBot = chatbot.processMessage(message);
                System.out.println("DEBUG: Chatbot processed: " + processedByBot);
                if (!processedByBot && connectionHandler != null && connectionHandler.isConnected()) {
                    connectionHandler.sendMessage(message);
                }
            }
        });

        messageField.setOnAction(event -> sendMessage());
        messageField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (connectionHandler != null && connectionHandler.isConnected()) {
                if (!isTyping.get() && !newValue.isEmpty()) {
                    isTyping.set(true);
                    connectionHandler.sendTypingStatus(true);
                    if (typingTimeline != null) {
                        typingTimeline.stop();
                    }
                    typingTimeline = new Timeline(
                            new KeyFrame(Duration.seconds(2), e -> {
                                isTyping.set(false);
                                connectionHandler.sendTypingStatus(false);
                            }));
                    typingTimeline.play();
                }
            }
        });
        fileButton.setOnAction(event -> {
            if (connectionHandler == null || !connectionHandler.isConnected()) {
                showAlert("Not Connected", "You must be connected to send a file.");
                return;
            }
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select File to Send");
            File selectedFile = fileChooser.showOpenDialog(null);
            if (selectedFile != null) {
                try {
                    connectionHandler.sendFile(selectedFile);
                    addSentMessage("File sent: " + selectedFile.getName());
                } catch (Exception e) {
                    showAlert("File Error", "Could not send file: " + e.getMessage());
                }
            }
        });
        

        emojiButton.setOnAction(event -> {
            // Create a simple emoji picker popup
            Stage emojiStage = new Stage();
            emojiStage.setTitle("Choose Emoji");
            emojiStage.setResizable(false);

            GridPane emojiGrid = new GridPane();
            emojiGrid.setPadding(new Insets(10));
            emojiGrid.setHgap(5);
            emojiGrid.setVgap(5);
            emojiGrid.setStyle("-fx-background-color: " + (isDarkMode ? "#2d2d2d" : "#f5f5f5") + ";");
            emojiGrid.setAlignment(Pos.CENTER);

            String[] emojis = {
                    "😀", "😁", "😂", "🤣", "😃", "😄", "😅", "😆",
                    "😉", "😊", "😋", "😎", "😍", "😘", "🥰", "😗",
                    "👍", "👋", "❤️", "💕", "🔥", "✨", "👀", "👏"
            };

            int col = 0;
            int row = 0;

            for (String emoji : emojis) {
                Button emojiBtn = new Button(emoji);

                // Try different font options for better emoji support
                Font emojiFont = null;
                try {
                    // Try system fonts that support emojis
                    emojiFont = Font.font("Apple Color Emoji", FontWeight.NORMAL, 18);
                    if (emojiFont.getName().equals("System")) {
                        emojiFont = Font.font("Segoe UI Emoji", FontWeight.NORMAL, 18);
                    }
                    if (emojiFont.getName().equals("System")) {
                        emojiFont = Font.font("Noto Color Emoji", FontWeight.NORMAL, 18);
                    }
                    if (emojiFont.getName().equals("System")) {
                        emojiFont = Font.font("Symbola", FontWeight.NORMAL, 18);
                    }
                } catch (Exception e) {
                    emojiFont = Font.font(18); // Fallback to default font
                }

                emojiBtn.setFont(emojiFont != null ? emojiFont : Font.font(18));

                // Set consistent button properties
                emojiBtn.setPrefSize(45, 45);
                emojiBtn.setMinSize(45, 45);
                emojiBtn.setMaxSize(45, 45);

                // Remove default button padding to center emoji better
                emojiBtn.setStyle(
                        "-fx-background-color: " + (isDarkMode ? "#404040" : "#ffffff") + ";" +
                                "-fx-border-color: " + (isDarkMode ? "#555555" : "#cccccc") + ";" +
                                "-fx-border-width: 1px;" +
                                "-fx-border-radius: 5px;" +
                                "-fx-background-radius: 5px;" +
                                "-fx-padding: 0;" +
                                "-fx-cursor: hand;");

                // Add hover effect
                String originalStyle = emojiBtn.getStyle();
                emojiBtn.setOnMouseEntered(e -> {
                    emojiBtn.setStyle(
                            "-fx-background-color: " + (isDarkMode ? "#505050" : "#e8e8e8") + ";" +
                                    "-fx-border-color: " + (isDarkMode ? "#666666" : "#aaaaaa") + ";" +
                                    "-fx-border-width: 1px;" +
                                    "-fx-border-radius: 5px;" +
                                    "-fx-background-radius: 5px;" +
                                    "-fx-padding: 0;" +
                                    "-fx-cursor: hand;");
                });

                emojiBtn.setOnMouseExited(e -> {
                    emojiBtn.setStyle(originalStyle);
                });

                // Emoji button click handler
                emojiBtn.setOnAction(e -> {
                    messageField.appendText(emoji);
                    emojiStage.close();
                });

                // Add button to grid
                emojiGrid.add(emojiBtn, col, row);
                GridPane.setHalignment(emojiBtn, HPos.CENTER);
                GridPane.setValignment(emojiBtn, VPos.CENTER);

                col++;
                if (col > 7) {
                    col = 0;
                    row++;
                }
            }

            Scene emojiScene = new Scene(emojiGrid);
            emojiStage.setScene(emojiScene);
            emojiStage.show();
        });

        saveButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Chat History");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            fileChooser.setInitialFileName("chat_history.txt");
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(chatHistory.toString());
                    showAlert("Success", "Chat history saved successfully.");
                } catch (IOException e) {
                    showAlert("Error", "Failed to save chat history: " + e.getMessage());
                }
            }
        });
        themeButton.setOnAction(event -> {
            isDarkMode = !isDarkMode;
            ImageView themeIcon = createIcon(isDarkMode ? "/icons/light_mode.png" : "/icons/dark_mode.png");
            themeButton.setGraphic(themeIcon);
            themeButton.setTooltip(new Tooltip(isDarkMode ? "Switch to Light Mode" : "Switch to Dark Mode"));
            Scene scene = ((Stage) themeButton.getScene().getWindow()).getScene();
            BorderPane root = (BorderPane) scene.getRoot();
            applyTheme(root);
            styleButton(connectButton, Color.web("#4CAF50"), Color.WHITE);
            styleButton(serverButton, Color.web("#2196F3"), Color.WHITE);
            styleButton(disconnectButton, Color.web("#f44336"), Color.WHITE);
            styleButton(sendButton, Color.web("#4CAF50"), Color.WHITE);
            styleButton(fileButton, Color.web("#FF9800"), Color.WHITE);
            styleButton(emojiButton, Color.web("#FFEB3B"), Color.BLACK);
            styleButton(saveButton, Color.web("#607D8B"), Color.WHITE);
            styleButton(themeButton, Color.web("#9C27B0"), Color.WHITE);
            styleButton(settingsButton, Color.web("#2196F3"), Color.WHITE);
            styleTextField(messageField);
            styleTextField(hostField);
            styleTextField(portField);
        });
        settingsButton.setOnAction(event -> {
            Stage settingsStage = new Stage();
            settingsStage.setTitle("Settings");
            VBox settingsBox = new VBox(10);
            settingsBox.setPadding(new Insets(15));
            settingsBox.setStyle("-fx-background-color: " + (isDarkMode ? "#2d2d2d" : "#f5f5f5") + ";");
            Label usernamePrompt = new Label("Username:");
            usernamePrompt.setTextFill(isDarkMode ? Color.WHITE : Color.BLACK);
            TextField usernameInput = new TextField(username);
            styleTextField(usernameInput);
            Label profilePrompt = new Label("Profile Picture:");
            profilePrompt.setTextFill(isDarkMode ? Color.WHITE : Color.BLACK);
            Button profileButton = new Button("Change Profile Picture");
            styleButton(profileButton, Color.web("#2196F3"), Color.WHITE);
            Label notificationLabel = new Label("Notifications:");
            notificationLabel.setTextFill(isDarkMode ? Color.WHITE : Color.BLACK);
            CheckBox notificationCheckbox = new CheckBox("Enable Desktop Notifications");
            notificationCheckbox.setSelected(enableNotifications);
            notificationCheckbox.setTextFill(isDarkMode ? Color.WHITE : Color.BLACK);
            Label readReceiptsLabel = new Label("Read Receipts:");
            readReceiptsLabel.setTextFill(isDarkMode ? Color.WHITE : Color.BLACK);
            CheckBox readReceiptsCheckbox = new CheckBox("Show Read Receipts");
            readReceiptsCheckbox.setSelected(showReadReceipts);
            readReceiptsCheckbox.setTextFill(isDarkMode ? Color.WHITE : Color.BLACK);
            Button saveSettingsButton = new Button("Save Settings");
            styleButton(saveSettingsButton, Color.web("#4CAF50"), Color.WHITE);
            settingsBox.getChildren().addAll(
                    usernamePrompt, usernameInput,
                    profilePrompt, profileButton,
                    notificationLabel, notificationCheckbox,
                    readReceiptsLabel, readReceiptsCheckbox,
                    saveSettingsButton);
            profileButton.setOnAction(e -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select Profile Picture");
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
                File selectedFile = fileChooser.showOpenDialog(settingsStage);
                if (selectedFile != null) {
                    try {
                        profileImage = new Image(selectedFile.toURI().toString());
                        profileImageView.setImage(profileImage);
                        Circle clip = new Circle(25);
                        clip.setCenterX(25);
                        clip.setCenterY(25);
                        profileImageView.setClip(clip);
                    } catch (Exception ex) {
                        showAlert("Image Error", "Could not load image: " + ex.getMessage());
                    }
                }
            });
            saveSettingsButton.setOnAction(e -> {
                username = usernameInput.getText();
                usernameLabel.setText(username);
                enableNotifications = notificationCheckbox.isSelected();
                showReadReceipts = readReceiptsCheckbox.isSelected();
                settingsStage.close();
                addSystemMessage("Settings updated successfully.");
            });
            Scene settingsScene = new Scene(settingsBox, 400, 350);
            settingsStage.setScene(settingsScene);
            settingsStage.show();
        });
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        if (connectionHandler == null || !connectionHandler.isConnected()) {
            showAlert("Not Connected", "You must be connected to send messages.");
            return;
        }
        connectionHandler.sendMessage(message);
        addSentMessage(message);
        messageField.clear();
    }

    public void addSentMessage(String message) {
        Platform.runLater(() -> {
            HBox messageBox = new HBox(10);
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            VBox messageContent = new VBox(3);
            messageContent.setPadding(new Insets(8, 12, 8, 12));
            messageContent.getStyleClass().addAll("message-bubble", "sent-message");
            Text messageText = new Text(message);
            messageText.setFill(Color.WHITE);
            messageText.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
            Text timeText = new Text(getCurrentTime());
            timeText.setFill(Color.rgb(255, 255, 255, 0.7));
            timeText.setFont(Font.font("Segoe UI", 9));
            messageContent.getChildren().addAll(messageText, timeText);
            DropShadow shadow = new DropShadow();
            shadow.setRadius(5.0);
            shadow.setOffsetX(2.0);
            shadow.setOffsetY(2.0);
            shadow.setColor(Color.rgb(0, 0, 0, 0.25));
            messageContent.setEffect(shadow);
            messageBox.getChildren().add(messageContent);
            messageArea.getChildren().add(messageBox);
            appendToChatHistory("You: " + message);
            scrollToBottom();
        });
    }

    public void addReceivedMessage(String message) {
        Platform.runLater(() -> {
            HBox messageBox = new HBox(10);
            messageBox.setAlignment(Pos.CENTER_LEFT);
            ImageView smallProfilePic;
            if (profileImage != null) {
                smallProfilePic = new ImageView(profileImage);
                smallProfilePic.setFitHeight(30); // Set size for small profile
                smallProfilePic.setFitWidth(30);
                smallProfilePic.setPreserveRatio(true);
                Circle smallClip = new Circle(15);
                smallClip.setCenterX(15);
                smallClip.setCenterY(15);
                smallProfilePic.setClip(smallClip);
            } else {
                Circle circle = new Circle(15, Color.web("#6366F1"));
                circle.setStroke(Color.WHITE);
                circle.setStrokeWidth(2);
                smallProfilePic = new ImageView();
            }
            smallProfilePic.setFitHeight(30);
            smallProfilePic.setFitWidth(30);
            Circle clip = new Circle(15);
            smallProfilePic.setClip(clip);
            VBox messageContent = new VBox(3);
            messageContent.setPadding(new Insets(8, 12, 8, 12));
            messageContent.getStyleClass().addAll("message-bubble", "received-message");
            Text messageText = new Text(message);
            messageText.setFill(Color.WHITE);
            messageText.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
            Text timeText = new Text(getCurrentTime());
            timeText.setFill(Color.rgb(255, 255, 255, 0.7));
            timeText.setFont(Font.font("Segoe UI", 9));
            messageContent.getChildren().addAll(messageText, timeText);
            DropShadow shadow = new DropShadow();
            shadow.setRadius(5.0);
            shadow.setOffsetX(2.0);
            shadow.setOffsetY(2.0);
            shadow.setColor(Color.rgb(0, 0, 0, 0.25));
            messageContent.setEffect(shadow);
            messageBox.getChildren().addAll(smallProfilePic, messageContent);
            messageArea.getChildren().add(messageBox);
            appendToChatHistory("Peer: " + message);
            if (enableNotifications && !messageField.getScene().getWindow().isFocused()) {
                showDesktopNotification("New Message", "Peer: " + message);
            }
            scrollToBottom();
        });
    }

    public void updateTypingIndicator(boolean isTyping) {
        Platform.runLater(() -> {
            if (isTyping) {
                typingLabel.setText("Peer is typing...");
                FadeTransition fadeTransition = new FadeTransition(Duration.millis(1000), typingLabel);
                fadeTransition.setFromValue(0.5);
                fadeTransition.setToValue(1.0);
                fadeTransition.setCycleCount(Animation.INDEFINITE);
                fadeTransition.setAutoReverse(true);
                typingLabel.getStyleClass().add("pulse-animation");
                typingLabel.getStyleClass().remove("pulse-animation");
            } else {
                typingLabel.setText("");
                for (Animation animation : typingLabel.getProperties().values().stream()
                        .filter(v -> v instanceof Animation).map(v -> (Animation) v)
                        .collect(Collectors.toList())) {
                    animation.stop();
                }
            }
        });
    }

    private class ConnectionHandler {
        private Socket socket;
        private ServerSocket serverSocket;
        private PrintWriter out;
        private BufferedReader in;
        private boolean connected = false;
        private Thread receiverThread;
        private String host;
        private int port;
        private App app;

        // Increased buffer sizes for better performance
        private static final int SOCKET_BUFFER_SIZE = 8 * 1024 * 1024; // 8MB
        private static final int STREAM_BUFFER_SIZE = 1024 * 1024; // 1MB
        private static final long MAX_FILE_SIZE = 2L * 1024 * 1024 * 1024; // 2GB
        private static final int SOCKET_TIMEOUT = 0; // No timeout for large transfers

        public ConnectionHandler(String host, int port, App app) {
            this.host = host;
            this.port = port;
            this.app = app;
        }

        public ConnectionHandler(int port, App app) {
            this.port = port;
            this.app = app;
        }

        public void startConnection() {
            receiverThread = new Thread(() -> {
                try {
                    // Connect to the server
                    socket = new Socket();

                    // Configure socket before connecting
                    socket.setTcpNoDelay(true);
                    socket.setKeepAlive(true);
                    socket.setSoTimeout(SOCKET_TIMEOUT); // No timeout
                    socket.setReceiveBufferSize(SOCKET_BUFFER_SIZE);
                    socket.setSendBufferSize(SOCKET_BUFFER_SIZE);
                    socket.setSoLinger(true, 30); // Proper close handling

                    // Connect with timeout
                    socket.connect(new java.net.InetSocketAddress(host, port), 30000);

                    setupStreams();
                    app.updateConnectionStatus("Connected to " + host + ":" + port);
                    connected = true;
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        processIncomingMessage(inputLine);
                    }
                } catch (IOException e) {
                    app.updateConnectionStatus("Connection failed: " + e.getMessage());
                } finally {
                    closeConnection();
                }
            });
            receiverThread.setDaemon(true);
            receiverThread.start();
        }

        public void startServer() {
            receiverThread = new Thread(() -> {
                try {
                    serverSocket = new ServerSocket();

                    // Configure server socket
                    serverSocket.setReuseAddress(true);
                    serverSocket.setReceiveBufferSize(SOCKET_BUFFER_SIZE);
                    serverSocket.bind(new java.net.InetSocketAddress(port));

                    app.updateConnectionStatus("Listening on port " + port);
                    socket = serverSocket.accept();

                    // Configure accepted socket
                    socket.setTcpNoDelay(true);
                    socket.setKeepAlive(true);
                    socket.setSoTimeout(SOCKET_TIMEOUT); // No timeout
                    socket.setReceiveBufferSize(SOCKET_BUFFER_SIZE);
                    socket.setSendBufferSize(SOCKET_BUFFER_SIZE);
                    socket.setSoLinger(true, 30);

                    setupStreams();
                    app.updateConnectionStatus("Client Connected: " + socket.getInetAddress().getHostAddress());
                    connected = true;
                    app.updateStatusIndicator(true);
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        processIncomingMessage(inputLine);
                    }
                } catch (IOException e) {
                    app.updateConnectionStatus("Server error: " + e.getMessage());
                } finally {
                    closeConnection();
                }
            });
            receiverThread.setDaemon(true);
            receiverThread.start();
        }

        private void processIncomingMessage(String message) {
            if (message.startsWith("CMD:")) {
                String cmd = message.substring(4);
                if (cmd.equals("TYPING:START")) {
                    app.updateTypingIndicator(true);
                } else if (cmd.equals("TYPING:STOP")) {
                    app.updateTypingIndicator(false);
                } else if (cmd.startsWith("FILE_TRANSFER:")) {
                    // Process file transfer in separate thread to avoid blocking
                    new Thread(() -> handleFileTransfer(cmd.substring(14))).start();
                }
            } else {
                app.addReceivedMessage(message);
            }
        }

        private void handleFileTransfer(String fileData) {
            try {
                // Parse the file data: filename:filesize:base64data
                String[] parts = fileData.split(":", 3);
                if (parts.length != 3) {
                    throw new IOException("Invalid file transfer format");
                }

                String fileName = parts[0];
                long expectedFileSize = Long.parseLong(parts[1]);
                String base64Data = parts[2];

                // Show receiving message
                Platform.runLater(() -> {
                    app.addSystemMessage("Receiving file: " + fileName + " (" +
                            String.format("%.1f MB", expectedFileSize / (1024.0 * 1024.0)) + ")");
                });

                long startTime = System.currentTimeMillis();

                // Use streaming Base64 decoder to avoid memory issues
                byte[] fileBytes;
                try {
                    fileBytes = Base64.getDecoder().decode(base64Data);
                } catch (OutOfMemoryError e) {
                    throw new IOException("File too large for available memory");
                }

                // Verify file size
                if (fileBytes.length != expectedFileSize) {
                    throw new IOException("File size mismatch. Expected: " + expectedFileSize +
                            ", Received: " + fileBytes.length);
                }

                // Create downloads directory if it doesn't exist
                File downloadsDir = new File("downloads");
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }

                // Save the file with proper error handling
                File receivedFile = new File(downloadsDir, fileName);
                try {
                    Files.write(receivedFile.toPath(), fileBytes,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING);
                } finally {
                    // Clear the byte array from memory immediately
                    fileBytes = null;
                    System.gc(); // Suggest garbage collection
                }

                long endTime = System.currentTimeMillis();
                double transferTime = (endTime - startTime) / 1000.0;
                double speedMBps = (expectedFileSize / (1024.0 * 1024.0)) / transferTime;

                // Notify successful file reception
                Platform.runLater(() -> {
                    app.addFileReceivedMessage(fileName, receivedFile);
                    app.addSystemMessage("File received successfully: " + fileName +
                            String.format(" (%.1f seconds, %.1f MB/s)", transferTime, speedMBps));
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    app.showAlert("File Receive Error", "Could not save file: " + e.getMessage());
                    app.addSystemMessage("File transfer failed: " + e.getMessage());
                });
            }
        }

        private void setupStreams() throws IOException {
            // Use larger buffers and proper encoding
            out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                            STREAM_BUFFER_SIZE),
                    false // Don't auto-flush for better performance
            );
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8),
                    STREAM_BUFFER_SIZE);
        }

        public void closeConnection() {
            connected = false;
            app.updateStatusIndicator(false);

            // Proper cleanup order
            try {
                if (out != null) {
                    out.flush();
                    out.close();
                }
            } catch (Exception e) {
                /* Ignore */ }

            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
                /* Ignore */ }

            try {
                if (socket != null && !socket.isClosed()) {
                    socket.shutdownOutput();
                    socket.shutdownInput();
                    socket.close();
                }
            } catch (Exception e) {
                /* Ignore */ }

            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (Exception e) {
                /* Ignore */ }

            app.updateConnectionStatus("Disconnected");
        }

        public void sendMessage(String message) {
            if (connected && out != null) {
                synchronized (out) {
                    out.println(message);
                    out.flush(); // Explicit flush for messages
                }
            }
        }

        public void sendTypingStatus(boolean isTyping) {
            if (connected && out != null) {
                synchronized (out) {
                    out.println("CMD:TYPING:" + (isTyping ? "START" : "STOP"));
                    out.flush();
                }
            }
        }

        public void sendFile(File file) throws IOException {
            if (file.length() > MAX_FILE_SIZE) {
                throw new IOException("File too large. Maximum size is " + (MAX_FILE_SIZE / (1024 * 1024)) + "MB.");
            }

            if (!connected || out == null) {
                throw new IOException("Not connected");
            }

            // Check available memory before proceeding
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long availableMemory = maxMemory - totalMemory + freeMemory;


            // Need approximately 3x file size in memory (original + base64 + string)
            long requiredMemory = file.length() * 4; // Conservative estimate

            System.out.println("Runtime memory is :"+runtime);
            System.out.println("MaxMemory memory is :"+maxMemory);
            System.out.println("Total memory is :"+totalMemory);
            System.out.println("Free memory is :"+freeMemory);
            System.out.println("Available memory is :"+availableMemory);

            if (requiredMemory > availableMemory) {
                throw new IOException("Insufficient memory for file transfer. Required: " +
                        (requiredMemory / (1024 * 1024)) + "MB, Available: " +
                        (availableMemory / (1024 * 1024)) + "MB");
            }

            // Send file in separate thread to avoid blocking
            new Thread(() -> {
                byte[] fileBytes = null;
                String encodedFile = null;

                try {
                    // Show sending message
                    Platform.runLater(() -> {
                        app.addSystemMessage("Sending file: " + file.getName() + " (" +
                                String.format("%.1f MB", file.length() / (1024.0 * 1024.0)) + ")");
                    });

                    long startTime = System.currentTimeMillis();

                    // Read file
                    try {
                        fileBytes = Files.readAllBytes(file.toPath());
                    } catch (OutOfMemoryError e) {
                        throw new IOException("File too large for available memory");
                    }

                    // Encode to Base64
                    try {
                        encodedFile = Base64.getEncoder().encodeToString(fileBytes);
                    } catch (OutOfMemoryError e) {
                        throw new IOException("File too large for Base64 encoding");
                    } finally {
                        // Clear file bytes from memory
                        fileBytes = null;
                        System.gc();
                    }

                    String fileName = file.getName();
                    long fileSize = file.length();

                    // Send file as single message
                    String fileMessage = "CMD:FILE_TRANSFER:" + fileName + ":" + fileSize + ":" + encodedFile;

                    synchronized (out) {
                        out.println(fileMessage);
                        out.flush();
                    }

                    long endTime = System.currentTimeMillis();
                    double transferTime = (endTime - startTime) / 1000.0;
                    double speedMBps = (fileSize / (1024.0 * 1024.0)) / transferTime;

                    // Notify successful file sending
                    Platform.runLater(() -> {
                        app.addSystemMessage("File sent successfully: " + fileName +
                                String.format(" (%.1f seconds, %.1f MB/s)", transferTime, speedMBps));
                    });

                } catch (IOException e) {
                    Platform.runLater(() -> {
                        app.addSystemMessage("File send failed: " + e.getMessage());
                        app.showAlert("File Transfer Error", e.getMessage());
                    });
                } catch (OutOfMemoryError e) {
                    Platform.runLater(() -> {
                        app.addSystemMessage("File too large for memory: " + file.getName());
                        app.showAlert("Memory Error", "File too large. Try:\n" +
                                "1. Increase JVM heap size (-Xmx4g)\n" +
                                "2. Use a smaller file\n" +
                                "3. Close other applications");
                    });
                } finally {
                    // Clean up memory
                    fileBytes = null;
                    encodedFile = null;
                    System.gc();
                }
            }, "FileTransferThread").start();
        }

        public boolean isConnected() {
            return connected && socket != null && !socket.isClosed() && socket.isConnected();
        }
    }

}
