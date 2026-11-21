package com.example.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class TranscriptionService {

   private OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

   public TranscriptionService(OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel) {
      this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
   }


   public String transcribeAudio(MultipartFile file) throws Exception {

      if (file == null || file.isEmpty()) {
         throw new IllegalArgumentException("Audio file is empty");
      }

      ByteArrayResource audioResource = new ByteArrayResource(file.getBytes()) {
         @Override
         public String getFilename() {
            return file.getOriginalFilename();
         }
      };

      AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource);

      AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(prompt);

      return response.getResult().getOutput();
   }
}
