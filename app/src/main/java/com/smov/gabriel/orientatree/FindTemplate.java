package com.smov.gabriel.orientatree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.adapters.TemplateAdapter;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Template;

import java.util.ArrayList;

public class FindTemplate extends AppCompatActivity {

    private RecyclerView template_recyclerview;
    private TemplateAdapter templateAdapter;
    private ArrayList<Template> templates;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_template);

        db = FirebaseFirestore.getInstance();

        templates = new ArrayList<>();

        db.collection("templates")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Template template = document.toObject(Template.class);
                            templates.add(template);
                        }
                        templateAdapter = new TemplateAdapter(FindTemplate.this, templates);
                        template_recyclerview = findViewById(R.id.template_recyclerview);
                        template_recyclerview.setAdapter(templateAdapter);
                        template_recyclerview.setLayoutManager(new LinearLayoutManager(FindTemplate.this));
                    }
                });
    }
}