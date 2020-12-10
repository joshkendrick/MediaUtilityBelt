package us.joshkendrick.MediaUtilityBelt.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Launcher extends Application {

  public static void main(String[] args) {
    launch();
  }

  @Override
  public void start(Stage stage) throws IOException {
    // which layout to load, path is relative to this class
    FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/primary.fxml"));
    Parent root = loader.load();

    stage.setTitle("MediaUtilityBelt v0.0.1");
    stage.setScene(new Scene(root, 1050, 480));
    stage.show();

    // set stage in controller, for the file picker
    PrimaryController controller = loader.getController();
    controller.setStage(stage);
  }
}
