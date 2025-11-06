package com.example.matchinghandapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class Home_volunteer extends AppCompatActivity {

    private GridLayout iconGrid;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_volunteer);

        iconGrid = findViewById(R.id.iconGrid);
        mAuth = FirebaseAuth.getInstance();

        setDashboardClicks();

        // Logout button
        LinearLayout logoutCard = findViewById(R.id.logoutCard);
        logoutCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                Toast.makeText(Home_volunteer.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Home_volunteer.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setDashboardClicks() {
        for (int i = 0; i < iconGrid.getChildCount(); i++) {
            LinearLayout card = (LinearLayout) iconGrid.getChildAt(i);
            final int index = i;

            card.setOnClickListener(v -> {
                switch (index) {
                    case 0:
                        Toast.makeText(Home_volunteer.this, "Opening Discover", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(Home_volunteer.this, DiscoverActivity.class));
                        break;

                    case 1:
                        Toast.makeText(Home_volunteer.this, "Opening Applications", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(Home_volunteer.this, MyApplicationsActivity.class));
                        break;

                    case 2:
                        Toast.makeText(Home_volunteer.this, "Opening Messages", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(Home_volunteer.this, ChatListActivity.class));
                        break;

                    case 3:
                        Toast.makeText(Home_volunteer.this, "Opening Saved", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(Home_volunteer.this, SavedActivity.class));
                        break;

                    case 4:
                        Toast.makeText(Home_volunteer.this, "Opening Profile", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(Home_volunteer.this, ProfileActivity.class));
                        break;

                    default:
                        Toast.makeText(Home_volunteer.this, "Feature coming soon!", Toast.LENGTH_SHORT).show();
                        break;
                }
            });
        }
    }
}
