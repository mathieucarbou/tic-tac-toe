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

import com.guestful.jaxrs.security.AuthenticationException
import com.guestful.jaxrs.security.subject.SubjectContext
import com.guestful.jaxrs.security.token.PassthroughToken
import me.carbou.mathieu.tictactoe.Env
import me.carbou.mathieu.tictactoe.db.DB
import me.carbou.mathieu.tictactoe.di.AppDirect
import me.carbou.mathieu.tictactoe.security.JaxrsOpenIdManager
import me.carbou.mathieu.tictactoe.security.OAuthServerRequest
import org.expressme.openid.Association
import org.expressme.openid.Authentication
import org.expressme.openid.Endpoint
import org.expressme.openid.OpenIdException
import org.glassfish.jersey.oauth1.signature.OAuth1Parameters
import org.glassfish.jersey.oauth1.signature.OAuth1Secrets
import org.glassfish.jersey.oauth1.signature.OAuth1Signature
import org.glassfish.jersey.oauth1.signature.OAuth1SignatureException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.inject.Inject
import javax.security.auth.login.LoginException
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

    private static final Logger LOGGER = LoggerFactory.getLogger(AppDirectResource)

    String wwwAuthenticateHeader = "OAuth realm=\"Tic-Tac-Toe\""

    volatile String alias
    volatile byte[] key

    @Inject @AppDirect Client client
    @Inject OAuth1Signature oAuthSignature
    @Inject JaxrsOpenIdManager openIdManager
    @Inject DB db

    @GET
    @Path("login/openid/connect")
    Response openid(@QueryParam("url") String url,
                    @QueryParam("account") String accountId,
                    @QueryParam("continue") String next,
                    @Context ContainerRequestContext request) {

        if (!url) {
            url = 'https://www.appdirect.com/openid/id'
        }

        Endpoint endpoint = openIdManager.lookupEndpoint(url)
        Association association = openIdManager.lookupAssociation(endpoint)
        String authUrl = openIdManager.getAuthenticationUrl(endpoint, association)

        LOGGER.trace("openid auth url: {}", authUrl)

        alias = endpoint.getAlias()
        key = association.getRawMacKey()
        // these 2 values should be put in Redis for example in user session by the time the callback is made.
        // for this PoC we will just put them in memory as if only one user at a time made a request

        return Response.temporaryRedirect(URI.create(authUrl)).build()
    }

    @GET
    @Path("login/openid/callback")
    Response openidReturn(@Context ContainerRequestContext request) {
        Authentication authentication
        try {
            authentication = openIdManager.getAuthentication(request, key, alias)
        } catch (OpenIdException e) {
            throw new AuthenticationException("OpenId auth failed", e, request)
        }

        LOGGER.trace("openid callback: {}", authentication)

        Map user = db.users.findOne([email: authentication.getEmail()], [id: 1])

        try {
            SubjectContext.login(new PassthroughToken("tic-tac-toe", user.id as String))
        } catch (LoginException e) {
            throw new AuthenticationException("Authentication: " + authentication, e, request)
        }

        return Response.temporaryRedirect(URI.create("/")).build()
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

        //should be better moved to a Jersey filter
        verifyOAuthSignature(request)

        String eventXML = client.target("https://www.appdirect.com/api/integration/v1/events/${token}")
            .request(MediaType.APPLICATION_XML_TYPE)
            .get()
            .readEntity(String)

        //TODO: add logic here to handle app direct event type and event data correctly
        println "Event:\n${eventXML}"

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

    private void verifyOAuthSignature(ContainerRequestContext request) throws OAuth1SignatureException {
        String auth = request.getHeaderString(HttpHeaders.AUTHORIZATION)
        if (auth == null) throw new NotAuthorizedException("Missing OAuth", wwwAuthenticateHeader)

        OAuthServerRequest osr = new OAuthServerRequest(request)
        OAuth1Parameters params = new OAuth1Parameters().readRequest(osr)

        if (!params) throw new NotAuthorizedException("Missing OAuth", wwwAuthenticateHeader)

        OAuth1Secrets secrets = new OAuth1Secrets().consumerSecret(Env.APPDIRECT_SECRET)
        if (!oAuthSignature.verify(new OAuthServerRequest(request), params, secrets)) {
            throw new NotAuthorizedException("Invalid OAuth", wwwAuthenticateHeader)
        }
    }

}
