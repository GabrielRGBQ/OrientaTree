package com.smov.gabriel.orientatree.model;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Date;

public class Activity implements Comparator<Activity> {

    private String id;
    private String visible_id;
    private String key;
    private String title;
    private String template;
    private String planner_id;

    private Date startTime;
    private Date finishTime;

    public Activity() {

    }

    public Activity(String id, String key, String title, String template, String planner_id,
                    Date startTime, Date finishTime) {
        this.id = id;
        this.visible_id = id.substring(0, Math.min(id.length(), 8));
        this.key = key;
        this.title = title;
        this.template = template;
        this.planner_id = planner_id;
        this.startTime = startTime;
        this.finishTime = finishTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVisible_id() {
        return visible_id;
    }

    public void setVisible_id(String visible_id) {
        this.visible_id = visible_id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTemplate() {
        return template;
    }

    public String getPlanner_id() {
        return planner_id;
    }

    public void setPlanner_id(String planner_id) {
        this.planner_id = planner_id;
    }

    public void setTemplate(String template) {
        this.template = template;
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

    @Override
    public int compare(Activity o1, Activity o2) {
        return o1.getStartTime().compareTo(o2.startTime);
    }
}
