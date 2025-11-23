package com.example.matchinghandapp.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

// Caches the current user's profile to avoid repeated Firestore lookups.
public class UserProfileCache {
    // Static variable to hold the cached profile string.
    private static String cachedProfile = null;

    // Retrieves the current user's profile, either from cache or by fetching from Firestore.
    public static String getCurrentUserProfile(Context context) {
        // Return cached profile if available.
        if (cachedProfile != null) return cachedProfile;

        // Get current user's ID.
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        // Handle anonymous users.
        if (uid == null) return "Anonymous user";

        // Fetch user document from Firestore.
        FirebaseFirestore.getInstance().collection("Users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // If document exists, build and cache the profile string.
                        String name = doc.getString("name");
                        String skills = doc.getString("skills");
                        String interests = doc.getString("interests");
                        String experience = doc.getString("experience");
                        String address = doc.getString("address");
                        cachedProfile = "Name: " + name + "\nSkills: " + skills +
                                "\nInterests: " + interests + "\nExperience: " + experience+ "\nLocation: " + address;
                    }
                })
                .addOnFailureListener(e -> Log.e("UserProfileCache", "Failed: " + e.getMessage()));

        // Return current state (might be null initially, returning a loading message).
        return cachedProfile != null ? cachedProfile : "Loading profile...";
    }
}
