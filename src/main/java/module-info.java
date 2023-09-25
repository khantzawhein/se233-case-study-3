module se233.chapter3 {
    requires javafx.controls;
    requires javafx.fxml;
    requires pdfbox;
    requires org.apache.logging.log4j;

    opens se233.chapter3 to javafx.fxml;
    opens se233.chapter3.controller to javafx.fxml;
    opens se233.chapter3.model to javafx.fxml;
    exports se233.chapter3;
    exports se233.chapter3.controller;
    exports se233.chapter3.helpers;
    exports se233.chapter3.model;
}