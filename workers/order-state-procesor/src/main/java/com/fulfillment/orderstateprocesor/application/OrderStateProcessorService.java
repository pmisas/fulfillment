package com.fulfillment.orderstateprocesor.application;

import com.fulfillment.orderstateprocesor.application.dto.ProcessEventCommand;

public interface OrderStateProcessorService {
    void process(ProcessEventCommand command);
}
