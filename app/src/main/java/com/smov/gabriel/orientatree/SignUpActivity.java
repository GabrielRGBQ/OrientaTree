package com.smov.gabriel.orientatree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smov.gabriel.orientatree.model.User;

public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout name_textfield, surname_textfield, email_textfield, password_textfield;
    private Button signUp_button;
    private CircularProgressIndicator progress_circular;
    private RadioGroup radioGroup;
    private RadioButton selectedRadioButton;

    private ConstraintLayout signUp_layout;

    private String name, surname, email, password, role;
    private String userID;
    private int selectedRadioButtonId;

    private FirebaseAuth mAuth;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            updateUIHome();
        }

        name_textfield = findViewById(R.id.name_textfield);
        surname_textfield = findViewById(R.id.surname_textfield);
        email_textfield = findViewById(R.id.email_textfield);
        password_textfield = findViewById(R.id.password_textfield);
        signUp_button = findViewById(R.id.signUp_button);
        progress_circular = findViewById(R.id.progress_circular);
        radioGroup = findViewById(R.id.radioGroup);

        signUp_layout = findViewById(R.id.signUp_layout);

        signUp_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = name_textfield.getEditText().getText().toString().trim();
                surname = surname_textfield.getEditText().getText().toString().trim();
                email = email_textfield.getEditText().getText().toString().trim();
                password = password_textfield.getEditText().getText().toString().trim();
                boolean any_error = false;
                if (TextUtils.isEmpty(name)) {
                    name_textfield.setError("nombre obligatorio");
                    any_error = true;
                } else {
                    if (name_textfield.isErrorEnabled()) {
                        name_textfield.setErrorEnabled(false);
                    }
                }
                if (TextUtils.isEmpty(surname)) {
                    surname_textfield.setError("apellidos obligatorios");
                    any_error = true;
                } else {
                    if (surname_textfield.isErrorEnabled()) {
                        surname_textfield.setErrorEnabled(false);
                    }
                }
                if (TextUtils.isEmpty(email)) {
                    email_textfield.setError("e-mail obligatorio");
                    any_error = true;
                } else {
                    if (email_textfield.isErrorEnabled()) {
                        email_textfield.setErrorEnabled(false);
                    }
                }
                /*if(TextUtils.isEmpty(email_confirmation)) {
                    email_confirmation_textfield.setError("confirmación e-mail obligatoria");
                    any_error = true;
                } else if (!email_confirmation.equals(email)) {
                    email_confirmation_textfield.setError("e-mail introducido no coincide");
                    any_error = true;
                } else {
                    if(email_confirmation_textfield.isErrorEnabled()) {
                        email_confirmation_textfield.setErrorEnabled(false);
                    }
                }*/
                if (TextUtils.isEmpty(password)) {
                    password_textfield.setError("password obligatorio");
                    any_error = true;
                } else {
                    if (password_textfield.isErrorEnabled()) {
                        password_textfield.setErrorEnabled(false);
                    }
                }
                if (password.length() < 6) {
                    password_textfield.setError("Al menos 6 caracteres");
                    any_error = true;
                } else {
                    if (password_textfield.isErrorEnabled()) {
                        password_textfield.setErrorEnabled(false);
                    }
                }
                selectedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                if (selectedRadioButtonId != -1) {
                    selectedRadioButton = findViewById(selectedRadioButtonId);
                    role = selectedRadioButton.getText().toString();
                } else {
                    any_error = true;
                    showSnackBar();
                }
                if (any_error == true) {
                    return;
                }
                progress_circular.setVisibility(View.VISIBLE);
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    updateUserData();
                                } else {
                                    progress_circular.setVisibility(View.GONE);
                                    try {
                                        throw task.getException();
                                    } catch (FirebaseAuthWeakPasswordException e) {
                                        password_textfield.setError("Password demasiado débil");
                                        password_textfield.setErrorEnabled(true);
                                    } catch (FirebaseAuthInvalidCredentialsException e) {
                                        email_textfield.setErrorEnabled(true);
                                        email_textfield.setError("El e-mail no es valido");
                                    } catch (FirebaseAuthUserCollisionException e) {
                                        email_textfield.setErrorEnabled(true);
                                        email_textfield.setError("Ya existe ese usuario");
                                    } catch (Exception e) {
                                        Toast.makeText(SignUpActivity.this, "Algo salió mal: " + task.getException().toString(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
            }
        });
    }

    private void updateUserData() {
        userID = mAuth.getCurrentUser().getUid();
        // Adds document to the users colletion. If that collection does not exist, it creates it
        DocumentReference documentReference = db.collection("users").document(userID);
        /*Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("surname", surname);
        user.put("email", email);
        user.put("role", selectedRbText);*/
        User user = new User(name, surname, email, role);
        documentReference
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progress_circular.setVisibility(View.GONE);
                        updateUIHome();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // TODO: handle this exception in a better way
                        Toast.makeText(SignUpActivity.this, "Algo salio mal: " + e.toString(), Toast.LENGTH_SHORT).show();
                        progress_circular.setVisibility(View.GONE);
                        updateUIHome();
                    }
                });
    }

    private void updateUIHome() {
        Intent intent = new Intent(SignUpActivity.this, WelcomeActivity.class);
        Bundle b = new Bundle();
        b.putString("name", name);
        b.putInt("previousActivity", 0); // flag to signal in next activity wether we come from log-in or sign-up
        intent.putExtras(b);
        IdentificationActivity.iAct.finish(); // finish IdentificationActivity
        startActivity(intent);
        finish();
    }

    private void showSnackBar() {
        Snackbar.make(signUp_layout, "Seleccionar Estudiante o Docente", Snackbar.LENGTH_LONG)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                })
                .setDuration(8000)
                .show();
    }
}