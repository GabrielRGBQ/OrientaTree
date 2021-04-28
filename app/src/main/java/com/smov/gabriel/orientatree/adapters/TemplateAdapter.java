package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Template;

import java.util.ArrayList;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.MyViewHolder> {

    private Context context;
    private ArrayList<Template> templates;
    private int position;

    public TemplateAdapter(Context context, ArrayList<Template> templates) {
        this.context = context;
        this.templates = templates;
    }

    @NonNull
    @Override
    public TemplateAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_template, parent, false);
        return new TemplateAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TemplateAdapter.MyViewHolder holder, int position) {

        this.position = position ;
        Template template = templates.get(position);

        holder.typeTemplate_textview.setText(template.getType());
        holder.titleTemplate_textview.setText(template.getName_id());
        holder.subtitleTemplate_textview.setText(template.getColor());

    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        FirebaseStorage storage;
        StorageReference storageReference;

        LinearLayout row_template_layout;
        TextView typeTemplate_textview, titleTemplate_textview, subtitleTemplate_textview;
        ImageView row_template_imageview;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            row_template_layout = itemView.findViewById(R.id.row_activity_template);
            typeTemplate_textview = itemView.findViewById(R.id.typeTemplate_textview);
            titleTemplate_textview = itemView.findViewById(R.id.titleTemplate_textview);
            subtitleTemplate_textview = itemView.findViewById(R.id.subtitleTemplate_textview);
            row_template_imageview = itemView.findViewById(R.id.row_template_imageView);

            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();
        }
    }

}
