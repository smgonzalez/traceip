package traceip.http.exceptions;

public class HttpException extends RuntimeException {

    private final int httpCode;
    private final String message;

    public HttpException(int httpCode, String message) {
        this.httpCode = httpCode;
        this.message = message;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public String getMessage() {
        return message;
    }
}
