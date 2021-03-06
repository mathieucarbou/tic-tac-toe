/**
 * Copyright (C) 2015 Mathieu Carbou (mathieu@carbou.me)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
