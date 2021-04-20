package com.smov.gabriel.orientatree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smov.gabriel.orientatree.model.User;

public class LogInActivity extends AppCompatActivity {

    private TextInputLayout email_textfield, password_textfield;
    private Button logIn_button;
    private CircularProgressIndicator progress_circular;

    private ConstraintLayout logIn_layout;

    private String email, password;
    private String name;
    private String userID;

    private FirebaseAuth mAuth;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        mAuth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();

        email_textfield = findViewById(R.id.email_textfield);
        password_textfield = findViewById(R.id.password_textfield);
        logIn_button = findViewById(R.id.logIn_button);
        progress_circular = findViewById(R.id.progress_circular);

        logIn_layout = findViewById(R.id.logIn_layout);

        logIn_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email = email_textfield.getEditText().getText().toString().trim();
                password = password_textfield.getEditText().getText().toString().trim();
                boolean any_error = false;
                if(TextUtils.isEmpty(email)) {
                    email_textfield.setError("e-mail obligatorio");
                    any_error = true;
                } else {
                    if(email_textfield.isErrorEnabled()) {
                        email_textfield.setErrorEnabled(false);
                    }
                }
                if(TextUtils.isEmpty(password)) {
                    password_textfield.setError("password obligatorio");
                    any_error = true;
                } else {
                    if(password_textfield.isErrorEnabled()) {
                        password_textfield.setErrorEnabled(false);
                    }
                }
                if(any_error == true) {
                    return;
                }
                progress_circular.setVisibility(View.VISIBLE);
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            getUserName();
                        } else {
                            try {
                                throw task.getException();
                            }
                            catch (FirebaseAuthInvalidUserException | FirebaseAuthInvalidCredentialsException e) {
                                progress_circular.setVisibility(View.GONE);
                                showSnackBar();
                            }
                            catch (Exception e) {
                                progress_circular.setVisibility(View.GONE);
                                Toast.makeText(LogInActivity.this, "Algo no funcionó: " + task.getException().toString(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }
        });

    }

    private void getUserName() {
        userID = mAuth.getCurrentUser().getUid();
        DocumentReference docRef = db.collection("users").document(userID);
        /*docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                progress_circular.setVisibility(View.GONE);
                if(task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()) {
                        name = (String)document.get("name");
                        updateUIHome();
                    } else {
                        Toast.makeText(LogInActivity.this, "El documento no existe", Toast.LENGTH_SHORT).show();
                        // TODO: handle this error
                    }
                } else {
                    Toast.makeText(LogInActivity.this, "No se ha leido el documento", Toast.LENGTH_SHORT).show();
                    // TODO: handle this error
                }
            }
        });*/
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                progress_circular.setVisibility(View.GONE);
                User user = documentSnapshot.toObject(User.class);
                if(user != null) {
                    name = user.getName();
                    updateUIHome();
                } else {
                    Toast.makeText(LogInActivity.this, "No se obtuvieron los datos del usuario", Toast.LENGTH_SHORT).show();
                    name = "user"; // so that the next Activity does not show "null" or something
                    updateUIHome(); // we continue with next activity anyway
                }
            }
        });
    }

    private void showSnackBar() {
        Snackbar.make(logIn_layout, "Usuario o password incorrectos", Snackbar.LENGTH_LONG)
            .setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            })
             .setDuration(8000)
             .show();
    }

    private void updateUIHome() {
        Intent intent = new Intent(LogInActivity.this, WelcomeActivity.class);
        Bundle b = new Bundle();
        b.putString("name", name);
        b.putInt("previousActivity", 1); // flag to signal in next activity wether we come from log-in or sign-up
        intent.putExtras(b);
        IdentificationActivity.iAct.finish(); // finish IdentificationActivity
        startActivity(intent);
        finish();
    }
}