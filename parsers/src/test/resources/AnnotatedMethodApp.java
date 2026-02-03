package com.example.test;

public class AnnotatedMethodApp {

  @RequestMapping(path = "/api/test", produces = "application/json")
  @GetMapping("/users")
  public String getUsers() {
    return callInternalMethod();
  }

  private String callInternalMethod() {
    return "data";
  }

  public static void main(String[] args) {
    new AnnotatedMethodApp().getUsers();
  }
}

@interface RequestMapping {
  String path();
  String produces();
}

@interface GetMapping {
  String value();
}
