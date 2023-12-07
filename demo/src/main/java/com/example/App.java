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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.cloud.aiplatform.v1beta1.EndpointName;
import com.google.cloud.aiplatform.v1beta1.PredictResponse;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceSettings;

import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;

import java.util.ArrayList;
import java.util.List;




public class App {
    public static void main(String[] args) throws Exception {
        String jsonKeyPath = "wazuh-393418-3cd3bc23ce58.json";
        String languageCode = "en-US"; // Change to your desired language code
        String translatedLanguage = "es";


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
            System.out.println("Transcript in 'en': "+ speech);
            // Translate the speech
            Translation translation = translate.translate(
                speech, Translate.TranslateOption.sourceLanguage("en"),
                Translate.TranslateOption.targetLanguage(translatedLanguage));
            System.out.println("Transcript in '" + translatedLanguage + "': " + translation.getTranslatedText());

            // Summarize the speech text here

        //     String instance =
        // "{ \"content\": \"Background: There is evidence that there have been significant changes \n"
        //     + "in Amazon rainforest vegetation over the last 21,000 years through the Last \n"
        //     + "Glacial Maximum (LGM) and subsequent deglaciation. Analyses of sediment \n"
        //     + "deposits from Amazon basin paleo lakes and from the Amazon Fan indicate that \n"
        //     + "rainfall in the basin during the LGM was lower than for the present, and this \n"
        //     + "was almost certainly associated with reduced moist tropical vegetation cover \n"
        //     + "in the basin. There is debate, however, over how extensive this reduction \n"
        //     + "was. Some scientists argue that the rainforest was reduced to small, isolated \n"
        //     + "refugia separated by open forest and grassland; other scientists argue that \n"
        //     + "the rainforest remained largely intact but extended less far to the north, \n"
        //     + "south, and east than is seen today. This debate has proved difficult to \n"
        //     + "resolve because the practical limitations of working in the rainforest mean \n"
        //     + "that data sampling is biased away from the center of the Amazon basin, and \n"
        //     + "both explanations are reasonably well supported by the available data.\n"
        //     + "\n"
        //     + "Q: What does LGM stands for?\n"
        //     + "A: Last Glacial Maximum.\n"
        //     + "\n"
        //     + "Q: What did the analysis from the sediment deposits indicate?\n"
        //     + "A: Rainfall in the basin during the LGM was lower than for the present.\n"
        //     + "\n"
        //     + "Q: What are some of scientists arguments?\n"
        //     + "A: The rainforest was reduced to small, isolated refugia separated by open forest"
        //     + " and grassland.\n"
        //     + "\n"
        //     + "Q: There have been major changes in Amazon rainforest vegetation over the last how"
        //     + " many years?\n"
        //     + "A: 21,000.\n"
        //     + "\n"
        //     + "Q: What caused changes in the Amazon rainforest vegetation?\n"
        //     + "A: The Last Glacial Maximum (LGM) and subsequent deglaciation\n"
        //     + "\n"
        //     + "Q: What has been analyzed to compare Amazon rainfall in the past and present?\n"
        //     + "A: Sediment deposits.\n"
        //     + "\n"
        //     + "Q: What has the lower rainfall in the Amazon during the LGM been attributed to?\n"
        //     + "A:\"}";
        //     String parameters =
        //         "{\n"
        //             + "  \"temperature\": 0.5,\n"
        //             + "  \"maxOutputTokens\": 1024,\n"
        //             + "  \"topP\": 1,\n"
        //             + "  \"topK\": 40\n"
        //             + "}";
        //     String project = "wazuh-393418";
        //     String location = "us-central1";
        //     String publisher = "google";
        //     String model = "text-bison@002";

        //     summarizeText(instance, project, location, publisher, model);
        }

        // Close the SpeechClient
        speechClient.close();
    }



    // public static void summarizeText(
    //     String instance,
    //     String parameters,
    //     String project,
    //     String location,
    //     String publisher,
    //     String model)
    //     throws IOException {
    //   String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);
    //   PredictionServiceSettings predictionServiceSettings =
    //       PredictionServiceSettings.newBuilder()
    //           .setEndpoint(endpoint)
    //           .build();
  
    //   // Initialize client that will be used to send requests. This client only needs to be created
    //   // once, and can be reused for multiple requests.
    //   try (PredictionServiceClient predictionServiceClient =
    //       PredictionServiceClient.create(predictionServiceSettings)) {
    //     final EndpointName endpointName =
    //         EndpointName.ofProjectLocationPublisherModelName(project, location, publisher, model);
  
    //     // Use Value.Builder to convert instance to a dynamically typed value that can be
    //     // processed by the service.
    //     Value.Builder instanceValue = Value.newBuilder();
    //     JsonFormat.parser().merge(instance, instanceValue);
    //     List<Value> instances = new ArrayList<>();
    //     instances.add(instanceValue.build());
  
    //     // Use Value.Builder to convert parameter to a dynamically typed value that can be
    //     // processed by the service.
    //     Value.Builder parameterValueBuilder = Value.newBuilder();
    //     JsonFormat.parser().merge(parameters, parameterValueBuilder);
    //     Value parameterValue = parameterValueBuilder.build();
  
    //     PredictResponse predictResponse =
    //         predictionServiceClient.predict(endpointName, instances, parameterValue);
    //     System.out.println("Predict Response");
    //     System.out.println(predictResponse);
    //   }
    // }
    

    private static InputStream getCredentialStream(String jsonKeyPath) {
        try {
            return App.class.getResourceAsStream(jsonKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
