package com.example.matchinghandapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyApplicationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MyApplicationsAdapter adapter;
    private List<ApplicationModel> applicationList;
    private FirebaseFirestore firestore;
    private ProgressDialog progressDialog;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_applications);

        recyclerView = findViewById(R.id.applicationsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        applicationList = new ArrayList<>();
        adapter = new MyApplicationsAdapter(this, applicationList);
        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading your applications...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        loadApplications();
    }

    private void loadApplications() {
        firestore.collection("Applications")
                .whereEqualTo("applicantId", currentUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "No applications found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot appDoc : querySnapshot.getDocuments()) {
                        String status = appDoc.getString("status");
                        String opportunityId = appDoc.getString("opportunityId");

                        if (opportunityId != null) {
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
                                            adapter.notifyDataSetChanged();
                                        }
                                        progressDialog.dismiss();
                                    })
                                    .addOnFailureListener(e -> progressDialog.dismiss());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error loading applications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
