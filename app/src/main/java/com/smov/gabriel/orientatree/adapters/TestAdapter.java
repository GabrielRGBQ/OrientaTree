package com.smov.gabriel.orientatree.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.Test;

import java.util.ArrayList;

public class TestAdapter extends RecyclerView.Adapter<TestAdapter.MyViewHolder> {

    private Context context;

    private ArrayList<Test> tests;
    private int position;

    public TestAdapter(Context context, ArrayList<Test> tests) {
        this.context = context;
        this.tests = tests;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_activity, parent, false);
        return new MyViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        this.position = position ;
        Test test = tests.get(position);

        holder.title_textView.setText(test.getTitle());
        holder.subtitle_textView.setText(test.getType());
        holder.resume_textView.setText(test.getResume());

        if(test.getColor() != null) {
            switch (test.getColor()) {
                case "Naranja":
                    holder.color_textView.setText("naranja");
                    holder.color_textView.setTextColor(Color.parseColor("#FFA233"));
                    break;
                case "Morado":
                    holder.color_textView.setText("morada");
                    holder.color_textView.setTextColor(Color.parseColor("#760EC3"));
                    break;
                default:
                    break;
            }
        }

        // get and set the activity picture
        StorageReference ref = holder.storageReference.child("activityImages/" + test.getId() + ".jpg");
        Glide.with(context)
                .load(ref)
                .diskCacheStrategy(DiskCacheStrategy.NONE ) // prevent caching
                .skipMemoryCache(true) // prevent caching
                .into(holder.rowImage_imageView);
    }

    @Override
    public int getItemCount() {
        return tests.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        FirebaseStorage storage;
        StorageReference storageReference;

        LinearLayout row_activity_layout; // not sure if needed
        TextView title_textView, subtitle_textView, resume_textView, color_textView;
        ImageView rowImage_imageView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            title_textView = itemView.findViewById(R.id.title_textView);
            subtitle_textView = itemView.findViewById(R.id.subtitle_textView);
            resume_textView = itemView.findViewById(R.id.resume_textView);
            color_textView = itemView.findViewById(R.id.color_textView);
            row_activity_layout = itemView.findViewById(R.id.row_activity_layout);
            rowImage_imageView = itemView.findViewById(R.id.rowImage_imageView);

            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();
        }
    }
}
