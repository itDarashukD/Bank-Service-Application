package com.example.bankService.it;

import static com.example.bankService.util.Constants.DZMITRY;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.execute;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.job;

import com.example.bankService.model.Client;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;


@ActiveProfiles({"integration-test"})
@SpringBootTest(
        properties = {
	       "camunda.bpm.job-execution.enabled=false",
	       // Disables job execution (asynchronous tasks, timers) during tests.
	       // Useful because tests often don't need actual async execution, and this speeds them up.


	       "camunda.bpm.generate-unique-process-engine-name=true",
	       // Generates a unique name for each Process Engine instance.
	       // Helps avoid conflicts when tests run in parallel.

	       "camunda.bpm.generate-unique-process-application-name=true",
	       // Generates a unique name for the Process Application.
	       // Also prevents conflicts when running multiple tests at the same time.


	       "spring.datasource.generate-unique-name=true"
	       // Creates a unique name for the H2 (or another embedded) database for each test.
	       // Useful for test isolation, so changes in one test don't affect others.
        },
        webEnvironment = WebEnvironment.RANDOM_PORT)
class SmsVerificationProcessIT {


    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;


    @BeforeEach
    void cleanUpProcesses() {
        runtimeService.createProcessInstanceQuery().list()
		    .forEach(instance -> {
		        try {
			   runtimeService.deleteProcessInstance(instance.getId(), "Test cleanup");
		        } catch (Exception e) {
			   //ignore
		        }
		    });
    }

    @AfterEach
    void completeRemainingTasks() {
        runtimeService.suspendProcessInstanceByProcessDefinitionKey("SmsVerification");
    }

    @DisplayName("smsVerificationEndToEndTest() should call all tasks and throw exception since "
	   + "no any active task in DepositOpening process to wait message,"
	   + " due to DepositOpening process was not started ")
    @Test
    void smsVerificationEndToEndTest_shouldInvokeAllTasks() {
        var variables = prepareVariables(DZMITRY);

        /*start SmsVerificationProcess process*/
        var smsVerificationProcess = runtimeService
	       .startProcessInstanceByKey("SmsVerification",
				       "smsVerificationBusinessKey",
				       variables);


        assertThat(smsVerificationProcess).isNotNull();
        assertThat(smsVerificationProcess).hasPassed("VerificationSmsStartMessageId",
					        "VerificationSmsHandlingId");

        assertThat(smsVerificationProcess).isWaitingAt("ProvideSmsCodeId");


        /*ProvideSmsCodeId user task*/
        var provideSmsCodeUserTask = taskService.createTaskQuery()
					   .taskName("Provide sms validation code")
					   .taskDefinitionKey("ProvideSmsCodeId")
					   .singleResult();
        assertThat(provideSmsCodeUserTask).isNotNull();

        var mobileCode = (Integer) runtimeService.getVariable(smsVerificationProcess.getId(), "sendMobileCode");

        var userTaskVariables = new HashMap<String, Object>();
        userTaskVariables.put("sendMobileCode", mobileCode);
        userTaskVariables.put("obtainedMobileCode", mobileCode);

        taskService.complete(provideSmsCodeUserTask.getId(), userTaskVariables);
        assertThat(smsVerificationProcess).hasPassed("ProvideSmsCodeId");
        assertThat(smsVerificationProcess).hasPassed("ValidateCodeFromSmsId");
        assertThat(smsVerificationProcess).hasPassed("Gateway_isSmsCodeValid");

        assertThat(smsVerificationProcess).isWaitingAt("VerificationSmsSenderEventId");

        //since there no any active task in DepositOpening process to wait message, due to DepositOpening process was not started
        assertThatThrownBy(() -> execute(job("VerificationSmsSenderEventId")))
	       .isInstanceOf(MismatchingMessageCorrelationException.class)
	       .hasMessageContaining("Cannot correlate message 'message_successes_sms_verification': No process definition or execution matches the parameters");
    }

    @DisplayName("smsVerification() should call tasks 3 times"
	   + " due to sms was not obtained by client ")
    @Test
    void smsVerification_shouldThrowBpmException_whenSmsIsObtained() {
        var variables = prepareVariables(DZMITRY);

        /*start SmsVerificationProcess process*/
        var smsVerificationProcess = runtimeService
	       .startProcessInstanceByKey("SmsVerification",
				       "smsVerificationBusinessKey",
				       variables);

        assertThat(smsVerificationProcess).isNotNull();
        assertThat(smsVerificationProcess).hasPassed("VerificationSmsStartMessageId",
					        "VerificationSmsHandlingId");

        assertThat(smsVerificationProcess).isWaitingAt("ProvideSmsCodeId");


        /*ProvideSmsCodeId user task*/
        var provideSmsCodeUserTask = taskService.createTaskQuery()
					   .taskName("Provide sms validation code")
					   .taskDefinitionKey("ProvideSmsCodeId")
					   .singleResult();
        assertThat(provideSmsCodeUserTask).isNotNull();

        var mobileCode = (Integer) runtimeService.getVariable(smsVerificationProcess.getId(), "sendMobileCode");

        var userTaskVariables = new HashMap<String, Object>();
        userTaskVariables.put("sendMobileCode", mobileCode);
        userTaskVariables.put("obtainedMobileCode", mobileCode);

        taskService.complete(provideSmsCodeUserTask.getId(), userTaskVariables);
        assertThat(smsVerificationProcess).hasPassed("ProvideSmsCodeId",
					 "ValidateCodeFromSmsId",
					 "Gateway_isSmsCodeValid");

        assertThat(smsVerificationProcess).isWaitingAt("VerificationSmsSenderEventId");

        //since there no any active task in DepositOpening process to wait message, due to DepositOpening process was not started
        assertThatThrownBy(() -> execute(job("VerificationSmsSenderEventId")))
	       .isInstanceOf(MismatchingMessageCorrelationException.class)
	       .hasMessageContaining("Cannot correlate message 'message_successes_sms_verification': No process definition or execution matches the parameters");
    }


    @DisplayName("smsVerification() should pass all tasks3 times and throw exception   "
	   + "due to sms validation attempts are exceeded,"
	   + "thrown exception should be handled in DepositOpening process "
	   + " and process should be finished in the Main process"
	   + "when sms code is not valid")
    @Test
    void smsVerification_shouldInvokeTasks3TimesAndThrowBpmException_whenSmsIsNotValid() {
        var variables = prepareVariables(DZMITRY);

        /*start MainDepositCreditProcess process*/
        var mainDepositCreditProcess = runtimeService
	       .startProcessInstanceByKey("MainDepositCreditProcess",
				       "depositOpeningBusinessKey",
				       variables);

        assertThat(mainDepositCreditProcess).isNotNull();
        assertThat(mainDepositCreditProcess).isWaitingAt("GoingToBankId");


        /*GoingToBankId*/
        var userTask = taskService.createTaskQuery()
			       .taskName("Going to the bank")
			       .taskDefinitionKey("GoingToBankId")
			       .singleResult();
        assertThat(userTask).isNotNull();

        var userTaskVariables = new HashMap<String, Object>();
        userTaskVariables.put("transportMode", "taxi");
        userTaskVariables.put("taxiCost", "15.50");

        taskService.complete(userTask.getId(), userTaskVariables);
        assertThat(mainDepositCreditProcess).hasPassed("GoingToBankId");


        /*GetTicketInQueueMachineId*/
        assertThat(mainDepositCreditProcess).isWaitingAt("GetTicketInQueueMachineId");
        assertThat(mainDepositCreditProcess).hasPassed("GatewayTaxiOrWalk");

        var getTicketInQueueMachineUserTask = taskService.createTaskQuery()
						   .taskName("GetTicketInQueueMachine")
						   .taskDefinitionKey("GetTicketInQueueMachineId")
						   .singleResult();
        assertThat(getTicketInQueueMachineUserTask).isNotNull();

        var getTicketInQueueMachineVariables = new HashMap<String, Object>();
        getTicketInQueueMachineVariables.put("ticket", "deposit");

        taskService.complete(getTicketInQueueMachineUserTask.getId(), getTicketInQueueMachineVariables);
        assertThat(mainDepositCreditProcess).hasPassed("PayForTheTaxi",
						 "GetTicketInQueueMachineId");

        assertThat(mainDepositCreditProcess).isWaitingAt("DepositOpeningProcessId");
        assertThat(mainDepositCreditProcess).hasPassed("GatewayDepositOrCredit");


        /* search for running call activity "OpenDeposit" */
        var openDeposit = runtimeService.createProcessInstanceQuery()
				    .processDefinitionKey("DepositOpening")
				    .variableValueEquals("correlationId", "testCorrelationId")
				    .active()
				    .singleResult();
        assertThat(openDeposit).isNotNull();

        assertThat(openDeposit).isWaitingAt("PassportProvidingId");
        execute(job("PassportProvidingId"));

        assertThat(openDeposit).hasPassed("PassportProvidingId",
				      "DepositListProvidingId");

        assertThat(openDeposit).isWaitingAt("DepositChoosingId");

        // DepositChoosingId user task
        var userTaskDepositChoosingId = taskService.createTaskQuery()
					      .processInstanceId(openDeposit.getProcessInstanceId())
					      .taskName("Take a look into deposit list and choose one of them")
					      .taskDefinitionKey("DepositChoosingId")
					      .singleResult();

        assertThat(userTaskDepositChoosingId).isNotNull();

        var depositChoosingIdVariables = new HashMap<String, Object>();
        depositChoosingIdVariables.put("depositName", "Early-Spring");

        taskService.complete(userTaskDepositChoosingId.getId(), depositChoosingIdVariables);

        assertThat(openDeposit).hasPassed("DepositChoosingId",
				      "GatewayIsDepositChosen",
				      "ClientExistingCheckingId",
				      "ClientPartialValidationId",
				      "GatewayMergeIsNewClientId",
				      "StartVerificationSmsDelegateId");

        assertThat(openDeposit).isWaitingAt("EndVerificationSmsDelegateId");

        /*SmsVerification process*/
        var smsVerificationProcess = runtimeService.createProcessInstanceQuery()
					      .processDefinitionKey("SmsVerification")
					      .processInstanceBusinessKey("depositOpeningBusinessKey")
					      .active()
					      .singleResult();
        assertThat(smsVerificationProcess).isNotNull();

        assertThat(smsVerificationProcess).hasPassed("VerificationSmsStartMessageId",
					        "VerificationSmsHandlingId");

        assertThat(smsVerificationProcess).isWaitingAt("ProvideSmsCodeId");


        /*1st time ProvideSmsCodeId user task*/
        var provideSmsCodeUserTask = taskService.createTaskQuery()
					   .taskName("Provide sms validation code")
					   .taskDefinitionKey("ProvideSmsCodeId")
					   .singleResult();
        assertThat(provideSmsCodeUserTask).isNotNull();

        var mobileCode = (Integer) runtimeService.getVariable(smsVerificationProcess.getId(), "sendMobileCode");
        var notValidSmsCode = 1234567;

        var smsVerificationUserTaskVariables = new HashMap<String, Object>();
        smsVerificationUserTaskVariables.put("sendMobileCode", mobileCode);
        smsVerificationUserTaskVariables.put("obtainedMobileCode", notValidSmsCode);

        taskService.complete(provideSmsCodeUserTask.getId(), smsVerificationUserTaskVariables);

        assertThat(smsVerificationProcess).hasPassed("ProvideSmsCodeId");
        assertThat(smsVerificationProcess).hasPassed("ValidateCodeFromSmsId");
        assertThat(smsVerificationProcess).hasPassed("Gateway_isSmsCodeValid");

        assertThat(smsVerificationProcess).hasNotPassed("Flow_validSmsCode");

        assertThat(smsVerificationProcess).isWaitingAt("ProvideSmsCodeId")
				      .variables()
				      .extracting("sendMobileCodeCount")
				      .isEqualTo(2);

        /*2nd time ProvideSmsCodeId user task*/
        var provideSmsCodeUserTask2Time = taskService.createTaskQuery()
					        .taskName("Provide sms validation code")
					        .taskDefinitionKey("ProvideSmsCodeId")
					        .singleResult();
        assertThat(provideSmsCodeUserTask2Time).isNotNull();

        taskService.complete(provideSmsCodeUserTask2Time.getId(), smsVerificationUserTaskVariables);

        assertThat(smsVerificationProcess).hasNotPassed("Flow_validSmsCode");

        assertThat(smsVerificationProcess).isWaitingAt("ProvideSmsCodeId").variables()
				      .extracting("sendMobileCodeCount")
				      .isEqualTo(3);

        /*3rd time ProvideSmsCodeId user task*/
        var provideSmsCodeUserTask3Time = taskService.createTaskQuery()
					        .taskName("Provide sms validation code")
					        .taskDefinitionKey("ProvideSmsCodeId")
					        .singleResult();
        assertThat(provideSmsCodeUserTask3Time).isNotNull();

        taskService.complete(provideSmsCodeUserTask3Time.getId(), smsVerificationUserTaskVariables);

        assertThat(smsVerificationProcess).hasNotPassed("Flow_validSmsCode");
        assertThat(smsVerificationProcess).hasPassed("ErrorNoMoreSmsValidationSmsAttemptsHandlerId");
        assertThat(smsVerificationProcess).isWaitingAt("ErrorNoMoreSmsValidationSmsAttemptsEndId");
        execute(job("ErrorNoMoreSmsValidationSmsAttemptsEndId"));


 	/*handle exception in Deposit opening*/
        assertThat(openDeposit).hasPassed("StartFailedVerificationSmsMessageId",
				      "EndFailedVerificationSmsErrorId");


        /*handle exception in Main process*/
        assertThat(mainDepositCreditProcess).hasPassed("EventSuddenOperationInterruptionErrorStartId");
        assertThat(mainDepositCreditProcess).isWaitingAt("ActivityRoadToHomeAfterBankVisit");


        /* start call activity "RoadToHome" */
        var goingHomeProcess = runtimeService.createProcessInstanceQuery()
					.processDefinitionKey("GoingHomeProcess")
					.variableValueEquals("correlationId", "testCorrelationId")
					.active()
					.singleResult();
        assertThat(goingHomeProcess).isNotNull();

        assertThat(goingHomeProcess).isWaitingAt("ChoseTransportToHomeId");
        execute(job("ChoseTransportToHomeId"));

        assertThat(goingHomeProcess).hasPassed("ChoseTransportToHomeId");
        assertThat(goingHomeProcess).hasPassed("PrintGoingHomeTransportId");

        assertThat(goingHomeProcess).isEnded();
        assertThat(mainDepositCreditProcess).isEnded();
    }

    private Map<String, Object> prepareVariables(Client client) {
        var variablesMap = new HashMap<String, Object>();
        variablesMap.put("client", client);
        variablesMap.put("correlationId", "testCorrelationId");

        return variablesMap;
    }
}