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
import com.google.api.client.util.store.FileDataStoreFactory;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class GooglePhotos {

  private static final File DATA_STORE_DIR =
      new File(System.getProperty("user.home"), ".store/mediaUtilityBelt");
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static FileDataStoreFactory dataStoreFactory;
  private static HttpTransport httpTransport;

  private Album album;
  private PhotosLibraryClient plClient;

  private static Credential authorize() throws Exception {
    // load client secrets
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(
            JSON_FACTORY,
            new InputStreamReader(
                PrimaryController.class.getResourceAsStream("/client_secrets.json")));
    // set up authorization code flow
    GoogleAuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                JSON_FACTORY,
                clientSecrets,
                Collections.singleton("https://www.googleapis.com/auth/photoslibrary.readonly"))
            .setDataStoreFactory(dataStoreFactory)
            .setAccessType("offline")
            .setApprovalPrompt("auto")
            .build();
    // authorize
    return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
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
                mapping.put(gItem.getFilename(), zdt);
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
      if (mapping.containsKey(mediaFile.getFilename())) {
        mediaFile.setGPhotosDateTime(mapping.get(mediaFile.getFilename()));
      }
    }
  }

  public boolean authenticate() {
    try {
      httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
      // authorization
      Credential credential = authorize();
      if (credential.getExpiresInSeconds() == null || credential.getExpiresInSeconds() < 0) {
        credential.refreshToken();
      }

      // Set up the Photos Library Client that interacts with the API
      OAuth2Credentials credentials =
          OAuth2Credentials.create(
              new AccessToken(
                  credential.getAccessToken(),
                  new Date(credential.getExpirationTimeMilliseconds())));
      PhotosLibrarySettings settings =
          PhotosLibrarySettings.newBuilder()
              .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
              .build();

      plClient = PhotosLibraryClient.initialize(settings);
    } catch (IOException e) {
      System.err.println(e.getMessage());
    } catch (Throwable t) {
      t.printStackTrace();
    }

    return true;
  }
}
