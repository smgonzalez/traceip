package traceip.tracer.country;

import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.ext.web.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Verticle encargado de consultar la información general de un pais
 * Mantiene una cache en memoria, con la información ya consultada, para retornarla mas rapido posteriormente.
 * Como en Vert.x, un verticle siempre corre en el mismo thread, no es necesario tener consideraciones sobre
 * la concurrencia.
 */
public class CountryInfoVerticle extends AbstractVerticle {

    public static final String COUNTRY_INFO_ADDRESS = "country-info";

    private String countryInfoUri;
    private WebClient webClient;

    private final Map<String, JsonObject> countryInfoByCode = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(CountryInfoVerticle.class);

    @Override
    public void start(Promise<Void> promise) {
        ConfigRetriever retriever = ConfigRetriever.create(vertx);
        retriever.getConfig(event -> initialize(event.result(), promise));
    }

    private void initialize(JsonObject config, Promise<Void> promise) {
        webClient = WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost(config.getString("api.country-info.host"))
                .setTrustAll(true));
        countryInfoUri = config.getString("api.country-info.uri");

        vertx.eventBus().<String>consumer(COUNTRY_INFO_ADDRESS).handler(this::countryInfoHandler);
        promise.complete();
    }

    /**
     * Responde el mensaje {@code message}, con la información de la pais indicado.
     * Si el pais está chacheado, la retorna directamente.
     * Si el pais no esta chacheado, se envia una consulta asincronica via REST, y la misma se cacheara posteriormente
     * @param message El mensaje con el código del pais
     */
    private void countryInfoHandler(Message<String> message) {
        String countryCode = message.body();

        JsonObject countryInfoCached = countryInfoByCode.get(countryCode);
        if (countryInfoCached != null) {
            message.reply(countryInfoCached);
        } else {
            webClient.get(String.format(countryInfoUri, countryCode))
                    .rxSend()
                    .map(apiResponse -> apiResponse.bodyAsJson(CountryInfo.class))
                    .subscribe(
                            countryInfo -> cacheAndReply(countryInfo, message),
                            throwable -> countryApiErrorHandler(throwable, message)
                    );
        }
    }

    /**
     * Cachea la información del pais, y responde el mensaje con la misma.
     *
     * @param countryInfo Objeto con la información que será cacheada
     * @param message Mensaje a responder
     */
    private void cacheAndReply(CountryInfo countryInfo, Message<String> message) {
        String isoCode = countryInfo.getIsoCode();
        JsonObject countryInfoJson = new JsonObject(Json.encode(countryInfo));
        countryInfoByCode.put(isoCode, countryInfoJson);
        message.reply(countryInfoJson);
    }

    private void countryApiErrorHandler(Throwable throwable, Message<String> message) {
        logger.error("An error ocurred trying to fetch country information", throwable);
        message.fail(500, throwable.getMessage());
    }

}
