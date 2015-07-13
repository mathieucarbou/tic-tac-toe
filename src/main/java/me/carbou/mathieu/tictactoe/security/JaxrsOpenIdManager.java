package me.carbou.mathieu.tictactoe.security;

import org.expressme.openid.Authentication;
import org.expressme.openid.Base64;
import org.expressme.openid.OpenIdException;
import org.expressme.openid.OpenIdManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.container.ContainerRequestContext;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author Mathieu Carbou (mathieu.carbou@gmail.com)
 */
public class JaxrsOpenIdManager extends OpenIdManager {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    private String returnTo;

    @Override
    public void setReturnTo(String returnTo) {
        super.setReturnTo(returnTo);
        this.returnTo = returnTo;
    }

    /**
     * Get authentication information from HTTP request, key.and alias
     */
    public Authentication getAuthentication(ContainerRequestContext request, byte[] key, String alias) {
        // verify:
        String identity = request.getUriInfo().getQueryParameters().getFirst("openid.identity");
        if (identity == null)
            throw new OpenIdException("Missing 'openid.identity'.");
        if (request.getUriInfo().getQueryParameters().getFirst("openid.invalidate_handle") != null)
            throw new OpenIdException("Invalidate handle.");
        String sig = request.getUriInfo().getQueryParameters().getFirst("openid.sig");
        if (sig == null)
            throw new OpenIdException("Missing 'openid.sig'.");
        String signed = request.getUriInfo().getQueryParameters().getFirst("openid.signed");
        if (signed == null)
            throw new OpenIdException("Missing 'openid.signed'.");
        if (!returnTo.equals(request.getUriInfo().getQueryParameters().getFirst("openid.return_to")))
            throw new OpenIdException("Bad 'openid.return_to'.");
        // check sig:
        String[] params = signed.split("[\\,]+");
        StringBuilder sb = new StringBuilder(1024);
        for (String param : params) {
            sb.append(param)
                .append(':');
            String value = request.getUriInfo().getQueryParameters().getFirst("openid." + param);
            if (value != null)
                sb.append(value);
            sb.append('\n');
        }
        String hmac = getHmacSha1(sb.toString(), key);
        if (!safeEquals(sig, hmac))
            throw new OpenIdException("Verify signature failed.");

        // set auth:
        Authentication auth = new Authentication();
        auth.setIdentity(identity);
        auth.setEmail(request.getUriInfo().getQueryParameters().getFirst("openid." + alias + ".value.email"));
        auth.setLanguage(request.getUriInfo().getQueryParameters().getFirst("openid." + alias + ".value.language"));
        auth.setGender(request.getUriInfo().getQueryParameters().getFirst("openid." + alias + ".value.gender"));
        auth.setFullname(getFullname(request, alias));
        auth.setFirstname(getFirstname(request, alias));
        auth.setLastname(getLastname(request, alias));
        return auth;
    }

    boolean safeEquals(String s1, String s2) {
        if (s1.length() != s2.length())
            return false;
        int result = 0;
        for (int i = 0; i < s1.length(); i++) {
            int c1 = s1.charAt(i);
            int c2 = s2.charAt(i);
            result |= (c1 ^ c2);
        }
        return result == 0;
    }

    String getFirstname(ContainerRequestContext request, String axa) {
        String name = request.getUriInfo().getQueryParameters().getFirst("openid." + axa + ".value.firstname");
        //If firstname is not supported try to get it from the fullname
        if (name == null) {
            name = request.getUriInfo().getQueryParameters().getFirst("openid." + axa + ".value.fullname");
            if (name != null) {
                int n = name.indexOf(' ');
                if (n != (-1))
                    name = name.substring(0, n);
            }
        }
        return name;
    }

    String getLastname(ContainerRequestContext request, String axa) {
        String name = request.getUriInfo().getQueryParameters().getFirst("openid." + axa + ".value.lastname");
        // If lastname is not supported try to get it from the fullname
        if (name == null) {
            name = request.getUriInfo().getQueryParameters().getFirst("openid." + axa + ".value.fullname");
            if (name != null) {
                int n = name.lastIndexOf(' ');
                if (n != (-1))
                    name = name.substring(n + 1);
            }
        }
        return name;
    }

    String getFullname(ContainerRequestContext request, String alias) {
        // If fullname is not supported then get combined first and last name
        String fname = request.getUriInfo().getQueryParameters().getFirst("openid." + alias + ".value.fullname");
        if (fname == null) {
            fname = request.getUriInfo().getQueryParameters().getFirst("openid." + alias + ".value.firstname");
            if (fname != null) {
                fname += " ";
            }
            fname += request.getUriInfo().getQueryParameters().getFirst("openid." + alias + ".value.lastname");
        }
        return fname;
    }

    String getHmacSha1(String data, byte[] key) {
        SecretKeySpec signingKey = new SecretKeySpec(key, HMAC_SHA1_ALGORITHM);
        Mac mac;
        try {
            mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new OpenIdException(e);
        }
        try {
            byte[] rawHmac = mac.doFinal(data.getBytes("UTF-8"));
            return Base64.encodeBytes(rawHmac);
        } catch (IllegalStateException | UnsupportedEncodingException e) {
            throw new OpenIdException(e);
        }
    }

}
