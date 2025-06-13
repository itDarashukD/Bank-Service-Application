package com.example.bankService.serivce.deposit.delegate.bank;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("sendSuccessVerificationSmsDelegate")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SendSuccessVerificationSmsDelegate implements JavaDelegate {

    private static final String START_SUCCESS_MESSAGE = "message_successes_sms_verification";

    RuntimeService runtimeService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("the SendSuccessVerificationSmsDelegate has started...");

        var businessKey = execution.getBusinessKey();

        runtimeService.createMessageCorrelation(START_SUCCESS_MESSAGE)
		    .processInstanceBusinessKey(businessKey)
		    .correlateWithResult();
    }
}
