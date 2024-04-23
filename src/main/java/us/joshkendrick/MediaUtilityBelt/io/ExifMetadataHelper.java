package us.joshkendrick.MediaUtilityBelt.io;

import com.google.protobuf.FieldType;
import com.sun.scenario.effect.Offset;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryType;
import org.apache.commons.imaging.formats.tiff.fieldtypes.AbstractFieldType;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoAscii;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import us.joshkendrick.MediaUtilityBelt.data.MediaFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ExifMetadataHelper {

  // EXIF Date/Time format
  private static final DateTimeFormatter writeFormatter =
      DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

  private static final TagInfoAscii OFFSET_TIME = new TagInfoAscii("OffsetTime", 36880, 7,TiffDirectoryType.EXIF_DIRECTORY_EXIF_IFD);
  private static final TagInfoAscii OFFSET_TIME_ORIGINAL = new TagInfoAscii("OffsetTimeOriginal", 36881, 7,TiffDirectoryType.EXIF_DIRECTORY_EXIF_IFD);
  private static final TagInfoAscii OFFSET_TIME_DIGITIZED = new TagInfoAscii("OffsetTimeDigitized", 36882, 7,TiffDirectoryType.EXIF_DIRECTORY_EXIF_IFD);

  public ZonedDateTime getEXIFDateTime(File file)
      throws IOException, ParseException {
    ImageMetadata metadata = Imaging.getMetadata(file);
    ZonedDateTime exifDateTime = null;
    if (metadata instanceof JpegImageMetadata jpegMetadata) {
      TiffField dateTimeValue =
          jpegMetadata.findExifValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);

      if (dateTimeValue == null) {
        dateTimeValue = jpegMetadata.findExifValue(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
      }

      // get the first timezone field that can be parsed
      TiffField timezoneOffset =
              jpegMetadata.findExifValue(OFFSET_TIME);
      if (!(timezoneOffset.getValue() instanceof String)) {
        timezoneOffset = jpegMetadata.findExifValue(OFFSET_TIME_ORIGINAL);
        if (!(timezoneOffset.getValue() instanceof String)) {
          timezoneOffset = jpegMetadata.findExifValue(OFFSET_TIME_DIGITIZED);
        }
      }
      var timezone = ZoneId.systemDefault();
      if (timezoneOffset.getValue() instanceof String timezoneOffsetStr) {
        timezone = ZoneOffset.of(timezoneOffsetStr).normalized();
      }

      if (dateTimeValue != null) {
        var dateTimeNoZone = LocalDateTime.parse(dateTimeValue.getStringValue(), writeFormatter);
        exifDateTime = ZonedDateTime.of(dateTimeNoZone, timezone);
      }
    }
    return exifDateTime;
  }

  public boolean writeEXIFDateTime(MediaFile mediaFile)
      throws IOException {
    File jpeg = mediaFile.getFile();

    TiffOutputSet outputSet = null;

    // note that metadata might be null if no metadata is found.
    final ImageMetadata metadata = Imaging.getMetadata(jpeg);
    final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
    if (null != jpegMetadata) {
      // note that exif might be null if no Exif metadata is found.
      final TiffImageMetadata exif = jpegMetadata.getExif();

      if (null != exif) {
        // TiffImageMetadata class is immutable (read-only).
        // TiffOutputSet class represents the Exif data to write.
        //
        // Usually, we want to update existing Exif metadata by
        // changing
        // the values of a few fields, or adding a field.
        // In these cases, it is easiest to use getOutputSet() to
        // start with a "copy" of the fields read from the image.
        outputSet = exif.getOutputSet();
      }
    }

    // if file does not contain any exif metadata, we create an empty
    // set of exif metadata. Otherwise, we keep all the other
    // existing tags.
    if (null == outputSet) {
      outputSet = new TiffOutputSet();
    }

    String newDateTimeValue = mediaFile.getDateTime().format(writeFormatter);

    TiffOutputDirectory exif = outputSet.getOrCreateExifDirectory();
    exif.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
    exif.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
    exif.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, newDateTimeValue);
    exif.add(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED, newDateTimeValue);

    return saveExifToJpeg(jpeg, outputSet);
  }

  private boolean saveExifToJpeg(File jpegFile, TiffOutputSet exif)
      throws IOException {
    String tempFileName = jpegFile.getAbsolutePath() + ".tmp";
    File tempFile = new File(tempFileName);

    BufferedOutputStream tempStream = new BufferedOutputStream(new FileOutputStream(tempFile));
    new ExifRewriter().updateExifMetadataLossless(jpegFile, tempStream, exif);
    tempStream.close();

    boolean result = false;
    if (jpegFile.delete()) {
      result = tempFile.renameTo(jpegFile);
    }
    return result;
  }
}
