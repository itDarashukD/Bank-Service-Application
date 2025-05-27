package com.example.bankService.serivce.deposit.delegate.client;


import static com.example.bankService.util.Constants.VERIFICATION_SMS_NOT_OBTAINED;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("smsObtainingByClientDelegate")
public class SmsObtainingByClientDelegate implements JavaDelegate {


    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("The SmsObtainingByClientDelegate has started...");

        var variables = execution.getVariables();

        if (!variables.containsKey("sendMobileCode")) {
	   throw new BpmnError(VERIFICATION_SMS_NOT_OBTAINED, "The verification sms is not obtained by client");
        }

    }
}
