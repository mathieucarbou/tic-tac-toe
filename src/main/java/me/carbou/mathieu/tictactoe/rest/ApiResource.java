package me.carbou.mathieu.tictactoe.rest;

import com.guestful.jaxrs.filter.jsend.Jsend;
import me.carbou.mathieu.tictactoe.Env;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@Path("/api")
@Jsend
public class ApiResource {

    @GET
    @Path("version")
    @Produces("application/json; charset=utf-8")
    public JsonObject getVersion() {
        return Json.createObjectBuilder()
            .add("version", Env.VERSION)
            .build();
    }

}
