User.java
package com.example;

public class User {
    private String id;
    private String name;
    private String password;

    public int getAge() {
        return 18;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}