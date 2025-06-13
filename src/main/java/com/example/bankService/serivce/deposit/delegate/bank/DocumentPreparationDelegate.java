package com.example.bankService.serivce.deposit.delegate.bank;

import static com.example.bankService.util.Constants.BLANK_DEPOSIT_CONTRACT;

import com.example.bankService.model.Client;
import com.example.bankService.model.Deposit;
import com.example.bankService.model.DepositContract;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("documentPreparationDelegate")
public class DocumentPreparationDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("the DocumentPreparationDelegate has started...");

        var client = (Client) execution.getVariable("client");
        var depositName = (String) execution.getVariable("depositName");
        var deposits = (List<Deposit>) execution.getVariable("bankDeposits");

        var choosenDeposit = deposits.stream()
				 .filter(deposit -> deposit.getName().equals(depositName))
				 .findAny()
				 .orElseThrow(() -> new IllegalArgumentException("Deposit with name " + depositName + "does not present"));

        //todo call to DB for deposit contract obtaining
        var blackDepositContract = BLANK_DEPOSIT_CONTRACT;

        var depositContract = fillDeposit(blackDepositContract, choosenDeposit, client);

        execution.setVariable("preparedDepositContract", depositContract);
    }

    private DepositContract fillDeposit(DepositContract blackDepositContract, Deposit choosenDeposit, Client client) {
        var passport = client.getPassport();

        return blackDepositContract.setId(UUID.randomUUID())
			        .setName(choosenDeposit.getName())
			        .setMinimalSum(choosenDeposit.getMinimalSum())
			        .setOpenDate(OffsetDateTime.now())
			        .setCloseDate(OffsetDateTime.now().plusMonths(choosenDeposit.getTermInMonth().longValue()))

			        .setClientName(passport.getName())
			        .setClientSurName(passport.getSurname())
			        .setClientPhoneNumber(client.getPhoneNumber());
    }


}
