package org.jbeans.config;

import java.util.Map;

/**
 *
 * @author jhogan
 *
 * Any class implementing this interface contributes properties to the overall configuration of the 
 * application.  This trigger mechanism is a startup task performed by the Configurator using
 * @Startup, @Singleton.
 * 
 * @see org.jbeans.config.Configuration.
 * 
 * @author adam bien, blog.adam-bien.com, ch191532
 */
public interface ConfigurationProvider {
    
    public Map<String, String> getConfiguration();

}
