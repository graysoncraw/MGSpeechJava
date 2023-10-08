package com.example;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.*;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.protobuf.ByteString;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class App {
    public static void main(String[] args) throws Exception {
        String jsonKeyPath = "wazuh-393418-3cd3bc23ce58.json";
        String languageCode = "en-US"; // Change to your desired language code

        // Initialize the SpeechClient using the JSON key file
        SpeechClient speechClient = SpeechClient.create(SpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(getCredentialStream(jsonKeyPath))))
                .build());

        // Set up audio input stream from microphone
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        TargetDataLine microphone;
        try {
            microphone = AudioSystem.getTargetDataLine(format);
            microphone.open(format);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
            return;
        }

        ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];

        // Start capturing audio from the microphone
        System.out.println("Listening for audio input. Press Q to stop.");
        microphone.start();

        while (true) {
            int bytesRead = microphone.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                audioBuffer.write(buffer, 0, bytesRead);
            }

            // Check for Ctrl+C to stop capturing
            if (System.in.available() > 0) {
                int key = System.in.read();
                if (key == 'Q' || key == 'q') {
                    break;
                }
            }
        }

        // Stop capturing audio
        microphone.stop();
        microphone.close();

        // Configure recognition request
        ByteString audioBytes = ByteString.copyFrom(audioBuffer.toByteArray());
        RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(AudioEncoding.LINEAR16)
                .setSampleRateHertz(16000)
                .setLanguageCode(languageCode)
                .build();

        RecognitionAudio audio = RecognitionAudio.newBuilder()
                .setContent(audioBytes)
                .build();

        // Perform the speech recognition
        RecognizeResponse response = speechClient.recognize(config, audio);

        // Print transcription results
        for (SpeechRecognitionResult result : response.getResultsList()) {
            System.out.println("Transcript: " + result.getAlternatives(0).getTranscript());
        }

        // Close the SpeechClient
        speechClient.close();
    }

    private static InputStream getCredentialStream(String jsonKeyPath) {
        try {
            return App.class.getResourceAsStream(jsonKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
