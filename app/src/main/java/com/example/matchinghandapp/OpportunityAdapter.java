package com.example.matchinghandapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

// Adapter for displaying a list of opportunities in a RecyclerView for NGO management.
public class OpportunityAdapter extends RecyclerView.Adapter<OpportunityAdapter.ViewHolder> {

    // Interface for handling edit and delete clicks on an item.
    public interface OnItemClickListener {
        void onEdit(OpportunityModel model);
        void onDelete(OpportunityModel model);
    }

    private List<OpportunityModel> list; // The list of opportunities to display.
    private OnItemClickListener listener; // The listener for item clicks.

    // Constructor to initialize the adapter.
    public OpportunityAdapter(List<OpportunityModel> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    // Creates a new ViewHolder when the RecyclerView needs one.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single opportunity item.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_opportunity, parent, false);
        return new ViewHolder(view);
    }

    // Binds the data to the views in a given ViewHolder.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OpportunityModel model = list.get(position);

        // Set the opportunity data to the UI components.
        holder.title.setText(model.getTitle());
        holder.date.setText(model.getDate());
        holder.location.setText(model.getLocation());
        holder.duration.setText(model.getDuration());

        // Load the opportunity image using Glide.
        Glide.with(holder.itemView.getContext())
                .load(model.getImageUrl())
                .placeholder(R.drawable.ic_image_placeholder) // Show a placeholder while loading.
                .into(holder.opImage);

        // Set click listeners for the edit and delete buttons.
        holder.editBtn.setOnClickListener(v -> listener.onEdit(model));
        holder.deleteBtn.setOnClickListener(v -> listener.onDelete(model));
    }

    // Returns the total number of items in the list.
    @Override
    public int getItemCount() {
        return list.size();
    }

    // ViewHolder class to hold the UI components for each opportunity item.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, location, duration;
        ImageButton editBtn, deleteBtn;
        ImageView opImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.opTitleText);
            date = itemView.findViewById(R.id.opDateText);
            location = itemView.findViewById(R.id.opLocationText);
            duration = itemView.findViewById(R.id.opDurationText);
            editBtn = itemView.findViewById(R.id.btnEdit);
            deleteBtn = itemView.findViewById(R.id.btnDelete);
            opImage = itemView.findViewById(R.id.opImageView);
        }
    }
}
