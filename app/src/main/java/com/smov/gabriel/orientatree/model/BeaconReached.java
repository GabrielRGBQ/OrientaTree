package com.smov.gabriel.orientatree.model;

import java.util.Date;

public class BeaconReached {

    private Date reachMoment;
    private String beacon_id;

    public BeaconReached () {

    }

    public BeaconReached(Date reachMoment, String beacon_id) {
        this.reachMoment = reachMoment;
        this.beacon_id = beacon_id;
    }

    public Date getReachMoment() {
        return reachMoment;
    }

    public void setReachMoment(Date reachMoment) {
        this.reachMoment = reachMoment;
    }

    public String getBeacon_id() {
        return beacon_id;
    }

    public void setBeacon_id(String beacon_id) {
        this.beacon_id = beacon_id;
    }
}
