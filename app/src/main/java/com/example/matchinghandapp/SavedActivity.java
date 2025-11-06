package com.example.matchinghandapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SavedActivity extends AppCompatActivity {

    private RecyclerView recyclerSaved;
    private DiscoverAdapter adapter;
    private List<OpportunityModel> savedList;
    private FirebaseFirestore firestore;
    private ProgressDialog progressDialog;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved);

        recyclerSaved = findViewById(R.id.recyclerSaved);
        recyclerSaved.setLayoutManager(new LinearLayoutManager(this));

        firestore = FirebaseFirestore.getInstance();
        savedList = new ArrayList<>();

        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        adapter = new DiscoverAdapter(savedList, this);
        recyclerSaved.setAdapter(adapter);
        adapter.preloadBookmarks();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading saved opportunities...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        if (currentUid == null) {
            Toast.makeText(this, "Please log in to view your saved opportunities", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        loadSavedOpportunities();
    }

    /**
     * 🔹 Load all bookmarked opportunities for this user
     */
    private void loadSavedOpportunities() {
        firestore.collection("Bookmarks")
                .document(currentUid)
                .collection("Bookmarks")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    savedList.clear();

                    if (querySnapshot.isEmpty()) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "No saved opportunities found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot snap : querySnapshot.getDocuments()) {
                        String opportunityId = snap.getString("opportunityId");

                        if (opportunityId == null || opportunityId.isEmpty()) continue;

                        firestore.collection("Opportunities")
                                .document(opportunityId)
                                .get()
                                .addOnSuccessListener(opSnap -> {
                                    if (opSnap.exists()) {
                                        OpportunityModel model = opSnap.toObject(OpportunityModel.class);
                                        if (model != null) {
                                            model.setId(opSnap.getId());

                                            // Optional: fetch organizer name again if needed
                                            String organizerId = opSnap.getString("createdBy");
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

                                                            savedList.add(model);
                                                            adapter.notifyDataSetChanged();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            model.setAddress("Unknown Organizer");
                                                            savedList.add(model);
                                                            adapter.notifyDataSetChanged();
                                                        });
                                            } else {
                                                model.setAddress("Unknown Organizer");
                                                savedList.add(model);
                                                adapter.notifyDataSetChanged();
                                            }
                                        }
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Log.e("SavedActivity", "Failed to load opportunity: " + e.getMessage()));
                    }

                    progressDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error loading saved opportunities: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
