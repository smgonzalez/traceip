package traceip.tracer;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.ext.web.client.WebClient;
import traceip.statistics.UpdateStatisticsVerticle;
import traceip.tracer.currency.CurrencyVerticle;
import traceip.http.exceptions.HttpException;
import traceip.http.exceptions.NotFoundException;
import traceip.tracer.country.CountryInfo;
import traceip.tracer.country.CountryInfoVerticle;
import traceip.tracer.country.Currency;
import traceip.tracer.distances.DistanceCalculator;
import traceip.tracer.distances.GeoLocation;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class IpInformationVerticle extends AbstractVerticle {

    public static final String IP_INFORMATION_ADDRESS = "ip-information";

    private static final String IP_LOCATION_HOST = "api.ip2country.info";
    private static final String IP_LOCATION_URI = "/ip?";

    private final GeoLocation refPointGeoLocation = new GeoLocation( -34.61315, -58.37723);

    private WebClient webClient;

    private final Logger logger = LoggerFactory.getLogger(IpInformationVerticle.class);

    @Override
    public void start() {
        webClient = WebClient.create(vertx, new WebClientOptions()
            .setTrustAll(true));
        vertx.eventBus().<String>consumer(IP_INFORMATION_ADDRESS).handler(this::ipInformationHandler);
    }

    private void ipInformationHandler(Message<String> message) {
        webClient
                .get(IP_LOCATION_HOST, IP_LOCATION_URI + message.body())
                .rxSend()
                .flatMap(response -> {
                    String countryCode = response.bodyAsJsonObject().getString("countryCode");
                    if (countryCode == null || countryCode.isEmpty()) {
                        throw new NotFoundException(404, "The IP provided, does not exist");
                    }
                    return vertx.eventBus().<JsonObject>rxRequest(CountryInfoVerticle.COUNTRY_INFO_ADDRESS, countryCode);
                })
                .flatMap(countryInfoJson -> enrichCountryInfoAndReply(countryInfoJson, message))
                .subscribe(
                        ipInformation -> updateStatisticsAndReply(ipInformation, message),
                        throwable -> ipInformationErrorHandler(throwable, message)
                );
    }

    private void updateStatisticsAndReply(JsonObject ipInformation, Message<String> message) {
        Long distance = ipInformation.getLong("distanceToRefPoint");
        vertx.eventBus().publish(UpdateStatisticsVerticle.UPDATE_STATISTICS_ADDRESS, distance);
        message.reply(ipInformation);
    }

    private Single<JsonObject> enrichCountryInfoAndReply(Message<JsonObject> reply, Message<String> message) {
        CountryInfo countryInfo = Json.mapper.convertValue (reply.body(), CountryInfo.class);
        GeoLocation geoLocation = getGeoLocation(countryInfo);
        Currency currency = countryInfo.getCurrencies().iterator().next();

        SingleSource<Long> asyncDistanceTo = getDistanceToRefPoint(geoLocation);
        SingleSource<Double> asyncDollarConvertion = getDollarConvertion(currency);

        return Single.zip(asyncDistanceTo, asyncDollarConvertion, (distanceTo, dollarConvertion) -> {
                IpInformation ipInformation = new IpInformation(
                        countryInfo.getIsoCode(),
                        countryInfo.getName(),
                        countryInfo.getLanguages(),
                        getLocalTime(countryInfo),
                        countryInfo.getTimezones(),
                        geoLocation,
                        distanceTo,
                        currency,
                        dollarConvertion
                );
                return new JsonObject(Json.encode(ipInformation));
        });
    }

    private SingleSource<Double> getDollarConvertion(Currency currency) {
        return vertx.eventBus().<Double>rxRequest(CurrencyVerticle.DOLLAR_CONVERTION_ADDRESS, currency.getCode())
                .map(Message::body);
    }

    private SingleSource<Long> getDistanceToRefPoint(GeoLocation geoLocation) {
        DistanceCalculator distanceCalculator = new DistanceCalculator(geoLocation, refPointGeoLocation);

        return vertx.<Long>rxExecuteBlocking(event -> event.complete(distanceCalculator.calculate()))
                .switchIfEmpty(Single.just(-1L));
    }

    private GeoLocation getGeoLocation(CountryInfo countryInfo) {
        List<Double> latlng = countryInfo.getLatlng();
        if (latlng.size() < 2) {
            return null;
        }
        return new GeoLocation(latlng.get(0), latlng.get(1));
    }

    private String getLocalTime(CountryInfo countryInfo) {
        if (countryInfo.getTimezones().isEmpty())
            return "";

        String timeZone = countryInfo.getTimezones().iterator().next();
        ZoneOffset timeOffset = "UTC".equals(timeZone)
                ? ZoneOffset.UTC
                : ZoneOffset.of(timeZone.replace("UTC", ""));

        LocalTime now = LocalTime.now(timeOffset);
        String localTime = now.format(DateTimeFormatter.ISO_TIME);
        return String.format("%s (%s)", localTime, timeZone);
    }

    private void ipInformationErrorHandler(Throwable throwable, Message<String> message) {
        if (throwable instanceof HttpException) {
            HttpException exception = (HttpException) throwable;
            message.fail(exception.getHttpCode(), exception.getMessage());
        } else {
            logger.error("An error ocurred trying to fetch IP Location", throwable);
            message.fail(500, throwable.getMessage());
        }

    }

}
