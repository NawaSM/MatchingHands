package com.example.matchinghandapp;

import android.app.ProgressDialog;
import android.os.Bundle;
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

public class ApplicationManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ApplicationManagementAdapter adapter;
    private List<ApplicationModel> applicationList;
    private FirebaseFirestore firestore;
    private ProgressDialog progressDialog;
    private String currentOrganizerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application_management);

        recyclerView = findViewById(R.id.applicationsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        applicationList = new ArrayList<>();
        adapter = new ApplicationManagementAdapter(this, applicationList);
        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();
        currentOrganizerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading volunteer applications...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        loadApplications();
    }

    private void loadApplications() {
        firestore.collection("Applications")
                .whereEqualTo("organizerId", currentOrganizerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "No volunteer applications found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    applicationList.clear();

                    for (DocumentSnapshot appDoc : querySnapshot.getDocuments()) {
                        ApplicationModel model = new ApplicationModel();

                        // Basic volunteer info
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

                        // Fetch opportunity details
                        firestore.collection("Opportunities")
                                .document(model.getOpportunityId())
                                .get()
                                .addOnSuccessListener(opDoc -> {
                                    if (opDoc.exists()) {
                                        model.setDate(opDoc.getString("date"));
                                        model.setImageUrl(opDoc.getString("imageUrl"));
                                        // ✅ Change this field name to match your Firestore (title or description)
                                        model.setOpportunityTitle(opDoc.getString("title"));
                                    }

                                    applicationList.add(model);
                                    adapter.notifyDataSetChanged();
                                    progressDialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(this, "Error loading opportunity info.", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error loading applications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
