package com.example.matchinghandapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

// Adapter for displaying a list of users in a RecyclerView for admin management.
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    // Data lists for users and their corresponding IDs.
    private final List<Map<String, Object>> userList;
    private final List<String> userIds;
    private final FirebaseFirestore firestore; // Firestore instance for database operations.

    // Constructor to initialize the adapter with user data.
    public UserAdapter(List<Map<String, Object>> userList, List<String> userIds) {
        this.userList = userList;
        this.userIds = userIds;
        this.firestore = FirebaseFirestore.getInstance();
    }

    // Creates a new ViewHolder when the RecyclerView needs one.
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single user item.
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(v);
    }

    // Binds the data to the views in a given ViewHolder.
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Map<String, Object> user = userList.get(position); // Get user data at this position.
        String userId = userIds.get(position); // Get user ID at this position.

        // Extract user details from the map.
        String email = (String) user.get("email");
        String phone = (String) user.get("phone");
        String role = (String) user.get("role");
        Boolean active = (Boolean) user.get("active");

        // Set the user data to the UI components.
        holder.tvEmail.setText(email != null ? email : "No Email");
        holder.tvPhone.setText("Phone: " + (phone != null ? phone : "N/A"));
        holder.tvRole.setText("Role: " + (role != null ? role : "unknown"));
        holder.spinnerRole.setSelection(getRoleIndex(role)); // Set spinner to the user's current role.
        holder.switchActive.setChecked(active != null ? active : true); // Set the active status switch.

        // Listener to update user role when a new role is selected from the spinner.
        holder.spinnerRole.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                String newRole = parent.getItemAtPosition(pos).toString();
                // Update Firestore only if the role has actually changed.
                if (role == null || !newRole.equals(role)) {
                    firestore.collection("Users").document(userId)
                            .update("role", newRole)
                            .addOnSuccessListener(aVoid -> Toast.makeText(view.getContext(), "Role updated to " + newRole, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(view.getContext(), "Failed to update role", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        // Listener for the active/blocked switch to update the user's status in Firestore.
        holder.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            firestore.collection("Users").document(userId)
                    .update("active", isChecked)
                    .addOnSuccessListener(aVoid -> Toast.makeText(buttonView.getContext(),
                            isChecked ? "User Activated" : "User Blocked",
                            Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(buttonView.getContext(),
                            "Failed to update status", Toast.LENGTH_SHORT).show());
        });
    }

    // Helper method to get the index of a role string for setting the spinner selection.
    private int getRoleIndex(String role) {
        if (role == null) return 0;
        switch (role.toLowerCase()) {
            case "volunteer":
                return 0;
            case "ngo":
                return 1;
            case "admin":
                return 2;
            default:
                return 0;
        }
    }

    // Returns the total number of items in the list.
    @Override
    public int getItemCount() {
        return userList.size();
    }

    // ViewHolder class to hold the UI components for each user item.
    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmail, tvPhone, tvRole;
        Spinner spinnerRole;
        Switch switchActive;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvRole = itemView.findViewById(R.id.tvRole);
            spinnerRole = itemView.findViewById(R.id.spinnerRole);
            switchActive = itemView.findViewById(R.id.switchActive);
        }
    }
}
