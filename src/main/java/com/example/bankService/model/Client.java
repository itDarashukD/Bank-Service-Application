package com.example.bankService.model;


import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Client {

    String id;
    String name;
    String surname;
    String address;
    String phoneNumber;
    LocalDate birthDate;
    Wallet wallet;
    Passport passport;

}
