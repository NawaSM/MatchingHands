package com.example.matchinghandapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddOpportunityActivity extends AppCompatActivity {

    private EditText opTitle, opDate, opTime, opAddress, opDuration, opDescription;
    private ImageView previewImage;
    private Button uploadImageBtn, saveBtn;
    private ChipGroup tagChipGroup;
    private Uri imageUri;
    private TextView label;

    private FirebaseFirestore firestore;
    private StorageReference storageRef;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    private String editId = null;
    private String existingImageUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_opportunity);

        firestore = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("opportunity_images");
        mAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);

        // Initialize Views
        opTitle = findViewById(R.id.opTitle);
        opDate = findViewById(R.id.opDate);
        label = findViewById(R.id.label);
        opTime = findViewById(R.id.opTime);
        opAddress = findViewById(R.id.opAddress);
        opDuration = findViewById(R.id.opDuration);
        opDescription = findViewById(R.id.opDescription);
        previewImage = findViewById(R.id.previewImage);
        uploadImageBtn = findViewById(R.id.uploadImageBtn);
        saveBtn = findViewById(R.id.saveBtn);
        tagChipGroup = findViewById(R.id.tagChipGroup);

        // Set click listeners for date & time pickers
        opDate.setOnClickListener(v -> showDatePicker());
        opTime.setOnClickListener(v -> showTimePicker());

        // Check if editing
        editId = getIntent().getStringExtra("opportunityId");
        if (editId != null && !editId.isEmpty()) {
            loadOpportunityData(editId);
            saveBtn.setText("Update Opportunity");
        }

        uploadImageBtn.setOnClickListener(v -> openImagePicker());
        saveBtn.setOnClickListener(v -> saveOrUpdateOpportunity());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) ->
                opDate.setText(String.format("%02d/%02d/%d", d, (m + 1), y)),
                year, month, day);
        dialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(this, (view, h, m) ->
                opTime.setText(String.format("%02d:%02d", h, m)), hour, minute, true);
        dialog.show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            previewImage.setImageURI(imageUri);
        }
    }

    private void loadOpportunityData(String id) {
        progressDialog.setMessage("Loading opportunity...");
        progressDialog.show();

        DocumentReference docRef = firestore.collection("Opportunities").document(id);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            progressDialog.dismiss();
            if (documentSnapshot.exists()) {
                String title = documentSnapshot.getString("title");
                String date = documentSnapshot.getString("date");
                String time = documentSnapshot.getString("time");
                String address = documentSnapshot.getString("location");
                String duration = documentSnapshot.getString("duration");
                String description = documentSnapshot.getString("description");
                existingImageUrl = documentSnapshot.getString("imageUrl");

                label.setText("Update Opportunity");
                opTitle.setText(title);
                opDate.setText(date);
                opTime.setText(time);
                opAddress.setText(address);
                opDuration.setText(duration);
                opDescription.setText(description);

                Glide.with(this)
                        .load(existingImageUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(previewImage);

                List<String> tags = (List<String>) documentSnapshot.get("tags");
                if (tags != null) {
                    highlightSelectedTags(tags);
                }
            } else {
                Toast.makeText(this, "Opportunity not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void highlightSelectedTags(List<String> savedTags) {
        for (int i = 0; i < tagChipGroup.getChildCount(); i++) {
            if (tagChipGroup.getChildAt(i) instanceof Chip) {
                Chip chip = (Chip) tagChipGroup.getChildAt(i);
                if (savedTags.contains(chip.getText().toString())) {
                    chip.setChecked(true);
                    chip.setChipBackgroundColorResource(R.color.teal_200);
                }
            }
        }
    }

    private void saveOrUpdateOpportunity() {
        String title = opTitle.getText().toString().trim();
        String date = opDate.getText().toString().trim();
        String time = opTime.getText().toString().trim();
        String address = opAddress.getText().toString().trim();
        String duration = opDuration.getText().toString().trim();
        String description = opDescription.getText().toString().trim();
        List<String> selectedTags = getSelectedTags();

        if (title.isEmpty() || date.isEmpty() || time.isEmpty() || address.isEmpty() || duration.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage(editId == null ? "Saving..." : "Updating...");
        progressDialog.show();

        if (imageUri != null) {
            StorageReference fileRef = storageRef.child(System.currentTimeMillis() + ".jpg");
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri ->
                            uploadToFirestore(uri.toString(), title, date, time, address, duration, description, selectedTags)))
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            uploadToFirestore(existingImageUrl, title, date, time, address, duration, description, selectedTags);
        }
    }

    private List<String> getSelectedTags() {
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < tagChipGroup.getChildCount(); i++) {
            if (tagChipGroup.getChildAt(i) instanceof Chip) {
                Chip chip = (Chip) tagChipGroup.getChildAt(i);
                if (chip.isChecked()) tags.add(chip.getText().toString());
            }
        }
        return tags;
    }

    /** ✅ Firestore Upload: Saves UID of logged-in user **/
    private void uploadToFirestore(String imageUrl, String title, String date, String time,
                                   String address, String duration, String description, List<String> tags) {

        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "unknown";

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("date", date);
        data.put("time", time);
        data.put("location", address);
        data.put("duration", duration);
        data.put("description", description);
        data.put("tags", tags);
        data.put("imageUrl", imageUrl);
        data.put("createdBy", uid); // ✅ Saves UID of creator/updater
        data.put("timestamp", System.currentTimeMillis());

        if (editId == null) {
            firestore.collection("Opportunities").add(data)
                    .addOnSuccessListener(doc -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Opportunity added successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            firestore.collection("Opportunities").document(editId).update(data)
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Opportunity updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
