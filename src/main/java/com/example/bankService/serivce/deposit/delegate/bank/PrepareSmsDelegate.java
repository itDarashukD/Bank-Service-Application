package com.example.bankService.serivce.deposit.delegate.bank;

import com.example.bankService.model.Client;
import java.util.Optional;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("prepareSmsDelegate")
public class PrepareSmsDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("the PrepareSmsDelegate has started...");

        var client = (Client) execution.getVariable("client");

        log.info(String.format("Preparation for the SMS sending to tel.number: %s ", client.getPhoneNumber()));

        var code = prepareSmsCode();
        log.info("Sending verification mobile code to client ....");
        execution.setVariable("sendMobileCode", code);

        var sendMobileCodeCount = (Integer) execution.getVariable("sendMobileCodeCount");

        Optional.ofNullable(sendMobileCodeCount)
	       .ifPresentOrElse(
		      (count) -> execution.setVariable("sendMobileCodeCount", count + 1),
		      () -> execution.setVariable("sendMobileCodeCount",1 ));
    }

    private int prepareSmsCode() {
        return new Random().nextInt(1_000_000);
    }
}
