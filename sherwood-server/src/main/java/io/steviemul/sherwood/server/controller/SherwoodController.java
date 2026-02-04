package io.steviemul.sherwood.server.controller;

import static io.steviemul.sherwood.server.constant.Routes.STATUS_ROUTE;
import static io.steviemul.sherwood.server.constant.Routes.UPLOAD_ROUTE;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class SherwoodController {

  @GetMapping(STATUS_ROUTE)
  public Mono<String> getStatus() {
    return Mono.just("RUNNING");
  }

  @PostMapping(UPLOAD_ROUTE)
  public Mono<Void> uploadSarif(@RequestPart("sarif") Mono<FilePart> sarifFile) {
    return sarifFile
        .doOnNext(
            file -> {
              log.info("Received SARIF upload: {}", file.filename());
              // TODO: Process the file
            })
        .then();
  }
}
