package com.example.bankService.serivce.deposit.delegate.bank;

import static com.example.bankService.util.Constants.LIMIT_OF_VERIFICATION_SMS_ATTEMPTS_EXCEEDED;

import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("smsValidationDelegate")
public class SmsValidationDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("the SmsValidationDelegate has started...");

        var obtainedMobileCode = (Integer) execution.getVariable("obtainedMobileCode");
        var sendMobileCode = (Integer) execution.getVariable("sendMobileCode");

        if (ObjectUtils.anyNull(obtainedMobileCode, sendMobileCode)) {
	   throw new IllegalArgumentException(
		  String.format("One of the arguments is null : %s , %s ! ", obtainedMobileCode, sendMobileCode)
	   );
        }

        if (Objects.equals(obtainedMobileCode, sendMobileCode)) {
	   execution.setVariable("isSmsCodeValid", true);
        } else {
	   var sendMobileCodeCount = (Integer) Optional.ofNullable(execution.getVariable("sendMobileCodeCount"))
						  .orElse(1);

	   if (sendMobileCodeCount == 3) {
	       throw new BpmnError(LIMIT_OF_VERIFICATION_SMS_ATTEMPTS_EXCEEDED,
				"The count of chances to verify mobile code is greater than allowed!");
	   }
	   execution.setVariable("isSmsCodeValid", false);

	   log.info("The verification sms code does not match the sent one!");
        }

    }
}