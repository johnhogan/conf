package org.jbeans.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.Produces;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;

/**
 * Load properties from the ${artifactId}.properties file. If a system
 * environment variable called ${artifactId}.config.dir is not available, the
 * default properties packaged with deployment is used.
 *
 *
 * @author jhogan
 * @version 1.0
 *
 * <code>JbeansConfigurationProvider</code>.
 */
@ApplicationScoped
@Produces
@Named
public class JbeansConfigurationProvider implements ConfigurationProvider {

    private static final Logger LOGGER = Logger.getLogger(JbeansConfigurationProvider.class.getName());
    private static final String JNDI_ROOT = "java:global/jbeans/";
    private static final String JNDI_SUFFIX = "/ENVIRONMENT_PROPERTIES_PATH";
    private String JNDI_PROPERTIES_PATH;
    static String APP_CONFIG_DIR;
    private static String APP_FQ_PROPS_FILE;
    private static String CONFIG_DIR_PROP;
    private static final String JASYPT_PW;

    Map<String, String> configuration = new HashMap<>();
    private static final String CONFIGURATION_FILE_MISSING = "Config file not found.  Check jbeans.config.dir OR JNDI prop";
    private static final String ERROR_MSG_ENCRYPTED_PROPS_MISSING_EKEY = "***** ERROR, app has encrypted properties, but -DeKey property is missing.";
    StandardPBEStringEncryptor encryptor= new StandardPBEStringEncryptor();
    private static boolean hasEncryptedProps;
    

    static {
        String temp = (String) System.getProperty(Configurator.JASYPT_PW_ENV_PROPERTY_VALUE);

        if (temp == null || temp.isEmpty()) {
            JASYPT_PW = "UNDEFINED";
        } else {
            JASYPT_PW = temp;
        }
    }

    /**
     * Initializes a JBeans related properties based on yourAppName.config.dir
     * absolute path to where yourAppName.properties is located.
     *
     * <code>JbeansConfigurationProvider</code> object.
     *
     * @throws org.jbeans.config.ConfigurationException
     * @see WebManifestConfiguration for information on how artifactId is
     * determined.
     */
    void initializeProps()
            throws ConfigurationException {
        if (Configurator.APPLICATION_NAME == null) {
            throw new ConfigurationException("artifactId cannot be null");
        }

        CONFIG_DIR_PROP = Configurator.APPLICATION_NAME + ".config.dir";

        LOGGER.log(Level.INFO, "INITIALIZING APPLICATION CONFIGURATION FOR: {0}", Configurator.APPLICATION_NAME);
        LOGGER.log(Level.INFO, "EXPECTED config dir property: {0}", CONFIG_DIR_PROP);

        if (APP_CONFIG_DIR == null) {
            LOGGER.log(Level.INFO, "CHECKING system environment -D{0} for ", CONFIG_DIR_PROP);
            try {
                APP_CONFIG_DIR = System.getProperty(CONFIG_DIR_PROP);
                if (APP_CONFIG_DIR == null) {
                    LOGGER.log(Level.INFO, "SYSTEM environment (-D) property= {0}", CONFIG_DIR_PROP + " not found.");
                    JNDI_PROPERTIES_PATH = JNDI_ROOT + Configurator.APPLICATION_NAME + JNDI_SUFFIX;
                    LOGGER.log(Level.INFO, "CHECKING for JNDI property named: {0}", JNDI_PROPERTIES_PATH);
                    APP_CONFIG_DIR = (String) InitialContext.doLookup(JNDI_PROPERTIES_PATH);
                }
            } catch (NamingException ex) {
                throw new ConfigurationException("no -D" + Configurator.APPLICATION_NAME + ".config.dir property OR JNDI prop: ." + JNDI_PROPERTIES_PATH + " was found."
                        + "  Please check your configuration.");
            }
        }
        LOGGER.log(Level.INFO, "APP_CONFIG_DIR= {0}", APP_CONFIG_DIR);
        LOGGER.log(Level.INFO, "INTERNAL_PROPS_FILE= {0}", Configurator.INTERNAL_PROPS_FILE);
        APP_FQ_PROPS_FILE = APP_CONFIG_DIR + File.separator + Configurator.EXTERNAL_PROPS_FILE;
        LOGGER.log(Level.INFO, "FULLY qualified props file= {0}", APP_FQ_PROPS_FILE);

        if (APP_CONFIG_DIR != null) {

            FileInputStream in = null;
            try {
                encryptor.setPassword(JASYPT_PW);
                Properties jbeansEnvProps = new EncryptableProperties(encryptor);

                in = new FileInputStream(APP_FQ_PROPS_FILE);
                jbeansEnvProps.load(in);

                populateConfigMap(jbeansEnvProps);
                
            } catch (FileNotFoundException ex) {
                Logger.getLogger(JbeansConfigurationProvider.class.getName()).log(Level.SEVERE, null, ex);
                throw new ConfigurationException(CONFIGURATION_FILE_MISSING);
            } catch (IOException ex) {
                Logger.getLogger(JbeansConfigurationProvider.class.getName()).log(Level.SEVERE, null, ex);
                throw new ConfigurationException(ex.getMessage());
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        Logger.getLogger(JbeansConfigurationProvider.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } else {
            LOGGER.log(Level.SEVERE, "An system property called {0} must exist to start this application.", CONFIG_DIR_PROP);
            throw new ConfigurationException("An system property called " + CONFIG_DIR_PROP + " must exist to start this application.");
        }
        configuration.put(Configurator.APPLICATION_NAME + ".config.dir", APP_CONFIG_DIR);
        Configurator.jbeansConfigProvider = this;
    }

    /**
     * Retrieves a property value as a string.
     *
     * @param key the property key.
     * @param def the default value.
     * @return property as String or null if not found.
     */
    public String getString(String key, String def) {
        String retval;
        retval = configuration.get(key);
        if (retval == null) {
            retval = def;
        }
        return retval;
    }

    /**
     * Retrieve the JBeans configuration object.
     *
     * @return Map<String, String> configuration.
     */
    @Override
    public Map<String, String> getConfiguration() {
        if (configuration.isEmpty()) {
            try {
                initializeProps();
            } catch (ConfigurationException ex) {
                Logger.getLogger(JbeansConfigurationProvider.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return configuration;
    }

    /**
     * This method adds or updates a property to the externally configured
     * appName.properties file.
     *
     * @param propName the property to add or update.
     * @param prpValue the value to set the add or udpate property to.
     *
     * @throws ConfigurationException a configuration exception
     * (FileNotFoundException, ...).
     */
    void saveOrUpdateProperties(String propName, String prpValue) throws ConfigurationException {
        Properties jbeansEnvProps = new Properties();
        FileInputStream in = null;
        FileOutputStream out = null;

        if (APP_CONFIG_DIR != null) {
            try {
                LOGGER.log(Level.INFO, "{0} nPROPERTIES:", Configurator.INTERNAL_PROPS_FILE);
                File file = new File(APP_FQ_PROPS_FILE);
                in = new FileInputStream(file);
                jbeansEnvProps.load(in);
                jbeansEnvProps.setProperty(propName, prpValue);

                out = new FileOutputStream(file);
                jbeansEnvProps.store(out, " -- property: " + propName + " was added or updated at runtime on: " + new Date());
                configuration.put(propName, prpValue);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(JbeansConfigurationProvider.class.getName()).log(Level.SEVERE, null, ex);
                throw new ConfigurationException(CONFIGURATION_FILE_MISSING);
            } catch (IOException ex) {
                Logger.getLogger(JbeansConfigurationProvider.class.getName()).log(Level.SEVERE, null, ex);
                throw new ConfigurationException(ex.getMessage());
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(JbeansConfigurationProvider.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(JbeansConfigurationProvider.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Delete a property from the overall configuration of an application using
     * the jbeans-config component.
     *
     * @param propName the property to be deleted.
     * @throws ConfigurationException thrown for misconfiguration like
     * FileNotFoundException. Or if there are encypted properties and an eKey
     * has not bee provided in the environment.
     */
    void deleteProperty(String propName) throws ConfigurationException {
        Properties jbeansEnvProps = new Properties();
        FileInputStream in = null;
        FileOutputStream out = null;

        if (APP_CONFIG_DIR != null) {
            try {
                LOGGER.log(Level.INFO, "{0} PROPERTIES:", Configurator.INTERNAL_PROPS_FILE);
                File file = new File(APP_FQ_PROPS_FILE);
                in = new FileInputStream(file);
                jbeansEnvProps.load(in);
                jbeansEnvProps.remove(propName);
                configuration.remove(propName);
                out = new FileOutputStream(file);
                jbeansEnvProps.store(out, " -- property: " + propName + " was deleted at runtime on: " + new Date());
            } catch (FileNotFoundException ex) {
                Logger.getLogger(JbeansConfigurationProvider.class.getName()).log(Level.SEVERE, null, ex);
                throw new ConfigurationException(CONFIGURATION_FILE_MISSING);
            } catch (IOException ex) {
                Logger.getLogger(JbeansConfigurationProvider.class.getName()).log(Level.SEVERE, null, ex);
                throw new ConfigurationException(ex.getMessage());
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        Logger.getLogger(JbeansConfigurationProvider.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ex) {
                        Logger.getLogger(JbeansConfigurationProvider.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    /*
     * This method checks property file values to determine if any are encrypted per Jasypt
     * requirements and are using the Jasypt prop prefix:  ENC(
     *
     * If any properties are encrypted, a system environment password called "eKey" must exist
     * in order to decrypt these values.
    
     * A ConfigurationException is thrown if there are encrypted properties, and the "eKey"
     * password has not been defined in the environment.
     *    
     */
    private boolean populateConfigMap(Properties props) throws ConfigurationException {
        boolean hasEncProps = false;
        if (props != null) {

            // Are there any encrypted properties?
            String propValues = props.toString();
            int encPrefixIdx = propValues.indexOf("ENC(");
            if (encPrefixIdx > -1) {
                hasEncProps = true;
            }

            Enumeration<String> propNames = (Enumeration<String>) props.propertyNames();
            while (propNames.hasMoreElements()) {
                String key = propNames.nextElement();
                String value = props.getProperty(key);
                configuration.put(key, value);
            }

            if (JASYPT_PW.equals("UNDEFINED") && hasEncProps) {
                LOGGER.log(Level.SEVERE, "eKey PROPERTY NOT FOUND");
                LOGGER.log(Level.SEVERE, "This application has encrypted properties, and no -DeKey was found.  Please add -eKey to the environment.");
                throw new ConfigurationException(ERROR_MSG_ENCRYPTED_PROPS_MISSING_EKEY);
            }

        }

        return hasEncProps;
    }

}
