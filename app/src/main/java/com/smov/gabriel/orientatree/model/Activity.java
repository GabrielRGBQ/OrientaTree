package com.smov.gabriel.orientatree.model;

import java.sql.Timestamp;
import java.util.Date;

public class Activity {

    private String id;
    private String title;
    private String type;
    private String color;

    private Date startTime;
    private Date finishTime;

    public Activity() {

    }

    public Activity(String id, String title, String type, String color,
                    Date startTime, Date finishTime) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.color = color;
        this.startTime = startTime;
        this.finishTime = finishTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date date) {
        this.startTime = date;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public String getResume() {
        return "Lorem ipsum dolor sit amet consectetur adipiscing elit enim laoreet suscipit, urna...";
    }
}
