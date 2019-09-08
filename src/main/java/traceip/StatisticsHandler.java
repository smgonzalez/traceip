package traceip;

import io.reactivex.disposables.Disposable;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;
import traceip.statistics.GetStatisticsVerticle;

public class StatisticsHandler implements Handler<RoutingContext> {

    private final Logger logger = LoggerFactory.getLogger(StatisticsHandler.class);

    @Override
    public void handle(RoutingContext event) {
        HttpServerResponse response = event.response();
        response.putHeader("content-type", "application/json");

        event.vertx().eventBus()
                .<JsonObject>rxRequest(GetStatisticsVerticle.GET_STATISTICS_ADDRESS, "GET")
                .subscribe(
                        statistics -> response.end(statistics.body().encode()),
                        throwable -> handleError(throwable, response)
                );
    }

    private void handleError(Throwable throwable, HttpServerResponse response) {
        logger.error(throwable);
        response.setStatusCode(500);
        response.end("Unable tu get statistics");
    }

}
