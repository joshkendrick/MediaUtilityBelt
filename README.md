## MediaUtilityBelt

a tool for correcting media EXIF data, last modified, date created, and chronologically renaming.

Check out the [releases](https://github.com/joshkendrick/MediaUtilityBelt/releases) if you just want to use it, but a few things you should know:

- `MediaUtilityBelt` works in 2 steps:
    1. It goes through a directory and finds media files. It gets information on those files and determines date/times and order. It tells you if it found anything it couldn't figure out. Review the output for correctness:
        - if the `newDateTime` column is populated, it will try to write a new date/time for lastModified, dateCreated, and EXIF if it's a jpeg.
        - if there's a value in `newFilename` it's going to rename the file with a better name.
    2. Then you can click `Write Changes` to make those changes.
- If you provide a Google Photos album name, it will attempt to find that album in your Google Photos and get the date/time by matching filenames.
- It will try to read a date/time from the filename, the pattern is yyyy-MM-dd_HHmmss
