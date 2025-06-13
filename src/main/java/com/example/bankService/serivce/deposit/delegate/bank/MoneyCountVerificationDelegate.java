package com.example.bankService.serivce.deposit.delegate.bank;

import static com.example.bankService.util.Constants.NOT_ENOUGH_MONEY;

import com.example.bankService.model.DepositContract;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("moneyCountVerificationDelegate")
public class MoneyCountVerificationDelegate implements JavaDelegate {


    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("The MoneyCountVerificationDelegate has started...");

        var paidMoneyDouble = (Double) execution.getVariable("paidMoney");
        var paidMoney = BigDecimal.valueOf(paidMoneyDouble);

        var preparedDepositContract = (DepositContract) execution.getVariable("preparedDepositContract");
        var minimalSumToReplenish = preparedDepositContract.getMinimalSum();

        if (isClientPayedEnoughMoney(paidMoney, minimalSumToReplenish)) {
	   putMoneyToSafe(paidMoney);
        } else {
	   throw new BpmnError(NOT_ENOUGH_MONEY,
			     "Client does not have enough money to deposit replenish");
        }

    }

    private void putMoneyToSafe(BigDecimal paidMoney) {
        log.info(String.format("The bank worker put relayed by client money %s to the safe...", paidMoney));
    }

    private boolean isClientPayedEnoughMoney(BigDecimal transferredMoneySum, BigDecimal minimalSumToReplenish) {
        log.info("Count the relayed by client money is...");

        return transferredMoneySum.compareTo(minimalSumToReplenish) >= 0;
    }
}
