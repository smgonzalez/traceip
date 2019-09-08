package traceip;

import io.reactivex.Single;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import traceip.statistics.GetStatisticsVerticle;
import traceip.statistics.UpdateStatisticsVerticle;
import traceip.tracer.currency.CurrencyVerticle;
import traceip.tracer.IpInformationVerticle;
import traceip.tracer.country.CountryInfoVerticle;

import java.util.Arrays;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

    private static final Integer PORT = 8080;
    private HttpServer server;

    private final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Promise<Void> promise) {
        Router router = Router.router(vertx);
        router.get("/ip-tracer").handler(new TraceIpHandler());
        router.get("/ip-statistics").handler(new StatisticsHandler());
        router.get("/*").handler(StaticHandler.create());

        List<Single<String>> verticlesToDeploy = Arrays.asList(
                vertx.rxDeployVerticle(new IpInformationVerticle()),
                vertx.rxDeployVerticle(new CountryInfoVerticle()),
                vertx.rxDeployVerticle(new CurrencyVerticle()),
                vertx.rxDeployVerticle(new GetStatisticsVerticle()),
                vertx.rxDeployVerticle(new UpdateStatisticsVerticle())
        );

        Single.zip(verticlesToDeploy, deploymentIds -> deploymentIds)
                .subscribe(objects -> {
                    this.server = vertx.createHttpServer()
                            .requestHandler(router)
                            .listen(PORT);
                    promise.complete();
                }, throwable -> {
                    String error = "Unable to deploy verticles";
                    logger.error(error, throwable);
                    promise.fail(error);
                });
    }

    @Override
    public void stop() throws Exception {
        server.close();
        super.stop();
    }

}