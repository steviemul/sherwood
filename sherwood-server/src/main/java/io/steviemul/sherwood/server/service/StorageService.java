package io.steviemul.sherwood.server.service;

import io.steviemul.sherwood.server.config.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@RequiredArgsConstructor
public class StorageService {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final StorageProperties storageProperties;

  public String uploadSarif(MultipartFile file) throws IOException {
    String key = generateKey(file.getOriginalFilename());

    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(storageProperties.getSarifBucket())
            .key(key)
            .contentType(file.getContentType())
            .contentLength(file.getSize())
            .build();

    s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

    return key;
  }

  public InputStream getObject(String storageKey) {
    GetObjectRequest request =
        GetObjectRequest.builder()
            .bucket(storageProperties.getSarifBucket())
            .key(storageKey)
            .build();

    return s3Client.getObject(request);
  }

  public String getDownloadUrl(String storageKey) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder()
            .bucket(storageProperties.getSarifBucket())
            .key(storageKey)
            .responseContentDisposition("attachment")
            .build();

    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10))
            .getObjectRequest(getObjectRequest)
            .build();

    PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

    return presignedRequest.url().toString();
  }

  private String generateKey(String originalFilename) {
    return "uploads/" + UUID.randomUUID() + "/" + originalFilename;
  }
}
