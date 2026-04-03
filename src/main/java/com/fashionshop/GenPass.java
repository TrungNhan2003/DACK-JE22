package com.fashionshop;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenPass {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String rawPassword = "123456";
        String hash = encoder.encode(rawPassword);

        System.out.println("Password gốc: " + rawPassword);
        System.out.println("Hash: " + hash);
        System.out.println("Độ dài: " + hash.length());
    }
}