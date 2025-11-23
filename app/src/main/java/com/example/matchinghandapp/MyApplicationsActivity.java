package com.example.matchinghandapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

// Displays a list of the user's applications with search, sort, and status filtering.
public class MyApplicationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MyApplicationsAdapter adapter;
    private EditText etSearch;
    private ImageView ivSort;
    private Spinner spinnerStatus;
    private ProgressDialog progressDialog;
    private List<ApplicationModel> applicationList; // Complete list of applications.
    private List<ApplicationModel> filteredList; // List displayed to the user.
    private FirebaseFirestore firestore;
    private String currentUid;
    private int currentSortMode = 0; // 0=default, 1=A-Z, 2=date
    private String currentStatusFilter = "All"; // All, Pending, Approved, Rejected

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_applications);

        // Initialize UI components.
        recyclerView = findViewById(R.id.applicationsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        etSearch = findViewById(R.id.etSearch);
        ivSort = findViewById(R.id.ivSort);
        spinnerStatus = findViewById(R.id.spinnerStatus);

        applicationList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new MyApplicationsAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading your applications...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Set up UI functionality.
        setupStatusSpinner();
        setupSearchBar();
        loadApplications();
    }

    // Sets up the dropdown for filtering by application status.
    private void setupStatusSpinner() {
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"All", "Pending", "Approved", "Rejected"});
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentStatusFilter = parent.getItemAtPosition(position).toString();
                applyFilters(); // Re-apply filters when status selection changes.
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // Loads all applications for the current user from Firestore.
    private void loadApplications() {
        firestore.collection("Applications")
                .whereEqualTo("applicantId", currentUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    applicationList.clear();
                    filteredList.clear();

                    if (querySnapshot.isEmpty()) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "No applications found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final int totalApplications = querySnapshot.size();
                    final int[] loadedCount = {0};

                    for (DocumentSnapshot appDoc : querySnapshot.getDocuments()) {
                        String status = appDoc.getString("status");
                        String opportunityId = appDoc.getString("opportunityId");

                        if (opportunityId != null) {
                            // Fetch details for each associated opportunity.
                            firestore.collection("Opportunities")
                                    .document(opportunityId)
                                    .get()
                                    .addOnSuccessListener(opDoc -> {
                                        if (opDoc.exists()) {
                                            ApplicationModel model = new ApplicationModel();
                                            model.setOpportunityId(opportunityId);
                                            model.setStatus(status != null ? status : "Pending");
                                            model.setTitle(opDoc.getString("title"));
                                            model.setLocation(opDoc.getString("location"));
                                            model.setDate(opDoc.getString("date"));
                                            model.setImageUrl(opDoc.getString("imageUrl"));

                                            applicationList.add(model);
                                        }
                                        loadedCount[0]++;
                                        if (loadedCount[0] == totalApplications) {
                                            progressDialog.dismiss();
                                            applyFilters();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("MyApplications", "Failed to load opportunity: " + e.getMessage());
                                        loadedCount[0]++;
                                        if (loadedCount[0] == totalApplications) {
                                            progressDialog.dismiss();
                                            applyFilters();
                                        }
                                    });
                        } else {
                            loadedCount[0]++;
                            if (loadedCount[0] == totalApplications) {
                                progressDialog.dismiss();
                                applyFilters();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error loading applications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Sets up the search bar and sort functionality.
    private void setupSearchBar() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ivSort.setOnClickListener(v -> {
            currentSortMode = (currentSortMode + 1) % 3; // Now 3 sort modes.
            sortApplications();

            String message;
            switch (currentSortMode) {
                case 1:
                    message = "Sorted A-Z";
                    break;
                case 2:
                    message = "Sorted by Date";
                    break;
                default:
                    message = "Default sort";
                    break;
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    // Applies the current search and status filters to the list.
    private void applyFilters() {
        filteredList.clear();
        String searchQuery = etSearch.getText().toString().toLowerCase().trim();

        for (ApplicationModel application : applicationList) {
            // Check if the application matches the current status filter.
            boolean matchesStatus = currentStatusFilter.equals("All") ||
                    (application.getStatus() != null &&
                            application.getStatus().equalsIgnoreCase(currentStatusFilter));

            if (!matchesStatus) continue;

            // If it matches the status, check if it matches the search query.
            if (searchQuery.isEmpty()) {
                filteredList.add(application);
            } else {
                boolean matchesTitle = application.getTitle() != null &&
                        application.getTitle().toLowerCase().contains(searchQuery);
                boolean matchesLocation = application.getLocation() != null &&
                        application.getLocation().toLowerCase().contains(searchQuery);
                boolean matchesStatusText = application.getStatus() != null &&
                        application.getStatus().toLowerCase().contains(searchQuery);

                if (matchesTitle || matchesLocation || matchesStatusText) {
                    filteredList.add(application);
                }
            }
        }

        // After filtering, apply sorting if needed.
        if (currentSortMode != 0) {
            sortApplications();
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    // Sorts the displayed list of applications.
    private void sortApplications() {
        if (currentSortMode == 1) {
            // Sort alphabetically by title.
            filteredList.sort((a1, a2) -> {
                String title1 = a1.getTitle() != null ? a1.getTitle() : "";
                String title2 = a2.getTitle() != null ? a2.getTitle() : "";
                return title1.compareToIgnoreCase(title2);
            });
        } else if (currentSortMode == 2) {
            // Sort by date.
            filteredList.sort((a1, a2) -> {
                String date1 = a1.getDate() != null ? a1.getDate() : "";
                String date2 = a2.getDate() != null ? a2.getDate() : "";
                return date1.compareTo(date2);
            });
        } else {
            // Default sort - re-apply filters to restore original order.
            applyFilters();
            return;
        }

        adapter.notifyDataSetChanged();
    }
}
