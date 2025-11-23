package com.example.matchinghandapp.utils;

import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.TextPart;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CompletableFuture;

// Uses the Gemini AI model to calculate a match score between a user and an opportunity.
public class GeminiMatchCalculator {

    private static final String TAG = "GeminiMatchCalculator"; // Log tag for this class.
    private static final String MODEL_NAME = "gemini-2.5-flash"; // The specific Gemini model to use.
    private static final String API_KEY = "AIzaSyD0nT8ju0kKlj0Lk9P477yrRqknFtUhqEg";

    // Calculates a match score between a user profile and an opportunity.
    public static CompletableFuture<Integer> calculateMatch(String userProfile, String opportunityInfo) {
        // Run the network request on a background thread.
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Initialize the Gemini model.
                GenerativeModel model = new GenerativeModel(MODEL_NAME, API_KEY);
                GenerativeModelFutures modelFutures = GenerativeModelFutures.from(model);

                // Build the prompt for the AI model to get a numeric match score.
                String prompt =
                        "Compare the following volunteer profile and opportunity. " +
                                "Respond ONLY with a number between 0 and 100 (no text, just digits) " +
                                "indicating how well they match.\n\n" +
                                "Volunteer Profile:\n" + userProfile + "\n\n" +
                                "Opportunity Details:\n" + opportunityInfo;

                // Wrap the prompt in a Content object.
                Content content = new Content.Builder()
                        .addText(prompt)
                        .build();

                // Send the content to the model.
                ListenableFuture<GenerateContentResponse> futureResponse =
                        modelFutures.generateContent(new Content[]{content});

                // Block and wait for the response within the async task.
                GenerateContentResponse response = futureResponse.get();

                // Extract the text part and sanitize it to get only digits.
                String text = response.getText() != null ? response.getText().trim() : "";
                Log.d(TAG, "Raw Gemini response: " + text);

                String digits = text.replaceAll("[^0-9]", "");

                int match = 50; // Default fallback score.
                if (!digits.isEmpty() && digits.length() <= 3) {  // Add length check
                    match = Integer.parseInt(digits);

                    // VALIDATION: Reject invalid matches
                    if (match <= 0 || match > 100) {
                        Log.w(TAG, "Invalid match value: " + match + " - using default 50");
                        match = 50;
                    }
                }

                Log.d(TAG, "Final parsed match: " + match + "%");
                return match;

            } catch (Exception e) {
                // Handle any exceptions during the API call.
                Log.e(TAG, "Gemini calculation failed: " + e.getMessage());
                return 50; // Return a default score on error.
            }
        });
    }
}
