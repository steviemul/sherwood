package io.steviemul.sherwood.server.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class StorageConfiguration {

  @Bean
  public S3Client s3Client(StorageProperties props) {
    return S3Client.builder()
        .endpointOverride(URI.create(props.getEndpoint()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())))
        .region(Region.of(props.getRegion()))
        .serviceConfiguration(
            S3Configuration.builder()
                .pathStyleAccessEnabled(true) // REQUIRED for MinIO
                .build())
        .build();
  }

  @Bean
  public S3Presigner s3Presigner(StorageProperties props) {
    return S3Presigner.builder()
        .endpointOverride(URI.create(props.getEndpoint()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())))
        .region(Region.of(props.getRegion()))
        .serviceConfiguration(
            S3Configuration.builder()
                .pathStyleAccessEnabled(true) // REQUIRED for MinIO
                .build())
        .build();
  }
}
