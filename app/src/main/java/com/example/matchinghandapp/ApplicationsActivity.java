package com.example.matchinghandapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// This activity is likely a placeholder for displaying a user's applications.
public class ApplicationsActivity extends AppCompatActivity {

    // Called when the activity is first created.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable edge-to-edge display.
        EdgeToEdge.enable(this);
        // Set the layout for this activity.
        setContentView(R.layout.activity_applications);
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
