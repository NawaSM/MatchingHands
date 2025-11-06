package com.example.matchinghandapp.utils;

import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.TextPart;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CompletableFuture;

public class GeminiMatchCalculator {

    private static final String TAG = "GeminiMatchCalculator";
    private static final String MODEL_NAME = "gemini-2.5-flash";
    private static final String API_KEY = "AIzaSyCjj6teu6P_xSnJPJT4m9ads-Hm12HWAWM"; // Replace with your Gemini API key

    public static CompletableFuture<Integer> calculateMatch(String userProfile, String opportunityInfo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // ✅ Initialize Gemini model
                GenerativeModel model = new GenerativeModel(MODEL_NAME, API_KEY);
                GenerativeModelFutures modelFutures = GenerativeModelFutures.from(model);

                // ✅ Build your prompt
                String prompt =
                        "Compare the following volunteer profile and opportunity. " +
                                "Respond ONLY with a number between 0 and 100 (no text, just digits) " +
                                "indicating how well they match.\n\n" +
                                "Volunteer Profile:\n" + userProfile + "\n\n" +
                                "Opportunity Details:\n" + opportunityInfo;

                // ✅ Wrap the text in a Content object
                Content content = new Content.Builder()
                        .addText(prompt)
                        .build();

                // ✅ Generate response from Gemini
                ListenableFuture<GenerateContentResponse> futureResponse =
                        modelFutures.generateContent(new Content[]{content});

                // ✅ Block and get response safely inside async thread
                GenerateContentResponse response = futureResponse.get();

                // ✅ Extract numeric value
                String text = response.getText() != null ? response.getText().trim() : "50";
                String digits = text.replaceAll("[^0-9]", "");

                int match = 50; // default fallback
                if (!digits.isEmpty()) match = Integer.parseInt(digits);
                if (match > 100) match = 100;
                if (match < 0) match = 0;

                Log.d(TAG, "Gemini returned match: " + match + "%");
                return match;

            } catch (Exception e) {
                Log.e(TAG, "Gemini calculation failed: " + e.getMessage());
                return 50; // default on error
            }
        });
    }
}
