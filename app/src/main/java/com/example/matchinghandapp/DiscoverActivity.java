package com.example.matchinghandapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.example.matchinghandapp.utils.GeminiMatchCalculator;
import com.example.matchinghandapp.utils.UserProfileCache;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class DiscoverActivity extends AppCompatActivity {

    private RecyclerView recyclerDiscover;
    private DiscoverAdapter adapter;
    private List<OpportunityModel> opportunityList;
    private FirebaseFirestore firestore;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);

        recyclerDiscover = findViewById(R.id.recyclerDiscover);
        recyclerDiscover.setLayoutManager(new LinearLayoutManager(this));

        firestore = FirebaseFirestore.getInstance();
        opportunityList = new ArrayList<>();
        adapter = new DiscoverAdapter(opportunityList, this);
        recyclerDiscover.setAdapter(adapter);
        adapter.preloadBookmarks();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading opportunities...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        loadOpportunities();
    }

    /**
     * 🔹 Load all opportunities and calculate Gemini match
     */
    private void loadOpportunities() {
        progressDialog.show();

        firestore.collection("Opportunities")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    opportunityList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "No opportunities found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        OpportunityModel model = snapshot.toObject(OpportunityModel.class);
                        if (model != null) {
                            model.setId(snapshot.getId());

                            String organizerId = snapshot.getString("createdBy");
                            if (organizerId != null && !organizerId.trim().isEmpty()) {
                                firestore.collection("Users")
                                        .document(organizerId)
                                        .get()
                                        .addOnSuccessListener(userSnap -> {
                                            if (userSnap.exists()) {
                                                String fullName = userSnap.getString("fullname");
                                                model.setAddress(fullName != null ? fullName : "Unknown Organizer");
                                            } else {
                                                model.setAddress("Unknown Organizer");
                                            }

                                            // ✅ After setting organizer name → calculate Gemini match
                                            calculateGeminiMatch(model);
                                        })
                                        .addOnFailureListener(e -> {
                                            model.setAddress("Unknown Organizer");
                                            calculateGeminiMatch(model);
                                        });
                            } else {
                                model.setAddress("Unknown Organizer");
                                calculateGeminiMatch(model);
                            }
                        }
                    }

                    progressDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 🔹 Calculate Gemini match asynchronously
     */
    private void calculateGeminiMatch(OpportunityModel model) {
        String userProfile = UserProfileCache.getCurrentUserProfile(this);
        String opportunityInfo = "Title: " + model.getTitle() +
                "\nDescription: " + model.getDescription() +
                "\nTags: " + model.getTags();

        GeminiMatchCalculator.calculateMatch(userProfile, opportunityInfo)
                .thenAccept(match -> new Handler(Looper.getMainLooper()).post(() -> {
                    model.setMatchPercentage(match + "% Match");
                    opportunityList.add(model);
                    adapter.notifyDataSetChanged();
                }))
                .exceptionally(e -> {
                    model.setMatchPercentage("N/A");
                    new Handler(Looper.getMainLooper()).post(() -> {
                        opportunityList.add(model);
                        adapter.notifyDataSetChanged();
                    });
                    return null;
                });
    }
}
