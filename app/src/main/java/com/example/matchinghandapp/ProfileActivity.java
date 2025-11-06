package com.example.matchinghandapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private EditText fullName, email, phone, interest, experience, skills, languages;
    private Button btnUpdate;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private ProgressDialog progressDialog;
    private DocumentReference userRef;
    private StorageReference storageRef;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("ProfileImages");
        String userId = mAuth.getCurrentUser().getUid();
        userRef = firestore.collection("Users").document(userId);

        profileImage = findViewById(R.id.ivProfileImage);
        fullName = findViewById(R.id.etFullName);
        email = findViewById(R.id.etEmail);
        phone = findViewById(R.id.etPhone);
        interest = findViewById(R.id.etInterest);
        experience = findViewById(R.id.etExperience);
        skills = findViewById(R.id.etSkills);
        languages = findViewById(R.id.etLanguages);
        btnUpdate = findViewById(R.id.btnUpdate);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        loadProfile();

        profileImage.setOnClickListener(v -> pickImageFromGallery());
        btnUpdate.setOnClickListener(v -> updateProfile());
    }

    private void loadProfile() {
        progressDialog.setMessage("Loading profile...");
        progressDialog.show();

        userRef.get().addOnSuccessListener(snapshot -> {
            progressDialog.dismiss();
            if (snapshot.exists()) {
                fullName.setText(snapshot.getString("fullname"));
                email.setText(snapshot.getString("email"));
                phone.setText(snapshot.getString("phone"));
                interest.setText(snapshot.getString("interest"));
                experience.setText(snapshot.getString("experience"));
                skills.setText(snapshot.getString("skills"));
                languages.setText(snapshot.getString("languages"));

                String imageUrl = snapshot.getString("profileImageUrl");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(this).load(imageUrl).into(profileImage);
                }
            } else {
                Toast.makeText(this, "No user profile found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void pickImageFromGallery() {
        // Simple image chooser
        android.content.Intent intent = new android.content.Intent();
        intent.setType("image/*");
        intent.setAction(android.content.Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
        }
    }

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

        Map<String, Object> map = new HashMap<>();
        map.put("fullName", name);
        map.put("phone", phoneNo);
        map.put("interest", interest.getText().toString());
        map.put("experience", experience.getText().toString());
        map.put("skills", skills.getText().toString());
        map.put("languages", languages.getText().toString());

        if (imageUri != null) {
            StorageReference ref = storageRef.child(mAuth.getCurrentUser().getUid() + ".jpg");
            ref.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        map.put("profileImageUrl", uri.toString());
                        saveProfile(map);
                    }))
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            saveProfile(map);
        }
    }

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
