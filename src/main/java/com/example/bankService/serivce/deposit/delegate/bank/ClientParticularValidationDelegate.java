package com.example.bankService.serivce.deposit.delegate.bank;

import com.example.bankService.model.Client;
import com.example.bankService.serivce.ValidationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component("clientParticularValidationDelegate")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClientParticularValidationDelegate implements JavaDelegate {

    ValidationService validationService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("the ClientParticularValidationDelegate has started...");

        var client = (Client) execution.getVariable("client");

        var isCriminal = validationService.isClientWantedByPolice(client);
        execution.setVariable("isCriminal", isCriminal);

        if (isCriminal) {
	   execution.setVariable("isValidUser", false);
        } else {
	   execution.setVariable("isValidUser", true);
        }

    }
}
