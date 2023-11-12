package au.com.expressionless.nish.exception;

import au.com.expressionless.nish.utils.ANSIColour;

public class BadDataException extends RuntimeException {
    

    public BadDataException(String message) {
        super(ANSIColour.doRed("Bad Data: " + message));
    }
}
