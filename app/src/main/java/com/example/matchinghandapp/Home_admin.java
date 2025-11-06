package com.example.matchinghandapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class Home_admin extends AppCompatActivity {

    private LinearLayout userManagementCard, profileCard, logoutCard,messagesCard;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_admin);

        mAuth = FirebaseAuth.getInstance();

        userManagementCard = findViewById(R.id.userManagementCard);
        profileCard = findViewById(R.id.profileCard);
        logoutCard = findViewById(R.id.logoutCard);
        messagesCard = findViewById(R.id.messagesCard);
        userManagementCard.setOnClickListener(v -> {
            Toast.makeText(this, "Opening User Management", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, UserManagementActivity.class));
        });
        messagesCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Replace with your NGO Application Management activity
                Intent intent = new Intent(Home_admin.this, ChatListActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
        profileCard.setOnClickListener(v -> {
            Toast.makeText(this, "Opening Profile", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ProfileActivity.class));
        });

        logoutCard.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Home_admin.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
