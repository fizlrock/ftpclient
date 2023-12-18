package test_ui_project;

import java.io.IOException;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class App extends Application {

	@Override
	public void start(Stage stage) throws IOException {

		showMainWindow(stage);
	}

	private void showMainWindow(Stage stage) throws IOException {
		URL ui_location = getClass().getResource("/ftp_ui.fxml");
		FXMLLoader fxmlLoader = new FXMLLoader(ui_location);
		Scene scene = new Scene(fxmlLoader.load(), 320, 240);
		stage.setTitle("Hello!");
		stage.setScene(scene);
		stage.show();
	}

	public static void main(String[] args) {
		launch();
	}

}