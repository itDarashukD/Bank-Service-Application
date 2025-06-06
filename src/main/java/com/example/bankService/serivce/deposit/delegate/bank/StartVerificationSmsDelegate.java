package com.example.bankService.serivce.deposit.delegate.bank;

import com.example.bankService.model.Client;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("startVerificationSmsDelegate")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StartVerificationSmsDelegate implements JavaDelegate {

    private static final String START_MESSAGE = "message_start_sms_verification";

    RuntimeService runtimeService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("the StartVerificationSmsDelegate has started...");

        var businessKey = execution.getProcessBusinessKey();

        var client = (Client) execution.getVariable("client");

        runtimeService.createMessageCorrelation(START_MESSAGE)
		    .processInstanceBusinessKey(businessKey)
		    .setVariable("client", client)
		    .correlateWithResult();
    }
}
