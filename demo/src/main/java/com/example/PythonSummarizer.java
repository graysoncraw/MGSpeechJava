package com.example;

import py4j.GatewayServer;
import py4j.Py4JException;
import py4j.Py4JNetworkException;
import py4j.NetworkUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class PythonSummarizer {
    private GatewayServer gatewayServer;

    public PythonSummarizer() {
        // Initialize Py4J Gateway Server
        this.gatewayServer = new GatewayServer(this);
    }

    public void startServer() {
        gatewayServer.start();
        //System.out.println("Py4J Gateway Server Started");
    }

    public void stopServer() {
        gatewayServer.shutdown();
        //System.out.println("Py4J Gateway Server Stopped");
    }

    public String getSummary(String text) {
        URL resourceUrl = getClass().getResource("/SummarizerServer.py");
        // String pythonScriptPath = resourceUrl.getPath();
        // System.out.println(pythonScriptPath);
        String pythonScriptPath = "demo/src/main/resources/SummarizerServer.py";

        ProcessBuilder processBuilder = new ProcessBuilder("python3.11", pythonScriptPath, text);
        //System.out.println("process built");
        try {
            Process process = processBuilder.start();
            processBuilder.redirectErrorStream(true);
            //System.out.println("process start built");

            // Read Python output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            //System.out.println("buffer reader");
            StringBuilder output = new StringBuilder();
            //System.out.println("string builder");
            //System.out.println("Text Summarized:\n" + output.toString());
            String line;
            
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                //System.out.println("append");
            }
            // Use gateway to access the Python SummarizerServer

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

