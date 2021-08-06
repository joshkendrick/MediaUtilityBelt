package us.joshkendrick.MediaUtilityBelt.io;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import com.google.photos.library.v1.proto.ListAlbumsRequest;
import com.google.photos.library.v1.proto.ListAlbumsResponse;
import com.google.photos.library.v1.proto.SearchMediaItemsRequest;
import com.google.photos.library.v1.proto.SearchMediaItemsResponse;
import com.google.photos.types.proto.Album;
import com.google.protobuf.Timestamp;
import us.joshkendrick.MediaUtilityBelt.app.PrimaryController;
import us.joshkendrick.MediaUtilityBelt.app.Printer;
import us.joshkendrick.MediaUtilityBelt.data.MediaFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class GooglePhotos {

  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static HttpTransport httpTransport;
  private static GoogleClientSecrets clientSecrets;
  private static Credential credential;
  private static GooglePhotos instance = null;
  private Album album;
  private PhotosLibraryClient plClient;

  private GooglePhotos() {}

  public static GooglePhotos getInstance() {
    if (instance == null) {
      instance = new GooglePhotos();
    }

    return instance;
  }

  // Since the app is in "Testing" in Google, the refresh token is only good for 7 days
  private boolean authorize() throws GeneralSecurityException, IOException, InterruptedException {
    // if there's no credential, or it's expired, try to create a new one
    if (credential == null
        || credential.getExpiresInSeconds() == null
        || credential.getExpiresInSeconds() <= 0) {
      // if it's expired, close existing client and httpTransport
      close();

      if (httpTransport == null) {
        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      }

      // load client secrets
      if (clientSecrets == null) {
        clientSecrets =
            GoogleClientSecrets.load(
                JSON_FACTORY,
                new InputStreamReader(
                    PrimaryController.class.getResourceAsStream("/client_secrets.json")));
      }
      // set up authorization code flow
      GoogleAuthorizationCodeFlow.Builder flowBuilder =
          new GoogleAuthorizationCodeFlow.Builder(
                  httpTransport,
                  JSON_FACTORY,
                  clientSecrets,
                  Collections.singleton("https://www.googleapis.com/auth/photoslibrary.readonly"))
              .setAccessType("online")
              .setApprovalPrompt("auto");
      // authorize
      credential =
          new AuthorizationCodeInstalledApp(flowBuilder.build(), new LocalServerReceiver())
              .authorize("user");
    }

    return (credential != null && credential.getExpiresInSeconds() > 0);
  }

  public boolean findAlbum(String albumName) {
    Optional<Album> album;
    ListAlbumsResponse response;
    Optional<ListAlbumsRequest> request = Optional.of(ListAlbumsRequest.getDefaultInstance());
    do {
      response = plClient.listAlbumsCallable().call(request.get());
      album =
          response.getAlbumsList().stream()
              .filter(gAlbum -> albumName.equals(gAlbum.getTitle()))
              .findFirst();

      if (response.getNextPageToken().isEmpty()) {
        request = Optional.empty();
      } else {
        request =
            Optional.of(
                request.get().toBuilder().setPageToken(response.getNextPageToken()).build());
      }
    } while (request.isPresent() && album.isEmpty());

    if (album.isPresent()) {
      this.album = album.get();
      return true;
    } else {
      return false;
    }
  }

  public void fillMediaInfo(List<MediaFile> files, Printer printer) {
    HashMap<String, ZonedDateTime> mapping = new HashMap<>();
    SearchMediaItemsResponse response;
    Optional<SearchMediaItemsRequest> request =
        Optional.of(SearchMediaItemsRequest.newBuilder().setAlbumId(album.getId()).build());
    do {
      printer.addSingleLineText("Requesting items from google...");
      response = plClient.searchMediaItemsCallable().call(request.get());
      printer.addSingleLineText(
          "Processing "
              + response.getMediaItemsList().size()
              + " items from Google Photos album...");
      response
          .getMediaItemsList()
          .forEach(
              gItem -> {
                Timestamp ts = gItem.getMediaMetadata().getCreationTime();
                Instant i = Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
                ZonedDateTime zdt = i.atZone(ZoneId.systemDefault());
                /*
                 * if there are files with the same name in the google photos album
                 * Ex: IMG_1128.jpg x2 in google, but IMG_1128.jpg and IMG_1128(1).jpg locally
                 * then the first date/time will be overwritten by the second
                 * I believe the best way to handle this is figure out the dupes, pull the dupes into
                 * a second album on google photos and locally and run those files separately
                 */
                mapping.put(gItem.getFilename().toLowerCase(), zdt);
              });

      if (response.getNextPageToken().isEmpty()) {
        request = Optional.empty();
      } else {
        request =
            Optional.of(
                request.get().toBuilder().setPageToken(response.getNextPageToken()).build());
      }
    } while (request.isPresent());

    for (MediaFile mediaFile : files) {
        mediaFile.setGPhotosDateTime(mapping.get(mediaFile.getFilename().toLowerCase()));
    }
  }

  public boolean authenticate() {
    try {
      // if there wasn't a credential, close up everything
      if (!authorize()) {
        close();
        return false;
      }
      // we have a valid credential, if the client is null, build it
      else if (plClient == null || plClient.isShutdown() || plClient.isTerminated()) {
        build(credential);
      }
      return true;
    } catch (IOException e) {
      System.err.println(e.getMessage());
    } catch (Throwable t) {
      t.printStackTrace();
    }

    return false;
  }

  private void close() throws InterruptedException, IOException {
    if (plClient != null) {
      plClient.shutdownNow();
      plClient.awaitTermination(15, TimeUnit.SECONDS);
      plClient.close();
    }

    if (httpTransport != null) {
      httpTransport.shutdown();
    }
  }

  private void build(Credential credential) throws IOException {
    // Set up the Photos Library Client that interacts with the API
    OAuth2Credentials credentials =
        OAuth2Credentials.create(
            new AccessToken(
                credential.getAccessToken(), new Date(credential.getExpirationTimeMilliseconds())));
    PhotosLibrarySettings settings =
        PhotosLibrarySettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
            .build();

    plClient = PhotosLibraryClient.initialize(settings);
  }
}
