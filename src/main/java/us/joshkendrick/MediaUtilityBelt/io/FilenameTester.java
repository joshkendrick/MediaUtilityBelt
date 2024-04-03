package us.joshkendrick.MediaUtilityBelt.io;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Pattern;

public class FilenameTester {
  private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HHmmss");

  public final ZonedDateTime parseFilenameForDateTime(String filename) {
    try {
      String pattern = "\\d{4}-\\d{2}-\\d{2}_\\d{6}_.*";
      if (Pattern.matches(pattern, filename)) {
        return dateFormat.parse(filename).toInstant().atZone(ZoneId.systemDefault());
      }
    } catch (ParseException e) {
      // do nothing
    }

    return null;
  }
}
