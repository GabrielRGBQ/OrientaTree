package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.R;
import com.smov.gabriel.orientatree.model.Template;

import java.util.ArrayList;
import java.util.List;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.MyViewHolder> implements Filterable {

    private Context context;
    private ArrayList<Template> templates;
    private ArrayList<Template> templates_full; // needed to restore when we relax search filters
    private int position;

    public TemplateAdapter(Context context, ArrayList<Template> templates) {
        this.context = context;
        this.templates = templates;
        this.templates_full = new ArrayList<>(templates); // this is a COPY, it does not point to the exact same data, but to a copy
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
        holder.subtitleTemplate_textview.setText("Balizas: 4");
        
        holder.row_template_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Card pulsada: " + position, Toast.LENGTH_SHORT).show();
            }
        });

        if(template.getColor() != null) {
            switch (template.getColor()) {
                case "Naranja":
                    holder.typeTemplate_textview.setTextColor(Color.parseColor("#FFA233"));
                    break;
                case "Roja":
                    holder.typeTemplate_textview.setTextColor(Color.parseColor("#E32A10"));
                    break;
                default:
                    break;
            }
        }

        StorageReference ref = holder.storageReference.child("templateImages/" + template.getName_id() + ".jpg");
        Glide.with(context)
                .load(ref)
                .diskCacheStrategy(DiskCacheStrategy.NONE ) // prevent caching
                .skipMemoryCache(true) // prevent caching
                .into(holder.row_template_imageview);

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

    // Code needed to perform realtime searches:
    @Override
    public Filter getFilter() {
        return templateFilter;
    }

   private Filter templateFilter = new Filter() {
       @Override
       protected FilterResults performFiltering(CharSequence constraint) {
           List<Template> filteredList = new ArrayList<>();
           if(constraint == null || constraint.length() == 0) {
               filteredList.addAll(templates_full);
           } else {
               String filterPattern = constraint.toString().toLowerCase().trim();
               for(Template template : templates_full) {
                   if(template.getName_id().toLowerCase().contains(filterPattern)) {
                       filteredList.add(template);
                   }
               }
           }
           FilterResults filterResults = new FilterResults();
           filterResults.values = filteredList;
           return filterResults;
       }

       @Override
       protected void publishResults(CharSequence constraint, FilterResults results) {
            templates.clear();
            templates.addAll((List)results.values);
            notifyDataSetChanged();
       }
   };

}
