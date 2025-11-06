package com.example.matchinghandapp.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserProfileCache {
    private static String cachedProfile = null;

    public static String getCurrentUserProfile(Context context) {
        if (cachedProfile != null) return cachedProfile;

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) return "Anonymous user";

        FirebaseFirestore.getInstance().collection("Users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
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

        return cachedProfile != null ? cachedProfile : "Loading profile...";
    }
}
