package io.steviemul.sherwood.server.service;

import io.steviemul.sherwood.server.config.StorageProperties;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class StorageService {

  private final S3Client s3Client;
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

  private String generateKey(String originalFilename) {
    return "uploads/" + UUID.randomUUID() + "/" + originalFilename;
  }
}
