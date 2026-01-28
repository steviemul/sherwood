package io.steviemul.sherwood.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static io.steviemul.sherwood.server.constant.Routes.STATUS_ROUTE;

@RestController
public class SherwoodController {

  @GetMapping(STATUS_ROUTE)
  public Mono<String> getStatus() {
    return Mono.just("RUNNING");
  }
}
