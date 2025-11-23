package com.example.matchinghandapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// Adapter for displaying a list of chat messages in a RecyclerView.
public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final List<MessageModel> list; // The list of messages to display.
    private final String currentUserId; // The ID of the current user.

    // View type constants for sent and received messages.
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    // Constructor to initialize the adapter.
    public MessageAdapter(Context context, List<MessageModel> list, String currentUserId) {
        this.context = context;
        this.list = list;
        this.currentUserId = currentUserId;
    }

    // Determines the view type for a message at a given position (sent or received).
    @Override
    public int getItemViewType(int position) {
        if (list.get(position).getSenderId().equals(currentUserId)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    // Creates a new ViewHolder based on the view type.
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the appropriate layout for sent or received messages.
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    // Binds the message data to the views in a given ViewHolder.
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel msg = list.get(position);
        // Set the message text based on the ViewHolder type.
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).tvMessage.setText(msg.getMessage());
        } else if (holder instanceof ReceivedViewHolder) {
            ((ReceivedViewHolder) holder).tvMessage.setText(msg.getMessage());
        }
    }

    // Returns the total number of messages in the list.
    @Override
    public int getItemCount() {
        return list.size();
    }

    // ViewHolder for sent messages.
    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }

    // ViewHolder for received messages.
    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }
}
