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
package me.carbou.mathieu.tictactoe.security;

import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.oauth1.signature.OAuth1Request;
import org.glassfish.jersey.server.ContainerRequest;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps a Jersey {@link ContainerRequestContext} object, implementing the
 * OAuth signature library {@link org.glassfish.jersey.oauth1.signature.OAuth1Request} interface.
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 * @author Paul C. Bryan <pbryan@sun.com>
 */
public class OAuthServerRequest implements OAuth1Request {

    private final ContainerRequestContext context;

    private static Set<String> EMPTY_SET = Collections.emptySet();

    private static List<String> EMPTY_LIST = Collections.emptyList();
    private final Value<MultivaluedMap<String, String>> formParams = Values.lazy(
        new Value<MultivaluedMap<String, String>>() {
            @Override
            public MultivaluedMap<String, String> get() {
                MultivaluedMap<String, String> params = null;
                final MediaType mediaType = context.getMediaType();
                if (mediaType != null && mediaType.equals(MediaType.APPLICATION_FORM_URLENCODED_TYPE)) {
                    final ContainerRequest jerseyRequest = (ContainerRequest) context;
                    jerseyRequest.bufferEntity();
                    final Form form = jerseyRequest.readEntity(Form.class);
                    params = form.asMap();
                }
                return params;
            }
        });

    /**
     * Create a new instance.
     *
     * @param context Container request context.
     */
    public OAuthServerRequest(ContainerRequestContext context) {
        this.context = context;
    }

    @Override
    public String getRequestMethod() {
        return context.getMethod();
    }

    @Override
    public URL getRequestURL() {
        try {
            return context.getUriInfo().getRequestUri().toURL();
        } catch (MalformedURLException ex) {
            Logger.getLogger(OAuthServerRequest.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private static Set<String> keys(MultivaluedMap<String, String> mvm) {
        if (mvm == null) {
            return EMPTY_SET;
        }
        return mvm.keySet();
    }

    private static List<String> values(MultivaluedMap<String, String> mvm, String key) {
        if (mvm == null) {
            return EMPTY_LIST;
        }
        List<String> v = mvm.get(key);
        if (v == null) {
            return EMPTY_LIST;
        }
        return v;
    }

    @Override
    public Set<String> getParameterNames() {
        HashSet<String> n = new HashSet<>();
        n.addAll(keys(context.getUriInfo().getQueryParameters()));
        n.addAll(keys(formParams.get()));

        return n;
    }

    @Override
    public List<String> getParameterValues(String name) {
        ArrayList<String> v = new ArrayList<>();
        v.addAll(values(context.getUriInfo().getQueryParameters(), name));
        v.addAll(values(formParams.get(), name));
        return v;
    }

    @Override
    public List<String> getHeaderValues(String name) {
        return context.getHeaders().get(name);
    }

    @Override
    public void addHeaderValue(String name, String value) throws IllegalStateException {
        throw new IllegalStateException("Modifying OAuthServerRequest unsupported");
    }

}
