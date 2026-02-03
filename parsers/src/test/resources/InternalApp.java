package com.example.internal;

public class InternalApp {

  public static void main(String[] args) {
    processPublic();
  }

  public static void processPublic() {
    System.out.println("Public processing");
  }

  // This helper is called by a non-entry method
  public static void internalHelper() {
    System.out.println("Internal helper");
  }

  // This calls the internal helper but is not reachable from main
  public static void isolatedMethod() {
    internalHelper();
  }
}
