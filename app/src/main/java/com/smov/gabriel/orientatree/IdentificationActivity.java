package com.smov.gabriel.orientatree;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;

public class IdentificationActivity extends AppCompatActivity {

    private Button logIn_button, signIn_button;

    FirebaseAuth mAuth;

    public static Activity iAct; // needed to finish this activity from another activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identification);

        iAct = this;

        mAuth = FirebaseAuth.getInstance();

        if(mAuth.getCurrentUser() != null) {
            updateUIHome();
        }

        logIn_button = findViewById(R.id.logIn_button);
        signIn_button = findViewById(R.id.signIn_button);

        logIn_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(IdentificationActivity.this, LogInActivity.class);
                startActivity(intent);
            }
        });

        signIn_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(IdentificationActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    private void updateUIHome() {
        Intent intent = new Intent(IdentificationActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}