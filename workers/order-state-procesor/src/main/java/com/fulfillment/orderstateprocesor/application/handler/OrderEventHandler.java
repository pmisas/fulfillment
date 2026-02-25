package com.fulfillment.orderstateprocesor.application.handler;

public interface OrderEventHandler {
    String eventType();          
    void handle(String payload); 
}
