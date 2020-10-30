package bio.ferlab.clin.exceptions;

public class FailedToUpdateIndexException extends RuntimeException {
    public FailedToUpdateIndexException(String index) {
        super(String.format("Failed to update index [%s]", index));
    }
}
