package com.example.matchinghandapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Displays bookmarked opportunities with search and sort functionality.
public class SavedActivity extends AppCompatActivity {

    private RecyclerView recyclerSaved;
    private DiscoverAdapter adapter;
    private EditText etSearch;
    private ImageView ivSort;
    private ProgressDialog progressDialog;
    private List<OpportunityModel> savedList;
    private List<OpportunityModel> filteredList;
    private FirebaseFirestore firestore;
    private String currentUid;
    private int currentSortMode = 0; // 0=default, 1=A-Z, 2=date

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved);

        // Initialize UI components.
        recyclerSaved = findViewById(R.id.recyclerSaved);
        recyclerSaved.setLayoutManager(new LinearLayoutManager(this));
        etSearch = findViewById(R.id.etSearch);
        ivSort = findViewById(R.id.ivSort);

        // Initialize Firebase services and data lists.
        firestore = FirebaseFirestore.getInstance();
        savedList = new ArrayList<>();
        filteredList = new ArrayList<>();
        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        adapter = new DiscoverAdapter(filteredList, this);
        recyclerSaved.setAdapter(adapter);
        adapter.preloadBookmarks();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading saved opportunities...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Check if user is logged in.
        if (currentUid == null) {
            Toast.makeText(this, "Please log in to view your saved opportunities", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        setupSearchBar();
        loadSavedOpportunities();
    }

    // Loads saved opportunities from Firestore.
    private void loadSavedOpportunities() {
        firestore.collection("Bookmarks")
                .document(currentUid)
                .collection("Bookmarks")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    savedList.clear();
                    filteredList.clear();

                    if (querySnapshot.isEmpty()) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "No saved opportunities found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final int totalBookmarks = querySnapshot.size();
                    final int[] loadedCount = {0};

                    for (DocumentSnapshot snap : querySnapshot.getDocuments()) {
                        String opportunityId = snap.getString("opportunityId");

                        if (opportunityId == null || opportunityId.isEmpty()) {
                            loadedCount[0]++;
                            if (loadedCount[0] == totalBookmarks) {
                                progressDialog.dismiss();
                                filteredList.addAll(savedList);
                                adapter.notifyDataSetChanged();
                            }
                            continue;
                        }

                        // Fetch opportunity details for each saved bookmark.
                        firestore.collection("Opportunities")
                                .document(opportunityId)
                                .get()
                                .addOnSuccessListener(opSnap -> {
                                    if (opSnap.exists()) {
                                        OpportunityModel model = opSnap.toObject(OpportunityModel.class);
                                        if (model != null) {
                                            model.setId(opSnap.getId());

                                            // Set the match percentage from the saved data.
                                            Map<String, Object> userMatches = (Map<String, Object>) opSnap.get("userMatches");
                                            if (userMatches != null && userMatches.containsKey(currentUid)) {
                                                Object matchObj = userMatches.get(currentUid);
                                                if (matchObj instanceof Long) {
                                                    Long matchValue = (Long) matchObj;
                                                    model.setMatchPercentage(matchValue + "% Match");
                                                } else {
                                                    model.setMatchPercentage("N/A");
                                                }
                                            } else {
                                                model.setMatchPercentage("N/A");
                                            }

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
                                                            loadedCount[0]++;

                                                            if (loadedCount[0] == totalBookmarks) {
                                                                progressDialog.dismiss();
                                                                filteredList.addAll(savedList);
                                                                adapter.notifyDataSetChanged();
                                                            }
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            model.setAddress("Unknown Organizer");
                                                            savedList.add(model);
                                                            loadedCount[0]++;

                                                            if (loadedCount[0] == totalBookmarks) {
                                                                progressDialog.dismiss();
                                                                filteredList.addAll(savedList);
                                                                adapter.notifyDataSetChanged();
                                                            }
                                                        });
                                            } else {
                                                model.setAddress("Unknown Organizer");
                                                savedList.add(model);
                                                loadedCount[0]++;

                                                if (loadedCount[0] == totalBookmarks) {
                                                    progressDialog.dismiss();
                                                    filteredList.addAll(savedList);
                                                    adapter.notifyDataSetChanged();
                                                }
                                            }
                                        }
                                    } else {
                                        loadedCount[0]++;
                                        if (loadedCount[0] == totalBookmarks) {
                                            progressDialog.dismiss();
                                            filteredList.addAll(savedList);
                                            adapter.notifyDataSetChanged();
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("SavedActivity", "Failed to load opportunity: " + e.getMessage());
                                    loadedCount[0]++;
                                    if (loadedCount[0] == totalBookmarks) {
                                        progressDialog.dismiss();
                                        filteredList.addAll(savedList);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error loading saved opportunities: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Sets up the search bar and sort functionality.
    private void setupSearchBar() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterOpportunities(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ivSort.setOnClickListener(v -> {
            currentSortMode = (currentSortMode + 1) % 3;
            sortOpportunities();

            String message = currentSortMode == 0 ? "Default sort" :
                    currentSortMode == 1 ? "Sorted A-Z" : "Sorted by Date";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    // Sorts the displayed opportunities.
    private void sortOpportunities() {
        if (currentSortMode == 1) {
            filteredList.sort((o1, o2) -> {
                String title1 = o1.getTitle() != null ? o1.getTitle() : "";
                String title2 = o2.getTitle() != null ? o2.getTitle() : "";
                return title1.compareToIgnoreCase(title2);
            });
        } else if (currentSortMode == 2) {
            filteredList.sort((o1, o2) -> {
                String date1 = o1.getDate() != null ? o1.getDate() : "";
                String date2 = o2.getDate() != null ? o2.getDate() : "";
                return date1.compareTo(date2);
            });
        } else {
            filteredList.clear();
            String currentQuery = etSearch.getText().toString();
            if (currentQuery.isEmpty()) {
                filteredList.addAll(savedList);
            } else {
                filterOpportunities(currentQuery);
                return;
            }
        }

        adapter.notifyDataSetChanged();
    }

    // Filters the displayed opportunities based on a search query.
    private void filterOpportunities(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(savedList);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();

            for (OpportunityModel opportunity : savedList) {
                boolean matchesTitle = opportunity.getTitle() != null &&
                        opportunity.getTitle().toLowerCase().contains(lowerCaseQuery);
                boolean matchesDescription = opportunity.getDescription() != null &&
                        opportunity.getDescription().toLowerCase().contains(lowerCaseQuery);

                boolean matchesTags = false;
                if (opportunity.getTags() != null) {
                    for (String tag : opportunity.getTags()) {
                        if (tag != null && tag.toLowerCase().contains(lowerCaseQuery)) {
                            matchesTags = true;
                            break;
                        }
                    }
                }

                boolean matchesOrganizer = opportunity.getAddress() != null &&
                        opportunity.getAddress().toLowerCase().contains(lowerCaseQuery);

                if (matchesTitle || matchesDescription || matchesTags || matchesOrganizer) {
                    filteredList.add(opportunity);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }
}
