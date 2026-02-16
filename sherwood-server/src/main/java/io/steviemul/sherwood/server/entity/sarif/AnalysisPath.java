package io.steviemul.sherwood.server.entity.sarif;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@Slf4j
public class AnalysisPath {

  private final String name;
  private final String qualifiedName;
  private final List<String> parameters;

  public String getFingerprint() {
    String nameStr = name != null ? name : "";
    String qualifiedNameStr = qualifiedName != null ? qualifiedName : "";
    String parametersStr = buildParametersString();

    String fingerprintInput = nameStr + "|" + qualifiedNameStr + parametersStr;

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(fingerprintInput.getBytes(StandardCharsets.UTF_8));
      String fingerprint = bytesToHex(hash);

      log.debug(
          "Generated fingerprint for path - Input: '{}', Fingerprint: {}",
          fingerprintInput,
          fingerprint);

      return fingerprint;
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  private String buildParametersString() {
    if (parameters == null || parameters.isEmpty()) {
      return "[]";
    }

    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < parameters.size(); i++) {
      if (i > 0) {
        sb.append("|");
      }
      sb.append(parameters.get(i) != null ? parameters.get(i) : "");
    }
    sb.append("]");
    return sb.toString();
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder hexString = new StringBuilder(2 * bytes.length);
    for (byte b : bytes) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }
}
