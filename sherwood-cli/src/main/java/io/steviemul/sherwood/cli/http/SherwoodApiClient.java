package io.steviemul.sherwood.cli.http;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
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
        new Request.Builder().url(baseUrl + "/api/sherwood/sarifs").post(requestBody).build();

    try (Response response = client.newCall(request).execute()) {
      return response.isSuccessful();
    }
  }

  /** Close the HTTP client and release resources. */
  @Override
  public void close() {
    try (ExecutorService executorService = client.dispatcher().executorService()) {
      executorService.shutdown();
      client.connectionPool().evictAll();
    }
  }
}
