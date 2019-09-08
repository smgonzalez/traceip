package traceip.statistics;

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.Message;

public class GetStatisticsVerticle extends AbstractVerticle {

    public static final String GET_STATISTICS_ADDRESS = "get-statistics";

    private JsonObject statistics = new JsonObject();

    @Override
    public void start() {
        vertx.eventBus().<JsonObject>consumer(UpdateStatisticsVerticle.PUBLISH_STATISTICS_ADDRESS).handler(this::updateLocalStatistics);
        vertx.eventBus().consumer(GET_STATISTICS_ADDRESS).handler(this::getLocalStatistics);
    }

    private void getLocalStatistics(Message<Object> message) {
        message.reply(statistics);
    }

    private void updateLocalStatistics(Message<JsonObject> statisticsMessage) {
        statistics = statisticsMessage.body();
    }
}
