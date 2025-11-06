package com.example.matchinghandapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class OpportunityDetailsActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView title, organizer, date, duration, location, description;
    private ChipGroup chipGroup;
    private Button applyButton;
    private ImageView messageButton;
    private ProgressDialog progressDialog;

    private FirebaseFirestore firestore;
    private String opportunityId;
    private String organizerPhone;
    private String documentOrganizerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opportunity_details);

        firestore = FirebaseFirestore.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // Initialize Views
        imageView = findViewById(R.id.opImage);
        title = findViewById(R.id.opTitle);
        organizer = findViewById(R.id.opOrganizer);
        date = findViewById(R.id.opDate);
        duration = findViewById(R.id.opDuration);
        location = findViewById(R.id.opLocation);
        description = findViewById(R.id.opDescription);
        chipGroup = findViewById(R.id.opTagGroup);
        applyButton = findViewById(R.id.applyButton);
        messageButton = findViewById(R.id.messageButton);

        // Get opportunity ID
        opportunityId = getIntent().getStringExtra("opportunityId");
        if (opportunityId != null) {
            loadOpportunityDetails(opportunityId);
        } else {
            Toast.makeText(this, "Error: Missing opportunity ID", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Apply Button → open apply activity
        applyButton.setOnClickListener(v -> {
            Intent intent = new Intent(OpportunityDetailsActivity.this, ApplyOpportunityActivity.class);
            intent.putExtra("opportunityId", opportunityId);
            intent.putExtra("organizerId", documentOrganizerId);
            startActivity(intent);
        });

        // WhatsApp message button
        messageButton.setOnClickListener(v -> {
            if (organizerPhone != null && !organizerPhone.isEmpty()) {
                String message = "Hello! I’m interested in your posted opportunity on MatchingHand.";
                openWhatsAppChat(organizerPhone, message);
            } else {
                Toast.makeText(this, "Organizer’s contact not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** 🔹 Load Opportunity Details */
    private void loadOpportunityDetails(String id) {
        progressDialog.setMessage("Loading details...");
        progressDialog.show();

        DocumentReference docRef = firestore.collection("Opportunities").document(id);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                progressDialog.dismiss();
                Toast.makeText(this, "Opportunity not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String titleStr = documentSnapshot.getString("title");
            String dateStr = documentSnapshot.getString("date");
            String durationStr = documentSnapshot.getString("duration");
            String locationStr = documentSnapshot.getString("location");
            String descStr = documentSnapshot.getString("description");
            String imageUrl = documentSnapshot.getString("imageUrl");
            List<String> tags = (List<String>) documentSnapshot.get("tags");
            documentOrganizerId = documentSnapshot.getString("createdBy");

            // Populate UI
            title.setText(titleStr != null ? titleStr : "Untitled");
            date.setText(dateStr != null ? dateStr : "-");
            duration.setText(durationStr != null ? durationStr : "-");
            location.setText(locationStr != null && !locationStr.isEmpty() ? locationStr : "Not specified");
            description.setText(descStr != null ? descStr : "-");

            // Load image
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.ic_image_placeholder);
            }

            // Tags
            chipGroup.removeAllViews();
            if (tags != null) {
                for (String tag : tags) {
                    Chip chip = new Chip(this);
                    chip.setText(tag);
                    chip.setChipBackgroundColorResource(R.color.blue_100);
                    chip.setTextColor(getResources().getColor(R.color.blue_800));
                    chip.setClickable(false);
                    chipGroup.addView(chip);
                }
            }

            // Load Organizer Info and Application Status
            if (documentOrganizerId != null && !documentOrganizerId.isEmpty()) {
                loadOrganizerInfo(documentOrganizerId, true);
            } else {
                organizer.setText("Unknown Organizer");
                checkIfAlreadyApplied(true);
            }

        }).addOnFailureListener(e -> {
            if (progressDialog.isShowing()) progressDialog.dismiss();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    /** 🔹 Load Organizer Info (name + phone) */
    private void loadOrganizerInfo(String organizerId, boolean checkAfter) {
        firestore.collection("Users").document(organizerId).get()
                .addOnSuccessListener(userSnap -> {
                    if (userSnap.exists()) {
                        String fullName = userSnap.getString("fullname");
                        organizerPhone = userSnap.getString("phone");
                        organizer.setText(fullName != null ? fullName : "Unknown Organizer");
                    } else {
                        organizer.setText("Unknown Organizer");
                    }
                    checkIfAlreadyApplied(checkAfter);
                })
                .addOnFailureListener(e -> {
                    organizer.setText("Unknown Organizer");
                    checkIfAlreadyApplied(checkAfter);
                });
    }

    /** 🔹 Check if user already applied */
    private void checkIfAlreadyApplied(boolean dismissAfter) {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        firestore.collection("Applications")
                .whereEqualTo("opportunityId", opportunityId)
                .whereEqualTo("applicantId", currentUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Hide button and show badge
                        applyButton.setVisibility(View.GONE);

                        TextView appliedBadge = new TextView(this);
                        appliedBadge.setText("✅ Applied Successfully");
                        appliedBadge.setTextColor(getResources().getColor(android.R.color.white));
                        appliedBadge.setBackgroundResource(R.drawable.applied_badge_bg);
                        appliedBadge.setPadding(40, 25, 40, 25);
                        appliedBadge.setTextSize(16);
                        appliedBadge.setGravity(Gravity.CENTER);

                        ViewGroup parentLayout = (ViewGroup) applyButton.getParent();
                        parentLayout.addView(appliedBadge);
                    } else {
                        applyButton.setVisibility(View.VISIBLE);
                    }

                    if (dismissAfter && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking application: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (dismissAfter && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                });
    }

    /** 🔹 Open WhatsApp Chat */
    private void openWhatsAppChat(String phoneNumber, String message) {
        try {
            String url = "https://wa.me/" + phoneNumber.replace("+", "") + "?text=" + Uri.encode(message);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open WhatsApp", Toast.LENGTH_SHORT).show();
        }
    }
}
