package me.carbou.mathieu.tictactoe;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.guestful.jaxrs.filter.cors.CorsFilter;
import com.guestful.jaxrs.filter.jsend.JSendFeature;
import com.guestful.jaxrs.json.JsonProvider;
import com.guestful.jersey.GApplication;
import com.guestful.jersey.container.Container;
import com.guestful.json.JsonMapper;
import com.guestful.logback.LogbackConfigurator;
import com.mycila.guice.ext.service.ServiceModule;
import me.carbou.mathieu.tictactoe.rest.ApiResource;
import me.carbou.mathieu.tictactoe.rest.StaticResource;
import me.carbou.mathieu.tictactoe.rest.UserResource;
import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.filter.HttpMethodOverrideFilter;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class Main extends GApplication {

    @Override
    public void configure(Container container) {
        LogbackConfigurator.configure(Thread.currentThread().getContextClassLoader().getResource("envs/" + Env.NAME + "/logback.xml"));
        container
            .setPort(Env.PORT)
            .setMaxWorkers(Env.WORKERS)
            .setContextPath("/");
    }

    @Override
    public void initialize() {

        // guice
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new ServiceModule());
        GuiceBridge.getGuiceBridge().initializeGuiceBridge(getServiceLocator());
        getServiceLocator().getService(GuiceIntoHK2Bridge.class).bridgeGuiceInjector(injector);

        property(ServerProperties.WADL_FEATURE_DISABLE, true);
        property(ServerProperties.APPLICATION_NAME, Env.MODULE);

        // server
        registerClasses(LifecycleListener.class);

        // content processing
        registerClasses(JsonProcessingFeature.class);
        register(new JsonProvider(injector.getInstance(JsonMapper.class)));

        // filters
        register(new CorsFilter()
            .setAllowedOrigins("*")
            .setAllowedHeaders("X-Requested-With,Content-Type,Accept,Origin,X-HTTP-Method-Override,Authorization")
            .setAllowedMethods("GET,POST,PUT,DELETE"));
        registerClasses(
            HttpMethodOverrideFilter.class,
            JSendFeature.class,
            GZipEncoder.class,
            DeflateEncoder.class,
            EncodingFilter.class
        );

        registerClasses(
            StaticResource.class,
            UserResource.class,
            ApiResource.class
        );
    }

    public static void main(String... args) throws Exception {
        new Main().run(args);
    }

}
