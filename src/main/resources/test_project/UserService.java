UserService.java
package com.example;

public class UserService {
    private User user;

    public User getUser(int id) {
        User user = new User();
        user.setPassword("123456");
        return user;
    }

    public void saveUser(User user) {
        this.user = user;
    }
}