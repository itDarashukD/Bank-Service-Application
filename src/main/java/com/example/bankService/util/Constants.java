package com.example.bankService.util;

import com.example.bankService.model.Client;
import com.example.bankService.model.Deposit;
import com.example.bankService.model.DepositContract;
import com.example.bankService.model.Passport;
import com.example.bankService.model.Wallet;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class Constants {

    public static final String MAIN_DEPOSIT_CREDIT_PROCESS = "MainDepositCreditProcess";

    private static final Wallet DZMITRY_WALLET = Wallet.builder()
						 .moneyCount(BigDecimal.valueOf(100.20))
						 .build();

    private static final Wallet TIK_WALLET = Wallet.builder()
					      .moneyCount(BigDecimal.valueOf(222.22))
					      .build();

    private static final Wallet JOHN_WALLET = Wallet.builder()
					      .moneyCount(BigDecimal.valueOf(20.20))
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

    private static final Passport JOHN_PASSPORT = Passport.builder()
						       .identicalNumber("KH555R555")
						       .name("John")
						       .surname("Doe")
						       .address("Garden Square")
						       .birthDate(LocalDate.parse("1983-03-02"))
						       .validFrom(LocalDate.parse("2022-01-12"))
						       .validTo(LocalDate.parse("2032-01-12"))
						       .build();

    private static final Passport MIKE_PASSPORT = Passport.builder()
						       .identicalNumber("MC999W999")
						       .name("Mike")
						       .surname("Klinton")
						       .address("White House")
						       .birthDate(LocalDate.parse("1965-01-04"))
						       .validFrom(LocalDate.parse("2020-12-05"))
						       .validTo(LocalDate.parse("2030-12-05"))
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

    public static final Client JOHN = Client.builder()
					  .id("1")
					  .name("John")
					  .surname("Doe")
					  .address("Garden Square")
					  .phoneNumber("+375999666")
					  .birthDate(LocalDate.parse("1983-03-02"))
					  .wallet(JOHN_WALLET)
					  .passport(JOHN_PASSPORT)
					  .build();

    public static final Client MIKE = Client.builder()
					  .id("4")
					  .name("Mike")
					  .surname("Klinton")
					  .address("White House")
					  .phoneNumber("+375222333")
					  .birthDate(LocalDate.parse("1965-01-04"))
					  .wallet(null)
					  .passport(MIKE_PASSPORT)
					  .build();

    public static final Client TIK = Client.builder()
				       .id("2")
				       .name("Tik")
				       .surname("Tak")
				       .address("Toe Square")
				       .phoneNumber("+375444555666")
				       .birthDate(LocalDate.parse("1981-01-01"))
				       .wallet(TIK_WALLET)
				       .passport(null)
				       .build();


    public static final String SUDDEN_OPERATION_INTERRUPTION_ERROR = "SUDDEN_OPERATION_INTERRUPTION_ERROR";
    public static final String VERIFICATION_SMS_NOT_OBTAINED = "VERIFICATION_SMS_NOT_OBTAINED";
    public static final String LIMIT_OF_VERIFICATION_SMS_ATTEMPTS_EXCEEDED = "LIMIT_OF_VERIFICATION_SMS_ATTEMPTS_EXCEEDED";
    public static final String NO_MORE_DEPOSITS_TO_OPEN = "NO_MORE_DEPOSITS_TO_OPEN";
    public static final String NOT_ENOUGH_MONEY = "NOT_ENOUGH_MONEY";


    private static final Deposit EARLY_SPRING = Deposit.builder()
						 .name("Early-Spring")
						 .currency("USD")
						 .isCapitalized(true)
						 .minimalSum(new BigDecimal("10.00"))
						 .percentage(10.00)
						 .currentSum(BigDecimal.ZERO)
						 .termInMonth(3)
						 .build();

    private static final Deposit HOT_SUMMER = Deposit.builder()
					        .name("Hot-Summer")
					        .currency("USD")
					        .isCapitalized(true)
					        .minimalSum(new BigDecimal("100.00"))
					        .percentage(15.00)
					        .currentSum(BigDecimal.ZERO)
					        .termInMonth(6)
					        .build();

    private static final Deposit COLORFUL_AUTUMN = Deposit.builder()
						    .name("Colorful_Autumn")
						    .currency("USD")
						    .isCapitalized(false)
						    .minimalSum(new BigDecimal("50.00"))
						    .percentage(12.00)
						    .currentSum(BigDecimal.ZERO)
						    .termInMonth(9)
						    .build();

    public static final List<Deposit> BANK_DEPOSITS = List.of(EARLY_SPRING, HOT_SUMMER, COLORFUL_AUTUMN);
    public static final List<Passport> BANK_ALREADY_CLIENTS_INFO = List.of(DZMITRY_PASSPORT, JOHN_PASSPORT);
    public static final List<Client> POLICE_WANTED_LIST = List.of(MIKE);
    public static final List<Client> BANK_BLACK_LIST = List.of(MIKE);


    public static final DepositContract BLANK_DEPOSIT_CONTRACT = new DepositContract();


}
