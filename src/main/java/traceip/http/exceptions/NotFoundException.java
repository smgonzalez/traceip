package traceip.http.exceptions;

public class NotFoundException extends HttpException {

    public NotFoundException(String message) {
        super(404, message);
    }

}
