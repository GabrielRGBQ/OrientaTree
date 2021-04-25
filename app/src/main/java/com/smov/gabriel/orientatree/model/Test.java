package com.smov.gabriel.orientatree.model;

public class Test {

    private String id;
    private String title;
    private String type;
    private String color;

    public Test () {

    }

    public Test(String id, String title, String type, String color) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.color = color;
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

    public String getResume() {
        return "Lorem ipsum dolor sit amet consectetur adipiscing elit enim laoreet suscipit, urna...";
    }
}
