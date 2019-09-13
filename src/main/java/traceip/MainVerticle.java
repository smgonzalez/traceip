package traceip;

import io.reactivex.Single;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import traceip.statistics.GetStatisticsVerticle;
import traceip.statistics.UpdateStatisticsVerticle;
import traceip.tracer.IpInformationVerticle;
import traceip.tracer.country.CountryInfoVerticle;
import traceip.tracer.currency.CurrencyVerticle;

import java.util.Arrays;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

    private HttpServer server;

    private final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Promise<Void> promise) {
        Router router = createRoutes();
        deployVerticles(promise, router);
    }

    private Router createRoutes() {
        Router router = Router.router(vertx);
        router.get("/ip-tracer").handler(new TraceIpHandler());
        router.get("/ip-statistics").handler(new StatisticsHandler());
        router.get("/*").handler(StaticHandler.create());
        return router;
    }

    private void deployVerticles(Promise<Void> promise, Router router) {
        DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(config());
        List<Single<String>> verticlesToDeploy = Arrays.asList(
                vertx.rxDeployVerticle(new IpInformationVerticle(), deploymentOptions),
                vertx.rxDeployVerticle(new CountryInfoVerticle(), deploymentOptions),
                vertx.rxDeployVerticle(new CurrencyVerticle(), deploymentOptions),
                vertx.rxDeployVerticle(new GetStatisticsVerticle(), deploymentOptions),
                vertx.rxDeployVerticle(new UpdateStatisticsVerticle(), deploymentOptions)
        );

        Single.zip(verticlesToDeploy, deploymentIds -> deploymentIds)
                .subscribe(objects -> {
                    this.server = vertx.createHttpServer()
                            .requestHandler(router)
                            .listen(config().getInteger("http.port"));
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
