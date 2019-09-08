package traceip.http.exceptions;

public class NotFoundException extends HttpException {

    public NotFoundException(int httpCode, String message) {
        super(httpCode, message);
    }

}
