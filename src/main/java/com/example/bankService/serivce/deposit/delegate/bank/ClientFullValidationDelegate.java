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
@Component("clientFullValidationDelegate")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClientFullValidationDelegate implements JavaDelegate {

    ValidationService validationService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("the ClientFullValidationDelegate has started...");

        var client = (Client) execution.getVariable("client");

        var isCriminal = validationService.isClientWantedByPolice(client);
        var isInBlackList = validationService.isClientInBlackList(client);
        var isValidPassport = validationService.isValidPassport(client);

        execution.setVariable("isCriminal", isCriminal);

        if (isCriminal || isInBlackList || !isValidPassport) {
	   execution.setVariable("isValidUser", false);
        } else {
	   execution.setVariable("isValidUser", true);
        }

    }
}
