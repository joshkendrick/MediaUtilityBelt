package us.joshkendrick.MediaUtilityBelt.io;

import javafx.concurrent.Task;
import us.joshkendrick.MediaUtilityBelt.app.Printer;
import us.joshkendrick.MediaUtilityBelt.data.MediaFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Committer extends Task<Integer> {

  private final List<MediaFile> items;
  private final Printer printer;

  public Committer(List<MediaFile> items, Printer printer) {
    this.items = items;

    this.printer = printer;
    this.printer.start();
  }

  @Override
  protected Integer call() {
    AtomicInteger errors = new AtomicInteger();
    printer.addDoubleLineText("Writing changes to files...");
    // if has a new date/time, try to write it to dateCreated
    // if jpeg, try to write new date/time to EXIF
    printer.addSingleLineText("Writing EXIF and file created times...");
    ExifMetadataHelper exifHelper = new ExifMetadataHelper();
    items.stream()
        .filter(MediaFile::hasNewDateTime)
        .peek(
            mediaFile -> {
              try {
                BasicFileAttributeView attrs =
                    Files.getFileAttributeView(
                        mediaFile.getFile().toPath(), BasicFileAttributeView.class);
                FileTime fileTime = FileTime.from(mediaFile.getDateTime().toInstant());
                // this writes all 3 time values, though a little useless to write lastModified
                // it is overwritten by subsequent file changes
                attrs.setTimes(fileTime, fileTime, fileTime);
              } catch (IOException e) {
                printer.addSingleLineText(
                    "ERROR writing new creation date/time to " + mediaFile.getFilename());
                e.printStackTrace();
                errors.getAndIncrement();
              }
            })
        .filter(MediaFile::isExif)
        .forEach(
            mediaFile -> {
              try {
                boolean success = exifHelper.writeEXIFDateTime(mediaFile);
                if (success) {
                  printer.addSingleLineText("Wrote new EXIF for " + mediaFile.getFilename());
                } else {
                  printer.addSingleLineText("ERROR writing new EXIF to " + mediaFile.getFilename());
                  errors.getAndIncrement();
                }
              } catch (IOException e) {
                printer.addSingleLineText(
                    "ERROR writing new EXIF date/time to " + mediaFile.getFilename());
                e.printStackTrace();
                errors.getAndIncrement();
              }
            });

    // if there is a new filename, try to rename the file
    items.stream()
        .filter(MediaFile::hasNewFilename)
        .forEach(
            mediaFile -> {
              String originalFilename = mediaFile.getFilename();
              boolean success = mediaFile.relocate();
              if (success) {
                printer.addSingleLineText(
                    "Renamed " + originalFilename + " to " + mediaFile.getNewFilename());
              } else {
                printer.addSingleLineText(
                    "ERROR renaming "
                        + originalFilename
                        + " to "
                        + mediaFile.getNewFilename()
                        + " failed");
                errors.getAndIncrement();
              }
            });

    return errors.get();
  }

  @Override
  protected void cancelled() {
    super.cancelled();

    printer.addDoubleLineText("CANCELLED");
    printer.stop();
  }

  @Override
  protected void succeeded() {
    super.succeeded();

    printer.addDoubleLineText("FINISHED");
    printer.stop();
  }
}
