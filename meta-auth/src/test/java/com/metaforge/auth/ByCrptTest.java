package com.metaforge.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class ByCrptTest {

    public static void main(String[] args) {
        String password = "123456";
        System.out.println(new BCryptPasswordEncoder().encode(password));
    }
}
