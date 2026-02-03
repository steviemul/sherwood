package com.example.test;

public class InitializerApp {
  // Field initializer - runs at class load
  private static String staticField = vulnerableStaticCall();
  
  // Instance field initializer - runs on construction
  private String instanceField = vulnerableInstanceCall();
  
  // Static initialization block - runs at class load
  static {
    System.out.println("Static block");
    anotherVulnerableCall();
  }
  
  // Instance initialization block - runs on construction  
  {
    System.out.println("Instance block");
    instanceInitCall();
  }
  
  public static void main(String[] args) {
    new InitializerApp();
  }
  
  public static String vulnerableStaticCall() {
    return "static";
  }
  
  public static void anotherVulnerableCall() {
    System.out.println("vulnerable");
  }
  
  public String vulnerableInstanceCall() {
    return "instance";
  }
  
  public void instanceInitCall() {
    System.out.println("init");
  }
}
