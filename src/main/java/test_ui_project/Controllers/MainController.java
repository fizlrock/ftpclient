
package test_ui_project.Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import test_ui_project.Models.*;
import test_ui_project.Models.Telnet.FTPController;
import test_ui_project.Models.Telnet.FTPController.ControllerState;

public class MainController {

	FTPController fc;

	@FXML
	TextField ip_text_field;
	@FXML
	TextField username_text_field;
	@FXML
	TextField password_text_field;

	@FXML
	TextArea filelist_text_area;
	@FXML
	TextArea logs_text_area;

	public MainController() {
		// ip_text_field.textProperty().set("91.222.128.11");
		// username_text_field.textProperty().set("testftp_guest");
		// password_text_field.textProperty().set("12345");
	}

	@FXML
	public void startButtonClick(ActionEvent event) {
		System.out.println("Доброе утро");
		String ip, user, pass;
		ip = ip_text_field.textProperty().getValue();
		pass = password_text_field.textProperty().getValue();
		user = username_text_field.textProperty().getValue();
		fc = new FTPController(ip, user, pass);
		fc.connect();
		fc.login();

		if (fc.getControllerState() == ControllerState.Failed) {
			fc.disconnect();
			logs_text_area.textProperty().set(fc.getLogs());
			return;
		}

		FTPTestModel model = new FTPTestModel(fc);
		boolean success = model.exec();
		if (success) {
			filelist_text_area.textProperty().setValue(model.file_tree_report.toString());
		}
		fc.disconnect();
		logs_text_area.textProperty().set(fc.getLogs());
	}
}
