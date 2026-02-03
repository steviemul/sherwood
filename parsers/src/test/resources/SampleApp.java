package com.example.test;

public class SampleApp {

  public static void main(String[] args) {
    processRequest();
  }

  public static void processRequest() {
    validateInput();
    executeLogic();
  }

  public static void validateInput() {
    System.out.println("Validating input");
  }

  public static void executeLogic() {
    performCalculation();
  }

  public static void performCalculation() {
    // This is our target method at line 21
    System.out.println("Performing calculation");
  }

  public static void unreachableMethod() {
    // This method is never called from main
    System.out.println("This is unreachable");
  }
}
