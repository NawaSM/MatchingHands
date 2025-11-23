package com.example.matchinghandapp;

// Data model representing a user in the application.
public class UserModel {
    // Fields for user data.
    private String userId, fullName, email, profileImageUrl, phone, address, role;

    // Default constructor required for calls to DataSnapshot.getValue(UserModel.class).
    public UserModel() {}

    // Getter for the user's unique ID.
    public String getUserId() { return userId; }
    // Setter for the user's unique ID.
    public void setUserId(String userId) { this.userId = userId; }

    // Getter for the user's full name.
    public String getFullName() { return fullName; }
    // Setter for the user's full name.
    public void setFullName(String fullName) { this.fullName = fullName; }

    // Getter for the user's email address.
    public String getEmail() { return email; }
    // Setter for the user's email address.
    public void setEmail(String email) { this.email = email; }

    // Getter for the URL of the user's profile image.
    public String getProfileImageUrl() { return profileImageUrl; }
    // Setter for the URL of the user's profile image.
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    // Getter for the user's phone number.
    public String getPhone() { return phone; }
    // Setter for the user's phone number.
    public void setPhone(String phone) { this.phone = phone; }

    // Getter for the user's address.
    public String getAddress() { return address; }
    // Setter for the user's address.
    public void setAddress(String address) { this.address = address; }

    // Getter for the user's role (e.g., "volunteer", "ngo").
    public String getRole() { return role; }
    // Setter for the user's role.
    public void setRole(String role) { this.role = role; }

    // Gets a display name for the user.
    public String getDisplayName() {
        // Use full name if it exists.
        if (fullName != null && !fullName.isEmpty()) {
            return fullName;
        }
        // Fallback to username from email.
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf("@"));
        }
        // Default if no name or email is available.
        return "Unknown User";
    }
}
