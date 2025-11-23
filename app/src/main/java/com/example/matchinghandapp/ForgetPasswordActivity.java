package com.example.matchinghandapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

// This activity handles the password reset process.
public class ForgetPasswordActivity extends AppCompatActivity {

    // UI elements.
    private EditText emailInput;
    private Button resetBtn;

    // Firebase services.
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog; // Shows loading progress.

    // Called when the activity is first created.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        // Initialize Firebase Auth and the progress dialog.
        mAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending reset link...");
        progressDialog.setCancelable(false);

        // Initialize UI components.
        emailInput = findViewById(R.id.emailInput);
        resetBtn = findViewById(R.id.resetBtn);

        // Set a click listener for the reset button.
        resetBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();

            // Validate the email input.
            if (email.isEmpty()) {
                emailInput.setError("Please enter your email");
                emailInput.requestFocus();
                return;
            }

            progressDialog.show(); // Show loading indicator.

            // Send a password reset email using Firebase Authentication.
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            Toast.makeText(this,
                                    "Password reset link sent to your email.",
                                    Toast.LENGTH_LONG).show();
                            finish(); // Close the activity.
                        } else {
                            // Handle errors.
                            Toast.makeText(this,
                                    "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
