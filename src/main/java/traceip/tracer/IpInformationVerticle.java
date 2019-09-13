package traceip.tracer;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import traceip.http.exceptions.BadRequestException;
import traceip.http.exceptions.HttpException;
import traceip.http.exceptions.NotFoundException;
import traceip.statistics.UpdateStatisticsVerticle;
import traceip.tracer.country.CountryInfo;
import traceip.tracer.country.CountryInfoVerticle;
import traceip.tracer.country.Currency;
import traceip.tracer.currency.CurrencyVerticle;
import traceip.tracer.distances.DistanceCalculator;
import traceip.tracer.distances.GeoLocation;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Verticle encargado de recopilar la siguiente información relacionada con una IP:
 *
 * <ul>
 *     <li>Pais de localizacion</li>
 *     <li>Información general del pais de localización</li>
 *     <li>Distancias hacia un punto de referencias fijo (Bs As)</li>
 *     <li>Información de la moneda del pais, y su cotización en dolares</li>
 * </ul>
 *
 * Para conseguir la información del pais, y la información de monedas, se comunica via EventBus hacia distintos verticles
 * Para conseguir la distancias a Bs As, realiza un calculo de manera no bloqueante
 *
 */
public class IpInformationVerticle extends AbstractVerticle {

    public static final String IP_INFORMATION_ADDRESS = "ip-information";

    private String ipLocationUri;
    private GeoLocation refPointGeoLocation;

    private WebClient webClient;

    private final Logger logger = LoggerFactory.getLogger(IpInformationVerticle.class);

    @Override
    public void start(Promise<Void> promise) {
        ConfigRetriever retriever = ConfigRetriever.create(vertx);
        retriever.getConfig(event -> initialize(event.result(), promise));
    }

    private void initialize(JsonObject config, Promise<Void> promise) {
        webClient = WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost(config.getString("api.ip-location.host"))
                .setTrustAll(true));

        ipLocationUri = config.getString("api.ip-location.uri");
        refPointGeoLocation = new GeoLocation(
                config.getDouble("refPoint.geolocation.latitude"),
                config.getDouble("refPoint.geolocation.longitude")
        );

        vertx.eventBus().<String>consumer(IP_INFORMATION_ADDRESS).handler(this::ipInformationHandler);
        promise.complete();
    }

    private void ipInformationHandler(Message<String> message) {
        webClient
                .get(String.format(ipLocationUri, message.body()))
                .rxSend()
                .flatMap(this::requestCountryInfo)
                .flatMap(this::enrichCountryInfo)
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

    /**
     * Enriquese la información del pais, con la información de la distancia a Bs As, y la cotizacion de
     * la moneda local.
     * Luego, response el mensaje original, con esta información
     *
     * @param countryInfoResponse Informacion general del pais
     * @return Single que al resolverse, retornara un JsonObject, con la informacion del pais enriquecida
     */
    private Single<JsonObject> enrichCountryInfo(Message<JsonObject> countryInfoResponse) {
        CountryInfo countryInfo = Json.mapper.convertValue (countryInfoResponse.body(), CountryInfo.class);
        GeoLocation geoLocation = getGeoLocation(countryInfo);
        Currency currency = countryInfo.getCurrencies().iterator().next();

        SingleSource<Long> asyncDistanceTo = getDistanceToRefPoint(geoLocation);
        SingleSource<Double> asyncDollarConvertion = getDollarConvertion(currency);

        return Single.zip(asyncDistanceTo, asyncDollarConvertion, (distanceTo, dollarConvertion) -> {
                IpInformation ipInformation = new IpInformation(
                        countryInfo.getIsoCode(),
                        countryInfo.getName(),
                        countryInfo.getNativeName(),
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

    /**
     * Obtiene la cotización del dolar
     *
     * @see traceip.tracer.currency.CurrencyVerticle
     * @param currency Moneda a consultar
     * @return SingleSource que al resolverse, retornara la cotizacion
     */
    private SingleSource<Double> getDollarConvertion(Currency currency) {
        return vertx.eventBus().<Double>rxRequest(CurrencyVerticle.DOLLAR_CONVERTION_ADDRESS, currency.getCode())
                .map(Message::body);
    }

    /**
     * Obtiene la distsancia a Bs As, realizando un calculo no bloqueante
     *
     * @param geoLocation Geolocalización
     * @return SingleSource que al resolverse, retornara la distancia
     */
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

    /**
     * Obtiene la hora actual del pais, en algun timezone definido para el mismo
     */
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

    /**
     * Valida la respuesta del servicio que retorna la localización de la IP,
     * y solicita la informacion general del pais
     *
     * @see traceip.tracer.country.CountryInfoVerticle
     * @param response Single que al resolverse, retornara un JsonObject con la informacion general del pais
     * @return
     */
    private Single<Message<JsonObject>> requestCountryInfo(HttpResponse<Buffer> response) {
        if (response.statusCode() == 400) {
            throw new BadRequestException("The IP provided is invalid");
        }
        String countryCode = response.bodyAsJsonObject().getString("countryCode");
        if (countryCode == null || countryCode.isEmpty()) {
            throw new NotFoundException("The IP provided, does not exist");
        }
        return vertx.eventBus().rxRequest(CountryInfoVerticle.COUNTRY_INFO_ADDRESS, countryCode);
    }
}
