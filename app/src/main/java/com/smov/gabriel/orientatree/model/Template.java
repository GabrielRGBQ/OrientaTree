package com.smov.gabriel.orientatree.model;

import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;
import java.util.ArrayList;

public class Template implements Serializable {

    private String template_id;
    private String name;
    private TemplateType type;
    private TemplateColor color;
    private String location;
    private String description;
    private String norms_id;
    private String map_id;

    private ArrayList<String> beacons;

    public Template() {

    }

    public Template(String template_id, String name, TemplateType type, TemplateColor color, String location,
                    String description, String norms_id, String map_id) {
        this.template_id = template_id;
        this.name = name;
        this.type = type;
        this.color = color;
        this.location = location;
        this.description = description;
        this.norms_id = norms_id;
        beacons = new ArrayList<>();
        this.map_id = map_id;
    }

    public TemplateType getType() {
        return type;
    }

    public void setType(TemplateType type) {
        this.type = type;
    }

    public TemplateColor getColor() {
        return color;
    }

    public void setColor(TemplateColor color) {
        this.color = color;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNorms_id() {
        return norms_id;
    }

    public void setNorms_id(String norms_id) {
        this.norms_id = norms_id;
    }

    public ArrayList<String> getBeacons() {
        return beacons;
    }

    public void setBeacons(ArrayList<String> beacons) {
        this.beacons = beacons;
    }

    public String getTemplate_id() {
        return template_id;
    }

    public void setTemplate_id(String template_id) {
        this.template_id = template_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMap_id() {
        return map_id;
    }

    public void setMap_id(String map_id) {
        this.map_id = map_id;
    }
}
