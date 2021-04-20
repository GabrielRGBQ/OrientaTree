package com.smov.gabriel.orientatree.model;

public class User {
    private String name;
    private String surname;
    private String email;
    private  String role;

    public User() {
    }

    public User(String name, String surname, String email, String role) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
