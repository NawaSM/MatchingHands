package com.example.matchinghandapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

// This activity serves as the main dashboard for admin users.
public class Home_admin extends AppCompatActivity {

    private FirebaseAuth mAuth; // Firebase authentication service.
    private FirebaseFirestore firestore;
    private ListenerRegistration unreadMessagesListener;
    private TextView messagesBadge;

    // Called when the activity is first created.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_admin);

        // Initialize Firebase Auth & Firestore.
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI components.
        messagesBadge = findViewById(R.id.messagesBadge);
        LinearLayout userManagementCard = findViewById(R.id.userManagementCard);
        LinearLayout profileCard = findViewById(R.id.profileCard);
        LinearLayout logoutCard = findViewById(R.id.logoutCard);
        LinearLayout messagesCard = findViewById(R.id.messagesCard);

        // Set click listeners for each dashboard card.
        userManagementCard.setOnClickListener(v -> startActivity(new Intent(this, UserManagementActivity.class)));
        profileCard.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        messagesCard.setOnClickListener(v -> startActivity(new Intent(this, ChatListActivity.class)));

        // Set a click listener for the logout card.
        logoutCard.setOnClickListener(v -> {
            mAuth.signOut(); // Sign out from Firebase.
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            // Go back to the main (login) activity.
            Intent intent = new Intent(Home_admin.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Close this activity.
        });
    }

    // Called when the activity is resumed.
    @Override
    protected void onResume() {
        super.onResume();
        checkUserStatus(); // Check if the user is still active.
        listenForUnreadMessages(); // Listen for new messages.
    }

    // Called when the activity is paused.
    @Override
    protected void onPause() {
        super.onPause();
        // Stop listening for new messages to save resources.
        if (unreadMessagesListener != null) {
            unreadMessagesListener.remove();
        }
    }

    // Checks if the current user is still active, otherwise logs them out.
    private void checkUserStatus() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        firestore.collection("Users").document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Boolean isActive = task.getResult().getBoolean("active");
                if (isActive != null && !isActive) {
                    mAuth.signOut();
                    Intent intent = new Intent(Home_admin.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    Toast.makeText(Home_admin.this, "Your account has been deactivated.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
    }

    // Listens for unread messages and updates the notification badge.
    private void listenForUnreadMessages() {
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUserId == null) return;

        unreadMessagesListener = firestore.collectionGroup("Messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("read", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        int unreadCount = snapshots.size();
                        messagesBadge.setText(String.valueOf(unreadCount));
                        messagesBadge.setVisibility(View.VISIBLE);
                    } else {
                        messagesBadge.setVisibility(View.GONE);
                    }
                });
    }
}
