package com.example.matchinghandapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This activity displays a list of users that the current user can chat with.
public class ChatListActivity extends AppCompatActivity {

    // UI and Firebase components.
    private RecyclerView userRecyclerView;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private List<UserModel> userList = new ArrayList<>();
    private UserAdapter adapter;
    private ListenerRegistration unreadMessagesListener;
    private final Map<String, Boolean> unreadStatus = new HashMap<>();

    // Called when the activity is first created.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        // Initialize UI and Firebase services.
        userRecyclerView = findViewById(R.id.userRecyclerView);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Set up the RecyclerView.
        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(userList, this::openChat, unreadStatus);
        userRecyclerView.setAdapter(adapter);

        loadUsers(); // Load the list of users from Firestore.
    }

    // Called when the activity is resumed.
    @Override
    protected void onResume() {
        super.onResume();
        listenForUnreadMessages(); // Start listening for unread messages.
    }

    // Called when the activity is paused.
    @Override
    protected void onPause() {
        super.onPause();
        // Stop listening for unread messages to save resources.
        if (unreadMessagesListener != null) {
            unreadMessagesListener.remove();
        }
    }

    // Loads all active users from Firestore except for the current user.
    private void loadUsers() {
        String currentUserId = auth.getCurrentUser().getUid();

        // Only fetch users who are marked as active.
        firestore.collection("Users").whereEqualTo("active", true).get()
                .addOnSuccessListener(querySnapshot -> {
                    userList.clear();
                    for (DocumentSnapshot snapshot : querySnapshot) {
                        UserModel user = snapshot.toObject(UserModel.class);
                        // Add user to the list if it's not the current user.
                        if (user != null && !snapshot.getId().equals(currentUserId)) {
                            user.setUserId(snapshot.getId());
                            userList.add(user);
                        }
                    }
                    adapter.notifyDataSetChanged(); // Refresh the list.
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Opens the chat screen for a selected user.
    private void openChat(UserModel user) {
        Intent intent = new Intent(this, MessagesActivity.class);
        intent.putExtra("chatPartnerId", user.getUserId());
        startActivity(intent);
    }

    // Listens for unread messages and updates the UI.
    private void listenForUnreadMessages() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) return;

        unreadMessagesListener = firestore.collectionGroup("Messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("read", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }

                    unreadStatus.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String senderId = doc.getString("senderId");
                            if (senderId != null) {
                                unreadStatus.put(senderId, true);
                            }
                        }
                    }
                    adapter.updateUnreadStatus(unreadStatus);
                });
    }

    // Adapter for displaying the list of users.
    static class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private final List<UserModel> userList;
        private final OnUserClickListener listener;
        private Map<String, Boolean> unreadStatus;

        // Interface for handling clicks on a user item.
        interface OnUserClickListener {
            void onUserClick(UserModel user);
        }

        // Constructor.
        UserAdapter(List<UserModel> userList, OnUserClickListener listener, Map<String, Boolean> unreadStatus) {
            this.userList = userList;
            this.listener = listener;
            this.unreadStatus = unreadStatus;
        }

        // Creates a new ViewHolder.
        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        // Binds data to the ViewHolder.
        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            UserModel user = userList.get(position);

            String displayName = user.getDisplayName();
            // Add "(admin)" to the name if the user is an admin.
            if ("admin".equalsIgnoreCase(user.getRole())) {
                displayName += " (admin)";
            }
            holder.tvName.setText(displayName);

            holder.tvEmail.setText(user.getEmail());

            // Load profile image or set a default.
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Picasso.get().load(user.getProfileImageUrl()).into(holder.imgProfile);
            } else {
                holder.imgProfile.setImageResource(R.drawable.ic_baseline_person_24);
            }

            // Show unread indicator if there are unread messages from this user.
            if (unreadStatus.getOrDefault(user.getUserId(), false)) {
                holder.unreadIndicator.setVisibility(View.VISIBLE);
            } else {
                holder.unreadIndicator.setVisibility(View.GONE);
            }

            // Set click listener for the item.
            holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
        }

        // Returns the number of users in the list.
        @Override
        public int getItemCount() {
            return userList.size();
        }

        // Updates the unread status map and refreshes the list.
        public void updateUnreadStatus(Map<String, Boolean> unreadStatus) {
            this.unreadStatus = unreadStatus;
            notifyDataSetChanged();
        }

        // ViewHolder class.
        static class UserViewHolder extends RecyclerView.ViewHolder {
            ImageView imgProfile;
            TextView tvName, tvEmail;
            View unreadIndicator;

            UserViewHolder(@NonNull View itemView) {
                super(itemView);
                imgProfile = itemView.findViewById(R.id.imgProfile);
                tvName = itemView.findViewById(R.id.tvName);
                tvEmail = itemView.findViewById(R.id.tvEmail);
                unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            }
        }
    }
}
