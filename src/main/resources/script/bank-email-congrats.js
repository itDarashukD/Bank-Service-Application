var ticket  = execution.getVariable('ticket') || 'your service';
var clientName  = execution.getVariable('clientName');

print('Dear ' + clientName + ', '
    + '\n By this Email we want to tell  '
    + '\n thank you for visiting our bank for obtaining your '
    + ticket + ' !'
    + '\n We hope that our services have met the highest standards. '
    + '\n If you have any questions, please feel free to contact us by our email. '
    + '\n Best regards,\nYour Bank Team '
    );