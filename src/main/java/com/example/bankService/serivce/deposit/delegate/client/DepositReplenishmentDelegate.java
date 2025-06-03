package com.example.bankService.serivce.deposit.delegate.client;

import static com.example.bankService.util.Constants.NOT_ENOUGH_MONEY;

import com.example.bankService.model.Client;
import com.example.bankService.model.DepositContract;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("depositReplenishmentDelegate")
public class DepositReplenishmentDelegate implements JavaDelegate {


    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("The DepositReplenishmentDelegate has started...");

        var client = (Client) execution.getVariable("client");
        var preparedDepositContract = (DepositContract) execution.getVariable("preparedDepositContract");

        if (!isClientHasEnoughMoney(client, preparedDepositContract)) {
	   throw new BpmnError(NOT_ENOUGH_MONEY,
			     "Client does not have enough money to deposit opening");
        }

    }

    private boolean isClientHasEnoughMoney(Client client, DepositContract preparedContract) {
        var moneyOnWallet = client.getWallet().getMoneyCount();
        var depositMinimalSum = preparedContract.getMinimalSum();

        return moneyOnWallet.compareTo(depositMinimalSum) >= 0;
    }
}
