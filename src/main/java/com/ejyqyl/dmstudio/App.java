package com.ejyqyl.dmstudio;

import com.ejyqyl.dmstudio.controller.MainController;
import com.ejyqyl.dmstudio.service.i18n.I18nService;
import com.ejyqyl.dmstudio.viewmodel.MainViewModel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.ResourceBundle;

/**
 * JavaFX Application entry point for DeluxeMenus Studio.
 */
public class App extends Application {

    private static Stage primaryStage;

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        // Global uncaught exception handler (Specification §9.2)
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();
            Platform.runLater(() -> showErrorDialog(throwable));
        });

        loadScene(stage);
    }

    private void loadScene(Stage stage) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", I18nService.getInstance().getCurrentLocale());
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"), bundle);
            Parent root = loader.load();

            MainController controller = loader.getController();
            MainViewModel viewModel = new MainViewModel();
            controller.init(viewModel, stage);

            Scene scene = new Scene(root, 1366, 768);
            scene.getStylesheets().add(getClass().getResource("/css/theme-dark.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle(I18nService.getInstance().get("app.title"));

            stage.setOnCloseRequest(e -> {
                if (!controller.checkUnsavedChanges()) {
                    e.consume();
                }
            });

            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog(e);
        }
    }

    private void showErrorDialog(Throwable throwable) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18nService.getInstance().get("dialog.error_title"));
        alert.setHeaderText("An unexpected error occurred");
        alert.setContentText(throwable.getMessage() != null ? throwable.getMessage() : throwable.toString());
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
