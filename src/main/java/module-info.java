module com.simcel {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.simcel to javafx.fxml;

    exports com.simcel;
    exports com.simcel.model;
    exports com.simcel.controller;
}
