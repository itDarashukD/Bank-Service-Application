package com.example.bankService.serivce.way.delegate;

import com.example.bankService.model.Client;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("taxiPaymentDelegate")
public class TaxiPaymentDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("the TaxiPaymentDelegate has started ...");

        var client = (Client) execution.getVariable("client");
        var taxiCost = (String) execution.getVariable("taxiCost");

        var moneyOnWallet = client.getWallet().getMoneyCount().subtract(new BigDecimal(taxiCost));
        log.info(String.format("Client just has peed for the taxi about %s ", taxiCost));

        client.getWallet().setMoneyCount(moneyOnWallet);

    }
}
