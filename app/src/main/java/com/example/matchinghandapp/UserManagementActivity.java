package com.example.matchinghandapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserManagementActivity extends AppCompatActivity {

    private RecyclerView usersRecycler;
    private FirebaseFirestore firestore;
    private List<Map<String, Object>> userList;
    private List<String> userIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        usersRecycler = findViewById(R.id.usersRecycler);
        usersRecycler.setLayoutManager(new LinearLayoutManager(this));

        firestore = FirebaseFirestore.getInstance();
        userList = new ArrayList<>();
        userIds = new ArrayList<>();

        loadUsers();
    }

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

                        usersRecycler.setAdapter(new UserAdapter(userList, userIds));

                    } else {
                        Toast.makeText(UserManagementActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(UserManagementActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
