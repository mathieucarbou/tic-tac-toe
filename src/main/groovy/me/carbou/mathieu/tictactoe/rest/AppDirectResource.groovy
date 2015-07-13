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

import me.carbou.mathieu.tictactoe.Env
import me.carbou.mathieu.tictactoe.security.ContainerOAuth1Request
import org.glassfish.jersey.oauth1.signature.*

import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.client.Client
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
@Path("/api/appdirect")
class AppDirectResource {

    @Inject Client client
    @Inject OAuth1Signature oAuthSignature

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
    @Path("subscription/{event}/{token}")
    @Produces("application/xml; charset=utf-8")
    String subscriptionCreated(@PathParam("event") String event,
                               @PathParam("token") String token,
                               @QueryParam("src") String eventUrl,
                               @Context ContainerRequestContext request) {

        if (!(event in ['created', 'updated', 'canceled', 'status-updated'])) {
            throw new NotFoundException(request.getUriInfo().getRequestUri().toString())
        }

        println("\nRequest: ${request.uriInfo.requestUri}\nQuery: ${request.uriInfo.queryParameters}\nHeaders: ${request.headers}")

        // commented since AppDirect OAuth signature does not match RFC standard for OAuth 1.0
        // and the signature validation process is not documented at http://info.appdirect.com/developers/docs/api_integration/oauth_api_authentication
        // verifySignature(request)

        String xml = client.target("https://www.appdirect.com/api/integration/v1/events/${token}")
            .request(MediaType.APPLICATION_XML_TYPE)
            .get()
            .readEntity(String)

        println "Body:\n${xml}"

        return success("my message", "my account id")
    }

    private static String success(String message, String accountIdentifier = null) {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<result>
    <success>true</success>
    <message>${message}</message>
    ${accountIdentifier ? ('<accountIdentifier>' + accountIdentifier + '</accountIdentifier>') : ''}
</result>"""
    }

    private static String error(String message) {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<result>
    <success>false</success>
    <errorCode>ACCOUNT_NOT_FOUND</errorCode>
    <message>${message}</message>
</result>"""
    }

    private void verifySignature(ContainerRequestContext request) throws OAuth1SignatureException {
        String auth = request.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth == null) throw new NotAuthorizedException("Missing OAuth", "OAuth");
        int start = auth.indexOf("oauth_signature=");
        if (start == -1) throw new NotAuthorizedException("Bad OAuth", "OAuth");
        int end = auth.indexOf("\"", start + 17);
        if (end == -1) throw new NotAuthorizedException("Bad OAuth", "OAuth");

        // establish the parameters that will be used to sign the request
        OAuth1Parameters params;
        try {
            params = new OAuth1Parameters()
                .consumerKey(Env.APPDIRECT_KEY)
                .signatureMethod(HmaSha1Method.NAME)
                .signature(URLDecoder.decode(auth.substring(start + 17, end), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        // establish the secrets that will be used to sign the request
        OAuth1Secrets secrets = new OAuth1Secrets().consumerSecret(Env.APPDIRECT_SECRET);

        // generate the digital signature and set in the request
        if (!oAuthSignature.verify(new ContainerOAuth1Request(request), params, secrets)) {
            throw new NotAuthorizedException("Invalid OAuth", "OAuth");
        }
    }

}
