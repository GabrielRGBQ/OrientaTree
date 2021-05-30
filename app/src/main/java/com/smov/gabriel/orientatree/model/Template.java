package com.smov.gabriel.orientatree.model;

import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;
import java.util.ArrayList;

public class Template implements Serializable {

    private String name_id;
    private String type;
    private String color;
    private String location;
    private String description;
    private String norms_id;

    private ArrayList<String> beacons;

    public Template() {

    }

    public Template(String name_id, String type, String color, String location,
                    String description, String norms_id) {
        this.name_id = name_id;
        this.type = type;
        this.color = color;
        this.location = location;
        this.description = description;
        this.norms_id = norms_id;
        beacons = new ArrayList<>();
    }

    public String getName_id() {
        return name_id;
    }

    public void setName_id(String name_id) {
        this.name_id = name_id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
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
}
