package com.example.matchinghandapp;

import androidx.annotation.NonNull;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private EditText email, password;
    private Button loginBtn;
    private TextView registerText, passwordBText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // ✅ Initialize progress dialog before any Firebase check
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading");
        progressDialog.setMessage("Checking your account...");
        progressDialog.setCancelable(false);

        // ✅ Check if already logged in before showing layout
        if (mAuth.getCurrentUser() != null) {
            progressDialog.show();
            redirectToHome();
            return;
        }

        // Not logged in → show login layout
        setContentView(R.layout.activity_main);

        // Initialize UI components
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        registerText = findViewById(R.id.registerText);
        passwordBText = findViewById(R.id.forgotPassword);

        // Setup Progress Dialog for login
        progressDialog.setTitle("Logging In");
        progressDialog.setMessage("Please wait...");

        // Login Button
        loginBtn.setOnClickListener(view -> loginUser());

        // Register & Forgot Password
        registerText.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, RegisterActivity.class)));

        passwordBText.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, ForgetPasswordActivity.class)));
    }

    private void loginUser() {
        String userEmail = email.getText().toString().trim();
        String userPass = password.getText().toString().trim();

        // Input validation
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

        progressDialog.show();

        // Firebase Authentication
        mAuth.signInWithEmailAndPassword(userEmail, userPass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        redirectToHome();
                    } else {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";
                        Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ✅ Redirect based on role
    private void redirectToHome() {
        if (mAuth.getCurrentUser() == null) {
            if (progressDialog.isShowing()) progressDialog.dismiss();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        firestore.collection("Users").document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }

                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot snapshot = task.getResult();
                        String role = snapshot.getString("role");

                        if (role == null) {
                            Toast.makeText(MainActivity.this, "User role not found!", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                            return;
                        }

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

                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "User data not found!", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // No action needed here anymore since we handle redirect in onCreate
    }
}
