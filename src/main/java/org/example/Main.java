package org.example;

import Tool.Server;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try{
            Server s = new Server(9000);
            s.Accept();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}