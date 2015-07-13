package me.carbou.mathieu.tictactoe.security;

import org.glassfish.jersey.oauth1.signature.OAuth1Request;

import javax.ws.rs.container.ContainerRequestContext;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class ContainerOAuth1Request implements OAuth1Request {

    private final ContainerRequestContext request;

    public ContainerOAuth1Request(ContainerRequestContext request) {
        this.request = request;
    }

    @Override
    public void addHeaderValue(String name, String value) throws IllegalStateException {
        request.getHeaders().add(name, value);
    }

    @Override
    public String getRequestMethod() {
        return request.getMethod();
    }

    @Override
    public URL getRequestURL() {
        try {
            final URI uri = request.getUriInfo().getRequestUri();
            return uri.toURL();
        } catch (MalformedURLException ex) {
            Logger.getLogger(ContainerOAuth1Request.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public Set<String> getParameterNames() {
        return request.getUriInfo().getQueryParameters().keySet();
    }

    @Override
    public List<String> getParameterValues(String name) {
        return request.getUriInfo().getQueryParameters().get(name);
    }

    @Override
    public List<String> getHeaderValues(String name) {
        return request.getHeaders().get(name);
    }

}
