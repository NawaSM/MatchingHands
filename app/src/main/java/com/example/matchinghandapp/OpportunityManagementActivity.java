package com.example.matchinghandapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

// Manages NGO's opportunities with search and sort functionality.
public class OpportunityManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ImageButton addButton;
    private EditText etSearch; // For searching opportunities.
    private ImageView ivSort; // For sorting opportunities.
    private ProgressDialog progressDialog;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private CollectionReference oppRef;
    private List<OpportunityModel> opportunityList; // Complete list from Firestore.
    private List<OpportunityModel> filteredList; // The filtered list that is displayed to the user.
    private OpportunityAdapter adapter;
    private int currentSortMode = 0; // Tracks the current sort mode: 0=default, 1=A-Z, 2=date.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opportunity_management);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        oppRef = firestore.collection("Opportunities");

        // Initialize UI components.
        recyclerView = findViewById(R.id.recyclerOpportunities);
        addButton = findViewById(R.id.addOpportunityBtn);
        etSearch = findViewById(R.id.etSearch);
        ivSort = findViewById(R.id.ivSort);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        opportunityList = new ArrayList<>();
        filteredList = new ArrayList<>();

        // Set up adapter with listeners for edit and delete actions.
        adapter = new OpportunityAdapter(filteredList, new OpportunityAdapter.OnItemClickListener() {
            @Override
            public void onEdit(OpportunityModel model) {
                Intent editIntent = new Intent(OpportunityManagementActivity.this, AddOpportunityActivity.class);
                editIntent.putExtra("opportunityId", model.getId());
                startActivity(editIntent);
            }

            @Override
            public void onDelete(OpportunityModel model) {
                confirmDelete(model);
            }
        });

        recyclerView.setAdapter(adapter);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // Set listener to open the 'Add Opportunity' screen.
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddOpportunityActivity.class);
            startActivity(intent);
        });

        setupSearchBar(); // Set up listeners for search and sort UI.
        loadOpportunities(); // Load initial data from Firestore.
    }

    // Fetches and displays opportunities created by the current user.
    private void loadOpportunities() {
        progressDialog.setMessage("Loading your opportunities...");
        progressDialog.show();

        String currentUserId = mAuth.getCurrentUser().getUid();

        oppRef.whereEqualTo("createdBy", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        progressDialog.dismiss();
                        Toast.makeText(OpportunityManagementActivity.this,
                                "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    opportunityList.clear();

                    if (value != null && !value.isEmpty()) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            OpportunityModel model = doc.toObject(OpportunityModel.class);
                            if (model != null) {
                                model.setId(doc.getId());
                                opportunityList.add(model);
                            }
                        }
                    }

                    // Apply any existing search or sort to the newly loaded data.
                    filterOpportunities(etSearch.getText().toString());
                    progressDialog.dismiss();

                    if (opportunityList.isEmpty()) {
                        Toast.makeText(OpportunityManagementActivity.this,
                                "You haven't created any opportunities yet.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Sets up listeners for the search bar and sort button.
    private void setupSearchBar() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterOpportunities(s.toString()); // Re-filter list on text change.
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ivSort.setOnClickListener(v -> {
            currentSortMode = (currentSortMode + 1) % 3; // Cycle through sort modes.
            sortOpportunities(); // Re-sort the currently filtered list.

            String message = currentSortMode == 0 ? "Default sort" :
                    currentSortMode == 1 ? "Sorted A-Z" : "Sorted by Date";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    // Sorts the displayed list of opportunities based on the current mode.
    private void sortOpportunities() {
        if (currentSortMode == 1) { // Alphabetical (A-Z) sort by title.
            filteredList.sort((o1, o2) -> {
                String title1 = o1.getTitle() != null ? o1.getTitle() : "";
                String title2 = o2.getTitle() != null ? o2.getTitle() : "";
                return title1.compareToIgnoreCase(title2);
            });
        } else if (currentSortMode == 2) { // Sort by date.
            filteredList.sort((o1, o2) -> {
                String date1 = o1.getDate() != null ? o1.getDate() : "";
                String date2 = o2.getDate() != null ? o2.getDate() : "";
                return date1.compareTo(date2);
            });
        } else {
            // Default sort (mode 0) re-applies the filter to restore original order.
            filterOpportunities(etSearch.getText().toString());
            return;
        }

        adapter.notifyDataSetChanged();
    }

    // Filters the main opportunity list into the displayed list based on the search query.
    private void filterOpportunities(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(opportunityList); // If query is empty, show all items.
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();

            for (OpportunityModel opportunity : opportunityList) {
                // Check if title, description, location, or tags match the query.
                boolean matchesTitle = opportunity.getTitle() != null &&
                        opportunity.getTitle().toLowerCase().contains(lowerCaseQuery);
                boolean matchesDescription = opportunity.getDescription() != null &&
                        opportunity.getDescription().toLowerCase().contains(lowerCaseQuery);
                boolean matchesLocation = opportunity.getLocation() != null &&
                        opportunity.getLocation().toLowerCase().contains(lowerCaseQuery);

                boolean matchesTags = false;
                if (opportunity.getTags() != null) {
                    for (String tag : opportunity.getTags()) {
                        if (tag != null && tag.toLowerCase().contains(lowerCaseQuery)) {
                            matchesTags = true;
                            break;
                        }
                    }
                }

                if (matchesTitle || matchesDescription || matchesLocation || matchesTags) {
                    filteredList.add(opportunity);
                }
            }
        }

        // After filtering, ensure the list is sorted according to the current mode.
        if(currentSortMode != 0) {
            sortOpportunities();
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    // Shows a confirmation dialog before deleting an opportunity.
    private void confirmDelete(OpportunityModel model) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Opportunity")
                .setMessage("Are you sure you want to delete this opportunity?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    oppRef.document(model.getId()).delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
