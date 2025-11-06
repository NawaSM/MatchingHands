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

public class MessagesActivity extends AppCompatActivity {

    private RecyclerView recyclerMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private MessageAdapter adapter;
    private List<MessageModel> messageList = new ArrayList<>();

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private String currentUserId;
    private String chatPartnerId;
    private String chatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerMessages = findViewById(R.id.recyclerMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        // ✅ Get current user
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "You must be logged in to chat.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // ✅ Get chat partner ID safely
        chatPartnerId = getIntent().getStringExtra("chatPartnerId");

        if (chatPartnerId == null || chatPartnerId.trim().isEmpty()) {
            Toast.makeText(this, "No chat partner provided. Opening system chat...", Toast.LENGTH_SHORT).show();
            chatPartnerId = "system_chat"; // Fallback chat
        }

        // ✅ Log for debugging
        Log.d("MessagesActivity", "CurrentUser: " + currentUserId);
        Log.d("MessagesActivity", "ChatPartner: " + chatPartnerId);

        // ✅ Build chatId safely
        if (chatPartnerId.equals("system_chat")) {
            chatId = "system_chat_" + currentUserId;
        } else {
            chatId = currentUserId.compareTo(chatPartnerId) < 0
                    ? currentUserId + "_" + chatPartnerId
                    : chatPartnerId + "_" + currentUserId;
        }

        // ✅ Setup RecyclerView
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter(this, messageList, currentUserId);
        recyclerMessages.setAdapter(adapter);

        // ✅ Load and listen to messages
        loadMessages();

        // ✅ Send message button
        btnSend.setOnClickListener(v -> sendMessage());
    }

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
                        }
                        adapter.notifyDataSetChanged();
                        recyclerMessages.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) return;

        String msgId = UUID.randomUUID().toString();
        Map<String, Object> message = new HashMap<>();
        message.put("messageId", msgId);
        message.put("senderId", currentUserId);
        message.put("receiverId", chatPartnerId);
        message.put("message", messageText);
        message.put("timestamp", FieldValue.serverTimestamp());

        firestore.collection("Chats").document(chatId)
                .collection("Messages")
                .document(msgId)
                .set(message)
                .addOnSuccessListener(aVoid -> etMessage.setText(""))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
