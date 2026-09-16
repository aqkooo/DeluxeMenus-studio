package com.ejyqyl.dmstudio;

/**
 * Main launcher entry point without extending javafx.application.Application.
 * Required to avoid "Error: JavaFX runtime components are missing" when running fat-jars.
 */
public final class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
