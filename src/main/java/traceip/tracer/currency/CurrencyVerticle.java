package traceip.tracer.currency;

import io.reactivex.Single;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;

import java.time.LocalDateTime;

public class CurrencyVerticle extends AbstractVerticle {

    public static final String DOLLAR_CONVERTION_ADDRESS = "dollar-convertion";

    private static final String apiHost = "data.fixer.io";
    private static final String apiUri = "/api/latest";
    private static final String apiKey = "bad78aebe52cdd744e24e78b88785ac8";

    private static final String euroCode = "EUR";
    private static final String dollarCode = "USD";

    private static final int EXCHANGES_EXPIRATION_TIME = 60;
    private static final int API_RETRY_DELAY = 5;

    private JsonObject euroExchanges;
    private LocalDateTime exchangeExpiration;

    private WebClient webClient;
    private final Logger logger = LoggerFactory.getLogger(CurrencyVerticle.class);

    @Override
    public void start(Promise<Void> promise) {
        webClient = WebClient.create(vertx, new WebClientOptions()
                .setTrustAll(true));

        vertx.eventBus().<String>consumer(DOLLAR_CONVERTION_ADDRESS).handler(this::dollarConvertionHandler);

        getExchangeInformation()
                .subscribe(entries -> {
                    logger.info("Finish getting exchanges");
                    parseAndCacheResponse(entries);
                    promise.complete();
                }, throwable -> {
                    logger.error("Unable to fetch dollar exchange information", throwable);
                    resetExchangesCache();
                    promise.complete();
                });
    }

    private void dollarConvertionHandler(Message<String> message) {
        LocalDateTime now = LocalDateTime.now();
        if (euroExchanges != null && exchangeExpiration != null && now.isBefore(exchangeExpiration)) {
            message.reply(getConvertion(message.body()));
        } else {
            getExchangeInformation().subscribe(convertions -> {
                logger.info("Finish getting exchanges");
                parseAndCacheResponse(convertions);
                message.reply(getConvertion(message.body()));
            }, throwable -> {
                logger.error("Unable to fetch dollar exchange information", throwable);
                resetExchangesCache();
                message.reply(0d);
            });
        }
    }

    private double getConvertion(String currencyCode) {
        if (euroExchanges.containsKey(currencyCode)) {
            Double euroConvertion = euroExchanges.getDouble(currencyCode);
            Double dollarConvertion = euroExchanges.getDouble(dollarCode);
            return 1d / (euroConvertion / dollarConvertion);
        } else {
            return 0d;      // Convertion not available
        }
    }

    private void parseAndCacheResponse(JsonObject response) {
        if (response.getBoolean("success")) {                       // Save new convertions
            this.euroExchanges = response.getJsonObject("rates");
            this.exchangeExpiration = LocalDateTime.now().plusMinutes(EXCHANGES_EXPIRATION_TIME);
        } else {
            logger.error("Error in exchanges API. All further dollar exchanges will be 0");
            resetExchangesCache();
        }
    }

    private void resetExchangesCache() {
        this.euroExchanges = new JsonObject();
        this.exchangeExpiration = LocalDateTime.now().plusMinutes(API_RETRY_DELAY);   // For not spending to many API uses
    }

    private Single<JsonObject> getExchangeInformation() {
        logger.info("Getting exchanges");
        return webClient.get(apiHost, apiUri)
                .addQueryParam("access_key", apiKey)
                .addQueryParam("base", euroCode)        // Only available base is EURO
                .rxSend()
                .map(HttpResponse::bodyAsJsonObject);
    }

    private void currencyApiErrorHandler(Throwable throwable, Message<String> message) {
        logger.error("An error ocurred trying to fetch dollar exchange information", throwable);
        message.fail(500, throwable.getMessage());
    }

}
