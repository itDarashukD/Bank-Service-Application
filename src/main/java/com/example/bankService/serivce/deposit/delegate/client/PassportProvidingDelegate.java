package com.example.bankService.serivce.deposit.delegate.client;


import static com.example.bankService.util.Constants.SUDDEN_OPERATION_INTERRUPTION_ERROR;

import com.example.bankService.model.Client;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("passportProvidingDelegate")
public class PassportProvidingDelegate implements JavaDelegate {


    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("The PassportProvidingDelegate has started...");

        var client = (Client) execution.getVariable("client");
        if (Objects.isNull(client.getPassport())) {
	   throw new BpmnError(SUDDEN_OPERATION_INTERRUPTION_ERROR, "The passport should be present!");
        }

        log.info(String.format("Client: %s  has provided passport", client.getName()));

    }
}
