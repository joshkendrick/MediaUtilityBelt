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

#### The privacy policy only exists here in order to get verified by Google. I don't retain any data, this app just pulls the media from your album, sees if the filenames match, and grabs the date/time from those photos/videos. Everything else is discarded after the call.

### Development:
Everything necessary to compile/build/deploy should be included in the pom. You should be able to import this as a maven project into IntelliJ.

There are 2 configurations you'll need to run in order to debug. Run the debug execution in the pom, then run a RemoteJVM Debugger config to debug it.

#### Setup:
Since this targets Java 11 (I used Amazon Coretto 11), to do development you will need to download the javafx sdk and put it on your computer someplace. You may have to update a config or setting to get it to build

If you want to run your own version, you will need to create a project on Google APIs Console, create your own OAuth consent screen with the google-photos-viewonly scope, add your google account as a test user, create an OAuth 2.0 Client ID and replace the client_secrets.json in this project with the one you download. It isn't hard, just not-obvious, but after all that, running it should work.