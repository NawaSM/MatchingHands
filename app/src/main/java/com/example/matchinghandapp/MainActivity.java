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
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

// The main entry point of the app, handling user login.
public class MainActivity extends AppCompatActivity {

    // UI elements for the login screen.
    private EditText email, password;
    private Button loginBtn;
    private TextView registerText, passwordBText;

    // Firebase services.
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private ProgressDialog progressDialog; // Shows loading progress.

    // Called when the activity is first created.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase services.
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize the progress dialog.
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading");
        progressDialog.setMessage("Checking your account...");
        progressDialog.setCancelable(false);

        // If a user is already logged in, check their status before redirecting.
        if (mAuth.getCurrentUser() != null) {
            progressDialog.show();
            checkUserStatusAndRedirect();
            return;
        }

        // If no user is logged in, show the login layout.
        setContentView(R.layout.activity_main);

        // Initialize UI components.
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        registerText = findViewById(R.id.registerText);
        passwordBText = findViewById(R.id.forgotPassword);

        // Configure the progress dialog for the login process.
        progressDialog.setTitle("Logging In");
        progressDialog.setMessage("Please wait...");

        // Set click listeners.
        loginBtn.setOnClickListener(view -> loginUser());
        registerText.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, RegisterActivity.class)));
        passwordBText.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, ForgetPasswordActivity.class)));
    }

    // Attempts to log in the user with the provided credentials.
    private void loginUser() {
        String userEmail = email.getText().toString().trim();
        String userPass = password.getText().toString().trim();

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

        progressDialog.show(); // Show loading indicator.

        // Sign in with Firebase Authentication.
        mAuth.signInWithEmailAndPassword(userEmail, userPass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        // Check if email is verified before proceeding.
                        if (user != null && user.isEmailVerified()) {
                            checkUserStatusAndRedirect();
                        } else {
                            // If email is not verified, show a dialog and sign the user out.
                            progressDialog.dismiss();
                            mAuth.signOut();
                            showEmailVerificationDialog(user);
                        }
                    } else {
                        // Handle login failures.
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";
                        Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Shows a dialog to the user to verify their email address.
    private void showEmailVerificationDialog(FirebaseUser user) {
        new AlertDialog.Builder(this)
                .setTitle("Email Not Verified")
                .setMessage("Please verify your email before logging in. Check your inbox for the verification link.\n\nDidn't receive it?")
                .setPositiveButton("Resend", (dialog, which) -> {
                    // Resend the verification email.
                    if (user != null) {
                        user.sendEmailVerification()
                                .addOnCompleteListener(resendTask -> {
                                    if (resendTask.isSuccessful()) {
                                        Toast.makeText(this, "Verification email sent!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(this, "Failed to resend email", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Checks the user's status (active/inactive) and then redirects them.
    private void checkUserStatusAndRedirect() {
        if (mAuth.getCurrentUser() == null) {
            if (progressDialog.isShowing()) progressDialog.dismiss();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        firestore.collection("Users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (progressDialog.isShowing()) progressDialog.dismiss();

                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot snapshot = task.getResult();

                        // Check if the user is active before allowing login.
                        Boolean isActive = snapshot.getBoolean("active");
                        if (isActive != null && !isActive) {
                            mAuth.signOut();
                            Toast.makeText(MainActivity.this, "Your account has been deactivated.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // User is active, proceed to redirect.
                        redirectToHome(snapshot);
                    } else {
                        // Handle cases where user data is not found.
                        mAuth.signOut();
                        Toast.makeText(MainActivity.this, "User data not found!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Redirects the user to the correct home screen based on their role.
    private void redirectToHome(DocumentSnapshot snapshot) {
        String role = snapshot.getString("role");

        if (role == null) {
            Toast.makeText(MainActivity.this, "User role not found!", Toast.LENGTH_SHORT).show();
            mAuth.signOut();
            return;
        }

        // Determine the correct home activity based on the user's role.
        Intent intent;
        switch (role.toLowerCase()) {
            case "admin":
                intent = new Intent(MainActivity.this, Home_admin.class);
                break;
            case "ngo":
                intent = new Intent(MainActivity.this, Home_ngos.class);
                break;
            default:
                intent = new Intent(MainActivity.this, Home_volunteer.class);
                break;
        }

        // Start the appropriate home activity and clear the task stack.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // The redirect logic is now handled in onCreate.
    }
}
