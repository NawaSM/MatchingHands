package com.example.matchinghandapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
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

public class OpportunityManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ImageButton addButton;
    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private CollectionReference oppRef;

    private List<OpportunityModel> opportunityList;
    private OpportunityAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opportunity_management);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        oppRef = firestore.collection("Opportunities");

        recyclerView = findViewById(R.id.recyclerOpportunities);
        addButton = findViewById(R.id.addOpportunityBtn);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        opportunityList = new ArrayList<>();
        adapter = new OpportunityAdapter(opportunityList, new OpportunityAdapter.OnItemClickListener() {
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

        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddOpportunityActivity.class);
            startActivity(intent);
        });

        loadOpportunities();
    }

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
                                model.setId(doc.getId()); // ✅ store document ID
                                opportunityList.add(model);
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                    progressDialog.dismiss();

                    if (opportunityList.isEmpty()) {
                        Toast.makeText(OpportunityManagementActivity.this,
                                "You haven't created any opportunities yet.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

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
