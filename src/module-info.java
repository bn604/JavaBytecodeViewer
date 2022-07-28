module core {
    
    requires javafx.controls;
    requires javafx.fxml;
    
    opens core to javafx.graphics, javafx.fxml;
    
}