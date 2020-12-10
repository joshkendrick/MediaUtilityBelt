package us.joshkendrick.MediaUtilityBelt.app;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import us.joshkendrick.MediaUtilityBelt.data.MediaFile;
import us.joshkendrick.MediaUtilityBelt.io.Committer;
import us.joshkendrick.MediaUtilityBelt.io.Processor;

import java.io.File;
import java.util.List;

public class PrimaryController {

  private final DirectoryChooser directoryChooser = new DirectoryChooser();
  private Stage stage;
  @FXML private Label pathLabel;
  @FXML private TextField gPhotosAlbumField;
  @FXML private TextArea outputBox;
  @FXML private Button writeChangesBtn;
  private File rootDir;
  private Processor processTask;
  private Committer commitTask;

  private List<MediaFile> items;

  public void setStage(Stage stage) {
    this.stage = stage;
  }

  public void onChooseDirectoryBtnClick() {
    directoryChooser.setInitialDirectory(
        rootDir != null && rootDir.exists() ? rootDir : new File(System.getProperty("user.dir")));
    rootDir = directoryChooser.showDialog(stage);

    if (rootDir != null) {
      pathLabel.setText(rootDir.getPath());
    }

    writeChangesBtn.setDisable(true);
    onStopBtnClick();
  }

  public void onGoBtnClick() {
    Printer printer = new Printer(outputBox);
    printer.clear();

    String gPhotosAlbumTitle = gPhotosAlbumField.getText();

    processTask = new Processor(rootDir, gPhotosAlbumTitle, printer);

    processTask.setOnSucceeded(
        workerStateEvent -> {
          items = processTask.getValue();

          writeChangesBtn.setDisable(false);
        });

    Thread thread = new Thread(processTask);
    thread.setDaemon(true);
    thread.start();
  }

  public void onStopBtnClick() {
    if (processTask != null) {
      processTask.cancel();
    }

    if (commitTask != null) {
      commitTask.cancel();
    }
  }

  public void onWriteBtnClick() {
    Printer printer = new Printer(outputBox);

    commitTask = new Committer(items, printer);

    commitTask.setOnSucceeded(
        workerStateEvent -> {
          int errors = commitTask.getValue();
          printer.addDoubleLineText(errors + " errors");

          writeChangesBtn.setDisable(true);
        });

    Thread thread = new Thread(commitTask);
    thread.setDaemon(true);
    thread.start();
  }
}
