var ticket  = execution.getVariable('ticket') || 'your service';
var clientName  = execution.getVariable('clientName');

print('Dear ' + clientName + ', '
    + '\n By sending this SMS we want to tell  '
    + '\n thank you for visiting our bank for obtaining your '
    + ticket + ' !'
    + '\n We hope that our services have met the highest standards. '
    + '\n If you have any questions, please feel free to contact us by call. '
    + '\n Best regards,\nYour Bank Team '
    );