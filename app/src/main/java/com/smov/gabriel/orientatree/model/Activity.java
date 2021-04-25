package com.smov.gabriel.orientatree.model;

public class Activity {

    // general attributes
    private String activity_id;
    private String title;
    private String type;
    private String organizer;

    // date attributes
    private int year;
    private int month;
    private int day;

    // start time attributes
    private int s_hour;
    private int s_minute;

    // finish time attributes
    private int e_hour;
    private int e_minute;

    public Activity () {
    }

    public Activity(String activity_id, String title, String type, String organizer, int year, int month, int day, int s_hour, int s_minute, int e_hour, int e_minute) {
        this.activity_id = activity_id;
        this.title = title;
        this.type = type;
        this.organizer = organizer;
        this.year = year;
        this.month = month;
        this.day = day;
        this.s_hour = s_hour;
        this.s_minute = s_minute;
        this.e_hour = e_hour;
        this.e_minute = e_minute;
    }

    public String getActivity_id() {
        return activity_id;
    }

    public void setActivity_id(String activity_id) {
        this.activity_id = activity_id;
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

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getS_hour() {
        return s_hour;
    }

    public void setS_hour(int s_hour) {
        this.s_hour = s_hour;
    }

    public int getS_minute() {
        return s_minute;
    }

    public void setS_minute(int s_minute) {
        this.s_minute = s_minute;
    }

    public int getE_hour() {
        return e_hour;
    }

    public void setE_hour(int e_hour) {
        this.e_hour = e_hour;
    }

    public int getE_minute() {
        return e_minute;
    }

    public void setE_minute(int e_minute) {
        this.e_minute = e_minute;
    }
}
