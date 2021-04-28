package com.smov.gabriel.orientatree.model;

public class Template {

    private String name_id;
    private String type;
    private String color;

    public Template() {

    }

    public Template(String name_id, String type, String color) {
        this.name_id = name_id;
        this.type = type;
        this.color = color;
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
}
