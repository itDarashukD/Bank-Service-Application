package com.example.bankService.model;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
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
public class DepositContract {

    UUID id;
    String name;
    BigDecimal minimalSum;
    OffsetDateTime openDate;
    OffsetDateTime closeDate;

    String clientName;
    String clientSurName;
    String clientPhoneNumber;

}
