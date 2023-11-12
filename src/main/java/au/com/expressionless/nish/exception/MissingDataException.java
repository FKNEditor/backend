package au.com.expressionless.nish.exception;

public class MissingDataException extends RuntimeException {
    
    public MissingDataException(long id) {
        super("Missing data for id: " + id);
    }
}
