package com.example;

import py4j.GatewayServer;
import py4j.Py4JException;
import py4j.Py4JNetworkException;
import py4j.Py4JNetworkUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PythonSummarizer {
    private GatewayServer gatewayServer;

    public PythonSummarizer() {
        // Initialize Py4J Gateway Server
        this.gatewayServer = new GatewayServer(this);
    }

    public void startServer() {
        gatewayServer.start();
        System.out.println("Py4J Gateway Server Started");
    }

    public void stopServer() {
        gatewayServer.shutdown();
        System.out.println("Py4J Gateway Server Stopped");
    }

    public String getSummary(String text) {
        String pythonScriptPath = "/path/to/your/python_script.py"; // Replace with your Python script path

        ProcessBuilder processBuilder = new ProcessBuilder("python", pythonScriptPath, text);
        try {
            Process process = processBuilder.start();

            // Read Python output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Wait for the process to finish
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output.toString();
            } else {
                throw new Py4JException("Python process returned non-zero exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        PythonSummarizer summarizer = new PythonSummarizer();
        summarizer.startServer();

        // Your Java code can interact with PythonSummarizer here
        // For example:
        String article = "Your article text goes here...";
        String summary = summarizer.getSummary(article);
        System.out.println("Summary: " + summary);

        summarizer.stopServer();
    }
}

