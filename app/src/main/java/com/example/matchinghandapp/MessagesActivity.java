package com.example.matchinghandapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

// This activity handles the real-time chat between two users.
public class MessagesActivity extends AppCompatActivity {

    // UI and Firebase components.
    private RecyclerView recyclerMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private MessageAdapter adapter;
    private List<MessageModel> messageList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    // Chat-related variables.
    private String currentUserId; // ID of the logged-in user.
    private String chatPartnerId; // ID of the user being chatted with.
    private String chatId; // The unique ID for the chat session.

    // Called when the activity is first created.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        // Initialize Firebase services.
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize UI components.
        recyclerMessages = findViewById(R.id.recyclerMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        // Get the current user's ID.
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "You must be logged in to chat.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Get the chat partner's ID from the intent.
        chatPartnerId = getIntent().getStringExtra("chatPartnerId");

        // Fallback to a system chat if no partner ID is provided.
        if (chatPartnerId == null || chatPartnerId.trim().isEmpty()) {
            Toast.makeText(this, "No chat partner provided. Opening system chat...", Toast.LENGTH_SHORT).show();
            chatPartnerId = "system_chat";
        }

        // Create a consistent and unique chat ID for the two users.
        if (chatPartnerId.equals("system_chat")) {
            chatId = "system_chat_" + currentUserId;
        } else {
            // Sort user IDs alphabetically to ensure the same chat ID for both users.
            chatId = currentUserId.compareTo(chatPartnerId) < 0
                    ? currentUserId + "_" + chatPartnerId
                    : chatPartnerId + "_" + currentUserId;
        }

        // Set up the RecyclerView for displaying messages.
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter(this, messageList, currentUserId);
        recyclerMessages.setAdapter(adapter);

        loadMessages(); // Load existing messages and listen for new ones.

        // Set a click listener for the send button.
        btnSend.setOnClickListener(v -> sendMessage());
    }

    // Loads messages from Firestore and listens for real-time updates.
    private void loadMessages() {
        firestore.collection("Chats").document(chatId)
                .collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("MessagesActivity", "Load error: ", e);
                        return;
                    }

                    messageList.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            MessageModel msg = doc.toObject(MessageModel.class);
                            messageList.add(msg);

                            // Mark message as read if the current user is the receiver.
                            if (msg.getReceiverId().equals(currentUserId) && !msg.isRead()) {
                                firestore.collection("Chats").document(chatId)
                                        .collection("Messages").document(doc.getId())
                                        .update("read", true);
                            }
                        }
                        adapter.notifyDataSetChanged(); // Refresh the list.
                        recyclerMessages.scrollToPosition(messageList.size() - 1); // Scroll to the latest message.
                    }
                });
    }

    // Sends a new message.
    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) return;

        String msgId = UUID.randomUUID().toString(); // Generate a unique ID for the message.
        Map<String, Object> message = new HashMap<>();
        message.put("messageId", msgId);
        message.put("senderId", currentUserId);
        message.put("receiverId", chatPartnerId);
        message.put("message", messageText);
        message.put("timestamp", FieldValue.serverTimestamp()); // Use server timestamp for consistency.
        message.put("read", false); // A new message is always unread.

        // Add the new message to Firestore.
        firestore.collection("Chats").document(chatId)
                .collection("Messages")
                .document(msgId)
                .set(message)
                .addOnSuccessListener(aVoid -> etMessage.setText("")) // Clear the input field on success.
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
