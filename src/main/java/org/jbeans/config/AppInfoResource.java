package org.jbeans.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.logging.Logger;

/**
 * This class exposes a web app's MANIFEST.MF properties, build info, ...
 *
 * @author jhogan
 */
@Singleton
@Startup
@Path("/appinfo")
@DependsOn(value = {"Configurator"})
public class AppInfoResource {

    private static final Logger LOGGER = Logger.getLogger(AppInfoResource.class.getName());

    @Inject
    Configurator config;

    Map<String, String> manifestMap;

    @PostConstruct
    public void initializeAppInfo() {
        LOGGER.info("INITIALIZING APPLICATION INFORMATION");
        manifestMap = new ConcurrentHashMap<>();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        LOGGER.info("Return appinfo in JSON format");
        return Response.ok(config.getJbeansConfiguration()).build();
    }

    @GET
    @Path("manifest")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getManifestProperties() {
        LOGGER.info("Return app manifest.mf in JSON format");
        return Response.ok(manifestMap).build();
    }

    public Map<String, String> getManifestMap() {
        return manifestMap;
    }

    public void addManifestMapEntry(String name, String value) {
        LOGGER.info("addManifestMapEntry: name=" + name + ", value=" + value);
        manifestMap.put(name, value);
    }
}
