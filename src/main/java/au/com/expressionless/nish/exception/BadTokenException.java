package au.com.expressionless.nish.exception;

public class BadTokenException extends RuntimeException {
    
    public BadTokenException(String token) {
        super(getMessage(token));
    }


    private static String getMessage(String token) {
        return "Error deserialising bad token: ".concat(token);
    }
}
