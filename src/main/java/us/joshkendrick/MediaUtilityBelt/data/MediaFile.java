package us.joshkendrick.MediaUtilityBelt.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Stream;

public class MediaFile implements Comparable<MediaFile> {

  private static final DateTimeFormatter filenameFormat =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss_");
  private final boolean isExif;
  private File file;
  private ZonedDateTime currentDateTime;

  private ZonedDateTime gPhotosDateTime;

  private ZonedDateTime exifDateTime;

  private ZonedDateTime filenameDateTime;

  private String properFilename;

  public MediaFile(File file) {
    this.file = file;
    String filename = getFilename().toLowerCase();
    this.isExif = Stream.of(".jpeg", ".jpg", ".heic").anyMatch(filename::endsWith);

    try {
      BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
      FileTime fileTime = attr.creationTime();
      currentDateTime = fileTime.toInstant().atZone(ZoneId.systemDefault());
    } catch (IOException e) {
      e.printStackTrace();
      currentDateTime = ZonedDateTime.now();
    }
  }

  @Override
  public int compareTo(MediaFile otherObj) {
    // first compare by date
    int distance = getDateTime().compareTo(otherObj.getDateTime());

    // if dates are the same, compare by filename
    if (distance == 0) {
      distance = getFilename().compareToIgnoreCase(otherObj.getFilename());
    }

    return distance;
  }

  public File getFile() {
    return file;
  }

  public String getFilename() {
    return file.getName();
  }

  public void setGPhotosDateTime(ZonedDateTime googleDateTime) {
    this.gPhotosDateTime = googleDateTime;
  }

  public void setEXIFDateTime(ZonedDateTime exifDateTime) {
    this.exifDateTime = exifDateTime;
  }

  public void setFilenameDateTime(ZonedDateTime filenameDateTime) {
    this.filenameDateTime = filenameDateTime;
  }

  public boolean isExif() {
    return isExif;
  }

  public boolean hasChanges() {
    return hasNewDateTime() || hasNewFilename();
  }

  public boolean hasNewDateTime() {
    return !getDateTime().equals(currentDateTime);
  }

  public boolean hasNewFilename() {
    return !getFilename().equals(properFilename);
  }

  public String getNewFilename() {
    return properFilename;
  }

  public String generateProperFilename(int count) {
    String currentName = getFilename();

    int dot = currentName.lastIndexOf(".");
    String extension = currentName.substring(dot).toLowerCase();

    String newBaseFilename = getDateTime().format(filenameFormat) + (String.format("%02d", count));

    properFilename = newBaseFilename + extension;
    return properFilename;
  }

  public boolean relocate() {
    Path path = Paths.get(file.toURI());
    File newFile = path.resolveSibling(properFilename).toFile();

    boolean result = file.renameTo(newFile);
    // update file to point to the new File
    if (result) {
      file = newFile;
    }
    return result;
  }

  public ZonedDateTime getDateTime() {
    return Stream.of(exifDateTime, gPhotosDateTime, filenameDateTime, currentDateTime)
        .filter(Objects::nonNull)
        .findFirst()
        .get();
  }
}
