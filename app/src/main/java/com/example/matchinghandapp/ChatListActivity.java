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
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView userRecyclerView;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private List<UserModel> userList = new ArrayList<>();
    private UserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        userRecyclerView = findViewById(R.id.userRecyclerView);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(userList, this::openChat);
        userRecyclerView.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        String currentUserId = auth.getCurrentUser().getUid();

        firestore.collection("Users").get()
                .addOnSuccessListener(querySnapshot -> {
                    userList.clear();
                    for (DocumentSnapshot snapshot : querySnapshot) {
                        UserModel user = snapshot.toObject(UserModel.class);
                        if (user != null && !snapshot.getId().equals(currentUserId)) {
                            user.setUserId(snapshot.getId());
                            userList.add(user);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openChat(UserModel user) {
        Intent intent = new Intent(this, MessagesActivity.class);
        intent.putExtra("chatPartnerId", user.getUserId());
        startActivity(intent);
    }

    // --- Adapter ---
    static class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private final List<UserModel> userList;
        private final OnUserClickListener listener;

        interface OnUserClickListener {
            void onUserClick(UserModel user);
        }

        UserAdapter(List<UserModel> userList, OnUserClickListener listener) {
            this.userList = userList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            UserModel user = userList.get(position);
            holder.tvName.setText(user.getName());
            holder.tvEmail.setText(user.getEmail());

            if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                Picasso.get().load(user.getProfileImage()).into(holder.imgProfile);
            } else {
                holder.imgProfile.setImageResource(R.drawable.ic_baseline_person_24);
            }

            holder.itemView.setOnClickListener(v -> listener.onUserClick(user));
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        static class UserViewHolder extends RecyclerView.ViewHolder {
            ImageView imgProfile;
            TextView tvName, tvEmail;

            UserViewHolder(@NonNull View itemView) {
                super(itemView);
                imgProfile = itemView.findViewById(R.id.imgProfile);
                tvName = itemView.findViewById(R.id.tvName);
                tvEmail = itemView.findViewById(R.id.tvEmail);
            }
        }
    }
}
