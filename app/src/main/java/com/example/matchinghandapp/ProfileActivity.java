package com.example.matchinghandapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This activity allows users to view and update their profile information.
public class ProfileActivity extends AppCompatActivity {

    // UI elements for the profile screen.
    private ImageView profileImage;
    private EditText fullName, email, phone, experience, skills, languages;
    private Button btnUpdate, btnSelectInterests;
    private ChipGroup selectedInterestsChipGroup;

    // Firebase services.
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private ProgressDialog progressDialog; // Shows loading progress.
    private DocumentReference userRef; // Reference to the user's document in Firestore.
    private StorageReference storageRef; // Reference to Firebase Storage for profile images.

    // Data variables.
    private Uri imageUri; // URI of the selected profile image.
    private List<String> selectedInterests = new ArrayList<>(); // List of user's interests.

    // Called when the activity is first created.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase services and get user reference.
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("ProfileImages");
        String userId = mAuth.getCurrentUser().getUid();
        userRef = firestore.collection("Users").document(userId);

        // Initialize UI components.
        profileImage = findViewById(R.id.ivProfileImage);
        fullName = findViewById(R.id.etFullName);
        email = findViewById(R.id.etEmail);
        phone = findViewById(R.id.etPhone);
        experience = findViewById(R.id.etExperience);
        skills = findViewById(R.id.etSkills);
        languages = findViewById(R.id.etLanguages);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnSelectInterests = findViewById(R.id.btnSelectInterests);
        selectedInterestsChipGroup = findViewById(R.id.selectedInterestsChipGroup);

        // Initialize the progress dialog.
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        loadProfile(); // Load user profile data from Firestore.

        // Set click listeners.
        profileImage.setOnClickListener(v -> pickImageFromGallery());
        btnUpdate.setOnClickListener(v -> updateProfile());
        btnSelectInterests.setOnClickListener(v -> showInterestSelectionDialog());
    }

    // Shows a dialog for selecting interests (tags).
    private void showInterestSelectionDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_tags, null);
        ChipGroup dialogChipGroup = dialogView.findViewById(R.id.dialogTagChipGroup);

        // Populate dialog with all available tags.
        for (String tag : Tags.ALL_TAGS) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setChecked(selectedInterests.contains(tag)); // Pre-check currently selected interests.
            dialogChipGroup.addView(chip);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Handle cancel and done button clicks.
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnDone).setOnClickListener(v -> {
            selectedInterests.clear();
            for (int i = 0; i < dialogChipGroup.getChildCount(); i++) {
                Chip chip = (Chip) dialogChipGroup.getChildAt(i);
                if (chip.isChecked()) {
                    selectedInterests.add(chip.getText().toString());
                }
            }
            updateSelectedInterestsDisplay(); // Refresh the UI.
            dialog.dismiss();
        });

        dialog.show();
    }

    // Updates the UI to show the currently selected interests.
    private void updateSelectedInterestsDisplay() {
        selectedInterestsChipGroup.removeAllViews();

        if (selectedInterests.isEmpty()) {
            btnSelectInterests.setText("Select Interests");
            return;
        }

        btnSelectInterests.setText(selectedInterests.size() + " interest(s) selected");

        // Add a chip for each selected interest.
        for (String interest : selectedInterests) {
            Chip chip = new Chip(this);
            chip.setText(interest);
            chip.setCloseIconVisible(true);
            // Allow removing an interest by clicking the close icon.
            chip.setOnCloseIconClickListener(v -> {
                selectedInterests.remove(interest);
                updateSelectedInterestsDisplay();
            });
            selectedInterestsChipGroup.addView(chip);
        }
    }

    // Loads the user's profile data from Firestore.
    private void loadProfile() {
        progressDialog.setMessage("Loading profile...");
        progressDialog.show();

        userRef.get().addOnSuccessListener(snapshot -> {
            progressDialog.dismiss();
            if (snapshot.exists()) {
                // Populate the UI with data from Firestore.
                fullName.setText(snapshot.getString("fullName"));
                email.setText(snapshot.getString("email"));
                phone.setText(snapshot.getString("phone"));
                experience.setText(snapshot.getString("experience"));
                skills.setText(snapshot.getString("skills"));
                languages.setText(snapshot.getString("languages"));

                String imageUrl = snapshot.getString("profileImageUrl");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(this).load(imageUrl).into(profileImage);
                }

                // Load interests, handling both old String and new List formats for backward compatibility.
                Object interestObj = snapshot.get("interest");
                if (interestObj != null) {
                    if (interestObj instanceof List) {
                        selectedInterests = new ArrayList<>((List<String>) interestObj);
                    } else if (interestObj instanceof String) {
                        // Convert old string format to a list.
                        String oldInterest = (String) interestObj;
                        if (!oldInterest.isEmpty()) {
                            selectedInterests = new ArrayList<>();
                            selectedInterests.add(oldInterest);
                        }
                    }
                    updateSelectedInterestsDisplay();
                }
            } else {
                Toast.makeText(this, "No user profile found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    // Opens the gallery to pick an image.
    private void pickImageFromGallery() {
        android.content.Intent intent = new android.content.Intent();
        intent.setType("image/*");
        intent.setAction(android.content.Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 100);
    }

    // Handles the result from the image picker.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri); // Show the selected image.
        }
    }

    // Updates the user's profile information in Firestore.
    private void updateProfile() {
        progressDialog.setMessage("Updating profile...");
        progressDialog.show();

        String name = fullName.getText().toString().trim();
        String phoneNo = phone.getText().toString().trim();

        if (name.isEmpty()) {
            fullName.setError("Enter your name");
            progressDialog.dismiss();
            return;
        }

        // Create a map to hold the updated profile data.
        Map<String, Object> map = new HashMap<>();
        map.put("fullName", name);
        map.put("phone", phoneNo);
        map.put("interest", selectedInterests);
        map.put("experience", experience.getText().toString());
        map.put("skills", skills.getText().toString());
        map.put("languages", languages.getText().toString());

        // If a new image was selected, upload it to Firebase Storage first.
        if (imageUri != null) {
            StorageReference ref = storageRef.child(mAuth.getCurrentUser().getUid() + ".jpg");
            ref.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        map.put("profileImageUrl", uri.toString());
                        saveProfile(map); // Save profile data with the new image URL.
                    }))
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            saveProfile(map); // Save profile data without changing the image.
        }
    }

    // Saves the profile data to Firestore.
    private void saveProfile(Map<String, Object> map) {
        userRef.update(map)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
