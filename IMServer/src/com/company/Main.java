package com.company;

import IMEServer.IMEServer;

public class Main {

    public static void main(String[] args) {
        try {
            IMEServer.startServer();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
