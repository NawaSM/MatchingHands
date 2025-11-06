package com.example.matchinghandapp;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ApplicationManagementAdapter extends RecyclerView.Adapter<ApplicationManagementAdapter.ViewHolder> {

    private final Context context;
    private final List<ApplicationModel> list;
    private final FirebaseFirestore firestore;

    public ApplicationManagementAdapter(Context context, List<ApplicationModel> list) {
        this.context = context;
        this.list = list;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_application_management, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApplicationModel model = list.get(position);

        // ✅ Display applicant + opportunity info
        holder.volunteerName.setText("Name: " + safe(model.getTitle()));
        holder.volunteerEmail.setText("Email: " + safe(model.getLocation())); // reused location for email earlier
        holder.volunteerPhone.setText("Phone: " + safe(model.getPhone()));
        holder.volunteerMotivation.setText("Motivation: " + safe(model.getMotivation()));
        holder.volunteerExperience.setText("Experience: " + safe(model.getExperience()));

        holder.opportunityTitle.setText("Opportunity: " + safe(model.getOpportunityTitle()));
        holder.date.setText("Opportunity created on: " + safe(model.getDate()));

        holder.status.setText(model.getStatus());

        // ✅ Dynamic badge colors
        switch (model.getStatus().toLowerCase()) {
            case "approved":
                holder.status.setBackgroundResource(R.drawable.badge_accepted);
                holder.status.setTextColor(Color.WHITE);
                break;
            case "rejected":
                holder.status.setBackgroundResource(R.drawable.badge_rejected);
                holder.status.setTextColor(Color.WHITE);
                break;
            default:
                holder.status.setBackgroundResource(R.drawable.badge_pending);
                holder.status.setTextColor(Color.WHITE);
                break;
        }

        // ✅ Show/hide buttons depending on status
        updateButtonVisibility(holder, model.getStatus());

        // ✅ Load opportunity image
        Glide.with(context)
                .load(model.getImageUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .into(holder.image);

        // ✅ Handle button clicks
        holder.btnApprove.setOnClickListener(v -> updateStatus(model, "Approved", holder));
        holder.btnReject.setOnClickListener(v -> updateStatus(model, "Rejected", holder));
        holder.btnPending.setOnClickListener(v -> updateStatus(model, "Pending", holder));
    }

    private void updateStatus(ApplicationModel model, String newStatus, ViewHolder holder) {
        new AlertDialog.Builder(context)
                .setTitle("Confirm Action")
                .setMessage("Are you sure you want to mark this as " + newStatus + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (model.getDocId() == null) {
                        Toast.makeText(context, "Missing document ID.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    firestore.collection("Applications")
                            .document(model.getDocId())
                            .update("status", newStatus)
                            .addOnSuccessListener(aVoid -> {
                                model.setStatus(newStatus);
                                Toast.makeText(context, "Marked as " + newStatus, Toast.LENGTH_SHORT).show();
                                updateButtonVisibility(holder, newStatus);
                                notifyDataSetChanged();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateButtonVisibility(ViewHolder holder, String status) {
        holder.btnApprove.setVisibility(View.GONE);
        holder.btnReject.setVisibility(View.GONE);
        holder.btnPending.setVisibility(View.GONE);

        switch (status.toLowerCase()) {
            case "pending":
                holder.btnApprove.setVisibility(View.VISIBLE);
                holder.btnReject.setVisibility(View.VISIBLE);
                break;
            case "approved":
                holder.btnPending.setVisibility(View.VISIBLE);
                holder.btnReject.setVisibility(View.VISIBLE);
                break;
            case "rejected":
                holder.btnPending.setVisibility(View.VISIBLE);
                holder.btnApprove.setVisibility(View.VISIBLE);
                break;
        }
    }

    private String safe(String text) {
        return text != null ? text : "-";
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView volunteerName, volunteerEmail, volunteerPhone, volunteerMotivation, volunteerExperience;
        TextView opportunityTitle, date, status;
        ImageView image;
        Button btnApprove, btnReject, btnPending;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            volunteerName = itemView.findViewById(R.id.volunteerName);
            volunteerEmail = itemView.findViewById(R.id.volunteerEmail);
            volunteerPhone = itemView.findViewById(R.id.volunteerPhone);
            volunteerMotivation = itemView.findViewById(R.id.volunteerMotivation);
            volunteerExperience = itemView.findViewById(R.id.volunteerExperience);

            opportunityTitle = itemView.findViewById(R.id.opportunityTitle);
            date = itemView.findViewById(R.id.date);
            status = itemView.findViewById(R.id.status);
            image = itemView.findViewById(R.id.image);

            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnPending = itemView.findViewById(R.id.btnPending);
        }
    }
}
