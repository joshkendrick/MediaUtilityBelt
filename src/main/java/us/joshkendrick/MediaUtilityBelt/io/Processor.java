package us.joshkendrick.MediaUtilityBelt.io;

import javafx.concurrent.Task;
import org.apache.commons.imaging.ImageReadException;
import us.joshkendrick.MediaUtilityBelt.app.Printer;
import us.joshkendrick.MediaUtilityBelt.data.MediaFile;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Processor extends Task<List<MediaFile>> {

  private static final DateTimeFormatter outputFormat =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final Printer printer;
  private final File rootDir;
  private final String gPhotosAlbumTitle;

  public Processor(File rootDir, String gPhotosAlbumTitle, Printer printer) {
    this.rootDir = rootDir;

    this.gPhotosAlbumTitle = gPhotosAlbumTitle;

    this.printer = printer;
    this.printer.start();
  }

  @Override
  protected List<MediaFile> call() {
    printer.addText("SCANNING...");

    List<MediaFile> items = findMediaFiles(rootDir);
    if (items == null || items.size() < 1) {
      return items;
    }

    // try to get date/times from google album
    if (gPhotosAlbumTitle != null && !gPhotosAlbumTitle.trim().isBlank()) {
      GooglePhotos gPhotos = GooglePhotos.getInstance();
      printer.addSingleLineText("Attempting to authenticate to Google Photos...");
      if (gPhotos.authenticate()) {
        printer.addSingleLineText("Successful authentication with google");
        if (gPhotos.findAlbum(gPhotosAlbumTitle)) {
          gPhotos.fillMediaInfo(items, printer);
        } else {
          printer.addDoubleLineText(
              "COULD NOT FIND GOOGLE PHOTOS ALBUM WITH TITLE: " + gPhotosAlbumTitle);
        }
      } else {
        printer.addDoubleLineText("COULD NOT AUTHENTICATE TO GOOGLE PHOTOS");
      }
    } else {
      printer.addDoubleLineText("No Google Photos album title provided -> skipping...");
    }

    // try to get date/times from EXIF for jpegs
    ExifMetadataHelper exifHelper = new ExifMetadataHelper();
    items.stream()
        .filter(MediaFile::isSupported)
        .forEach(
            mediaFile -> {
              try {
                mediaFile.setEXIFDateTime(exifHelper.getEXIFDateTime(mediaFile.getFile()));
              } catch (IOException | ImageReadException | ParseException e) {
                printer.addSingleLineText(
                    "ERROR: could not parse EXIF for " + mediaFile.getFilename());
                e.printStackTrace();
              }
            });

    // try to parse the filename to a date/time
    FilenameTester filenameTester = new FilenameTester();
    items.forEach(
        mediaFile -> {
          ZonedDateTime filenameDateTime =
              filenameTester.parseFilenameForDateTime(mediaFile.getFilename());
          mediaFile.setFilenameDateTime(filenameDateTime);
        });

    // sort the items, by date time, then by filename
    Collections.sort(items);
    // generate properFilenames for all MediaFiles
    // use a count and regenerating if it matches the previous should prevent any potential
    // duplicates
    String previousFilename = "";
    for (MediaFile mediaFile : items) {
      int count = 1;
      String properFilename = mediaFile.generateProperFilename(count);
      while (previousFilename.equals(properFilename)) {
        properFilename = mediaFile.generateProperFilename(count++);
      }
      previousFilename = properFilename;
    }

    // collect the items that will change and print them
    printer.addDoubleLineText(
        String.format("%50s  |  %19s  |  %s", "file", "new datetime", "new filename"));
    return items.stream()
        .filter(MediaFile::hasChanges)
        .peek(this::printMediaFile)
        .collect(Collectors.toList());
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

  private List<MediaFile> findMediaFiles(File parentDir) {
    List<MediaFile> mediaFiles = new ArrayList<>();

    File[] files =
        parentDir.listFiles(
            file -> {
              String path = file.getPath().toLowerCase();
              return path.endsWith(".jpeg")
                  || path.endsWith(".jpg")
                  || path.endsWith(".png")
                  || path.endsWith(".bmp")
                  || path.endsWith(".mp4")
                  || path.endsWith(".mov")
                  || path.endsWith(".heic");
            });
    if (files == null || files.length < 1) {
      printer.addDoubleLineText("NO FILES FOUND IN " + parentDir.getPath());
    } else {
      mediaFiles = Arrays.stream(files).map(MediaFile::new).collect(Collectors.toList());
    }

    File[] dirs = parentDir.listFiles(File::isDirectory);
    if (dirs != null) {
      for (File directory : dirs) {
        mediaFiles.addAll(findMediaFiles(directory));
      }
    }

    return mediaFiles;
  }

  private void printMediaFile(MediaFile mediaFile) {
    // pad the filepath to at least 50 characters, left-aligned. 52 with the last 2 spaces
    String formattedFilepath = String.format("%-50s  ", mediaFile.getFile().getPath());
    // only show the last 52 characters
    printer.addSingleLineText(formattedFilepath.substring(formattedFilepath.length() - 52));
    if (mediaFile.hasNewDateTime()) {
      printer.addText("|  " + mediaFile.getDateTime().format(outputFormat) + "  |");
    } else {
      // 19 spaces in the middle, for the same length as mOutputFormat
      printer.addText("|                       |");
    }

    if (mediaFile.hasNewFilename()) {
      printer.addText("  " + mediaFile.getNewFilename());
    }
  }
}
