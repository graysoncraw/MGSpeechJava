package com.example;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;

import com.google.cloud.speech.v1.*;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

import com.google.protobuf.ByteString;

import javax.sound.sampled.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import py4j.GatewayServer;

public class App {
    public static void main(String[] args) throws Exception {
        // GatewayServer gatewayServer = new GatewayServer(new PythonSummarizer());
        // gatewayServer.start();
        PythonSummarizer summarize = new PythonSummarizer();
        summarize.startServer();
        
        String jsonKeyPath = "wazuh-393418-3cd3bc23ce58.json";
        String languageCode = "en-US"; // Change to your desired language code
        String translatedLanguage = "es";
        String completePhrase = "";

        // Initialize the SpeechClient using the JSON key file
        SpeechClient speechClient = SpeechClient.create(SpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(getCredentialStream(jsonKeyPath))))
                .build());

        // Initialize the Translate using the JSON key file
        ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(getCredentialStream(jsonKeyPath));
        Translate translate = TranslateOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();


        // Set up audio input stream from microphone

        // Sample Rate: 16000 Hz (samples per second). This sets the number of audio samples to be captured per second.
        // Sample Size In Bits: 16 bits. This specifies the number of bits used to represent each audio sample.
        // Channels: 1. This indicates mono audio.
        // Signed: true.
        // Big-Endian: false. It specifies the byte order for the audio data.

        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        //TargetDataLine is a part of the Java Sound API and is used to capture audio data
        TargetDataLine microphone;
        //Tries to find input microphone. If it cannot, it shuts down the code.
        try {
            microphone = AudioSystem.getTargetDataLine(format);
            microphone.open(format);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
            return;
        }

        // This is used to store the captured audio data in memory.
        ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
        // It defines how much audio data will be captured at once.
        int bufferSize = 4096;
        // Temporarily stores audio data
        byte[] buffer = new byte[bufferSize];

        // Start capturing audio from the microphone
        System.out.println("Listening for audio input. Press Q to stop.");
        microphone.start();

        while (true) {
            //Takes the amount of bytes read from the microphone and stores it
            int bytesRead = microphone.read(buffer, 0, buffer.length);
            // As long as the microphone is taking in audio, it will continuously write to the audioBuffer object
            if (bytesRead > 0) {
                audioBuffer.write(buffer, 0, bytesRead);
            }

            // Check for Q to stop capturing
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

        // ByteString is a data structure used in the Google Cloud API to represent binary data.
        ByteString audioBytes = ByteString.copyFrom(audioBuffer.toByteArray());

        RecognitionConfig config = RecognitionConfig.newBuilder()
                //Specifies the audio encoding format, which is set to LINEAR16, indicating 16-bit linear PCM encoding.
                .setEncoding(AudioEncoding.LINEAR16)
                // Sets the sample rate to 16,000 Hz, matching the sample rate of the captured audio.
                .setSampleRateHertz(16000)
                // Sets the language code for the recognition. It's based on the language you want to recognize (e.g., "en-US" for US English).
                .setLanguageCode(languageCode)
                .setEnableAutomaticPunctuation(true)
                .build();

        // RecognitionAudio is a data structure used in the Google Cloud API to detect audio from bytes.
        RecognitionAudio audio = RecognitionAudio.newBuilder()
                .setContent(audioBytes)
                .build();

        // Perform the speech recognition from Google Cloud API
        RecognizeResponse response = speechClient.recognize(config, audio);

        // Print transcription results
        for (SpeechRecognitionResult result : response.getResultsList()) {
            // For each recognition result, you extract the recognized transcript (text) from the 
            // first alternative (the most likely transcription) and store it
            String speech = result.getAlternatives(0).getTranscript();
            completePhrase += speech;
        }
        System.out.println("Transcript in 'en': "+ completePhrase);
        // Translate the speech
            Translation translation = translate.translate(
                completePhrase, Translate.TranslateOption.sourceLanguage("en"),
                Translate.TranslateOption.targetLanguage(translatedLanguage));
            System.out.println("\nTranscript in '" + translatedLanguage + "': " + translation.getTranslatedText());

        String thesummary = summarize.getSummary(completePhrase);
        System.out.println("\nSummary: " + thesummary);

        summarize.stopServer();

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
