import me.carbou.mathieu.tictactoe.Env
import org.glassfish.jersey.client.oauth1.ConsumerCredentials
import org.glassfish.jersey.client.oauth1.OAuth1ClientSupport
import org.glassfish.jersey.filter.LoggingFilter
import org.glassfish.jersey.jsonp.JsonProcessingFeature
import org.glassfish.jersey.oauth1.signature.HmaSha1Method

import javax.ws.rs.Priorities
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.core.Feature
import javax.ws.rs.core.MediaType

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
class AppDirect {

    public static void main(String... args) {
        ConsumerCredentials consumerCredentials = new ConsumerCredentials(Env.APPDIRECT_KEY, Env.APPDIRECT_SECRET);
        Feature oauth = OAuth1ClientSupport.builder(consumerCredentials)
            .signatureMethod(HmaSha1Method.NAME)
            .feature()
            .build();
        Client client = ClientBuilder.newBuilder()
            .build()
            .register(oauth)
            .register(JsonProcessingFeature.class) // javax.json support
            .register(LoggingFilter.class, Priorities.USER);
        String eventXML = client.target("https://www.appdirect.com/api/integration/v1/events/dummyOrder")
            .request(MediaType.APPLICATION_XML_TYPE)
            .get()
            .readEntity(String)
        println "Event:\n${eventXML}"
    }

}
