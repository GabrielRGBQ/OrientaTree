package com.smov.gabriel.orientatree.model;

import java.sql.Timestamp;
import java.util.Date;

public class Activity {

    private String id;
    private String title;
    private String template;

    private Date startTime;
    private Date finishTime;

    public Activity() {

    }

    public Activity(String id, String title, String template,
                    Date startTime, Date finishTime) {
        this.id = id;
        this.title = title;
        this.template = template;
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

    public String getTemplate() {
        return template;
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
}
