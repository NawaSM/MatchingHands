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

public class OpportunityAdapter extends RecyclerView.Adapter<OpportunityAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onEdit(OpportunityModel model);
        void onDelete(OpportunityModel model);
    }

    private List<OpportunityModel> list;
    private OnItemClickListener listener;

    public OpportunityAdapter(List<OpportunityModel> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_opportunity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OpportunityModel model = list.get(position);

        holder.title.setText(model.getTitle());
        holder.date.setText(model.getDate());
        holder.location.setText(model.getLocation());
        holder.duration.setText(model.getDuration());

        Glide.with(holder.itemView.getContext())
                .load(model.getImageUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .into(holder.opImage);

        holder.editBtn.setOnClickListener(v -> listener.onEdit(model));
        holder.deleteBtn.setOnClickListener(v -> listener.onDelete(model));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

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
