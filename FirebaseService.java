package com.chatapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import javafx.application.Platform;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FirebaseService {
    private TextField emailField;
    private PasswordField passwordField;
    private LoginController loginController;

    public FirebaseService(LoginController loginController, TextField emailField, PasswordField passwordField) {
        this.loginController = loginController;
        this.emailField = emailField;
        this.passwordField = passwordField;
    }

    public boolean signUp() {
        String email = emailField.getText();
        String password = passwordField.getText();
        try {
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password)
                    .setDisabled(false);

            UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
            System.out.println("Successfully created user: " + userRecord.getUid());

            // IMPORTANT: Also store user in Realtime Database
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
            String userId = userRecord.getUid();

            // Create user data object
            java.util.Map<String, Object> userData = new java.util.HashMap<>();
            userData.put("email", email.toLowerCase().trim());
            userData.put("password", password); // Note: In production, never store plain passwords
            userData.put("uid", userId);
            userData.put("createdAt", System.currentTimeMillis());

            // Store user data in database
            usersRef.child(userId).setValueAsync(userData);

            showAlert("Success", "User created successfully.");
            return true;
        } catch (FirebaseAuthException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to create user: " + e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to store user data: " + e.getMessage());
            return false;
        }
    }

    public boolean login() {
        String email = emailField.getText();
        String password = passwordField.getText();

        // Input validation
        if (email == null || email.trim().isEmpty()) {
            showAlert("Error", "Please enter your email address.");
            return false;
        }

        if (password == null || password.trim().isEmpty()) {
            showAlert("Error", "Please enter your password.");
            return false;
        }

        try {
            String apiKey = "AIzaSyBQVX6KR_jjMLtJtm25fFQw297IlX2uymA"; // Secure this in production!
            URL url = new URL("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + apiKey);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            // Add timeouts to prevent hanging
            conn.setConnectTimeout(15000); // 15 seconds
            conn.setReadTimeout(15000); // 15 seconds

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("email", email.trim());
            jsonRequest.put("password", password);
            jsonRequest.put("returnSecureToken", true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonRequest.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                showAlert(true);
                return true;
            } else {
                showAlert("Invalid Login", "Invalid email or password. Please check your credentials.");
                return false;
            }
        } catch (java.net.SocketTimeoutException e) {
            showAlert("Connection Error", "Connection timeout. Please check your internet connection and try again.");
            return false;
        } catch (java.net.UnknownHostException e) {
            showAlert("Network Error", "Unable to connect to server. Please check your internet connection.");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Login failed: " + e.getMessage());
            return false;
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("card");

        File cssFile = new File("chatapp\\src\\main\\java\\com\\chatapp\\CSS.css");
        if (cssFile.exists()) {
            dialogPane.getStylesheets().add(cssFile.toURI().toString());
        }

        alert.showAndWait();
    }

    private void showAlert(boolean isLoggedIn) {
        if (isLoggedIn) {
            loginController.navigateToPage2();
        } else {
            Label msg = new Label("Welcome");
            msg.setFont(Font.font("Roboto", FontWeight.BOLD, 30));
            msg.setTextFill(Color.WHITE);

            Label subtitle = new Label("You've successfully signed in");
            subtitle.setFont(Font.font("Roboto", 16));
            subtitle.setTextFill(Color.web("#F3F4F6"));

            Circle profileCircle = new Circle(50);
            profileCircle.setFill(Color.web("#60A5FA"));
            Text initial = new Text("U");
            initial.setFill(Color.WHITE);
            initial.setFont(Font.font("Roboto", FontWeight.BOLD, 30));
            StackPane profilePane = new StackPane(profileCircle, initial);

            DropShadow profileShadow = new DropShadow();
            profileShadow.setRadius(15);
            profileShadow.setColor(Color.rgb(0, 0, 0, 0.4));
            profilePane.setEffect(profileShadow);

            Button logoutButton = new Button("Logout");
            logoutButton.getStyleClass().add("accent-button");
            logoutButton.setPrefWidth(150);

            Button continueButton = new Button("Continue");
            continueButton.getStyleClass().add("primary-button");
            continueButton.setPrefWidth(150);

            HBox buttonBox = new HBox(20, continueButton, logoutButton);
            buttonBox.setAlignment(Pos.CENTER);

            Image img = new Image("file:src/main/resources/images/Page1bg.jpg", 800, 600, true, true);
            ImageView iv = new ImageView(img);
            iv.setFitHeight(600);
            iv.setFitWidth(800);

            VBox vBox = new VBox(25);
            vBox.setAlignment(Pos.CENTER);
            vBox.setPadding(new Insets(30));
            vBox.getStyleClass().add("auth-form");
            vBox.setStyle("-fx-background-color: rgba(31, 41, 55, 0.85);");
            vBox.getChildren().addAll(profilePane, msg, subtitle, buttonBox);

            setupWelcomeAnimations(vBox);

            StackPane stackPane = new StackPane(iv, vBox);
            StackPane.setAlignment(vBox, Pos.CENTER);

            logoutButton.setOnAction(event -> loginController.initializeLoginScene());
            continueButton.setOnAction(event -> loginController.navigateToPage2());

            Scene scene = new Scene(stackPane, 800, 600);

            File cssFile = new File("chatapp\\src\\main\\java\\com\\chatapp\\CSS.css");
            if (cssFile.exists()) {
                scene.getStylesheets().add(cssFile.toURI().toString());
            }

            loginController.setPrimaryStageScene(scene);
        }
    }

    private void setupWelcomeAnimations(VBox vBox) {
        vBox.setOpacity(0);
        vBox.setTranslateY(50);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), vBox);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideUp = new TranslateTransition(Duration.millis(800), vBox);
        slideUp.setFromY(50);
        slideUp.setToY(0);

        ParallelTransition parallelTransition = new ParallelTransition(fadeIn, slideUp);
        parallelTransition.setDelay(Duration.millis(300));
        parallelTransition.play();
    }

    public void resetPassword(String email, String newPassword) {
        try {
            // Validate inputs first
            if (email == null || email.trim().isEmpty()) {
                Platform.runLater(() -> {
                    loginController.showAlert(Alert.AlertType.ERROR, "Email cannot be empty.");
                });
                return;
            }

            if (newPassword == null || newPassword.trim().isEmpty()) {
                Platform.runLater(() -> {
                    loginController.showAlert(Alert.AlertType.ERROR, "New password cannot be empty.");
                });
                return;
            }

            String searchEmail = email.trim().toLowerCase();

            // First, update Firebase Authentication password
            try {
                UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(searchEmail);
                UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(userRecord.getUid())
                        .setPassword(newPassword);

                FirebaseAuth.getInstance().updateUser(request);
                System.out.println("Firebase Auth password updated successfully");

                // Then update Realtime Database
                updateDatabasePassword(searchEmail, newPassword);

            } catch (FirebaseAuthException e) {
                System.out.println("Firebase Auth error: " + e.getMessage());
                if (e.getAuthErrorCode().name().equals("USER_NOT_FOUND")) {
                    Platform.runLater(() -> {
                        loginController.showAlert(Alert.AlertType.ERROR,
                                "Email not found in authentication system. Please sign up first.");
                    });
                } else {
                    Platform.runLater(() -> {
                        loginController.showAlert(Alert.AlertType.ERROR,
                                "Authentication error: " + e.getMessage());
                    });
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                loginController.showAlert(Alert.AlertType.ERROR,
                        "Firebase service error: " + e.getMessage());
            });
        }
    }

    private void updateDatabasePassword(String email, String newPassword) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                AtomicBoolean userFound = new AtomicBoolean(false);

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    if (userSnapshot.hasChild("email")) {
                        String dbEmail = userSnapshot.child("email").getValue(String.class);

                        if (dbEmail != null && dbEmail.toLowerCase().equals(email)) {
                            userFound.set(true);
                            System.out.println("Updating database password for: " + email);

                            // Update password in database
                            userSnapshot.getRef().child("password").setValueAsync(newPassword);
                            break;
                        }
                    }
                }

                Platform.runLater(() -> {
                    if (userFound.get()) {
                        loginController.showAlert(Alert.AlertType.INFORMATION,
                                "Password reset successfully! You can now login with your new password.");
                    } else {
                        loginController.showAlert(Alert.AlertType.WARNING,
                                "Authentication password updated, but user not found in database records.");
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Database error: " + databaseError.getMessage());
                Platform.runLater(() -> {
                    loginController.showAlert(Alert.AlertType.WARNING,
                            "Authentication password updated, but database update failed: "
                                    + databaseError.getMessage());
                });
            }
        });
    }

}
