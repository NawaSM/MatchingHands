package com.example.matchinghandapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

// This activity handles the process of a user applying for an opportunity.
public class ApplyOpportunityActivity extends AppCompatActivity {

    // UI elements for the application form.
    private EditText fullName, phoneNumber, emailAddress, motivation, experience;
    private Button applyButton;

    // Firebase services.
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private ProgressDialog progressDialog; // Shows loading progress.

    // Data variables for the opportunity being applied to.
    private String opportunityId, organizerId;

    // Called when the activity is first created.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_opportunity);

        // Initialize Firebase services.
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize UI components.
        fullName = findViewById(R.id.fullName);
        phoneNumber = findViewById(R.id.phoneNumber);
        emailAddress = findViewById(R.id.emailAddress);
        motivation = findViewById(R.id.motivation);
        experience = findViewById(R.id.experience);
        applyButton = findViewById(R.id.applyButton);

        // Initialize the progress dialog.
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Submitting application...");

        // Get the opportunity and organizer IDs from the intent.
        opportunityId = getIntent().getStringExtra("opportunityId");
        organizerId = getIntent().getStringExtra("organizerId");

        loadUserData(); // Pre-fill user data from their profile.

        // Set a click listener for the apply button.
        applyButton.setOnClickListener(v -> submitApplication());
    }

    // Loads the current user's profile information from Firestore to pre-fill the form.
    private void loadUserData() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firestore.collection("Users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Set the text of the input fields with user data.
                        fullName.setText(documentSnapshot.getString("fullName"));
                        phoneNumber.setText(documentSnapshot.getString("phone"));
                        emailAddress.setText(documentSnapshot.getString("email"));
                    } else {
                        Toast.makeText(this, "Profile not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // Submits the application to Firestore.
    private void submitApplication() {
        // Get the data from the input fields.
        String name = fullName.getText().toString().trim();
        String phone = phoneNumber.getText().toString().trim();
        String email = emailAddress.getText().toString().trim();
        String motivationText = motivation.getText().toString().trim();
        String experienceText = experience.getText().toString().trim();

        // Validate the input.
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please ensure your details are filled in.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(motivationText)) {
            motivation.setError("Please write a motivation statement.");
            return;
        }

        progressDialog.show(); // Show loading indicator.

        String applicantId = auth.getCurrentUser().getUid();

        // Create a map to hold the application data.
        Map<String, Object> applicationData = new HashMap<>();
        applicationData.put("applicantId", applicantId);
        applicationData.put("organizerId", organizerId);
        applicationData.put("opportunityId", opportunityId);
        applicationData.put("fullName", name);
        applicationData.put("phone", phone);
        applicationData.put("email", email);
        applicationData.put("motivation", motivationText);
        applicationData.put("experience", experienceText);
        applicationData.put("status", "Pending"); // Initial status is always "Pending".
        applicationData.put("timestamp", System.currentTimeMillis());

        // Add the application data as a new document in the "Applications" collection.
        firestore.collection("Applications")
                .add(applicationData)
                .addOnSuccessListener(documentReference -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Application submitted successfully!", Toast.LENGTH_LONG).show();
                    finish(); // Close the activity.
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
