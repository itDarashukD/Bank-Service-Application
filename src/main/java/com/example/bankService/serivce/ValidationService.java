package com.example.bankService.serivce;

import com.example.bankService.model.Client;

public interface ValidationService {

    boolean isClientWantedByPolice(Client client);

    boolean isClientInBlackList(Client client);

    boolean isValidPassport(Client client);

}
