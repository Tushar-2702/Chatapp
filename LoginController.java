package com.chatapp;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;

import java.io.FileInputStream;
import java.io.IOException;

public class LoginController extends Application {
    private Stage primaryStage;
    private FirebaseService firebaseService;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Initialize Firebase
        try {
            FileInputStream serviceAccount = new FileInputStream("D:\\Download 2\\chatapp_finalday\\chatapp\\chatapp\\src\\main\\java\\com\\chatapp\\JSON.json");
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://chatappp-1098b-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .build();
            FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Load and show login scene
        Scene scene = createLoginAndSignUpScene();
        primaryStage.setTitle("ChatApp");
        primaryStage.setScene(scene);

        scene.getStylesheets().add(getClass().getResource("/com/chatapp/CSS.css").toExternalForm());

        primaryStage.show();
    }

    private Scene createLoginAndSignUpScene() {
        // Background Image
        Image bgImage = new Image(getClass().getResource("/images/loginpagebg.jpg").toExternalForm());
        ImageView bgView = new ImageView(bgImage);
        bgView.setPreserveRatio(true);

        VBox layout = new VBox(20);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);
        layout.getStyleClass().add("auth-form");

        // App Logo
        Image logoImage = new Image(getClass().getResource("/icons/chat_icon.png").toExternalForm());
        ImageView logoView = new ImageView(logoImage);
        logoView.setFitHeight(120);
        logoView.setFitWidth(120);
        logoView.setPreserveRatio(true);
        Circle clip = new Circle(60, 60, 60);
        logoView.setClip(clip);
        logoView.setEffect(new Reflection());

        // Title and subtitle
        Label titleLabel = new Label("Welcome to ChatApp");
        titleLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 30));
        titleLabel.setTextFill(Color.web("#2563EB"));

        Label subtitleLabel = new Label("Connect and chat securely");
        subtitleLabel.setFont(Font.font("Roboto", 16));
        subtitleLabel.setTextFill(Color.web("#6B7280"));

        // Email Field
        TextField emailField = new TextField();
        emailField.setPromptText("Email Address");
        emailField.setPrefWidth(300);

        ImageView emailIcon = new ImageView(new Image(getClass().getResource("/icons/emailicon.png").toExternalForm()));
        emailIcon.setFitWidth(20);
        emailIcon.setFitHeight(20);
        emailIcon.setPreserveRatio(true);

        HBox emailBox = new HBox(10, emailIcon, emailField);
        emailBox.setAlignment(Pos.CENTER);
        emailBox.setMaxWidth(350);
        emailBox.setPadding(new Insets(5));
        emailBox.setStyle("-fx-background-color: white; -fx-background-radius: 25; -fx-padding: 8 12 8 12;");

        // Password Field
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setPrefWidth(300);

        ImageView passwordIcon = new ImageView(
                new Image(getClass().getResource("/icons/passwordicon.png").toExternalForm()));
        passwordIcon.setFitWidth(20);
        passwordIcon.setFitHeight(20);
        passwordIcon.setPreserveRatio(true);

        HBox passwordBox = new HBox(10, passwordIcon, passwordField);
        passwordBox.setAlignment(Pos.CENTER);
        passwordBox.setMaxWidth(350);
        passwordBox.setPadding(new Insets(5));
        passwordBox.setStyle("-fx-background-color: white; -fx-background-radius: 25; -fx-padding: 8 12 8 12;");

        // Buttons
        Button loginButton = new Button("Log In");
        Button signUpButton = new Button("Sign Up");
        loginButton.setPrefWidth(150);
        signUpButton.setPrefWidth(150);
        loginButton.getStyleClass().add("primary-button");
        signUpButton.getStyleClass().add("secondary-button");

        addButtonAnimation(loginButton);
        addButtonAnimation(signUpButton);

        firebaseService = new FirebaseService(this, emailField, passwordField);
        loginButton.setOnAction(e -> firebaseService.login());
        signUpButton.setOnAction(e -> firebaseService.signUp());

        HBox buttonBox = new HBox(20, loginButton, signUpButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));

        // Forgot Password
        Hyperlink forgotPassword = new Hyperlink("Forgot Password?");
        forgotPassword.setTextFill(Color.web("#3B82F6"));
        forgotPassword.setOnAction(e -> showForgotPasswordDialog());

        // Add components to layout
        layout.getChildren().addAll(
                logoView, titleLabel, subtitleLabel,
                emailBox, passwordBox, buttonBox, forgotPassword);

        // Wrap the form in an HBox for left alignment
        HBox contentWrapper = new HBox();
        contentWrapper.setAlignment(Pos.CENTER);
        contentWrapper.setPadding(new Insets(50, 0, 50, 50));
        contentWrapper.getChildren().add(layout);

        // Root StackPane with background
        StackPane root = new StackPane();
        root.getChildren().addAll(bgView, contentWrapper);

        Scene scene = new Scene(root, 800, 600);
        bgView.setPreserveRatio(false);
        bgView.fitWidthProperty().bind(scene.widthProperty());
        bgView.fitHeightProperty().bind(scene.heightProperty());

        return scene;
    }

    private void addButtonAnimation(Button button) {
        ScaleTransition st = new ScaleTransition(Duration.seconds(0.3), button);
        st.setByX(0.05);
        st.setByY(0.05);
        st.setCycleCount(ScaleTransition.INDEFINITE);
        st.setAutoReverse(true);
        button.setOnMouseEntered(e -> st.play());
        button.setOnMouseExited(e -> st.stop());
    }

    private void showForgotPasswordDialog() {
    try {
        // Create custom dialog with consistent styling
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Reset Password");
        dialog.setHeaderText(null);
        dialog.setResizable(false);

        // Add a dummy button type to make the dialog closeable
        ButtonType dummyButtonType = new ButtonType("", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(dummyButtonType);
        
        // Hide the dummy button
        Button dummyButton = (Button) dialog.getDialogPane().lookupButton(dummyButtonType);
        if (dummyButton != null) {
            dummyButton.setVisible(false);
            dummyButton.setManaged(false);
        }

        // Main container with consistent styling
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(30));
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.getStyleClass().add("auth-form");

        // Title label matching login page style
        Label titleLabel = new Label("Reset Your Password");
        titleLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#2563EB"));

        Label subtitleLabel = new Label("Enter your email and new password");
        subtitleLabel.setFont(Font.font("Roboto", 14));
        subtitleLabel.setTextFill(Color.web("#6B7280"));

        // Email Field with icon (matching login page style)
        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email address");
        emailField.setPrefWidth(280);

        ImageView emailIcon = new ImageView(
                new Image(getClass().getResource("/icons/emailicon.png").toExternalForm()));
        emailIcon.setFitWidth(20);
        emailIcon.setFitHeight(20);
        emailIcon.setPreserveRatio(true);

        HBox emailBox = new HBox(10, emailIcon, emailField);
        emailBox.setAlignment(Pos.CENTER);
        emailBox.setMaxWidth(330);
        emailBox.setPadding(new Insets(5));
        emailBox.setStyle("-fx-background-color: white; -fx-background-radius: 25; -fx-padding: 8 12 8 12;");

        // Password Field with icon (matching login page style)
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Enter new password (min 6 characters)");
        newPasswordField.setPrefWidth(280);

        ImageView passwordIcon = new ImageView(
                new Image(getClass().getResource("/icons/passwordicon.png").toExternalForm()));
        passwordIcon.setFitWidth(20);
        passwordIcon.setFitHeight(20);
        passwordIcon.setPreserveRatio(true);

        HBox passwordBox = new HBox(10, passwordIcon, newPasswordField);
        passwordBox.setAlignment(Pos.CENTER);
        passwordBox.setMaxWidth(330);
        passwordBox.setPadding(new Insets(5));
        passwordBox.setStyle("-fx-background-color: white; -fx-background-radius: 25; -fx-padding: 8 12 8 12;");

        // Create custom buttons
        Button resetButton = new Button("Reset Password");
        Button cancelButton = new Button("Cancel");

        // Style buttons to match login page
        resetButton.setPrefWidth(150);
        cancelButton.setPrefWidth(150);

        resetButton.getStyleClass().add("primary-button");
        cancelButton.getStyleClass().add("secondary-button");

        resetButton.setDisable(true);

        // Add button animations (matching login page)
        addButtonAnimation(resetButton);
        addButtonAnimation(cancelButton);

        // Create button container with proper alignment
        HBox buttonBox2 = new HBox(20, resetButton, cancelButton);
        buttonBox2.setAlignment(Pos.CENTER);
        buttonBox2.setPadding(new Insets(15, 0, 0, 0));

        // Add all components to main container
        mainContainer.getChildren().addAll(titleLabel, subtitleLabel, emailBox, passwordBox, buttonBox2);

        // Create action handlers
        resetButton.setOnAction(e -> {
            String email = emailField.getText().trim();
            String password = newPasswordField.getText();

            if (email.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Please fill in all fields.");
                return;
            }

            if (password.length() < 6) {
                showAlert(Alert.AlertType.ERROR, "Password must be at least 6 characters long.");
                return;
            }

            if (!email.contains("@") || !email.contains(".")) {
                showAlert(Alert.AlertType.ERROR, "Please enter a valid email address.");
                return;
            }

            // Initialize FirebaseService if not already done
            if (firebaseService == null) {
                TextField dummyEmail = new TextField();
                PasswordField dummyPassword = new PasswordField();
                firebaseService = new FirebaseService(this, dummyEmail, dummyPassword);
            }

            // Call reset password method
            firebaseService.resetPassword(email, password);
            dialog.close();
        });

        // Fix cancel button functionality
        cancelButton.setOnAction(e -> {
            dialog.setResult(null);
            dialog.close();
        });

        // Input validation
        Runnable checkInput = () -> {
            boolean validEmail = emailField.getText() != null &&
                    emailField.getText().trim().length() > 0 &&
                    emailField.getText().contains("@");
            boolean validPassword = newPasswordField.getText() != null &&
                    newPasswordField.getText().length() >= 6;
            resetButton.setDisable(!(validEmail && validPassword));
        };

        emailField.textProperty().addListener((obs, oldText, newText) -> checkInput.run());
        newPasswordField.textProperty().addListener((obs, oldText, newText) -> checkInput.run());

        dialog.getDialogPane().setContent(mainContainer);

        // Apply CSS styling
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("card");
        try {
            String cssResource = getClass().getResource("/com/chatapp/CSS.css").toExternalForm();
            if (cssResource != null) {
                dialogPane.getStylesheets().add(cssResource);
            }
        } catch (Exception e) {
            System.out.println("CSS file not found for dialog styling");
        }

        // Enable closing the dialog with X button or ESC key
        dialog.setOnCloseRequest(e -> {
            dialog.setResult(null);
            dialog.close();
        });

        // Focus on email field
        Platform.runLater(emailField::requestFocus);

        // Show dialog
        dialog.showAndWait();

    } catch (Exception e) {
        e.printStackTrace();
        showAlert(Alert.AlertType.ERROR, "Error opening password reset dialog: " + e.getMessage());
    }
}

    public void showAlert(Alert.AlertType type, String message) {
        try {
            Alert alert = new Alert(type);
            alert.setTitle("Password Reset");
            alert.setHeaderText(null);
            alert.setContentText(message != null ? message : "An error occurred");

            // Apply CSS styling if available
            DialogPane dialogPane = alert.getDialogPane();
            if (dialogPane != null) {
                dialogPane.getStyleClass().add("card");

                try {
                    String cssResource = getClass().getResource("/com/chatapp/CSS.css").toExternalForm();
                    if (cssResource != null) {
                        dialogPane.getStylesheets().add(cssResource);
                    }
                } catch (Exception e) {
                    // CSS file not found, continue without styling
                    System.out.println("CSS file not found, continuing without custom styling");
                }
            }

            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback alert without styling
            Alert fallbackAlert = new Alert(type != null ? type : Alert.AlertType.INFORMATION);
            fallbackAlert.setContentText(message != null ? message : "An error occurred");
            fallbackAlert.showAndWait();
        }
    }

    public void navigateToPage2() {
        Platform.runLater(() -> {
            App chatApp = new App();
            try {
                chatApp.start(primaryStage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void setPrimaryStageScene(Scene scene) {
        primaryStage.setScene(scene);
    }

    public void initializeLoginScene() {
        Scene scene = createLoginAndSignUpScene();
        primaryStage.setScene(scene);
    }
}
