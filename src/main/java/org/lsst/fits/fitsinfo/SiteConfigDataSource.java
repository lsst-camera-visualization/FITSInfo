package org.lsst.fits.fitsinfo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Serves per-deployment site configuration from the bundled {@code sites.json}
 * resource, so that the {@code index.html} and {@code view.html} front-end pages
 * can derive the site catalog, host-to-environment mapping and per-site viewer
 * settings from data rather than hard-wired code.
 *
 * @author tonyj
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class SiteConfigDataSource {

    private static final String SUFFIX_TOKEN = "{suffix}";
    private static final Map<String, Object> CONFIG = loadConfig();

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadConfig() {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
        };
        try (InputStream in = SiteConfigDataSource.class.getResourceAsStream("/sites.json")) {
            if (in == null) {
                throw new IllegalStateException("sites.json not found on classpath");
            }
            return mapper.readValue(in, typeRef);
        } catch (IOException x) {
            throw new UncheckedIOException("Unable to read sites.json", x);
        }
    }

    /**
     * Global catalog: the environment resolved for {@code host}, plus the ordered
     * list of sites ({@code key}, {@code label}, {@code active}) visible in that
     * environment's data-source dropdown.
     */
    @GET
    @Path("/config")
    public Map<String, Object> config(@DefaultValue("") @QueryParam("host") String host) {
        Map<String, Object> environment = resolveEnvironment(host);
        Map<String, Object> allSites = sites();

        List<Map<String, Object>> catalog = new ArrayList<>();
        for (Object key : (List<Object>) environment.get("sites")) {
            Map<String, Object> site = (Map<String, Object>) allSites.get(key);
            if (site == null) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("key", key);
            entry.put("label", site.get("label"));
            entry.put("active", site.get("active"));
            catalog.add(entry);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("environment", environment.get("name"));
        result.put("suffix", environment.get("suffix"));
        result.put("paddedNodeNames", environment.get("paddedNodeNames"));
        result.put("sites", catalog);
        return result;
    }

    /**
     * The configuration for a single site, with any {@code {suffix}} token in its
     * IIIF base URL substituted using the environment resolved for {@code host}.
     */
    @GET
    @Path("/{site}/config")
    public Map<String, Object> siteConfig(@PathParam("site") String siteName,
            @DefaultValue("") @QueryParam("host") String host) {
        Map<String, Object> site = (Map<String, Object>) sites().get(siteName);
        if (site == null) {
            throw new NotFoundException("Unknown site: " + siteName);
        }
        String suffix = (String) resolveEnvironment(host).get("suffix");

        // Copy so we do not mutate the cached config when substituting the suffix.
        Map<String, Object> result = new LinkedHashMap<>(site);
        Object iiif = result.get("iiif");
        if (iiif instanceof String) {
            result.put("iiif", ((String) iiif).replace(SUFFIX_TOKEN, suffix == null ? "" : suffix));
        }
        return result;
    }

    /**
     * Return the first environment whose {@code match} substring occurs in the
     * host name. The trailing environment has an empty {@code match} and so acts
     * as the catch-all default.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveEnvironment(String host) {
        String h = host == null ? "" : host;
        List<Object> environments = (List<Object>) CONFIG.get("environments");
        for (Object o : environments) {
            Map<String, Object> env = (Map<String, Object>) o;
            String match = (String) env.get("match");
            if (match == null || match.isEmpty() || h.contains(match)) {
                return env;
            }
        }
        // sites.json should always end with an empty-match default; fail loudly if not.
        throw new IllegalStateException("No environment matched host '" + host + "' and no default is configured");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sites() {
        return (Map<String, Object>) CONFIG.get("sites");
    }
}
