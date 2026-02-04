package io.steviemul.sherwood.server.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@Component
@RequiredArgsConstructor
public class StorageInitializer {

  private final S3Client s3Client;
  private final StorageProperties storageProperties;

  @PostConstruct
  void init() {
    try {
      s3Client.headBucket(b -> b.bucket(storageProperties.getSarifBucket()));
    } catch (NoSuchBucketException e) {
      s3Client.createBucket(b -> b.bucket(storageProperties.getSarifBucket()));
    }
  }
}
