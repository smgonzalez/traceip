package traceip;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;
import traceip.tracer.IpInformationVerticle;

import java.util.regex.Pattern;

public class TraceIpHandler implements Handler<RoutingContext> {

    private static final Pattern IP_REGEXP = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");

    private final Logger logger = LoggerFactory.getLogger(TraceIpHandler.class);

    @Override
    public void handle(RoutingContext event) {
        HttpServerRequest request = event.request();
        HttpServerResponse response = event.response();
        response.putHeader("content-type", "application/json");

        String ip = request.getParam("ip");
        if (ip == null) {
            response.setStatusCode(400);
            response.end("Missing parameter [ip]");
        } else if (!IP_REGEXP.matcher(ip).matches()) {
            response.setStatusCode(400);
            response.end("Invalid parameter [ip]");
        } else {
            event.vertx().eventBus()
                    .<JsonObject>rxRequest(IpInformationVerticle.IP_INFORMATION_ADDRESS, ip)
                    .subscribe(
                            reply -> response.end(reply.body().encode()),
                            throwable -> handleError(throwable, response)
                    );
        }
    }

    private void handleError(Throwable throwable, HttpServerResponse response) {
        if (throwable instanceof ReplyException) {
            ReplyException replyException = (ReplyException) throwable;
            response.setStatusCode(replyException.failureCode());
            response.end(replyException.getMessage());
        } else {
            logger.error(throwable);
            response.setStatusCode(500);
            response.end("Internal server error");
        }

    }
}
