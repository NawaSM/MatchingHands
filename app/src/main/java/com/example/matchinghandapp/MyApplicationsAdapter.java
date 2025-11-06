package com.example.matchinghandapp;

import android.content.Context;
import android.content.Intent;
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

public class MyApplicationsAdapter extends RecyclerView.Adapter<MyApplicationsAdapter.ViewHolder> {

    private Context context;
    private List<ApplicationModel> list;

    public MyApplicationsAdapter(Context context, List<ApplicationModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_application, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApplicationModel model = list.get(position);

        holder.title.setText(model.getTitle());
        holder.location.setText(model.getLocation());
        holder.date.setText(model.getDate());
        holder.status.setText(model.getStatus());

        // Change color based on status
        switch (model.getStatus().toLowerCase()) {
            case "approved":
                holder.status.setTextColor(context.getResources().getColor(R.color.teal_700));
                break;
            case "rejected":
                holder.status.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
                break;
            default:
                holder.status.setTextColor(context.getResources().getColor(R.color.gray_700));
                break;
        }

        Glide.with(context)
                .load(model.getImageUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .into(holder.image);

        holder.card.setOnClickListener(v -> {
            Intent intent = new Intent(context, OpportunityDetailsActivity.class);
            intent.putExtra("opportunityId", model.getOpportunityId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

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
