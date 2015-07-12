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
package me.carbou.mathieu.tictactoe.rest

import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.client.Client
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@Path("/api/appdirect")
class AppDirectResource {

    @Inject Client client

    @GET
    @Path("login/openid")
    Response login(@QueryParam("openid_url") String openidUrl,
                   @QueryParam("account_id") String accountId,
                   @Context ContainerRequestContext request) {

        println("\nRequest: ${request.uriInfo.requestUri}\nQuery: ${request.uriInfo.queryParameters}\nHeaders: ${request.headers}")
        /*
OpenID Login URL: AppDirect will redirect users to this URL to log in for single sign-on.
This URL must contain the {openid} placeholder, which will tell your OpenID consumer which
provider to use to resolve the user identity. It may also contain the {accountIdentifier} placeholder if you need it.
OpenID Login HTTP Method: Whether the OpenID Login URL should receive a HTTP GET or a POST.
OpenID Realm: Realm for the OpenID consumer. Setting this parameter correctly ensures that users have a seamless SSO experience.
        */
        return Response.temporaryRedirect(URI.create('/')).build()
    }

    @GET
    @Path("subscription/created/{token}")
    @Produces("application/xml; charset=utf-8")
    String subscriptionCreated(@PathParam("token") String token,
                               @QueryParam("src") String eventUrl,
                               @Context ContainerRequestContext request) {

        println("\nRequest: ${request.uriInfo.requestUri}\nQuery: ${request.uriInfo.queryParameters}\nHeaders: ${request.headers}")

        String xml = client.target("https://www.appdirect.com/api/integration/v1/events/${token}")
            .request(MediaType.APPLICATION_XML_TYPE)
            .get()
            .readEntity(String)

        println "Body:\n${xml}"

        /*
AppDirect will call this URL when users purchase new subscriptions (SUBSCRIPTION_ORDER events as described in the Event API document).
This URL can either be non-interactive (default and recommended behavior) or interactive.
This URL must contain the {eventUrl} placeholder which will be replaced by the URL of the order event at runtime.
        */

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<result>
    <success>true</success>
    <message>Account creation successful</message>
    <accountIdentifier>1234</accountIdentifier>
</result>
"""
    }

    @GET
    @Path("subscription/updated/{token}")
    @Produces("application/xml; charset=utf-8")
    String subscriptionUpdated(@PathParam("token") String token,
                               @QueryParam("src") String eventUrl,
                               @Context ContainerRequestContext request) {

        println("\nRequest: ${request.uriInfo.requestUri}\nQuery: ${request.uriInfo.queryParameters}\nHeaders: ${request.headers}")

        String xml = client.target("https://www.appdirect.com/api/integration/v1/events/${token}")
            .request(MediaType.APPLICATION_XML_TYPE)
            .get()
            .readEntity(String)

        println "Body:\n${xml}"

        /*
AppDirect will call this URL when users upgrade/downgrade subscriptions (SUBSCRIPTION_CHANGE events as described in the Event API document).
This URL can only be non-interactive.
This URL must contain the {eventUrl} placeholder which will be replaced by the URL of the order event at runtime.
        */

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<result>
    <success>true</success>
    <message>message</message>
</result>
"""
    }

    @GET
    @Path("subscription/canceled/{token}")
    @Produces("application/xml; charset=utf-8")
    String subscriptionCanceled(@PathParam("token") String token,
                                @QueryParam("src") String eventUrl,
                                @Context ContainerRequestContext request) {

        String xml = client.target("https://www.appdirect.com/api/integration/v1/events/${token}")
            .request(MediaType.APPLICATION_XML_TYPE)
            .get()
            .readEntity(String)

        println "Body:\n${xml}"

        // call https://www.appdirect.com/api/integration/v1/events/${token} to get event

        /*
AppDirect will call this URL when users cancel subscriptions (SUBSCRIPTION_CANCEL events as described in the Event API document).
This URL can only be non-interactive.
This URL must contain the {eventUrl} placeholder, which will be replaced by the URL of the order event at runtime.
        */

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<result>
    <success>true</success>
    <message>message</message>
</result>
"""
    }

    @GET
    @Path("subscription/status-updated/{token}")
    @Produces("application/xml; charset=utf-8")
    String subscriptionStatus(@PathParam("token") String token,
                              @QueryParam("src") String eventUrl,
                              @Context ContainerRequestContext request) {

        String xml = client.target("https://www.appdirect.com/api/integration/v1/events/${token}")
            .request(MediaType.APPLICATION_XML_TYPE)
            .get()
            .readEntity(String)

        println "Body:\n${xml}"

        // call https://www.appdirect.com/api/integration/v1/events/${token} to get event

        /*
AppDirect will call this URL when a subscription status changes, e.g, when a subscription becomes suspended after a free trial expires,
or gets automatically cancelled some time after an invoice is overdue (SUBSCRIPTION_STATUS events described in the Event API document).
This URL can only be non-interactive.
This URL must contain the {eventUrl} placeholder, which will be replaced by the URL of the order event at runtime.
        */

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<result>
    <success>true</success>
    <message>message</message>
</result>
"""
    }

}

/*
In case of error:

<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<result>
    <success>false</success>
    <errorCode>ACCOUNT_NOT_FOUND</errorCode>
    <message>The account TEST123 could not be found.</message>
</result>

 */