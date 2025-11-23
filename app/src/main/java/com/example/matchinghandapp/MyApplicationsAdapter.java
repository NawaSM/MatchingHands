package com.example.matchinghandapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

// Adapter for displaying a list of a user's applications in a RecyclerView.
public class MyApplicationsAdapter extends RecyclerView.Adapter<MyApplicationsAdapter.ViewHolder> {

    private final Context context;
    private final List<ApplicationModel> list; // The list of applications to display.

    // Constructor to initialize the adapter.
    public MyApplicationsAdapter(Context context, List<ApplicationModel> list) {
        this.context = context;
        this.list = list;
    }

    // Creates a new ViewHolder when the RecyclerView needs one.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single application item.
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_application, parent, false);
        return new ViewHolder(view);
    }

    // Binds the data to the views in a given ViewHolder.
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApplicationModel model = list.get(position);

        // Set the application data to the UI components.
        holder.title.setText(model.getTitle());
        holder.location.setText(model.getLocation());
        holder.date.setText(model.getDate());
        holder.status.setText(model.getStatus());

        // Change the status badge color based on the application status.
        switch (model.getStatus().toLowerCase()) {
            case "approved":
                holder.status.setBackgroundResource(R.drawable.badge_accepted);
                holder.status.setTextColor(Color.WHITE);
                break;
            case "rejected":
                holder.status.setBackgroundResource(R.drawable.badge_rejected);
                holder.status.setTextColor(Color.WHITE);
                break;
            default: // For "Pending" or other statuses.
                holder.status.setBackgroundResource(R.drawable.badge_pending);
                holder.status.setTextColor(Color.WHITE);
                break;
        }

        // Load the opportunity image using Glide.
        Glide.with(context)
                .load(model.getImageUrl())
                .placeholder(R.drawable.ic_image_placeholder) // Show a placeholder while loading.
                .into(holder.image);

        // Set a click listener to open the opportunity details when the card is clicked.
        holder.card.setOnClickListener(v -> {
            Intent intent = new Intent(context, OpportunityDetailsActivity.class);
            intent.putExtra("opportunityId", model.getOpportunityId());
            context.startActivity(intent);
        });
    }

    // Returns the total number of items in the list.
    @Override
    public int getItemCount() {
        return list.size();
    }

    // ViewHolder class to hold the UI components for each application item.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, location, date, status;
        ImageView image;
        CardView card;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            title = itemView.findViewById(R.id.title);
            location = itemView.findViewById(R.id.location);
            date = itemView.findViewById(R.id.date);
            status = itemView.findViewById(R.id.status);
            image = itemView.findViewById(R.id.image);
        }
    }
}
