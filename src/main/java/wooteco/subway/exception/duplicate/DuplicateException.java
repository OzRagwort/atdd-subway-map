package wooteco.subway.exception.duplicate;

public class DuplicateException extends IllegalArgumentException {
    public DuplicateException(String message) {
        super(message);
    }
}
