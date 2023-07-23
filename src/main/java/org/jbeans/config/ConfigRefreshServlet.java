package org.jbeans.config;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Produces;

/**
 * This class loads project build related information from war files manifest found
 * in META-INF/MANIFEST.MF.
 * 
 * @author jhogan
 */
@Produces
@WebServlet(name = "jbeansManifestInfoServlet", urlPatterns = {"/configRefresh"})
public class ConfigRefreshServlet extends HttpServlet {
    
    private static final Logger LOGGER = Logger.getLogger(ConfigRefreshServlet.class.getName());
    
    @Inject
    Configurator config;

    /**
     * Initialize configuration information at startup and add to overall application
     * configuration.
     * @throws ServletException 
     */
    @Override
    public void init() throws ServletException {}

    /**
     * Processes configRefresh requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.  Reloads configurated application properties.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet ConfigRefreshServlet</title>");
            out.println("</head>");
            out.println("<body>");
            if (Configurator.APP_CONFIG_ADMIN_ENABLED){
                config.mergeWithCustomConfiguration();
                out.println("<h1>ConfigRefreshServlet reloaded properties ok for app: "+Configurator.APPLICATION_NAME+".</h1>");
            }
            else {
                out.println("<h1>ConfigRefreshServlet Configurator admin disabled for "+Configurator.APPLICATION_NAME);
            }
            out.println("</body>");
            out.println("</html>");
        }
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Startup servlet to add to application configuration.";
    }
    
}
