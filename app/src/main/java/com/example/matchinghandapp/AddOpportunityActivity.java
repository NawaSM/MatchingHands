package com.example.matchinghandapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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

// This activity allows NGOs to add a new volunteering opportunity or edit an existing one.
public class AddOpportunityActivity extends AppCompatActivity {

    // UI elements for opportunity details form.
    private EditText opTitle, opDate, opTime, opAddress, opDuration, opDescription;
    private ImageView previewImage;
    private Button uploadImageBtn, saveBtn, selectTagsBtn;
    private ChipGroup selectedTagsChipGroup;
    private Uri imageUri; // URI of the selected image.
    private TextView label; // Label for the activity (e.g., "Add Opportunity").

    // Firebase services.
    private FirebaseFirestore firestore;
    private StorageReference storageRef;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog; // Shows loading progress.

    private String editId = null; // ID of the opportunity being edited.
    private String existingImageUrl = null; // URL of image for existing opportunity.
    private List<String> selectedTags = new ArrayList<>(); // List of tags for the opportunity.

    // Called when the activity is first created.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_opportunity);

        // Initialize Firebase services.
        firestore = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("opportunity_images");
        mAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);

        // Initialize Views.
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
        selectTagsBtn = findViewById(R.id.selectTagsBtn);
        selectedTagsChipGroup = findViewById(R.id.selectedTagsChipGroup);

        // Set click listeners for various buttons and fields.
        opDate.setOnClickListener(v -> showDatePicker());
        opTime.setOnClickListener(v -> showTimePicker());
        uploadImageBtn.setOnClickListener(v -> openImagePicker());
        selectTagsBtn.setOnClickListener(v -> showTagSelectionDialog());
        saveBtn.setOnClickListener(v -> saveOrUpdateOpportunity());

        // Check if the activity was started for editing an existing opportunity.
        editId = getIntent().getStringExtra("opportunityId");
        if (editId != null && !editId.isEmpty()) {
            loadOpportunityData(editId); // Load existing data.
            saveBtn.setText("Update Opportunity");
        }
    }

    // Shows a dialog for selecting tags for the opportunity.
    private void showTagSelectionDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_tags, null);
        ChipGroup dialogChipGroup = dialogView.findViewById(R.id.dialogTagChipGroup);

        // Populate the dialog with all available tags.
        for (String tag : Tags.ALL_TAGS) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setChecked(selectedTags.contains(tag)); // Pre-check currently selected tags.
            dialogChipGroup.addView(chip);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Handle cancel button click.
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        // Handle done button click, updating the list of selected tags.
        dialogView.findViewById(R.id.btnDone).setOnClickListener(v -> {
            selectedTags.clear();
            for (int i = 0; i < dialogChipGroup.getChildCount(); i++) {
                Chip chip = (Chip) dialogChipGroup.getChildAt(i);
                if (chip.isChecked()) {
                    selectedTags.add(chip.getText().toString());
                }
            }
            updateSelectedTagsDisplay(); // Refresh the UI.
            dialog.dismiss();
        });

        dialog.show();
    }

    // Updates the UI to show the currently selected tags.
    private void updateSelectedTagsDisplay() {
        selectedTagsChipGroup.removeAllViews(); // Clear existing chips.

        if (selectedTags.isEmpty()) {
            selectTagsBtn.setText("Select Tags");
            return;
        }

        selectTagsBtn.setText(selectedTags.size() + " tag(s) selected");

        // Add a chip for each selected tag.
        for (String tag : selectedTags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCloseIconVisible(true);
            // Allow removing a tag by clicking the close icon.
            chip.setOnCloseIconClickListener(v -> {
                selectedTags.remove(tag);
                updateSelectedTagsDisplay(); // Refresh the UI.
            });
            selectedTagsChipGroup.addView(chip);
        }
    }

    // Shows a DatePickerDialog to select a date.
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

    // Shows a TimePickerDialog to select a time.
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(this, (view, h, m) ->
                opTime.setText(String.format("%02d:%02d", h, m)), hour, minute, true);
        dialog.show();
    }

    // Opens an image picker to select an image from the device.
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }

    // Handles the result from the image picker.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            previewImage.setImageURI(imageUri); // Show selected image.
        }
    }

    // Loads the data of an existing opportunity from Firestore.
    private void loadOpportunityData(String id) {
        progressDialog.setMessage("Loading opportunity...");
        progressDialog.show();

        DocumentReference docRef = firestore.collection("Opportunities").document(id);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            progressDialog.dismiss();
            if (documentSnapshot.exists()) {
                // Populate the form fields with data from Firestore.
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

                // Load the existing image using Glide.
                Glide.with(this)
                        .load(existingImageUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(previewImage);

                // Load existing tags.
                List<String> tags = (List<String>) documentSnapshot.get("tags");
                if (tags != null) {
                    selectedTags = new ArrayList<>(tags);
                    updateSelectedTagsDisplay();
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

    // Saves a new opportunity or updates an existing one.
    private void saveOrUpdateOpportunity() {
        // Get data from input fields.
        String title = opTitle.getText().toString().trim();
        String date = opDate.getText().toString().trim();
        String time = opTime.getText().toString().trim();
        String address = opAddress.getText().toString().trim();
        String duration = opDuration.getText().toString().trim();
        String description = opDescription.getText().toString().trim();

        // Validate input.
        if (title.isEmpty() || date.isEmpty() || time.isEmpty() || address.isEmpty() || duration.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTags.isEmpty()) {
            Toast.makeText(this, "Please select at least one tag", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage(editId == null ? "Saving..." : "Updating...");
        progressDialog.show();

        // If a new image was selected, upload it to Firebase Storage.
        if (imageUri != null) {
            StorageReference fileRef = storageRef.child(System.currentTimeMillis() + ".jpg");
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri ->
                            // After upload, save data to Firestore.
                            uploadToFirestore(uri.toString(), title, date, time, address, duration, description, selectedTags)))
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // If no new image, save data directly to Firestore with the existing image URL.
            uploadToFirestore(existingImageUrl, title, date, time, address, duration, description, selectedTags);
        }
    }

    // Uploads the opportunity data to Firestore.
    private void uploadToFirestore(String imageUrl, String title, String date, String time,
                                   String address, String duration, String description, List<String> tags) {

        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "unknown";

        // Create a map to hold the opportunity data.
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("date", date);
        data.put("time", time);
        data.put("location", address);
        data.put("duration", duration);
        data.put("description", description);
        data.put("tags", tags);
        data.put("imageUrl", imageUrl);
        data.put("createdBy", uid);
        data.put("timestamp", System.currentTimeMillis());

        // If it's a new opportunity, add a new document.
        if (editId == null) {
            firestore.collection("Opportunities").add(data)
                    .addOnSuccessListener(doc -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Opportunity added successfully!", Toast.LENGTH_SHORT).show();
                        finish(); // Close the activity.
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Otherwise, update the existing document.
            firestore.collection("Opportunities").document(editId).update(data)
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Opportunity updated successfully!", Toast.LENGTH_SHORT).show();
                        finish(); // Close the activity.
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
