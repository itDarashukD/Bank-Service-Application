package com.example.bankService.it;


import static com.example.bankService.util.Constants.DZMITRY;
import static com.example.bankService.util.Constants.JOHN;
import static com.example.bankService.util.Constants.MIKE;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.execute;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.job;

import com.example.bankService.model.Client;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.camunda.bpm.engine.ManagementService;
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
	       // Disables job execution (asynchronous tasks, timers) during tests.
	       // Useful because tests often don't need actual async execution, and this speeds them up.
	       "camunda.bpm.job-execution.enabled=false",
	       // Generates a unique name for each Process Engine instance.
	       // Helps avoid conflicts when tests run in parallel.
	       "camunda.bpm.generate-unique-process-engine-name=true",
	       // Generates a unique name for the Process Application.
	       // Also prevents conflicts when running multiple tests at the same time.
	       "camunda.bpm.generate-unique-process-application-name=true",
	       // Creates a unique name for the H2 (or another embedded) database for each test.
	       // Useful for test isolation, so changes in one test don't affect others.
	       "spring.datasource.generate-unique-name=true"
        },
        webEnvironment = WebEnvironment.RANDOM_PORT
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositOpeningProcessIT {

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    TaskService taskService;

    @Autowired
    ManagementService managementService;

    @BeforeEach
    void cleanUpProcesses() {
        runtimeService.createProcessInstanceQuery().list()
		    .forEach(instance -> {
		        try {
			   runtimeService.deleteProcessInstance(instance.getId(), "Test cleanup");
		        } catch (Exception e) {
			   //ignore it
		        }
		    });
    }

    @AfterEach
    void completeRemainingTasks() {
        runtimeService.suspendProcessInstanceByProcessDefinitionKey("MainDepositCreditProcess");
        runtimeService.suspendProcessInstanceByProcessDefinitionKey("DepositOpening");
        runtimeService.suspendProcessInstanceByProcessDefinitionKey("GoingHome");
    }


    @Test
    void depositEndToEndTest_shouldInvokeAllTasks() {
        var variables = prepareVariables(DZMITRY);

        /*start MainDepositCreditProcess process*/
        var mainDepositCreditProcess = runtimeService.startProcessInstanceByKey("MainDepositCreditProcess",
								        "depositOpeningBusinessKey",
								        variables);

        assertThat(mainDepositCreditProcess).isNotNull();
        assertThat(mainDepositCreditProcess).isWaitingAt("GoingToBankId");


        /*start GoingToBankId UT*/
        var goingToBank = taskService.createTaskQuery()
				 .taskName("Going to the bank")
				 .taskDefinitionKey("GoingToBankId")
				 .singleResult();
        assertThat(goingToBank).isNotNull();

        var userTaskVariables = new HashMap<String, Object>();
        userTaskVariables.put("transportMode", "taxi");
        userTaskVariables.put("taxiCost", "15.50");

        taskService.complete(goingToBank.getId(), userTaskVariables);
        assertThat(mainDepositCreditProcess).hasPassed("GoingToBankId");


        /*start GetTicketInQueueMachineId UT*/
        assertThat(mainDepositCreditProcess).isWaitingAt("GetTicketInQueueMachineId");
        assertThat(mainDepositCreditProcess).hasPassed("Gateway_0s7l7xt");

        var getTicketInQueueMachine = taskService.createTaskQuery()
					    .taskName("GetTicketInQueueMachine")
					    .taskDefinitionKey("GetTicketInQueueMachineId")
					    .singleResult();
        assertThat(getTicketInQueueMachine).isNotNull();

        var getTicketInQueueMachineVariables = new HashMap<String, Object>();
        getTicketInQueueMachineVariables.put("ticket", "deposit");

        taskService.complete(getTicketInQueueMachine.getId(), getTicketInQueueMachineVariables);
        assertThat(mainDepositCreditProcess).hasPassed("PayForTheTaxi", "GetTicketInQueueMachineId");


        /* search for running call activity "OpenDeposit" */
        var openDeposit = runtimeService.createProcessInstanceQuery()
				    .processDefinitionKey("DepositOpening")
				    .variableValueEquals("correlationId", "testCorrelationId")
				    .active()
				    .singleResult();
        assertThat(openDeposit).isNotNull();

        assertThat(openDeposit).isWaitingAt("PassportProvidingId");
        execute(job("PassportProvidingId"));

        assertThat(openDeposit).hasPassed("PassportProvidingId", "DepositListProvidingId");
        assertThat(openDeposit).isWaitingAt("DepositChoosingId");


        /*start DepositChoosingId UT*/
        var userTaskDepositChoosing = taskService.createTaskQuery()
					    .processInstanceId(openDeposit.getProcessInstanceId())
					    .taskName("Take a look in to deposit list and choose one of them")
					    .taskDefinitionKey("DepositChoosingId")
					    .singleResult();
        assertThat(userTaskDepositChoosing).isNotNull();

        var userTaskDepositChoosingVariables = new HashMap<String, Object>();
        userTaskDepositChoosingVariables.put("depositName", "Early-Spring");

        taskService.complete(userTaskDepositChoosing.getId(), userTaskDepositChoosingVariables);

        assertThat(openDeposit).hasPassed("GatewayIsDepositChosenId",
				      "Gateway_1nzz7fv",
				      "ClientExistingCheckingId",
				      "ClientParticularValidationId",
				      "GatewayMergeIsNewClientId",
				      "GatewayIsSuccessValidationId",
				      "StartVerificationSmsDelegateId");

        assertThat(openDeposit).isWaitingAt("EndVerificationSmsDelegateId");


        /*start SmsVerification process*/
        var smsVerification = runtimeService.createProcessInstanceQuery()
				        .processDefinitionKey("SmsVerification")
				        .processInstanceBusinessKey("depositOpeningBusinessKey")
				        .active()
				        .singleResult();
        assertThat(smsVerification).isNotNull();

        assertThat(smsVerification).hasPassed("VerificationSmsStartMessageId",
					 "PrepareAndSendVerificationSmsId",
					 "VerificationSmsHandlingId");

        assertThat(smsVerification).isWaitingAt("ProvideSmsValidationCodeId");


        /*start ProvideSmsCodeUserTask UT*/
        var provideSmsCodeUserTask = taskService.createTaskQuery()
					   .taskName("Provide sms validation code")
					   .taskDefinitionKey("ProvideSmsValidationCodeId")
					   .singleResult();
        assertThat(provideSmsCodeUserTask).isNotNull();

        var mobileCode = (Integer) runtimeService.getVariable(smsVerification.getId(), "sendMobileCode");

        var provideSmsCodeUserTaskVariables = new HashMap<String, Object>();
        provideSmsCodeUserTaskVariables.put("sendMobileCode", mobileCode);
        provideSmsCodeUserTaskVariables.put("obtainedMobileCode", mobileCode);

        taskService.complete(provideSmsCodeUserTask.getId(), provideSmsCodeUserTaskVariables);

        assertThat(smsVerification).hasPassed("ProvideSmsValidationCodeId",
					 "ValidateCodeFromSmsId",
					 "GatewayIsSmsCodeValid");

        assertThat(smsVerification).isWaitingAt("SendSuccessVerificationSmsId");
        execute(job("SendSuccessVerificationSmsId"));

        /* Continue deposit opening process*/
        assertThat(openDeposit).hasPassed("EndVerificationSmsDelegateId",
				      "DocumentsPreparationId");

        assertThat(openDeposit).isWaitingAt("ReadAndSignContractId");


        /*start ReadAndSignContract UT*/
        var readAndSignContract = taskService.createTaskQuery()
					.processInstanceId(openDeposit.getProcessInstanceId())
					.taskName("Read and sign contract")
					.taskDefinitionKey("ReadAndSignContractId")
					.singleResult();
        assertThat(readAndSignContract).isNotNull();

        var readAndSignContractUserTaskVariables = new HashMap<String, Object>();
        readAndSignContractUserTaskVariables.put("isContractSigned", "true");

        taskService.complete(readAndSignContract.getId(), readAndSignContractUserTaskVariables);

        assertThat(openDeposit).hasPassed("ReadAndSignContractId",
				      "Gateway_1ls52z7",
				      "DepositReplenishmentId");

        assertThat(openDeposit).isWaitingAt("CountOfMoneyToReplenishId");

        /* MoneyToReplenish user task*/
        var moneyToReplenish = taskService.createTaskQuery()
				      .processInstanceId(openDeposit.getProcessInstanceId())
				      .taskName("Choose how much money you want to replenish")
				      .taskDefinitionKey("CountOfMoneyToReplenishId")
				      .singleResult();
        assertThat(moneyToReplenish).isNotNull();

        var moneyToReplenishUserTaskVariables = new HashMap<String, Object>();
        moneyToReplenishUserTaskVariables.put("paidMoney", 25.0);

        taskService.complete(moneyToReplenish.getId(), moneyToReplenishUserTaskVariables);

        assertThat(openDeposit).hasPassed("CountOfMoneyToReplenishId",
				      "MoneyCountVerificationId",
				      "SignalToFinishDepositProcessId");
        /* Finish deposit opening process*/
        assertThat(openDeposit).isEnded();


        /* start call activity RoadToHome*/
        var goingHomeProcess = runtimeService.createProcessInstanceQuery()
					.processDefinitionKey("GoingHome")
					.variableValueEquals("correlationId", "testCorrelationId")
					.active()
					.singleResult();
        assertThat(goingHomeProcess).isNotNull();

        assertThat(goingHomeProcess).isWaitingAt("ChooseTransportToHomeId");
        execute(job("ChooseTransportToHomeId"));

        assertThat(goingHomeProcess).hasPassed("ChooseTransportToHomeId",
					  "GoingHomeProcessPrint");

        assertThat(goingHomeProcess).isEnded();


        /* start process BankEmailCongrats*/
        var bankEmailCongrats = runtimeService.createProcessInstanceQuery()
					 .processDefinitionKey("BankEmailCongratsId")
					 .active()
					 .singleResult();
        assertThat(bankEmailCongrats).isNotNull();

        assertThat(bankEmailCongrats).hasPassed("SignalStartEmailCongratsId");

        assertThat(bankEmailCongrats).isWaitingAt("EmailDelayTimerId");

        var emailDelayTimerJob = managementService.createJobQuery()
					     .timers()
					     .activityId("EmailDelayTimerId")
					     .processInstanceId(bankEmailCongrats.getProcessInstanceId())
					     .singleResult();
        assertThat(emailDelayTimerJob).isNotNull();
        managementService.executeJob(emailDelayTimerJob.getId());

        assertThat(bankEmailCongrats).hasPassed("EmailDelayTimerId",
					   "ActivityCongratsEmailAfterwardsId",
					   "CongratsEndEvent");

        assertThat(bankEmailCongrats).isEnded();


        /* start process BankSmsCongrats*/
        var bankSmsCongrats = runtimeService.createProcessInstanceQuery()
				        .processDefinitionKey("BankSmsCongratsId")
				        .active()
				        .singleResult();
        assertThat(bankSmsCongrats).isNotNull();

        assertThat(bankSmsCongrats).hasPassed("SignalStartSmsCongratsId");

        assertThat(bankSmsCongrats).isWaitingAt("SmsDelayTimerId");

        var smsDelayTimerJob = managementService.createJobQuery()
					   .timers()
					   .activityId("SmsDelayTimerId")
					   .processInstanceId(bankSmsCongrats.getProcessInstanceId())
					   .singleResult();
        assertThat(smsDelayTimerJob).isNotNull();
        managementService.executeJob(smsDelayTimerJob.getId());

        assertThat(bankSmsCongrats).hasPassed("SmsDelayTimerId",
					 "ActivityCongratsSmsAfterwardsId",
					 "SmsCongratsEndEvent");

        assertThat(bankSmsCongrats).isEnded();
    }


    @Test
    void depositEndToEndTest_shouldCallToThePolice() {
        var variables = prepareVariables(MIKE);

        /*start MainDepositCreditProcess process*/
        var mainDepositCreditProcess = runtimeService.startProcessInstanceByKey("MainDepositCreditProcess",
								        "testBusinessKey",
								        variables);

        assertThat(mainDepositCreditProcess).isNotNull();
        assertThat(mainDepositCreditProcess).isWaitingAt("GoingToBankId");


        /*start GoingToBankId UT*/
        var goingToBank = taskService.createTaskQuery()
				 .taskName("Going to the bank")
				 .taskDefinitionKey("GoingToBankId")
				 .singleResult();
        assertThat(goingToBank).isNotNull();

        var userTaskVariables = new HashMap<String, Object>();
        userTaskVariables.put("transportMode", "walk");
        userTaskVariables.put("taxiCost", "0");

        assertThat(mainDepositCreditProcess).hasNotPassed("PayForTheTaxi");

        taskService.complete(goingToBank.getId(), userTaskVariables);
        assertThat(mainDepositCreditProcess).hasPassed("GoingToBankId");


        /*start GetTicketInQueueMachineId UT*/
        assertThat(mainDepositCreditProcess).isWaitingAt("GetTicketInQueueMachineId");
        assertThat(mainDepositCreditProcess).hasPassed("Gateway_0s7l7xt");

        var getTicketInQueueMachine = taskService.createTaskQuery()
					    .taskName("GetTicketInQueueMachine")
					    .taskDefinitionKey("GetTicketInQueueMachineId")
					    .singleResult();
        assertThat(getTicketInQueueMachine).isNotNull();

        var getTicketInQueueMachineVariables = new HashMap<String, Object>();
        getTicketInQueueMachineVariables.put("ticket", "deposit");

        taskService.complete(getTicketInQueueMachine.getId(), getTicketInQueueMachineVariables);
        assertThat(mainDepositCreditProcess).hasPassed("GetTicketInQueueMachineId");


        /* search for running call activity "OpenDeposit" */
        var openDeposit = runtimeService.createProcessInstanceQuery()
				    .processDefinitionKey("DepositOpening")
				    .variableValueEquals("correlationId", "testCorrelationId")
				    .active()
				    .singleResult();
        assertThat(openDeposit).isNotNull();

        assertThat(openDeposit).isWaitingAt("PassportProvidingId");
        execute(job("PassportProvidingId"));

        assertThat(openDeposit).hasPassed("PassportProvidingId", "DepositListProvidingId");
        assertThat(openDeposit).isWaitingAt("DepositChoosingId");


        /*start DepositChoosingId UT*/
        var userTaskDepositChoosing = taskService.createTaskQuery()
					    .processInstanceId(openDeposit.getProcessInstanceId())
					    .taskName("Take a look in to deposit list and choose one of them")
					    .taskDefinitionKey("DepositChoosingId")
					    .singleResult();
        assertThat(userTaskDepositChoosing).isNotNull();

        var userTaskDepositChoosingVariables = new HashMap<String, Object>();
        userTaskDepositChoosingVariables.put("depositName", "Early-Spring");

        taskService.complete(userTaskDepositChoosing.getId(), userTaskDepositChoosingVariables);

        assertThat(openDeposit).hasPassed("GatewayIsDepositChosenId",
				      "Gateway_1nzz7fv",
				      "ClientExistingCheckingId",
				      "ClientFullValidationId",
				      "GatewayMergeIsNewClientId",
				      "GatewayIsSuccessValidationId",
				      "GatewayIsClientCriminalId",
				      "CallThePoliceId",
				      "ClientIsCriminalErrorId");
        assertThat(openDeposit).hasNotPassed("StartVerificationSmsDelegateId",
					"SignalToFinishDepositProcessId");

        assertThat(openDeposit).isEnded();

        assertThat(mainDepositCreditProcess).hasPassed("ClientIsCriminalStartErrorId",
						 "ActivityRunOutOfTheBankId",
						 "ClientIsCriminalEndEventId");

        assertThat(mainDepositCreditProcess).isEnded();
    }

    @DisplayName("Should trow exception when client rejects to sign contract more than available number of deposits "
	   + "(The client hesitated and changed his mind to sign the contract,"
	   + " decided to see again what are the contributions) ")
    @Test
    void depositEndToEndTest_shouldInvokeAllTasks_whenContractIsNotSigned() {
        var variables = prepareVariables(DZMITRY);

        /*start MainDepositCreditProcess process*/
        var mainDepositCreditProcess = runtimeService.startProcessInstanceByKey("MainDepositCreditProcess",
								        "depositOpeningBusinessKey",
								        variables);

        assertThat(mainDepositCreditProcess).isNotNull();
        assertThat(mainDepositCreditProcess).isWaitingAt("GoingToBankId");


        /*start GoingToBankId UT*/
        var goingToBank = taskService.createTaskQuery()
				 .taskName("Going to the bank")
				 .taskDefinitionKey("GoingToBankId")
				 .singleResult();
        assertThat(goingToBank).isNotNull();

        var userTaskVariables = new HashMap<String, Object>();
        userTaskVariables.put("transportMode", "taxi");
        userTaskVariables.put("taxiCost", "15.50");

        taskService.complete(goingToBank.getId(), userTaskVariables);
        assertThat(mainDepositCreditProcess).hasPassed("GoingToBankId");


        /*start GetTicketInQueueMachineId UT*/
        assertThat(mainDepositCreditProcess).isWaitingAt("GetTicketInQueueMachineId");
        assertThat(mainDepositCreditProcess).hasPassed("Gateway_0s7l7xt");

        var getTicketInQueueMachine = taskService.createTaskQuery()
					    .taskName("GetTicketInQueueMachine")
					    .taskDefinitionKey("GetTicketInQueueMachineId")
					    .singleResult();
        assertThat(getTicketInQueueMachine).isNotNull();

        var getTicketInQueueMachineVariables = new HashMap<String, Object>();
        getTicketInQueueMachineVariables.put("ticket", "deposit");

        taskService.complete(getTicketInQueueMachine.getId(), getTicketInQueueMachineVariables);
        assertThat(mainDepositCreditProcess).hasPassed("PayForTheTaxi", "GetTicketInQueueMachineId");


        /* search for running call activity "OpenDeposit" */
        var openDeposit = runtimeService.createProcessInstanceQuery()
				    .processDefinitionKey("DepositOpening")
				    .variableValueEquals("correlationId", "testCorrelationId")
				    .active()
				    .singleResult();
        assertThat(openDeposit).isNotNull();

        assertThat(openDeposit).isWaitingAt("PassportProvidingId");
        execute(job("PassportProvidingId"));

        assertThat(openDeposit).hasPassed("PassportProvidingId", "DepositListProvidingId");
        assertThat(openDeposit).isWaitingAt("DepositChoosingId");


        /*start DepositChoosingId UT*/
        var userTaskDepositChoosing = taskService.createTaskQuery()
					    .processInstanceId(openDeposit.getProcessInstanceId())
					    .taskName("Take a look in to deposit list and choose one of them")
					    .taskDefinitionKey("DepositChoosingId")
					    .singleResult();
        assertThat(userTaskDepositChoosing).isNotNull();

        var userTaskDepositChoosingVariables = new HashMap<String, Object>();
        userTaskDepositChoosingVariables.put("depositName", "Early-Spring");

        taskService.complete(userTaskDepositChoosing.getId(), userTaskDepositChoosingVariables);

        assertThat(openDeposit).hasPassed("GatewayIsDepositChosenId",
				      "Gateway_1nzz7fv",
				      "ClientExistingCheckingId",
				      "ClientParticularValidationId",
				      "GatewayMergeIsNewClientId",
				      "GatewayIsSuccessValidationId",
				      "StartVerificationSmsDelegateId");

        assertThat(openDeposit).isWaitingAt("EndVerificationSmsDelegateId");


        /*start SmsVerification process*/
        var smsVerification = runtimeService.createProcessInstanceQuery()
				        .processDefinitionKey("SmsVerification")
				        .processInstanceBusinessKey("depositOpeningBusinessKey")
				        .active()
				        .singleResult();
        assertThat(smsVerification).isNotNull();

        assertThat(smsVerification).hasPassed("VerificationSmsStartMessageId",
					 "PrepareAndSendVerificationSmsId",
					 "VerificationSmsHandlingId");

        assertThat(smsVerification).isWaitingAt("ProvideSmsValidationCodeId");


        /*start ProvideSmsCodeUserTask UT*/
        var provideSmsCodeUserTask = taskService.createTaskQuery()
					   .taskName("Provide sms validation code")
					   .taskDefinitionKey("ProvideSmsValidationCodeId")
					   .singleResult();
        assertThat(provideSmsCodeUserTask).isNotNull();

        var mobileCode = (Integer) runtimeService.getVariable(smsVerification.getId(), "sendMobileCode");

        var provideSmsCodeUserTaskVariables = new HashMap<String, Object>();
        provideSmsCodeUserTaskVariables.put("sendMobileCode", mobileCode);
        provideSmsCodeUserTaskVariables.put("obtainedMobileCode", mobileCode);

        taskService.complete(provideSmsCodeUserTask.getId(), provideSmsCodeUserTaskVariables);

        assertThat(smsVerification).hasPassed("ProvideSmsValidationCodeId",
					 "ValidateCodeFromSmsId",
					 "GatewayIsSmsCodeValid");

        assertThat(smsVerification).isWaitingAt("SendSuccessVerificationSmsId");
        execute(job("SendSuccessVerificationSmsId"));

        /* Continue deposit opening process*/
        assertThat(openDeposit).hasPassed("EndVerificationSmsDelegateId",
				      "DocumentsPreparationId");

        assertThat(openDeposit).isWaitingAt("ReadAndSignContractId");


        /*start ReadAndSignContract UT*/
        var readAndSignContract = taskService.createTaskQuery()
					.processInstanceId(openDeposit.getProcessInstanceId())
					.taskName("Read and sign contract")
					.taskDefinitionKey("ReadAndSignContractId")
					.singleResult();
        assertThat(readAndSignContract).isNotNull();

        var readAndSignContractUserTaskVariables = new HashMap<String, Object>();
        readAndSignContractUserTaskVariables.put("isContractSigned", "false");

        taskService.complete(readAndSignContract.getId(), readAndSignContractUserTaskVariables);

        assertThat(openDeposit).hasPassed("ReadAndSignContractId",
				      "Gateway_1ls52z7");

        assertThat(openDeposit).hasNotPassed("DepositReplenishmentId");

        assertThat(openDeposit).isWaitingAt("DepositChoosingId");


        /*second traversing of tasks :*/
        var secondUserTaskDepositChoosing = taskService.createTaskQuery()
						 .processInstanceId(openDeposit.getProcessInstanceId())
						 .taskName("Take a look in to deposit list and choose one of them")
						 .taskDefinitionKey("DepositChoosingId")
						 .singleResult();
        assertThat(secondUserTaskDepositChoosing).isNotNull();

        var secondUserTaskDepositChoosingVariables = new HashMap<String, Object>();
        secondUserTaskDepositChoosingVariables.put("depositName", "Hot-Summer");

        taskService.complete(secondUserTaskDepositChoosing.getId(), secondUserTaskDepositChoosingVariables);

        assertThat(openDeposit).hasPassed("DepositChoosingId",
				      "GatewayIsDepositChosenId",
				      "Gateway_1nzz7fv");


        /*Link checking*/
        assertThat(openDeposit).isWaitingAt("DepositChoosingCountEndLinkId");
        execute(job("DepositChoosingCountEndLinkId"));

        assertThat(openDeposit).hasPassed("DocumentsPreparationId");

        assertThat(openDeposit).isWaitingAt("ReadAndSignContractId");


        /*second ReadAndSignContract  UT*/
        var secondTimeReadAndSignContract = taskService.createTaskQuery()
						 .processInstanceId(openDeposit.getProcessInstanceId())
						 .taskName("Read and sign contract")
						 .taskDefinitionKey("ReadAndSignContractId")
						 .singleResult();
        assertThat(secondTimeReadAndSignContract).isNotNull();

        var secondTimeReadAndSignContractUserTaskVariables = new HashMap<String, Object>();
        secondTimeReadAndSignContractUserTaskVariables.put("isContractSigned", "false");

        taskService.complete(secondTimeReadAndSignContract.getId(), secondTimeReadAndSignContractUserTaskVariables);

        assertThat(openDeposit).hasPassed("ReadAndSignContractId",
				      "Gateway_1ls52z7");

        assertThat(openDeposit).hasNotPassed("DepositReplenishmentId");

        assertThat(openDeposit).isWaitingAt("DepositChoosingId");


        /*third traversing of tasks :*/
        var thirdUserTaskDepositChoosing = taskService.createTaskQuery()
						.processInstanceId(openDeposit.getProcessInstanceId())
						.taskName("Take a look in to deposit list and choose one of them")
						.taskDefinitionKey("DepositChoosingId")
						.singleResult();
        assertThat(thirdUserTaskDepositChoosing).isNotNull();

        var thirdUserTaskDepositChoosingVariables = new HashMap<String, Object>();
        thirdUserTaskDepositChoosingVariables.put("depositName", "Colorful_Autumn");

        taskService.complete(thirdUserTaskDepositChoosing.getId(), thirdUserTaskDepositChoosingVariables);

        assertThat(openDeposit).hasPassed("DepositChoosingId",
				      "GatewayIsDepositChosenId",
				      "Gateway_1nzz7fv");


        /*Link checking*/
        assertThat(openDeposit).isWaitingAt("DepositChoosingCountEndLinkId");
        execute(job("DepositChoosingCountEndLinkId"));

        assertThat(openDeposit).hasPassed("DocumentsPreparationId");

        assertThat(openDeposit).isWaitingAt("ReadAndSignContractId");

        /*third ReadAndSignContract  UT*/
        var thirdTimeReadAndSignContract = taskService.createTaskQuery()
						.processInstanceId(openDeposit.getProcessInstanceId())
						.taskName("Read and sign contract")
						.taskDefinitionKey("ReadAndSignContractId")
						.singleResult();
        assertThat(thirdTimeReadAndSignContract).isNotNull();

        var thirdTimeReadAndSignContractUserTaskVariables = new HashMap<String, Object>();
        thirdTimeReadAndSignContractUserTaskVariables.put("isContractSigned", "false");

        /*should throw exception*/
        taskService.complete(thirdTimeReadAndSignContract.getId(), thirdTimeReadAndSignContractUserTaskVariables);

        assertThat(openDeposit).hasPassed("ErrorNoMoreDepositHandleId",
				      "ErrorNoMoreDepositThrowId");
        assertThat(openDeposit).isEnded();

        /*continue main process*/
        assertThat(mainDepositCreditProcess).hasPassed("EventNoMoreDepositsErrorStartId");
        assertThat(mainDepositCreditProcess).isWaitingAt("ActivityRoadToHomeAfterNoMoreDeposits");

        /* start call activity RoadToHome*/
        var goingHomeProcess = runtimeService.createProcessInstanceQuery()
					.processDefinitionKey("GoingHome")
					.variableValueEquals("correlationId", "testCorrelationId")
					.active()
					.singleResult();
        assertThat(goingHomeProcess).isNotNull();

        assertThat(goingHomeProcess).isWaitingAt("ChooseTransportToHomeId");
        execute(job("ChooseTransportToHomeId"));

        assertThat(goingHomeProcess).hasPassed("ChooseTransportToHomeId",
					  "GoingHomeProcessPrint");

        assertThat(goingHomeProcess).isEnded();

        assertThat(mainDepositCreditProcess).hasPassed("EventSuddenOperationInterruptionErrorWhenNoMoreDepositsEndId");
        assertThat(mainDepositCreditProcess).isEnded();

    }

    @DisplayName("Should trow exception and finish entire process when client does not have enough money on the wallet"
	   + " to deposit opening , since deposit has minimal required amount")
    @Test
    void depositEnDToEndTest_whenClientDoesNotHaveEnoughMoney_shouldThrowExceptionAndFinishEntireProcess() {
        var variables = prepareVariables(JOHN); //John has only 20.20 in his wallet

        /*start MainDepositCreditProcess process*/
        var mainDepositCreditProcess = runtimeService.startProcessInstanceByKey("MainDepositCreditProcess",
								        "depositOpeningBusinessKey",
								        variables);

        assertThat(mainDepositCreditProcess).isNotNull();
        assertThat(mainDepositCreditProcess).isWaitingAt("GoingToBankId");


        /*start GoingToBankId UT*/
        var goingToBank = taskService.createTaskQuery()
				 .taskName("Going to the bank")
				 .taskDefinitionKey("GoingToBankId")
				 .singleResult();
        assertThat(goingToBank).isNotNull();

        var userTaskVariables = new HashMap<String, Object>();
        userTaskVariables.put("transportMode", "taxi");
        userTaskVariables.put("taxiCost", "15.50");

        taskService.complete(goingToBank.getId(), userTaskVariables);
        assertThat(mainDepositCreditProcess).hasPassed("GoingToBankId")
				        .variables()
				        .extracting("client")
				        .extracting("wallet")
				        .extracting("moneyCount")
				        .isEqualTo(BigDecimal.valueOf(4.7)); // money when client is already paid for the taxi

        /*start GetTicketInQueueMachineId UT*/
        assertThat(mainDepositCreditProcess).isWaitingAt("GetTicketInQueueMachineId");
        assertThat(mainDepositCreditProcess).hasPassed("Gateway_0s7l7xt");

        var getTicketInQueueMachine = taskService.createTaskQuery()
					    .taskName("GetTicketInQueueMachine")
					    .taskDefinitionKey("GetTicketInQueueMachineId")
					    .singleResult();
        assertThat(getTicketInQueueMachine).isNotNull();

        var getTicketInQueueMachineVariables = new HashMap<String, Object>();
        getTicketInQueueMachineVariables.put("ticket", "deposit");

        taskService.complete(getTicketInQueueMachine.getId(), getTicketInQueueMachineVariables);
        assertThat(mainDepositCreditProcess).hasPassed("PayForTheTaxi", "GetTicketInQueueMachineId");


        /* search for running call activity "OpenDeposit" */
        var openDeposit = runtimeService.createProcessInstanceQuery()
				    .processDefinitionKey("DepositOpening")
				    .variableValueEquals("correlationId", "testCorrelationId")
				    .active()
				    .singleResult();
        assertThat(openDeposit).isNotNull();

        assertThat(openDeposit).isWaitingAt("PassportProvidingId");
        execute(job("PassportProvidingId"));

        assertThat(openDeposit).hasPassed("PassportProvidingId", "DepositListProvidingId");
        assertThat(openDeposit).isWaitingAt("DepositChoosingId");


        /*start DepositChoosingId UT*/
        var userTaskDepositChoosing = taskService.createTaskQuery()
					    .processInstanceId(openDeposit.getProcessInstanceId())
					    .taskName("Take a look in to deposit list and choose one of them")
					    .taskDefinitionKey("DepositChoosingId")
					    .singleResult();
        assertThat(userTaskDepositChoosing).isNotNull();

        var userTaskDepositChoosingVariables = new HashMap<String, Object>();
        userTaskDepositChoosingVariables.put("depositName", "Early-Spring");

        taskService.complete(userTaskDepositChoosing.getId(), userTaskDepositChoosingVariables);

        assertThat(openDeposit).hasPassed("GatewayIsDepositChosenId",
				      "Gateway_1nzz7fv",
				      "ClientExistingCheckingId",
				      "ClientParticularValidationId",
				      "GatewayMergeIsNewClientId",
				      "GatewayIsSuccessValidationId",
				      "StartVerificationSmsDelegateId");

        assertThat(openDeposit).isWaitingAt("EndVerificationSmsDelegateId");


        /*start SmsVerification process*/
        var smsVerification = runtimeService.createProcessInstanceQuery()
				        .processDefinitionKey("SmsVerification")
				        .processInstanceBusinessKey("depositOpeningBusinessKey")
				        .active()
				        .singleResult();
        assertThat(smsVerification).isNotNull();

        assertThat(smsVerification).hasPassed("VerificationSmsStartMessageId",
					 "PrepareAndSendVerificationSmsId",
					 "VerificationSmsHandlingId");

        assertThat(smsVerification).isWaitingAt("ProvideSmsValidationCodeId");


        /*start ProvideSmsCodeUserTask UT*/
        var provideSmsCodeUserTask = taskService.createTaskQuery()
					   .taskName("Provide sms validation code")
					   .taskDefinitionKey("ProvideSmsValidationCodeId")
					   .singleResult();
        assertThat(provideSmsCodeUserTask).isNotNull();

        var mobileCode = (Integer) runtimeService.getVariable(smsVerification.getId(), "sendMobileCode");

        var provideSmsCodeUserTaskVariables = new HashMap<String, Object>();
        provideSmsCodeUserTaskVariables.put("sendMobileCode", mobileCode);
        provideSmsCodeUserTaskVariables.put("obtainedMobileCode", mobileCode);

        taskService.complete(provideSmsCodeUserTask.getId(), provideSmsCodeUserTaskVariables);

        assertThat(smsVerification).hasPassed("ProvideSmsValidationCodeId",
					 "ValidateCodeFromSmsId",
					 "GatewayIsSmsCodeValid");

        assertThat(smsVerification).isWaitingAt("SendSuccessVerificationSmsId");
        execute(job("SendSuccessVerificationSmsId"));

        /* Continue deposit opening process*/
        assertThat(openDeposit).hasPassed("EndVerificationSmsDelegateId",
				      "DocumentsPreparationId");

        assertThat(openDeposit).isWaitingAt("ReadAndSignContractId");


        /*start ReadAndSignContract UT*/
        var readAndSignContract = taskService.createTaskQuery()
					.processInstanceId(openDeposit.getProcessInstanceId())
					.taskName("Read and sign contract")
					.taskDefinitionKey("ReadAndSignContractId")
					.singleResult();
        assertThat(readAndSignContract).isNotNull();

        var readAndSignContractUserTaskVariables = new HashMap<String, Object>();
        readAndSignContractUserTaskVariables.put("isContractSigned", "true");

        taskService.complete(readAndSignContract.getId(), readAndSignContractUserTaskVariables);

        // should throw an exception in DepositReplenishmentId
        assertThat(openDeposit).hasPassed("ErrorNotEnoughMoneyHandleId1");
        assertThat(openDeposit).hasPassed("ErrorNotEnoughMoneyId1")
			    .variables()
			    .extracting("client")
			    .extracting("wallet")
			    .extracting("moneyCount")
			    .isEqualTo(BigDecimal.valueOf(4.7));

        assertThat(openDeposit).isEnded();

        assertThat(mainDepositCreditProcess).hasPassed("EventNotEnoughMoneyErrorStartId");
        assertThat(mainDepositCreditProcess).isWaitingAt("RoadToHomeAfterNoEnoughMoneyExceptionCallActivityId");

        /* start call activity RoadToHome*/
        var goingHomeProcess = runtimeService.createProcessInstanceQuery()
					.processDefinitionKey("GoingHome")
					.variableValueEquals("correlationId", "testCorrelationId")
					.active()
					.singleResult();
        assertThat(goingHomeProcess).isNotNull();

        assertThat(goingHomeProcess).isWaitingAt("ChooseTransportToHomeId");
        execute(job("ChooseTransportToHomeId"));

        assertThat(goingHomeProcess).hasPassed("ChooseTransportToHomeId",
					  "GoingHomeProcessPrint");

        assertThat(goingHomeProcess).isEnded();

        assertThat(mainDepositCreditProcess).hasPassed("EventSuddenOperationInterruptionErrorWhenNotEnoughMoneyEndId");
        assertThat(mainDepositCreditProcess).isEnded();

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
        var mainDepositCreditProcess = runtimeService.startProcessInstanceByKey("MainDepositCreditProcess",
								        "depositOpeningBusinessKey",
								        variables);

        assertThat(mainDepositCreditProcess).isNotNull();
        assertThat(mainDepositCreditProcess).isWaitingAt("GoingToBankId");


        /*start GoingToBankId UT*/
        var goingToBank = taskService.createTaskQuery()
				 .taskName("Going to the bank")
				 .taskDefinitionKey("GoingToBankId")
				 .singleResult();
        assertThat(goingToBank).isNotNull();

        var userTaskVariables = new HashMap<String, Object>();
        userTaskVariables.put("transportMode", "taxi");
        userTaskVariables.put("taxiCost", "15.50");

        taskService.complete(goingToBank.getId(), userTaskVariables);
        assertThat(mainDepositCreditProcess).hasPassed("GoingToBankId");


        /*start GetTicketInQueueMachineId UT*/
        assertThat(mainDepositCreditProcess).isWaitingAt("GetTicketInQueueMachineId");
        assertThat(mainDepositCreditProcess).hasPassed("Gateway_0s7l7xt");

        var getTicketInQueueMachine = taskService.createTaskQuery()
					    .taskName("GetTicketInQueueMachine")
					    .taskDefinitionKey("GetTicketInQueueMachineId")
					    .singleResult();
        assertThat(getTicketInQueueMachine).isNotNull();

        var getTicketInQueueMachineVariables = new HashMap<String, Object>();
        getTicketInQueueMachineVariables.put("ticket", "deposit");

        taskService.complete(getTicketInQueueMachine.getId(), getTicketInQueueMachineVariables);
        assertThat(mainDepositCreditProcess).hasPassed("PayForTheTaxi", "GetTicketInQueueMachineId");


        /* search for running call activity "OpenDeposit" */
        var openDeposit = runtimeService.createProcessInstanceQuery()
				    .processDefinitionKey("DepositOpening")
				    .variableValueEquals("correlationId", "testCorrelationId")
				    .active()
				    .singleResult();
        assertThat(openDeposit).isNotNull();

        assertThat(openDeposit).isWaitingAt("PassportProvidingId");
        execute(job("PassportProvidingId"));

        assertThat(openDeposit).hasPassed("PassportProvidingId", "DepositListProvidingId");
        assertThat(openDeposit).isWaitingAt("DepositChoosingId");


        /*start DepositChoosingId UT*/
        var userTaskDepositChoosing = taskService.createTaskQuery()
					    .processInstanceId(openDeposit.getProcessInstanceId())
					    .taskName("Take a look in to deposit list and choose one of them")
					    .taskDefinitionKey("DepositChoosingId")
					    .singleResult();
        assertThat(userTaskDepositChoosing).isNotNull();

        var userTaskDepositChoosingVariables = new HashMap<String, Object>();
        userTaskDepositChoosingVariables.put("depositName", "Early-Spring");

        taskService.complete(userTaskDepositChoosing.getId(), userTaskDepositChoosingVariables);

        assertThat(openDeposit).hasPassed("GatewayIsDepositChosenId",
				      "Gateway_1nzz7fv",
				      "ClientExistingCheckingId",
				      "ClientParticularValidationId",
				      "GatewayMergeIsNewClientId",
				      "GatewayIsSuccessValidationId",
				      "StartVerificationSmsDelegateId");

        assertThat(openDeposit).isWaitingAt("EndVerificationSmsDelegateId");


        /*start SmsVerification process*/
        var smsVerification = runtimeService.createProcessInstanceQuery()
				        .processDefinitionKey("SmsVerification")
				        .processInstanceBusinessKey("depositOpeningBusinessKey")
				        .active()
				        .singleResult();
        assertThat(smsVerification).isNotNull();

        assertThat(smsVerification).hasPassed("VerificationSmsStartMessageId",
					 "PrepareAndSendVerificationSmsId",
					 "VerificationSmsHandlingId");

        assertThat(smsVerification).isWaitingAt("ProvideSmsValidationCodeId");


        /*start ProvideSmsCodeUserTask UT*/
        /*1st time sms code providing*/
        var provideSmsCodeUserTask = taskService.createTaskQuery()
					   .taskName("Provide sms validation code")
					   .taskDefinitionKey("ProvideSmsValidationCodeId")
					   .singleResult();
        assertThat(provideSmsCodeUserTask).isNotNull();

        var mobileCode = (Integer) runtimeService.getVariable(smsVerification.getId(), "sendMobileCode");
        var notValidSmsCode = 1234567;

        var provideSmsCodeUserTaskVariables = new HashMap<String, Object>();
        provideSmsCodeUserTaskVariables.put("sendMobileCode", mobileCode);
        provideSmsCodeUserTaskVariables.put("obtainedMobileCode", notValidSmsCode);

        taskService.complete(provideSmsCodeUserTask.getId(), provideSmsCodeUserTaskVariables);

        assertThat(smsVerification).hasPassed("ProvideSmsValidationCodeId",
					 "ValidateCodeFromSmsId",
					 "GatewayIsSmsCodeValid");
        assertThat(smsVerification).hasNotPassed("Flow_12zh4ga");

        assertThat(smsVerification).isWaitingAt("ProvideSmsValidationCodeId")
			        .variables()
			        .extracting("sendMobileCodeCount")
			        .isEqualTo(2);


        /*2nd time sms code providing*/
        var provideSmsCodeUserTask2Time = taskService.createTaskQuery()
					        .taskName("Provide sms validation code")
					        .taskDefinitionKey("ProvideSmsValidationCodeId")
					        .singleResult();
        assertThat(provideSmsCodeUserTask2Time).isNotNull();

        taskService.complete(provideSmsCodeUserTask2Time.getId(), provideSmsCodeUserTaskVariables);

        assertThat(smsVerification).hasPassed("ProvideSmsValidationCodeId",
					 "ValidateCodeFromSmsId",
					 "GatewayIsSmsCodeValid");
        assertThat(smsVerification).hasNotPassed("Flow_12zh4ga");

        assertThat(smsVerification).isWaitingAt("ProvideSmsValidationCodeId")
			        .variables()
			        .extracting("sendMobileCodeCount")
			        .isEqualTo(3);

        /*3rt time sms code providing*/
        var provideSmsCodeUserTask3Time = taskService.createTaskQuery()
					        .taskName("Provide sms validation code")
					        .taskDefinitionKey("ProvideSmsValidationCodeId")
					        .singleResult();
        assertThat(provideSmsCodeUserTask3Time).isNotNull();

        taskService.complete(provideSmsCodeUserTask3Time.getId(), provideSmsCodeUserTaskVariables);

        assertThat(smsVerification).hasPassed("ErrorNoMoreSmsValidationAttempts");
        assertThat(smsVerification).isWaitingAt("SendFailedVerificationSmsId");
        assertThat(smsVerification).hasNotPassed("Flow_12zh4ga");

        execute(job("SendFailedVerificationSmsId"));

        /*handle exception in deposit opening*/
        assertThat(openDeposit).hasPassed("StartFailedVerificationSmsMessageId",
				      "Activity_0c9bagm",
				      "EndFailedVerificationSmsErrorId");

        /*handle exception in main process*/
        assertThat(mainDepositCreditProcess).hasPassed("EventSuddenOperationInterruptionErrorStartId");
        assertThat(mainDepositCreditProcess).isWaitingAt("RoadToHomeAfterExeptionId");

        /* start call activity RoadToHome*/
        var goingHomeProcess = runtimeService.createProcessInstanceQuery()
					.processDefinitionKey("GoingHome")
					.variableValueEquals("correlationId", "testCorrelationId")
					.active()
					.singleResult();
        assertThat(goingHomeProcess).isNotNull();

        assertThat(goingHomeProcess).isWaitingAt("ChooseTransportToHomeId");
        execute(job("ChooseTransportToHomeId"));

        assertThat(goingHomeProcess).hasPassed("ChooseTransportToHomeId",
					  "GoingHomeProcessPrint");

        assertThat(goingHomeProcess).isEnded();
        assertThat(mainDepositCreditProcess).isEnded();
    }


    private Map<String, Object> prepareVariables(Client client) {
        var variableMap = new HashMap<String, Object>();
        variableMap.put("client", client);
        variableMap.put("correlationId", "testCorrelationId");

        return variableMap;
    }


}