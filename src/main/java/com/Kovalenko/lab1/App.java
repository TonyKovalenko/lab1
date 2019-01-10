package com.Kovalenko.lab1;


import com.Kovalenko.lab1.controller.Controller;


public class App {

    static void start() {
        Controller myController = Controller.INSTANCE;
        myController.run();
    }

    public static void main(String[] args) {
        App.start();
    }

}
