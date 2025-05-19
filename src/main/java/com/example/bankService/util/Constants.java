package com.example.bankService.util;

import com.example.bankService.model.Client;
import com.example.bankService.model.Passport;
import com.example.bankService.model.Wallet;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Constants {

    public static final String MAIN_DEPOSIT_CREDIT_PROCESS = "MainDepositCreditProcess";

    private static final Wallet DZMITRY_WALLET = Wallet.builder()
						.moneyCount(BigDecimal.valueOf(100.20))
						.build();

    private static final Passport DZMITRY_PASSPORT = Passport.builder()
					    .identicalNumber("KH123H123")
					    .name("Dzmitry")
					    .surname("Dar")
					    .address("Solo")
					    .birthDate(LocalDate.parse("1988-12-03"))
					    .validFrom(LocalDate.parse("2021-11-11"))
					    .validTo(LocalDate.parse("2031-11-11"))
					    .build();

    public static final Client DZMITRY = Client.builder()
					  .id("1")
					  .name("Dzmitry")
					  .surname("Dar")
					  .address("Solo")
					  .phoneNumber("+375111222333")
					  .birthDate(LocalDate.parse("1988-12-03"))
					  .wallet(DZMITRY_WALLET)
					  .passport(DZMITRY_PASSPORT)
					  .build();


}
