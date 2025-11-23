package com.example.matchinghandapp;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscoverAdapter extends RecyclerView.Adapter<DiscoverAdapter.ViewHolder> {

    private final List<OpportunityModel> opportunityList;
    private final Context context;
    private final FirebaseFirestore firestore;
    private final String currentUid;
    private final Map<String, Boolean> bookmarkCache = new HashMap<>();
    private boolean bookmarksLoaded = false;

    public DiscoverAdapter(List<OpportunityModel> opportunityList, Context context) {
        this.opportunityList = opportunityList;
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_discover_opportunity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OpportunityModel model = opportunityList.get(position);

        holder.opTitle.setText(model.getTitle());
        holder.opOrganization.setText(model.getAddress() != null ? model.getAddress() : "Unknown Organizer");
        holder.opDate.setText(model.getDate());
        holder.opDuration.setText(model.getDuration());
        holder.opTime.setText(model.getTime() != null ? model.getTime() : "-");

        // Set the match percentage from the model. No recalculation here.
        holder.opMatchPercent.setText(model.getMatchPercentage());

        if (model.getImageUrl() != null && !model.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(model.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(holder.opImage);
        } else {
            holder.opImage.setImageResource(R.drawable.ic_image_placeholder);
        }

        holder.opTagGroup.removeAllViews();
        if (model.getTags() != null && !model.getTags().isEmpty()) {
            for (String tag : model.getTags()) {
                Chip chip = new Chip(context);
                chip.setText(tag);
                chip.setChipBackgroundColorResource(R.color.blue_100);
                chip.setTextColor(context.getResources().getColor(R.color.blue_800));
                chip.setClickable(false);
                holder.opTagGroup.addView(chip);
            }
        }

        holder.opBookmark.setImageResource(R.drawable.ic_bookmark_border);

        if (bookmarksLoaded && model.getId() != null) {
            boolean isBookmarked = bookmarkCache.getOrDefault(model.getId(), false);
            holder.opBookmark.setImageResource(
                    isBookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_border
            );
        }

        holder.opBookmark.setOnClickListener(v -> {
            if (currentUid == null) {
                Toast.makeText(context, "Please log in to bookmark", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean currentlyBookmarked = bookmarkCache.getOrDefault(model.getId(), false);
            DocumentReference bookmarkRef = firestore.collection("Bookmarks")
                    .document(currentUid)
                    .collection("Bookmarks")
                    .document(model.getId());

            if (currentlyBookmarked) {
                bookmarkRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            bookmarkCache.put(model.getId(), false);
                            holder.opBookmark.setImageResource(R.drawable.ic_bookmark_border);
                            Toast.makeText(context, "Removed from bookmarks", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Log.e("DiscoverAdapter", "Failed to delete bookmark: " + e.getMessage()));
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("opportunityId", model.getId());
                data.put("userId", currentUid);
                data.put("title", model.getTitle());
                data.put("date", model.getDate());
                data.put("imageUrl", model.getImageUrl());
                data.put("timestamp", System.currentTimeMillis());

                bookmarkRef.set(data)
                        .addOnSuccessListener(aVoid -> {
                            bookmarkCache.put(model.getId(), true);
                            holder.opBookmark.setImageResource(R.drawable.ic_bookmark_filled);
                            Toast.makeText(context, "Bookmarked successfully", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Log.e("DiscoverAdapter", "Failed to add bookmark: " + e.getMessage()));
            }
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OpportunityDetailsActivity.class);
            intent.putExtra("opportunityId", model.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return opportunityList.size();
    }

    public void preloadBookmarks() {
        if (currentUid == null || bookmarksLoaded) return;

        firestore.collection("Bookmarks")
                .document(currentUid)
                .collection("Bookmarks")
                .get()
                .addOnSuccessListener(query -> {
                    bookmarkCache.clear();
                    for (DocumentSnapshot snap : query.getDocuments()) {
                        bookmarkCache.put(snap.getId(), true);
                    }
                    bookmarksLoaded = true;
                    new Handler(Looper.getMainLooper()).post(this::notifyDataSetChanged);
                })
                .addOnFailureListener(e ->
                        Log.e("DiscoverAdapter", "Failed to load bookmarks: " + e.getMessage()));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView opImage, opBookmark;
        TextView opTitle, opOrganization, opDate, opDuration, opTime, opMatchPercent;
        ChipGroup opTagGroup;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            opImage = itemView.findViewById(R.id.opImage);
            opBookmark = itemView.findViewById(R.id.opBookmark);
            opTitle = itemView.findViewById(R.id.opTitle);
            opOrganization = itemView.findViewById(R.id.opOrganization);
            opDate = itemView.findViewById(R.id.opDate);
            opDuration = itemView.findViewById(R.id.opDuration);
            opTime = itemView.findViewById(R.id.opTime);
            opMatchPercent = itemView.findViewById(R.id.opMatchPercent);
            opTagGroup = itemView.findViewById(R.id.opTagGroup);
        }
    }
}
