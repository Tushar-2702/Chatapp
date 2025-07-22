package com.chatapp;

import javafx.application.Application;
import javafx.stage.Stage;

public class AppLauncher extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        // Start the application with the login screen
        LoginController loginController = new LoginController();
        try {
            loginController.start(primaryStage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}


