package com.smov.gabriel.orientatree.model;

import com.google.firebase.firestore.GeoPoint;

import java.util.Comparator;

public class Beacon implements Comparator<Beacon> {

    private GeoPoint location;
    private int number;
    private String name;
    private String beacon_id;

    public Beacon() {

    }

    public Beacon(String beacon_id, GeoPoint location, int number, String name) {
        this.beacon_id = beacon_id;
        this.location = location;
        this.number = number;
        this.name = name;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBeacon_id() {
        return beacon_id;
    }

    public void setBeacon_id(String beacon_id) {
        this.beacon_id = beacon_id;
    }

    @Override
    public int compare(Beacon o1, Beacon o2) {
        return o1.getNumber() - o2.getNumber();
    }
}