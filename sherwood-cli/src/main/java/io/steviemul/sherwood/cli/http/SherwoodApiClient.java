package io.steviemul.sherwood.cli.http;

import java.io.File;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** HTTP client for communicating with the Sherwood server API. */
public class SherwoodApiClient implements AutoCloseable {

  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  private final OkHttpClient client;
  private final String baseUrl;

  /**
   * Create a new Sherwood API client.
   *
   * @param baseUrl Base URL of the Sherwood server (e.g., "<a
   *     href="http://localhost:12080">...</a>")
   */
  public SherwoodApiClient(String baseUrl) {
    this.client = new OkHttpClient();
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  /**
   * Check if the Sherwood server is running.
   *
   * @return true if server responds with "RUNNING", false otherwise
   */
  public boolean isServerRunning() {
    try {
      Request request = new Request.Builder().url(baseUrl + "/sherwood/status").get().build();

      try (Response response = client.newCall(request).execute()) {
        return response.isSuccessful()
            && response.body() != null
            && "RUNNING".equals(response.body().string());
      }
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Upload a SARIF file to the Sherwood server.
   *
   * @param sarifFile The SARIF file to upload
   * @return true if upload was successful, false otherwise
   * @throws IOException if an I/O error occurs during upload
   */
  public boolean uploadSarif(File sarifFile) throws IOException {
    if (!sarifFile.exists() || !sarifFile.isFile()) {
      throw new IllegalArgumentException("SARIF file does not exist: " + sarifFile);
    }

    RequestBody requestBody =
        new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("sarif", sarifFile.getName(), RequestBody.create(sarifFile, JSON))
            .build();

    Request request =
        new Request.Builder().url(baseUrl + "/sherwood/upload").post(requestBody).build();

    try (Response response = client.newCall(request).execute()) {
      return response.isSuccessful();
    }
  }

  /**
   * Upload a SARIF file to the Sherwood server with custom filename.
   *
   * @param sarifFile The SARIF file to upload
   * @param filename The filename to use in the multipart request
   * @return true if upload was successful, false otherwise
   * @throws IOException if an I/O error occurs during upload
   */
  public boolean uploadSarif(File sarifFile, String filename) throws IOException {
    if (!sarifFile.exists() || !sarifFile.isFile()) {
      throw new IllegalArgumentException("SARIF file does not exist: " + sarifFile);
    }

    RequestBody requestBody =
        new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("sarif", filename, RequestBody.create(sarifFile, JSON))
            .build();

    Request request =
        new Request.Builder().url(baseUrl + "/sherwood/upload").post(requestBody).build();

    try (Response response = client.newCall(request).execute()) {
      return response.isSuccessful();
    }
  }

  /** Close the HTTP client and release resources. */
  @Override
  public void close() {
    client.dispatcher().executorService().shutdown();
    client.connectionPool().evictAll();
  }
}
