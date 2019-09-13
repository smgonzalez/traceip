package traceip.statistics;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.Message;

/**
 * Recibe las actualizaciones del uso del API, y notifica al/los Verticle/s encargados de retornarlas.
 */
public class UpdateStatisticsVerticle extends AbstractVerticle {

    public static final String PUBLISH_STATISTICS_ADDRESS = "publish-statistics";
    public static final String UPDATE_STATISTICS_ADDRESS = "update-use-statistics";

    private final Logger logger = LoggerFactory.getLogger(UpdateStatisticsVerticle.class);

    private Long max = 0L;
    private Long min = 0L;
    private Long total = 0L;
    private Long totalUses = 0L;

    @Override
    public void start() {
        vertx.eventBus().<Long>consumer(UPDATE_STATISTICS_ADDRESS).handler(this::updateStatistics);
    }

    private void updateStatistics(Message<Long> distanceMessage) {
        Long distance = distanceMessage.body();
        max = Math.max(max, distance);
        min = min == 0L ? distance : Math.min(min, distance);
        total += distance;
        totalUses++;

        DistanceStatistics statistics = new DistanceStatistics(max, min, total / totalUses);
        vertx.eventBus().publish(PUBLISH_STATISTICS_ADDRESS, new JsonObject(Json.encode(statistics)));
        distanceMessage.reply("OK");
    }
}
