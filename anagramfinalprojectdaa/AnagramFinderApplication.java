package com.example.anagramfinalprojectdaa;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AnagramFinderApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Fixed line - using the class directly for resource loading
        Parent root = FXMLLoader.load(AnagramFinderApplication.class.getResource("AnagramFinderFXVisual.fxml"));
        primaryStage.setTitle("Anagram Finder");
        primaryStage.setScene(new Scene(root));
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(500);
        primaryStage.show();
        primaryStage.setTitle("AnagramFinder");
    }

    public static void main(String[] args) {
        launch(args);
    }
}