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
@Component("sendFailedVerificationSmsDelegate")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SendFailedVerificationSmsDelegate implements JavaDelegate {

    private static final String START_FAIL_MESSAGE = "message_failed_sms_verification";

    RuntimeService runtimeService;

    @Override
    public void execute(DelegateExecution execution) {
        log.info("the SendFailedVerificationSmsDelegate has started...");

        var processBusinessKey = execution.getProcessBusinessKey();

        runtimeService.createMessageCorrelation(START_FAIL_MESSAGE)
		    .processInstanceBusinessKey(processBusinessKey)
		    .correlateWithResult();
    }

}
