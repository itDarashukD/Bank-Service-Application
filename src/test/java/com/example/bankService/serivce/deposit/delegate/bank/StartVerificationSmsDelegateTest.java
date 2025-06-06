package com.example.bankService.serivce.deposit.delegate.bank;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.example.bankService.model.Client;
import com.example.bankService.util.AbstractTestBase;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.extension.mockito.CamundaMockito;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("Tests for StartVerificationSmsDelegate class")
@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
class StartVerificationSmsDelegateTest extends AbstractTestBase {


    @InjectMocks
    StartVerificationSmsDelegate delegate;

    @Mock
    RuntimeService runtimeService;
    @Mock
    MessageCorrelationBuilder builder;


    @Captor
    ArgumentCaptor<String> messageNameCaptor;
    @Captor
    ArgumentCaptor<String> businessKeyCaptor;
    @Captor
    ArgumentCaptor<String> variableNameCaptor;
    @Captor
    ArgumentCaptor<Client> clientCaptor;


    DelegateExecution execution;

    @BeforeEach
    void setUp() {
        execution = CamundaMockito.delegateExecutionFake();
    }

    @Test
    void execute_shouldVerifyThatSendArgumentsAreCorrect() throws Exception {
        when(runtimeService.createMessageCorrelation(messageNameCaptor.capture())).thenReturn(builder);
        when(builder.processInstanceBusinessKey(businessKeyCaptor.capture())).thenReturn(builder);
        when(builder.setVariable(variableNameCaptor.capture(), clientCaptor.capture())).thenReturn(builder);

        execution.setProcessBusinessKey(TEST_BUSINESS_KEY);
        execution.setVariable("client", CLIENT_MONEY_10);

        delegate.execute(execution);

        assertAll(
	       () -> assertThat(messageNameCaptor.getValue()).isEqualTo("message_start_sms_verification"),
 	       () -> assertThat(businessKeyCaptor.getValue()).isEqualTo(TEST_BUSINESS_KEY),
	       () -> assertThat(variableNameCaptor.getValue()).isEqualTo("client"),
	       () -> assertThat(clientCaptor.getValue()).isEqualTo(CLIENT_MONEY_10));
    }
}