package com.example.matchinghandapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

// This activity handles new user registration.
public class RegisterActivity extends AppCompatActivity {

    // UI elements for the registration form.
    private EditText email, password, phone, address;
    private Button registerBtn;

    // Firebase services.
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private ProgressDialog progressDialog; // Shows registration progress.

    // Called when the activity is first created.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase services.
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI components.
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        phone = findViewById(R.id.phone);
        address = findViewById(R.id.address);
        registerBtn = findViewById(R.id.registerBtn);

        // Initialize the progress dialog.
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Registering User");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        // Set a click listener for the register button.
        registerBtn.setOnClickListener(v -> createUser());
    }

    // Creates a new user account.
    private void createUser() {
        // Get user input from the form.
        String userEmail = email.getText().toString().trim();
        String userPass = password.getText().toString().trim();
        String userPhone = phone.getText().toString().trim();
        String userAddress = address.getText().toString().trim();

        // Validate user input.
        if (TextUtils.isEmpty(userEmail)) {
            email.setError("Email is required");
            email.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            email.setError("Enter a valid email");
            email.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(userPass)) {
            password.setError("Password is required");
            password.requestFocus();
            return;
        }
        if (userPass.length() < 6) {
            password.setError("Password must be at least 6 characters");
            password.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(userPhone)) {
            phone.setError("Phone number is required");
            phone.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(userAddress)) {
            address.setError("Address is required");
            address.requestFocus();
            return;
        }

        progressDialog.show(); // Show loading indicator.

        // Create a new user in Firebase Authentication.
        mAuth.createUserWithEmailAndPassword(userEmail, userPass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Send a verification email to the new user.
                        user.sendEmailVerification()
                                .addOnCompleteListener(verifyTask -> {
                                    if (verifyTask.isSuccessful()) {
                                        String uid = user.getUid();

                                        // Prepare user data to be stored in Firestore.
                                        HashMap<String, Object> userMap = new HashMap<>();
                                        userMap.put("email", userEmail);
                                        userMap.put("phone", userPhone);
                                        userMap.put("address", userAddress);
                                        userMap.put("role", "volunteer"); // Default role.
                                        userMap.put("uid", uid);
                                        userMap.put("emailVerified", false);
                                        userMap.put("createdAt", System.currentTimeMillis());
                                        userMap.put("active", true); // Set new users as active by default.

                                        // Store the user data in a new Firestore document.
                                        firestore.collection("Users").document(uid)
                                                .set(userMap)
                                                .addOnCompleteListener(saveTask -> {
                                                    progressDialog.dismiss();

                                                    if (saveTask.isSuccessful()) {
                                                        mAuth.signOut(); // Sign out until email is verified.

                                                        // Show a dialog informing the user to verify their email.
                                                        new AlertDialog.Builder(this)
                                                                .setTitle("Verify Your Email")
                                                                .setMessage("A verification email has been sent to " + userEmail +
                                                                        ". Please verify your email before logging in.")
                                                                .setPositiveButton("OK", (dialog, which) -> {
                                                                    finish(); // Return to the login screen.
                                                                })
                                                                .setCancelable(false)
                                                                .show();
                                                    } else {
                                                        Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } else {
                                        progressDialog.dismiss();
                                        Toast.makeText(this, "Failed to send verification email", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        // Handle registration errors.
                        progressDialog.dismiss();
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
