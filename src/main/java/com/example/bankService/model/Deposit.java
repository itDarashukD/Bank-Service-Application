package com.example.bankService.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
@Builder
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Deposit implements Serializable {

    String name;
    BigDecimal minimalSum;
    BigDecimal currentSum;
    OffsetDateTime openDate;
    OffsetDateTime closeDate;
    Double percentage;
    Boolean isCapitalized;
    String currency;
    Integer termInMonth;

}
