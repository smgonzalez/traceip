package traceip.tracer.country;

import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.ext.web.client.WebClient;

import java.util.HashMap;
import java.util.Map;

public class CountryInfoVerticle extends AbstractVerticle {

    public static final String COUNTRY_INFO_ADDRESS = "country-info";

    private static final String COUNTRY_INFO_API = "restcountries.eu";
    private static final String COUNTRY_INFO_URI = "/rest/v2/alpha/";

    private WebClient webClient;

    private final Map<String, JsonObject> countryInfoByCode = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(CountryInfoVerticle.class);

    @Override
    public void start() {
        webClient = WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost(COUNTRY_INFO_API)
                .setTrustAll(true));

        vertx.eventBus().<String>consumer(COUNTRY_INFO_ADDRESS).handler(this::countryInfoHandler);
    }

    private void countryInfoHandler(Message<String> message) {
        String countryCode = message.body();

        JsonObject countryInfoCached = countryInfoByCode.get(countryCode);
        if (countryInfoCached != null) {
            message.reply(countryInfoCached);
        } else {
            webClient.get(COUNTRY_INFO_URI + countryCode)
                    .rxSend()
                    .map(apiResponse -> apiResponse.bodyAsJson(CountryInfo.class))
                    .subscribe(
                            countryInfo -> cacheAndReply(countryInfo, message),
                            throwable -> countryApiErrorHandler(throwable, message)
                    );
        }
    }

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
