package com.smov.gabriel.orientatree;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

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

    private Toolbar toolbar;
    private ActionBar ab;

    private RecyclerView template_recyclerview;
    private TemplateAdapter templateAdapter;
    private ArrayList<Template> templates;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_template);

        db = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.new_activity_toolbar);
        setSupportActionBar(toolbar);
        //ab = getSupportActionBar();
        //ab.setHomeAsUpIndicator(R.drawable.ic_close);
        //ab.setDisplayHomeAsUpEnabled(true);

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
                        templateAdapter = new TemplateAdapter(FindTemplate.this,FindTemplate.this, templates);
                        template_recyclerview = findViewById(R.id.template_recyclerview);
                        template_recyclerview.setAdapter(templateAdapter);
                        template_recyclerview.setLayoutManager(new GridLayoutManager(FindTemplate.this, 2));
                    }
                });
    }

    // show the search menu and perform its logic
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView)searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                templateAdapter.getFilter().filter(newText);
                return false;
            }
        });
        return true;
    }
}