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

public class ApplyOpportunityActivity extends AppCompatActivity {

    private EditText fullName, phoneNumber, emailAddress, motivation, experience;
    private Button applyButton;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private ProgressDialog progressDialog;

    private String opportunityId, organizerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_opportunity);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        fullName = findViewById(R.id.fullName);
        phoneNumber = findViewById(R.id.phoneNumber);
        emailAddress = findViewById(R.id.emailAddress);
        motivation = findViewById(R.id.motivation);
        experience = findViewById(R.id.experience);
        applyButton = findViewById(R.id.applyButton);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Submitting application...");

        // Get opportunity ID from intent
        opportunityId = getIntent().getStringExtra("opportunityId");
        organizerId = getIntent().getStringExtra("organizerId");

        loadUserData();

        applyButton.setOnClickListener(v -> submitApplication());
    }

    /** 🔹 Load current user profile info from Firestore */
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

    /** 🔹 Submit the application */
    private void submitApplication() {
        String name = fullName.getText().toString().trim();
        String phone = phoneNumber.getText().toString().trim();
        String email = emailAddress.getText().toString().trim();
        String motivationText = motivation.getText().toString().trim();
        String experienceText = experience.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please ensure your details are filled in.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(motivationText)) {
            motivation.setError("Please write a motivation statement.");
            return;
        }

        progressDialog.show();

        String applicantId = auth.getCurrentUser().getUid();

        Map<String, Object> applicationData = new HashMap<>();
        applicationData.put("applicantId", applicantId);
        applicationData.put("organizerId", organizerId);
        applicationData.put("opportunityId", opportunityId);
        applicationData.put("fullName", name);
        applicationData.put("phone", phone);
        applicationData.put("email", email);
        applicationData.put("motivation", motivationText);
        applicationData.put("experience", experienceText);
        applicationData.put("status", "Pending");
        applicationData.put("timestamp", System.currentTimeMillis());

        firestore.collection("Applications")
                .add(applicationData)
                .addOnSuccessListener(documentReference -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Application submitted successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
