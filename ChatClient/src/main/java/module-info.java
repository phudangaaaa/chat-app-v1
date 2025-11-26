module com.chatapp.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.web;
    requires com.google.gson;
    requires org.slf4j;
    requires emoji.java;

    opens com.chatapp.client to javafx.fxml;
    opens com.chatapp.client.controller to javafx.fxml;
    opens com.chatapp.client.model to com.google.gson;

    exports com.chatapp.client;
    exports com.chatapp.client.controller;
    exports com.chatapp.client.model;
}
