package traceip.tracer.currency;

import io.reactivex.Single;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;

import java.time.LocalDateTime;

/**
 * Al crearse este Verticle, consultara la cotización en EUROS de todas las monedas disponibles, y las guardara en memoria.
 * Luego de pasado el tiempo {@code exchangesExpirationTime}, volver a consultar las cotizaciones.
 *
 * En caso de que la consulta de las cotizaciones falle, se reintentara en no menos de lo indicado en {@code apiRetryDelay},
 * para de esta forma, no desgastar los usos limitados del API.
 *
 */
public class CurrencyVerticle extends AbstractVerticle {

    public static final String DOLLAR_CONVERTION_ADDRESS = "dollar-convertion";

    private String apiUri;
    private String apiKey;

    private String euroCode;
    private String dollarCode;

    private int exchangesExpirationTime;
    private int apiRetryDelay;

    private JsonObject euroExchanges;
    private LocalDateTime exchangeExpiration;

    private WebClient webClient;
    private final Logger logger = LoggerFactory.getLogger(CurrencyVerticle.class);

    @Override
    public void start(Promise<Void> promise) {
        ConfigRetriever retriever = ConfigRetriever.create(vertx);
        retriever.getConfig(event -> initialize(event.result(), promise));
    }

    private void initialize(JsonObject config, Promise<Void> promise) {
        webClient = WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost(config.getString("api.currency-info.host"))
                .setTrustAll(true));

        this.apiUri = config.getString("api.currency-info.uri");
        this.apiKey = config.getString("api.currency-info.key");
        this.euroCode = config.getString("api.currency-info.euroCode");
        this.dollarCode = config.getString("api.currency-info.dollarCode");
        this.exchangesExpirationTime = config.getInteger("api.currency-info.exchangesExpirationTime");
        this.apiRetryDelay = config.getInteger("api.currency-info.apiRetryDelay");

        vertx.eventBus().<String>consumer(DOLLAR_CONVERTION_ADDRESS).handler(this::dollarConvertionHandler);

        getExchangeInformation()
                .subscribe(exchanges -> {
                    logger.info("Finish getting exchanges");
                    parseAndCacheResponse(exchanges);
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
            return 0d;  // Convertion not available
        }
    }

    private void parseAndCacheResponse(JsonObject response) {
        if (response.getBoolean("success")) {                       // Save new convertions
            this.euroExchanges = response.getJsonObject("rates");
            this.exchangeExpiration = LocalDateTime.now().plusMinutes(exchangesExpirationTime);
        } else {
            logger.error("Error in exchanges API. All further dollar exchanges will be 0");
            resetExchangesCache();
        }
    }

    private void resetExchangesCache() {
        this.euroExchanges = new JsonObject();
        this.exchangeExpiration = LocalDateTime.now().plusMinutes(apiRetryDelay);   // For not spending to many API uses
    }

    private Single<JsonObject> getExchangeInformation() {
        logger.info("Getting exchanges");
        return webClient.get(apiUri)
                .addQueryParam("access_key", apiKey)
                .addQueryParam("base", euroCode)        // Only available base is EURO
                .rxSend()
                .map(HttpResponse::bodyAsJsonObject);
    }

}
