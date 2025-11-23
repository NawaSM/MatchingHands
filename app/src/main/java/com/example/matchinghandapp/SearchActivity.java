package com.example.matchinghandapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// This activity is for the search functionality (currently a placeholder).
public class SearchActivity extends AppCompatActivity {

    // Called when the activity is first created.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable edge-to-edge display.
        EdgeToEdge.enable(this);
        // Set the layout for this activity.
        setContentView(R.layout.activity_search);
        // Set a listener to handle window insets for system bars.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // Get the insets for the system bars (status bar, navigation bar).
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply the insets as padding to the main view to avoid overlapping with system UI.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            // Return the insets to be consumed.
            return insets;
        });
    }
}
