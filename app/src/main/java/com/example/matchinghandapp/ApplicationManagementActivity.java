package com.example.matchinghandapp;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

// This activity allows NGOs to manage incoming applications for their opportunities.
public class ApplicationManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ApplicationManagementAdapter adapter;
    private EditText etSearch; // For searching applications.
    private ImageView ivSort; // For sorting applications.
    private Spinner spinnerStatus; // For filtering applications by status.
    private ProgressDialog progressDialog;
    private List<ApplicationModel> applicationList; // Complete list of applications.
    private List<ApplicationModel> filteredList; // The filtered list that is displayed to the user.
    private FirebaseFirestore firestore;
    private String currentOrganizerId;
    private int currentSortMode = 0; // Tracks the current sort mode: 0=default, 1=A-Z, 2=date.
    private String currentStatusFilter = "All"; // Tracks the current status filter.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application_management);

        // Initialize UI components.
        recyclerView = findViewById(R.id.applicationsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        etSearch = findViewById(R.id.etSearch);
        ivSort = findViewById(R.id.ivSort);
        spinnerStatus = findViewById(R.id.spinnerStatus);

        applicationList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new ApplicationManagementAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();
        currentOrganizerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading volunteer applications...");
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

    // Loads all applications for the current NGO from Firestore.
    private void loadApplications() {
        firestore.collection("Applications")
                .whereEqualTo("organizerId", currentOrganizerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    applicationList.clear();

                    if (querySnapshot.isEmpty()) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "No volunteer applications found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final int totalApplications = querySnapshot.size();
                    final int[] loadedCount = {0};

                    for (DocumentSnapshot appDoc : querySnapshot.getDocuments()) {
                        ApplicationModel model = new ApplicationModel();

                        // Set application details from the document.
                        model.setDocId(appDoc.getId());
                        model.setApplicantId(appDoc.getString("applicantId"));
                        model.setOrganizerId(appDoc.getString("organizerId"));
                        model.setOpportunityId(appDoc.getString("opportunityId"));
                        model.setStatus(appDoc.getString("status"));
                        model.setTitle(appDoc.getString("fullName"));
                        model.setLocation(appDoc.getString("email"));
                        model.setPhone(appDoc.getString("phone"));
                        model.setMotivation(appDoc.getString("motivation"));
                        model.setExperience(appDoc.getString("experience"));

                        // Fetch details for each associated opportunity.
                        firestore.collection("Opportunities")
                                .document(model.getOpportunityId())
                                .get()
                                .addOnSuccessListener(opDoc -> {
                                    if (opDoc.exists()) {
                                        model.setDate(opDoc.getString("date"));
                                        model.setImageUrl(opDoc.getString("imageUrl"));
                                        model.setOpportunityTitle(opDoc.getString("title"));
                                    }

                                    applicationList.add(model);
                                    loadedCount[0]++;

                                    // Apply filters once all applications and their details are loaded.
                                    if (loadedCount[0] == totalApplications) {
                                        progressDialog.dismiss();
                                        applyFilters();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("ApplicationManagement", "Failed to load opportunity: " + e.getMessage());
                                    applicationList.add(model);
                                    loadedCount[0]++;

                                    if (loadedCount[0] == totalApplications) {
                                        progressDialog.dismiss();
                                        applyFilters();
                                    }
                                });
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
            currentSortMode = (currentSortMode + 1) % 3;
            sortApplications();

            String message = currentSortMode == 0 ? "Default sort" :
                    currentSortMode == 1 ? "Sorted A-Z" : "Sorted by Date";
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
                boolean matchesName = application.getTitle() != null &&
                        application.getTitle().toLowerCase().contains(searchQuery);
                boolean matchesEmail = application.getLocation() != null &&
                        application.getLocation().toLowerCase().contains(searchQuery);
                boolean matchesOpportunity = application.getOpportunityTitle() != null &&
                        application.getOpportunityTitle().toLowerCase().contains(searchQuery);
                boolean matchesStatusText = application.getStatus() != null &&
                        application.getStatus().toLowerCase().contains(searchQuery);

                if (matchesName || matchesEmail || matchesOpportunity || matchesStatusText) {
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
            // Sort alphabetically by applicant name.
            filteredList.sort((a1, a2) -> {
                String name1 = a1.getTitle() != null ? a1.getTitle() : "";
                String name2 = a2.getTitle() != null ? a2.getTitle() : "";
                return name1.compareToIgnoreCase(name2);
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
