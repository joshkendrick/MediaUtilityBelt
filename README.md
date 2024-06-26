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

### Development:
Everything necessary to compile/build/deploy should be included in the pom. You should be able to import this as a maven project into IntelliJ.

There are 2 configurations you'll need to run in order to debug. Run the debug execution in the pom, then run a RemoteJVM Debugger config to debug it.

#### Publishing:
`mvn clean compile package`

Note that when trying to publish, the built jar will appear in a "shade/" subdirectory - NOT under "target/" directory 

Once the jar is built, you should be able to use it by double-clicking the jar (if you have Java set up correctly on your machine)
- make sure to test the program outside IntelliJ. If issues are encountered, you can start the program through shell with java -jar <filename>.jar and any stack trace will print to the console

#### Setup:
Since this targets Java 17, to do development you will need to download the javafx sdk and put it on your computer someplace. You may have to update a config or setting to get it to build

If you want to run your own version, you will need to create a project on Google APIs Console, create your own OAuth consent screen with the google-photos-viewonly scope, add your google account as a test user, create an OAuth 2.0 Client ID and replace the client_secrets.json in this project with the one you download. It isn't hard, just not-obvious, but after all that, running it should work.

### Google APIs
I tried a few different ways of getting this published. In the end, I decided it just wasn't worth the trouble for a freebie app where I'm probably the only person who will ever use it:

1. I started with an "External" in "Testing" as a POC.
2. To get rid of all the warning screens on consent, you have to get "Approved" which involves a whole bunch of things, web pages, privacy policies, videos, terms of service
3. Google suggested just being an "Internal" app
    - I created an org, moved the app, etc.
    - However, could not figure out how to add my personal Gmail (where all my photos/videos are) to the organization. So also didnt solve the problem
4. At this point, decide just keep it an "External" app in "Testing" since no one will ever use it anyway

After using it for a few months, I noticed every time I tried to re-use it after a long amount of time, the token refresh wasn't working. I worked on it for a while, found a note that says an app is in "Testing", the refresh token expires after 7 days. So storing the refresh token, the "offline" mode wasn't really going to work anyway.

Final solution, it runs "online" in "auto". You'll have to re-consent each time you start the app, but whatever.