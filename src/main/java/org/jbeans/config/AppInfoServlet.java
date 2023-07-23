package org.jbeans.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jhogan
 * 
 * The AppInfoServlet adds manifest/build information to application configuration.
 * And uses the AppInfoResource to access the Configurator and add manifest properties 
 * to it.
 * 
 */
@WebServlet(urlPatterns = "/appinfoservlet", 
            loadOnStartup=1)
public class AppInfoServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AppInfoServlet.class.getName());

    @Inject
    Configurator config;
    
    @Inject
    AppInfoResource appInfoResource;
    
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AppInfoServlet() {
		super();
	}

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		LOGGER.log(Level.INFO, "Loading manifest properties,  adding to app configuration.");

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(getServletContext().getResourceAsStream("/META-INF/MANIFEST.MF")));
		LOGGER.log(Level.INFO, "Loaded manifest ok.");
		try {
			while (reader.ready()) {
				String line = reader.readLine();
				String[] arrOfStr = line.split(":", 2);
				if (arrOfStr != null && arrOfStr.length==2) {
					String name=arrOfStr[0].trim();
					String value=arrOfStr[1].trim();
					appInfoResource.addManifestMapEntry(name, value);
				}
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "ERROR, caught IOException, message={0}", e);
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}
