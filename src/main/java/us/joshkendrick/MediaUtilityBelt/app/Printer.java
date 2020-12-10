package us.joshkendrick.MediaUtilityBelt.app;

import javafx.animation.AnimationTimer;
import javafx.scene.control.TextArea;

public class Printer extends AnimationTimer {

  private static final int CAPACITY = 150000;

  private final TextArea textArea;

  private StringBuffer buffer = new StringBuffer(CAPACITY);

  // ------------------------------------------------------------------------
  // Constructors
  // ------------------------------------------------------------------------

  public Printer(TextArea textArea) {
    this.textArea = textArea;
  }

  // ------------------------------------------------------------------------
  // Super Methods
  // ------------------------------------------------------------------------

  @Override
  public synchronized void handle(long now) {
    flush();
  }

  @Override
  public synchronized void stop() {
    flush();

    super.stop();
  }

  private synchronized void flush() {
    textArea.appendText(buffer.toString());
    buffer = new StringBuffer(CAPACITY);
  }

  public synchronized void addText(String text) {
    buffer.append(text);
  }

  public synchronized void addSingleLineText(String text) {
    addText("\n" + text);
  }

  public synchronized void addDoubleLineText(String text) {
    addText("\n\n" + text);
  }

  public synchronized void clear() {
    buffer = new StringBuffer(CAPACITY);
    textArea.clear();
  }
}
