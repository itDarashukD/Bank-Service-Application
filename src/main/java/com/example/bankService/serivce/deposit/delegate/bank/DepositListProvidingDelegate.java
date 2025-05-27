package com.example.bankService.serivce.deposit.delegate.bank;

import static com.example.bankService.util.Constants.BANK_DEPOSITS;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("depositListProvidingDelegate")
public class DepositListProvidingDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("the DepositListProvidingDelegate has started...");

        //todo obtain the list of deposits from DB

        log.info(String.format("The list of deposits provided by bank : %s ", BANK_DEPOSITS));

        execution.setVariable("bankDeposits", BANK_DEPOSITS);
    }
}
