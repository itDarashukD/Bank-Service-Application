package com.example.bankService.serivce.deposit.delegate.bank;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;


@DisplayName("Tests for SmsValidationDelegate class")
@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class SmsValidationDelegateTest extends AbstractTestBase {

    private static final String OBTAINED_MOBILE_CODE = "obtainedMobileCode";
    private static final String SEND_MOBILE_CODE = "sendMobileCode";
    private static final String SEND_MOBILE_CODE_BPM_ERROR = "The count of chances to verify mobile code is greater than allowed";
    private static final String ONE_OF_THE_ARGUMENTS_IS_NULL = "One of the arguments is null";


    @InjectMocks
    SmsValidationDelegate delegate;

    DelegateExecution execution1;
    DelegateExecution execution2;
    DelegateExecution execution3;
    DelegateExecution execution4;
    DelegateExecution execution5;


    @BeforeEach
    void setUp() {
        execution1 = CamundaMockito.delegateExecutionFake();
        execution2 = CamundaMockito.delegateExecutionFake();
        execution3 = CamundaMockito.delegateExecutionFake();
        execution4 = CamundaMockito.delegateExecutionFake();
        execution5 = CamundaMockito.delegateExecutionFake();

        execution1.setVariable(OBTAINED_MOBILE_CODE, null);
        execution1.setVariable(SEND_MOBILE_CODE, SEND_MOBILE_CODE_22222);

        execution2.setVariable(OBTAINED_MOBILE_CODE, OBTAINED_MOBILE_CODE_22222);
        execution2.setVariable(SEND_MOBILE_CODE, null);

        execution3.setVariable(OBTAINED_MOBILE_CODE, null);
        execution3.setVariable(SEND_MOBILE_CODE, null);

        execution4.setVariable(OBTAINED_MOBILE_CODE, OBTAINED_MOBILE_CODE_22222);
        execution4.setVariable(SEND_MOBILE_CODE, SEND_MOBILE_CODE_22222);

        execution5.setVariable(OBTAINED_MOBILE_CODE, OBTAINED_MOBILE_CODE_22222);
        execution5.setVariable(SEND_MOBILE_CODE, SEND_MOBILE_CODE_11111);
    }


    @DisplayName("execute() when one of the codes is null")
    @TestFactory
    Collection<DynamicTest> execute_shouldThrowException_whenSomeArgumentIsNull() {
        return Arrays.asList(
	       dynamicTest("1st when obtained code is null",
			 () -> assertThatThrownBy(() -> delegate.execute(execution1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(ONE_OF_THE_ARGUMENTS_IS_NULL)),

	       dynamicTest("2nd when send code is null",
			 () -> assertThatThrownBy(() -> delegate.execute(execution2))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(ONE_OF_THE_ARGUMENTS_IS_NULL)),

	       dynamicTest("3rd when both codes are null",
			 () -> assertThatThrownBy(() -> delegate.execute(execution3))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining(ONE_OF_THE_ARGUMENTS_IS_NULL)));
    }

    @DisplayName("execute() when codes are equal")
    @Test
    void execute_shouldSetVariableToExecution_whenArgumentsAreEqual() throws Exception {
        delegate.execute(execution4);

        assertThat(execution4.getVariable("isSmsCodeValid")).isEqualTo(true);
    }

    @DisplayName("execute() when codes are NOT equal")
    @Test
    void execute_shouldSetVariableToExecution_whenArgumentsAreNotEqual() throws Exception {
        execution5.setVariable("sendMobileCodeCount", 1);

        delegate.execute(execution5);

        assertThat(execution5.getVariable("isSmsCodeValid")).isEqualTo(false);
        assertThat(execution5.getVariable("sendMobileCodeCount")).isEqualTo(1);
    }

    @DisplayName("execute() when codes are NOT equal and count of sending is 3")
    @Test
    void execute_shouldThrowBpmError_whenCountOfSendingIs3() throws Exception {
        execution5.setVariable("sendMobileCodeCount", 3);

        assertThatThrownBy(() -> delegate.execute(execution5))
	       .isInstanceOf(BpmnError.class)
	       .hasMessageContaining(SEND_MOBILE_CODE_BPM_ERROR);

        assertThat(execution5.hasVariable("isSmsCodeValid")).isFalse();
    }


}