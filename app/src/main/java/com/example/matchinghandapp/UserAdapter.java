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

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<Map<String, Object>> userList;
    private final List<String> userIds;
    private final FirebaseFirestore firestore;

    public UserAdapter(List<Map<String, Object>> userList, List<String> userIds) {
        this.userList = userList;
        this.userIds = userIds;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Map<String, Object> user = userList.get(position);
        String userId = userIds.get(position);

        String email = (String) user.get("email");
        String phone = (String) user.get("phone");
        String role = (String) user.get("role");
        Boolean active = (Boolean) user.get("active");

        holder.tvEmail.setText(email != null ? email : "No Email");
        holder.tvPhone.setText("Phone: " + (phone != null ? phone : "N/A"));
        holder.tvRole.setText("Role: " + (role != null ? role : "unknown"));
        holder.spinnerRole.setSelection(getRoleIndex(role));
        holder.switchActive.setChecked(active != null ? active : true);

        // 🔹 Role change listener
        holder.spinnerRole.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                String newRole = parent.getItemAtPosition(pos).toString();
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

        // 🔹 Active/Block toggle
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

    @Override
    public int getItemCount() {
        return userList.size();
    }

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
