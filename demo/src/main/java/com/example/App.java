package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
import java.util.HashMap;
import java.util.Map;

import py4j.GatewayServer;

public class App {

    private JFrame frame;
    private JButton startButton;
    private JButton stopButton;
    private JButton resetButton;

    private PythonSummarizer summarize;
    private String jsonKeyPath = "wazuh-393418-3cd3bc23ce58.json";
    private String languageCode = "en-US"; // Change to your desired language code
    private boolean stopRecording = false;
    private ByteArrayOutputStream audioBuffer;
    private String completePhrase = "";
    private Translate translate;
    private SpeechClient speechClient;
    // TargetDataLine is a part of the Java Sound API and is used to capture audio
    // data
    private TargetDataLine microphone;
    private JTextArea transcriptTextAreaEn;
    private JTextArea transcriptTextAreaEs;
    private JTextArea summaryTextArea;
    private JTextArea processTextArea;
    private String selectedLanguage = "es";
    private Map<String, String> languageMap;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new App();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public App() {
        summarize = new PythonSummarizer();

        frame = new JFrame("MGSpeechAI GUI");
        frame.setSize(500, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        startButton = new JButton("Start Recording");
        stopButton = new JButton("Stop Recording");
        resetButton = new JButton("Reset");

        stopButton.setEnabled(false);

        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startButton.setEnabled(false);
                stopButton.setEnabled(true);

                try {
                    captureAndProcessAudio();
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                // This is used to store the captured audio data in memory.
                audioBuffer = new ByteArrayOutputStream();
                // It defines how much audio data will be captured at once.
                int bufferSize = 4096;
                // Temporarily stores audio data
                byte[] buffer = new byte[bufferSize];

                new Thread(() -> {
                    while (!stopRecording) {
                        // Takes the amount of bytes read from the microphone and stores it
                        int bytesRead = microphone.read(buffer, 0, buffer.length);
                        // As long as the microphone is taking in audio, it will continuously write to
                        // the audioBuffer object
                        if (bytesRead > 0) {
                            audioBuffer.write(buffer, 0, bytesRead);
                        }
                    }

                    // Stop capturing audio
                    microphone.stop();
                    microphone.close();
                    processTextArea.setText("");
                    System.out.println("\nProcessing audio. This may take a moment.");
                    processTextArea.append("Processing audio. This may take a moment.");

                    // After stopping, display results in the GUI
                    SwingUtilities.invokeLater(() -> {
                        startButton.setEnabled(false);
                        stopButton.setEnabled(false);

                        // Stop recording logic
                        displayResults();
                    });
                }).start();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startButton.setEnabled(false);
                stopButton.setEnabled(false);
                stopRecording = true;
            }
        });

        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Perform reset actions
                resetProgram();
            }
        });

        transcriptTextAreaEn = new JTextArea();
        transcriptTextAreaEn.setLineWrap(true); // Enable line wrapping
        transcriptTextAreaEn.setWrapStyleWord(true); // Wrap at word boundaries
        transcriptTextAreaEn.setEditable(false);
        transcriptTextAreaEn.setPreferredSize(new Dimension(400, 200));

        transcriptTextAreaEs = new JTextArea();
        transcriptTextAreaEs.setLineWrap(true); // Enable line wrapping
        transcriptTextAreaEs.setWrapStyleWord(true); // Wrap at word boundaries
        transcriptTextAreaEs.setEditable(false);
        transcriptTextAreaEs.setPreferredSize(new Dimension(400, 200));

        summaryTextArea = new JTextArea();
        summaryTextArea.setLineWrap(true); // Enable line wrapping
        summaryTextArea.setWrapStyleWord(true); // Wrap at word boundaries
        summaryTextArea.setEditable(false);
        summaryTextArea.setPreferredSize(new Dimension(400, 200));

        processTextArea = new JTextArea();
        processTextArea.setEditable(false);
        processTextArea.setPreferredSize(new Dimension(400, 20));

        languageMap = new HashMap<>();
        languageMap.put("English", "en");
        languageMap.put("Spanish", "es");
        languageMap.put("French", "fr");
        languageMap.put("Chinese", "zh");
        languageMap.put("Hindi", "hi");
        languageMap.put("Arabic", "ar");
        languageMap.put("Russian", "ru");
        languageMap.put("Tamil", "ta");
        languageMap.put("German", "de");
        languageMap.put("Japanese", "ja");
        languageMap.put("Korean", "ko");
        languageMap.put("Portuguese", "pt");
        languageMap.put("Polish", "po");

        // Create a JComboBox for language selection
        String[] displayLanguages = languageMap.keySet().toArray(new String[0]);
        JComboBox<String> languageComboBox = new JComboBox<>(displayLanguages);
        languageComboBox.setSelectedIndex(11); // Set the default selection to "Spanish"

        // Add an ActionListener to handle language selection
        languageComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedLanguage = (String) languageComboBox.getSelectedItem();
                selectedLanguage = languageMap.get(selectedLanguage);
                // Handle the selected language (you can set it in your code)
                System.out.println("Selected Language: " + selectedLanguage);
            }
        });

        JPanel textAreasPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        textAreasPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        textAreasPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        JPanel languagePanel = new JPanel();
        languagePanel.add(new JLabel("Transcript in"));
        languagePanel.add(languageComboBox);
        languagePanel.add(new JLabel(":"));

        // First Column
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        textAreasPanel.add(new JLabel("Transcript:"), gbc);
        textAreasPanel.add(languagePanel, gbc);
        textAreasPanel.add(new JLabel("Transcript summarized:"), gbc);
        textAreasPanel.add(new JLabel("Processing information:"), gbc);

        // Second Column
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        textAreasPanel.add(transcriptTextAreaEn, gbc);
        textAreasPanel.add(transcriptTextAreaEs, gbc);
        textAreasPanel.add(summaryTextArea, gbc);
        textAreasPanel.add(processTextArea, gbc);

        JPanel resetPanel = new JPanel();
        resetPanel.add(resetButton);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(resetButton);

        frame.setLayout(new BorderLayout());
        frame.add(textAreasPanel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void captureAndProcessAudio() throws Exception {
        // Initialize the SpeechClient using the JSON key file
        speechClient = SpeechClient.create(SpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider
                        .create(ServiceAccountCredentials.fromStream(getCredentialStream(jsonKeyPath))))
                .build());

        // Initialize the Translate using the JSON key file
        ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(getCredentialStream(jsonKeyPath));
        translate = TranslateOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();

        // Set up audio input stream from microphone

        // Sample Rate: 16000 Hz (samples per second). This sets the number of audio
        // samples to be captured per second.
        // Sample Size In Bits: 16 bits. This specifies the number of bits used to
        // represent each audio sample.
        // Channels: 1. This indicates mono audio.
        // Signed: true.
        // Big-Endian: false. It specifies the byte order for the audio data.

        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);

        // Tries to find input microphone. If it cannot, it shuts down the code.
        try {
            microphone = AudioSystem.getTargetDataLine(format);
            microphone.open(format);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
            return;
        }

        // Start capturing audio from the microphone
        System.out.println("Listening for audio input...");
        processTextArea.append("Listening for audio input...");
        microphone.start();

    }

    private void displayResults() {
        stopRecording = true;

        // ByteString is a data structure used in the Google Cloud API to represent
        // binary data.
        ByteString audioBytes = ByteString.copyFrom(audioBuffer.toByteArray());

        RecognitionConfig config = RecognitionConfig.newBuilder()
                // Specifies the audio encoding format, which is set to LINEAR16, indicating
                // 16-bit linear PCM encoding.
                .setEncoding(AudioEncoding.LINEAR16)
                // Sets the sample rate to 16,000 Hz, matching the sample rate of the captured
                // audio.
                .setSampleRateHertz(16000)
                // Sets the language code for the recognition. It's based on the language you
                // want to recognize (e.g., "en-US" for US English).
                .setLanguageCode(languageCode)
                .setEnableAutomaticPunctuation(true)
                .build();

        // RecognitionAudio is a data structure used in the Google Cloud API to detect
        // audio from bytes.
        RecognitionAudio audio = RecognitionAudio.newBuilder()
                .setContent(audioBytes)
                .build();

        // Perform the speech recognition from Google Cloud API
        RecognizeResponse response = speechClient.recognize(config, audio);

        // Print transcription results
        for (SpeechRecognitionResult result : response.getResultsList()) {
            // For each recognition result, you extract the recognized transcript (text)
            // from the
            // first alternative (the most likely transcription) and store it
            String speech = result.getAlternatives(0).getTranscript();
            completePhrase += speech;
        }
        processTextArea.setText("");
        processTextArea.setText("Complete!");
        System.out.println("\nTranscript: " + completePhrase);
        transcriptTextAreaEn.setText(completePhrase);
        // Translate the speech
        Translation translation = translate.translate(
                completePhrase, Translate.TranslateOption.sourceLanguage("en"),
                Translate.TranslateOption.targetLanguage(selectedLanguage));
        System.out.println("\nTranscript in '" + selectedLanguage + "': " + translation.getTranslatedText());
        transcriptTextAreaEs.setText(translation.getTranslatedText());

        String thesummary = summarize.getSummary(completePhrase);
        System.out.println("\nSummary: " + thesummary);
        summaryTextArea.setText(thesummary);

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
    private void resetProgram() {
        // Reset text areas and other relevant components
        transcriptTextAreaEn.setText("");
        transcriptTextAreaEs.setText("");
        summaryTextArea.setText("");
        processTextArea.setText("");
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        stopRecording = false;
        completePhrase = "";
    }

}
