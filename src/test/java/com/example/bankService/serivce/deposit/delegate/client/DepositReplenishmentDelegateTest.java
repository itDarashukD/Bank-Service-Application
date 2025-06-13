package com.example.bankService.serivce.deposit.delegate.client;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.example.bankService.util.AbstractTestBase;
import java.util.Arrays;
import java.util.Collection;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.extension.mockito.CamundaMockito;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("Tests for DepositReplenishmentDelegate class")
@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class DepositReplenishmentDelegateTest extends AbstractTestBase {

    @InjectMocks
    DepositReplenishmentDelegate delegate;

    DelegateExecution execution1;
    DelegateExecution execution2;
    DelegateExecution execution3;
    DelegateExecution execution4;

    @BeforeEach
    void setUp() {
        execution1 = CamundaMockito.delegateExecutionFake();
        execution2 = CamundaMockito.delegateExecutionFake();
        execution3 = CamundaMockito.delegateExecutionFake();
        execution4 = CamundaMockito.delegateExecutionFake();

        execution1.setVariable("preparedDepositContract", CONTRACT_MIN_SUM_0);
        execution1.setVariable("client", CLIENT_MONEY_0);

        execution2.setVariable("preparedDepositContract", CONTRACT_MIN_SUM_10);
        execution2.setVariable("client", CLIENT_MONEY_10);

        execution3.setVariable("preparedDepositContract", CONTRACT_MIN_SUM_10);
        execution3.setVariable("client", CLIENT_MONEY_20);

        execution4.setVariable("preparedDepositContract", CONTRACT_MIN_SUM_30);
        execution4.setVariable("client", CLIENT_MONEY_20);
    }

    @DisplayName("execute() does client has enough money to deposit replenish")
    @TestFactory
    Collection<DynamicTest> execute_doesClientHasEnoughMoneyToDepositReplenish() {
        return Arrays.asList(dynamicTest("1st when client has enough money", () -> assertDoesNotThrow(() -> delegate.execute(execution1))),
			  dynamicTest("2nd when client has enough money", () -> assertDoesNotThrow(() -> delegate.execute(execution2))),
			  dynamicTest("3rd when client has enough money", () -> assertDoesNotThrow(() -> delegate.execute(execution3))),
			  dynamicTest("4th when client does NOT have enough money",
				     () -> assertThatThrownBy(() -> delegate.execute(execution4))
					    .isInstanceOf(BpmnError.class)
					    .hasMessageContaining("Client does not have enough money to deposit opening")));
    }
}