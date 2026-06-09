UserController.java
package com.example;

public class UserController {
    private UserService userService = new UserService();

    public void handleUser(User user) {
        String password = user.getPassword();
        userService.saveUser(user);
    }
}
