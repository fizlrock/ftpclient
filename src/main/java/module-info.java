module test_ui_project {
    requires javafx.controls;
		requires javafx.fxml;

    opens test_ui_project.Controllers to javafx.fxml;

    exports test_ui_project;
}
