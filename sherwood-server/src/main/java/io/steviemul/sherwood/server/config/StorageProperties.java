package io.steviemul.sherwood.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "storage.s3")
@Getter
@Setter
public class StorageProperties {

  private String endpoint;
  private String region;
  private String accessKey;
  private String secretKey;
  private String sarifBucket;
}
