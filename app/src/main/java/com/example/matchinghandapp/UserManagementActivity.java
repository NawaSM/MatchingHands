package com.example.matchinghandapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This activity allows admins to manage all users in the system.
public class UserManagementActivity extends AppCompatActivity {

    private RecyclerView usersRecycler;
    private EditText etSearch; // For searching users.
    private ImageView ivSort; // For sorting users.
    private FirebaseFirestore firestore;
    private List<Map<String, Object>> userList; // Complete list of users.
    private List<Map<String, Object>> filteredList; // The filtered list that is displayed.
    private List<String> userIds;
    private List<String> filteredUserIds;
    private int currentSortMode = 0; // Tracks the current sort mode: 0=default, 1=A-Z.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        // Initialize UI components.
        usersRecycler = findViewById(R.id.usersRecycler);
        usersRecycler.setLayoutManager(new LinearLayoutManager(this));
        etSearch = findViewById(R.id.etSearch);
        ivSort = findViewById(R.id.ivSort);

        // Initialize Firestore and data lists.
        firestore = FirebaseFirestore.getInstance();
        userList = new ArrayList<>();
        filteredList = new ArrayList<>();
        userIds = new ArrayList<>();
        filteredUserIds = new ArrayList<>();

        setupSearchBar();
        loadUsers();
    }

    // Loads all users from the "Users" collection in Firestore.
    private void loadUsers() {
        firestore.collection("Users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        userList.clear();
                        userIds.clear();

                        QuerySnapshot snapshot = task.getResult();
                        for (DocumentSnapshot doc : snapshot) {
                            Map<String, Object> userData = new HashMap<>(doc.getData());
                            userList.add(userData);
                            userIds.add(doc.getId());
                        }

                        // Initially, the filtered list is the same as the full list.
                        filterUsers(etSearch.getText().toString());

                    } else {
                        Toast.makeText(UserManagementActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(UserManagementActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // Sets up listeners for the search bar and sort button.
    private void setupSearchBar() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        ivSort.setOnClickListener(v -> {
            currentSortMode = (currentSortMode + 1) % 2;
            sortUsers();

            String message = currentSortMode == 0 ? "Default sort" : "Sorted A-Z";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    // Sorts the displayed list of users.
    private void sortUsers() {
        // A helper class to keep user data and ID together during sorting.
        class UserData {
            Map<String, Object> data;
            String id;
            UserData(Map<String, Object> data, String id) { this.data = data; this.id = id; }
        }

        if (currentSortMode == 1) {
            List<UserData> combinedList = new ArrayList<>();
            for (int i = 0; i < filteredList.size(); i++) {
                combinedList.add(new UserData(filteredList.get(i), filteredUserIds.get(i)));
            }

            // Sort alphabetically by name, falling back to email if name is not available.
            combinedList.sort((u1, u2) -> {
                String name1 = u1.data.get("fullname") != null ? u1.data.get("fullname").toString() : "";
                String name2 = u2.data.get("fullname") != null ? u2.data.get("fullname").toString() : "";

                if (name1.isEmpty()) {
                    name1 = u1.data.get("email") != null ? u1.data.get("email").toString() : "";
                }
                if (name2.isEmpty()) {
                    name2 = u2.data.get("email") != null ? u2.data.get("email").toString() : "";
                }

                return name1.compareToIgnoreCase(name2);
            });

            // Re-populate the filtered lists with the sorted data.
            filteredList.clear();
            filteredUserIds.clear();
            for (UserData userData : combinedList) {
                filteredList.add(userData.data);
                filteredUserIds.add(userData.id);
            }
        } else {
            // Default sort (mode 0) re-applies the filter to restore original order.
            filterUsers(etSearch.getText().toString());
            return;
        }

        usersRecycler.setAdapter(new UserAdapter(filteredList, filteredUserIds));
    }

    // Filters the main user list into the displayed list based on the search query.
    private void filterUsers(String query) {
        filteredList.clear();
        filteredUserIds.clear();

        if (query.isEmpty()) {
            filteredList.addAll(userList);
            filteredUserIds.addAll(userIds);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();

            for (int i = 0; i < userList.size(); i++) {
                Map<String, Object> user = userList.get(i);

                // Check if name, email, or role match the query.
                boolean matchesName = user.get("fullname") != null &&
                        user.get("fullname").toString().toLowerCase().contains(lowerCaseQuery);
                boolean matchesEmail = user.get("email") != null &&
                        user.get("email").toString().toLowerCase().contains(lowerCaseQuery);
                boolean matchesRole = user.get("role") != null &&
                        user.get("role").toString().toLowerCase().contains(lowerCaseQuery);

                if (matchesName || matchesEmail || matchesRole) {
                    filteredList.add(user);
                    filteredUserIds.add(userIds.get(i));
                }
            }
        }

        // After filtering, ensure the list is sorted and update the adapter.
        if(currentSortMode != 0) {
            sortUsers();
        } else {
            usersRecycler.setAdapter(new UserAdapter(filteredList, filteredUserIds));
        }
    }
}
