package com.example.overload;

public class OverloadedApp {

  public static void main(String[] args) {
    process("hello");
    process("world", 42);
  }

  public static void process(String message) {
    System.out.println("Single arg: " + message);
  }

  public static void process(String message, int count) {
    System.out.println("Two args: " + message + " x " + count);
  }
}
