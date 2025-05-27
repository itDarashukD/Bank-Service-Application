package com.example.bankService.serivce.deposit.delegate.bank;

import static com.example.bankService.util.Constants.BANK_ALREADY_CLIENTS_INFO;

import com.example.bankService.model.Client;
import com.example.bankService.model.Passport;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("clientExistingCheckingDelegate")
public class ClientExistingCheckingDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("the ClientExistingCheckingDelegate has started...");
        boolean isExistingUser = false;

        var client = (Client) execution.getVariable("client");
        var passport = client.getPassport();

        var bankClientsInfo = BANK_ALREADY_CLIENTS_INFO;

        //todo call to DB for checking client existing...
        isExistingUser = bankClientsInfo.stream()
				    .anyMatch(info -> matchesClientInfo(info, passport));

        if (isExistingUser) {
	   log.info(String.format("The user with name : %s is already client of our bank", client.getName()));
        } else {
	   log.info(String.format("The user with name : %s is not yet client of our bank", client.getName()));
        }

        execution.setVariable("isExistingUser", isExistingUser);
    }

    private boolean matchesClientInfo(Passport info, Passport passport) {
        return info.getIdenticalNumber().equals(passport.getIdenticalNumber())
	       && info.getName().equals(passport.getName())
	       && info.getSurname().equals(passport.getSurname())
	       && info.getBirthDate().equals(passport.getBirthDate());
    }
}
