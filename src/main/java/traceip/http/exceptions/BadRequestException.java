package traceip.http.exceptions;

public class BadRequestException extends HttpException {

    public BadRequestException(String message) {
        super(400, message);
    }

}
