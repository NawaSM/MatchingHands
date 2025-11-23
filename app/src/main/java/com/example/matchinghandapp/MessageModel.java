package com.example.matchinghandapp;

import com.google.firebase.Timestamp;

// Data model representing a single chat message.
public class MessageModel {
    private String messageId; // The unique ID of the message.
    private String senderId; // The ID of the user who sent the message.
    private String receiverId; // The ID of the user who received the message.
    private String message; // The text content of the message.
    private Timestamp timestamp; // The time the message was sent.
    private boolean read; // Whether the message has been read by the recipient.

    // Default constructor required for Firestore data mapping.
    public MessageModel() {}

    // Getter for the message ID.
    public String getMessageId() { return messageId; }
    // Setter for the message ID.
    public void setMessageId(String messageId) { this.messageId = messageId; }

    // Getter for the sender's ID.
    public String getSenderId() { return senderId; }
    // Setter for the sender's ID.
    public void setSenderId(String senderId) { this.senderId = senderId; }

    // Getter for the receiver's ID.
    public String getReceiverId() { return receiverId; }
    // Setter for the receiver's ID.
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    // Getter for the message content.
    public String getMessage() { return message; }
    // Setter for the message content.
    public void setMessage(String message) { this.message = message; }

    // Getter for the timestamp.
    public com.google.firebase.Timestamp getTimestamp() { return timestamp; }
    // Setter for the timestamp.
    public void setTimestamp(com.google.firebase.Timestamp timestamp) { this.timestamp = timestamp; }

    // Getter for the read status.
    public boolean isRead() { return read; }
    // Setter for the read status.
    public void setRead(boolean read) { this.read = read; }
}
