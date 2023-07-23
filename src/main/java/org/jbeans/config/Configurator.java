package org.jbeans.config;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 *
 * This class is based on JEE Configurator Pattern described in Adam Bien's
 * book: Java EE Patterns, see:
 * http://www.adam-bien.com/roller/abien/entry/real_world_java_ee_patterns
 *
 * It has been updated for use as a jar library, and added support for 
 * manifest properties, encrypted props and write through props admin 
 * 
 * Author: jhogan Date: 01/22/2017 Time: 19:06
 */
@Startup
@Singleton
@Path("/configuration")
@Produces(APPLICATION_JSON)
public class Configurator {

    static final String INTERNAL_PROPS_FILE = "jbeans-app.properties";
    private static final String PROPERTY_APP_PROPS_FILE = "jbeans.app.props.file";
    private static final String PROPERTY_ADMIN_ENABLED = "jbeans.app.props.admin.enabled";
    private static final String PROPERTY_APP_NAME = "jbeans.app.name";
    private static final String JASYPT_PW_ENV_PROPERTY_NAME = "jbeans.app.props.encKey.name";

    static Boolean APP_CONFIG_ADMIN_ENABLED = Boolean.FALSE;
    static String EXTERNAL_PROPS_FILE;
    static String APPLICATION_NAME;
    static String JASYPT_PW_ENV_PROPERTY_VALUE;

    public Configurator() {
    }

    private static final Logger LOGGER = Logger.getLogger(Configurator.class.getName());
    Map<String, String> configuration;
    private Set<String> unconfiguredFields;

    @Inject
    private Instance<ConfigurationProvider> configurationProvider;

    static JbeansConfigurationProvider jbeansConfigProvider;

    /**
     * Retrieve the application configuration. This method loads and merges all
     * application configurations for all classes implementing
     * ConfigurationProvider. This allows for configuration to be loaded from
     * multiple sources and merges them into one configuration for the
     * application. Provider implementations can import configuration from
     * properties files, database, xml, ...
     */
    @PostConstruct
    void fetchConfiguration() {
        LOGGER.log(Level.INFO, "@PostConstruct fetching configuration, hashcode={0}", this.hashCode());
        this.configuration = new ConcurrentHashMap<>();
        this.unconfiguredFields = new HashSet<>();
        initializeAppProperties();
        mergeWithCustomConfiguration();
    }

    /**
     * This method is used to load the jbeans-app.properties which is an app specific
     * properties file defines three properties required by the Configurator.
     * They are used to find application specific JNDI prop in an application
     * server's configuration.  And this JNDI prop's value points to an external
     * properties file. The jbeans-config component may be in use
     * by multiple applications within an application server, and each
     * application's jbeans-app.properties allows them to use separate
     * properties files on a file system.
     * 
     * The configurator  supports Jasypt encrypted properties also.  These
     * properties should appear in the properties files in a format of:
     *     my.secret.prop=ENC(xasdffxYY7xxx)
     * The Configurator will automatically decrypt these properties.
     * 
     * properties files. The properties in jbeans-app.properties are:
     *     jbeans.app.name=jee-config-demo
     *     jbeans.app.props.file=jee-config-demo.properties
     *     jbeans.app.props.admin.enabled=false
     */
    void initializeAppProperties() {
        LOGGER.log(Level.INFO, "Now LOADING jbeans-app.properties");

        Properties jbeansEnvProps = new Properties();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream fis = classLoader.getResourceAsStream(INTERNAL_PROPS_FILE);
            LOGGER.log(Level.INFO, "fis = {0}", fis);
            jbeansEnvProps.load(fis);
            for (Map.Entry<Object, Object> e : jbeansEnvProps.entrySet()) {
                String key = (String) e.getKey();
                String value = e.getValue().toString().trim();
                switch (key) {
                    case PROPERTY_APP_NAME:
                        APPLICATION_NAME = value;
                        LOGGER.log(Level.INFO, "Configured jbeans.app.name = {0}", APPLICATION_NAME);
                        break;
                    case JASYPT_PW_ENV_PROPERTY_NAME:
                        JASYPT_PW_ENV_PROPERTY_VALUE = value;
                        break;
                    case PROPERTY_APP_PROPS_FILE:
                        EXTERNAL_PROPS_FILE = value;
                        LOGGER.log(Level.INFO, "Configured jbeans.app.props.file = {0}", EXTERNAL_PROPS_FILE);
                        break;
                    case PROPERTY_ADMIN_ENABLED:
                        if (value == null || value.trim().isEmpty()) {
                            LOGGER.log(Level.INFO, "Configured jbeans.app.props.admin.enabled = null, defaulting APP_CONFIG_ADMIN_ENABLED to false");
                        } else {
                            if (value.equalsIgnoreCase("true")) {
                                APP_CONFIG_ADMIN_ENABLED = Boolean.TRUE;
                                LOGGER.log(Level.INFO, "Configured jbeans.app.props.admin.enabled = {0}", APP_CONFIG_ADMIN_ENABLED);
                            } else {
                                LOGGER.log(Level.INFO, "Configured jbeans.app.props.admin.enabled != true/TRUE.  Using default=false, value from jbeans.properties = {0}", value);
                            }
                        }
                        break;
                }
                configuration.put(key, value);
            }
            LOGGER.log(Level.INFO, "props configuration= {0}", configuration);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
        }
    }

    public boolean doesCustomConfigurationExist() {
        return !configurationProvider.isUnsatisfied();
    }

    /*
     * merge configuration from all defined configuration providers in the application.
     */
    public void mergeWithCustomConfiguration() {
        LOGGER.log(Level.INFO, "Merging configuration");
        for (ConfigurationProvider provider : configurationProvider) {

            LOGGER.log(Level.INFO, "Configuration provider= {0}", provider);

            Map<String, String> customConfiguration = provider.getConfiguration();
            this.configuration.putAll(customConfiguration);

        }
    }

    String obtainConfigurableName(InjectionPoint ip) {
        AnnotatedField field = (AnnotatedField) ip.getAnnotated();
        Configurable configurable = field.getAnnotation(Configurable.class);
        if (configurable != null) {
            return configurable.value();
        } else {
            return ip.getMember().getName();
        }
    }

    @javax.enterprise.inject.Produces
    public String getString(InjectionPoint point) {
        String fieldName = obtainConfigurableName(point);
        return getValueForKey(fieldName);
    }

    private String getValueForKey(String fieldName) {
        String valueForFieldName = configuration.get(fieldName);
        if (valueForFieldName == null) {
            this.unconfiguredFields.add(fieldName);
        }

        return valueForFieldName;
    }

    @javax.enterprise.inject.Produces
    public long getLong(InjectionPoint point) {
        String stringValue = getString(point);
        if (stringValue == null) {
            return 0;
        }
        return Long.parseLong(stringValue);
    }

    @javax.enterprise.inject.Produces
    public float getFloat(InjectionPoint point) {
        String stringValue = getString(point);
        if (stringValue == null) {
            return Float.parseFloat("0.0");
        }
        return Float.parseFloat(stringValue);
    }

    @javax.enterprise.inject.Produces
    public int getInteger(InjectionPoint point) {
        String stringValue = getString(point);
        if (stringValue == null) {
            return 0;
        }
        return Integer.parseInt(stringValue);
    }

    @javax.enterprise.inject.Produces
    public boolean getBoolean(InjectionPoint point) {
        //LOGGER.log(Level.INFO, "inject Boolean for ip= {0}", point);
        String stringValue = getString(point);
        if (stringValue == null) {
            return false;
        }
        return Boolean.parseBoolean(stringValue);
    }

    public Set<String> getUnconfiguredFields() {
        return this.unconfiguredFields;
    }

    /**
     * Retrieve an individual property value in the current configuration.
     *
     * @param key the desired property to retrieve.
     *
     * @return the value associated with the input key.
     */
    @GET
    @Path("{key}")
    public String getEntry(@PathParam("key") String key) {
        return configuration.get(key);
    }

    /**
     * Retrieve JSON configuration attribute name/value pairs.
     *
     * @return the current configuration in JSON.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getManifestProperties() {
		LOGGER.info("Returning app configuration map in JSON format");
        return Response.ok(configuration).build();
    }

    /**
     * Add or update an application property.
     *
     * @param key the property to be added or updated.
     * @param value the value of the new or updated property.
     * @param uriInfo the @Context UriInfo
     * @return
     */
    @PUT
    @Path("{key}")
    @Consumes(TEXT_PLAIN)
    public Response addEntry(@PathParam("key") String key, String value, @Context UriInfo uriInfo) {
        Response response = null;

        if (APP_CONFIG_ADMIN_ENABLED) {
            for (ConfigurationProvider provider : configurationProvider) {
                if (provider instanceof JbeansConfigurationProvider) {
                    JbeansConfigurationProvider jbeansProv = (JbeansConfigurationProvider) provider;
                    Map<String, String> map = jbeansProv.getConfiguration();
                    if (map.containsKey(key)) {
                        response = Response.noContent().build();
                    } else {
                        URI uri = uriInfo.getAbsolutePathBuilder().build(key);
                        response = Response.created(uri).build();
                    }
                    map.put(key, value);
                    try {
                        LOGGER.log(Level.INFO, "add/updating prop: ={0}, value={1}", new Object[]{key, value});
                        LOGGER.log(Level.INFO, "jbeansConfig ={0}", provider);
                        jbeansProv.saveOrUpdateProperties(key, value);
                    } catch (ConfigurationException ex) {
                        Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, "ERROR: unable to update properties file.");
                    }
                }
            }
        } else {
            if (response == null) {
                // Admin demonstrator only allowing changes to JbeansConfigurationProvider properties if enabled.
                URI uri = uriInfo.getAbsolutePathBuilder().build(key);
                response = Response.created(uri).build();
            }
        }

        return response;
    }

    /**
     * Delete an attribute from the configuration.
     *
     * @param key
     * @return
     */
    @DELETE
    @Path("{key}")
    public Response deleteEntry(@PathParam("key") String key) {

        if (APP_CONFIG_ADMIN_ENABLED) {
            try {
                // remove from property file.
                jbeansConfigProvider.deleteProperty(key);
                // remove from in memory configuration.
                configuration.remove(key);
            } catch (ConfigurationException ex) {
                Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, "ERROR: unable to update properties file.");
            }
        }
        return Response.noContent().build();
    }

    public void debugEnabled() {
        this.configuration.put("debug", Boolean.TRUE.toString());
    }

    public void debugDisabled() {
        this.configuration.put("debug", Boolean.FALSE.toString());
    }

    public Map<String, String> getJbeansConfiguration() {
        return configuration;
    }

}
