package com.artechra.jcatest;

import com.artechra.jcacalculator.CalculatorConnection;
import com.artechra.jcacalculator.CalculatorConnectionFactory;
import com.artechra.jcacalculator.CalculationType;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.resource.Referenceable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * This class implements a painfully simple servlet that just accepts a GET
 * request, retrieves a couple of Simple Adapter JCA connections and then
 * uses them to perform calculations on randomly generated lists of numbers.
 *
 * @author Eoin Woods
 */
public class JcaTestServlet extends HttpServlet {
    private static final long serialVersionUID = 5025992118231269061L;

    /**
     * Implement a response to HTTP's GET verb
     *
     * @param req  the HTTP request parameters
     * @param resp the HTTP response object to use to send the response
     * @throws ServletException if a functional problem occurs
     * @throws IOException      if a simple network problem occurs
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        this.printPage(resp.getWriter());
    }

    /**
     * Perform the processing for this servlet (get JCA connections, generate
     * lists of parameters and run calculations on them, displaying the results).
     *
     * @param out an output writer to which the HTML should be written
     * @throws ServletException if the request can't be processed
     */
    private void printPage(PrintWriter out) throws ServletException {

        // Write the boiler plate start of page
        out.println("<html>");
        out.println("<head><title>Artechra JCA Calculator Testing Servlet</title></head>");
        out.println("<body>");
        out.println("<h1>Artechra JCA Calculator Tests</h1>");
        out.println("<p>This is a servlet to test the Calculator JCA Adapter</p>");
        out.println("<hr/>");

        // Generate a randomly bounded list (3..15 long) containing values from
        // -100 .. 100.
        List<Integer> operands = new ArrayList<Integer>();
        Random generator = new Random();
        int numberOfOperands = 3 + Math.abs(generator.nextInt()) % 12; // From 3 to 15 operands
        for (int idx = 1; idx <= numberOfOperands; idx++) {
            operands.add(generator.nextInt() % 100);
        }

        // For each possible calculation type, get a corresponding JCA connection to
        // the Calculator Adapter and perform the calculation on the numbers
        for (CalculationType ct : CalculationType.values()) {
            out.println("<p>Result of calling operation " + ct + " on " + operands + " is ");
            CalculatorConnection jcaConnection = null;
            try {
                jcaConnection = this.getConnection(ct);
                out.println(jcaConnection.performOperationWhileIWait(operands));
            } catch (Exception ex) {
                throw new ServletException("Unexpected exception from the JCA adapter when executing an operation", ex);
            } finally {
                // This is quite important because we don't want to leak connections so we
                // do everything we can to close the connection
                if (jcaConnection != null) {
                    jcaConnection.close();
                }
            }
        }

        // Print the boiler plate end of page
        out.println("</p>");
        out.println("<p><i>[" + new java.util.Date().toString() + "]</i></p>");
        out.println("<hr/>");
        out.println("<p><b>Refresh this page to call the adapter again.</b></p>");
        out.println("</body>");
        out.println("</html>");
    }


    /**
     * Retrieve a SimpleJcaAdapter connection for a specified calculation type
     * @param calcType the calculation the connection should process
     * @return a calculation implementing SimpleConnection for the calculation type
     * @throws ServletException if the connection could not be provided
     */
    private CalculatorConnection getConnection(CalculationType calcType) throws ServletException {
        CalculatorConnection ret;
        try {
            Context ctx = new InitialContext();
            Object obj = ctx.lookup("java:comp/env/jca/CalculatorAdapter");
            System.out.println("CF=" + obj) ;
            System.out.println("CF class=" + obj.getClass());
            System.out.println("CF isa CalculatorConnectionFactory: " + (obj instanceof CalculatorConnectionFactory));
            System.out.println("CF isa Referencable: " + (obj instanceof Referenceable));
            System.out.println("CF isa Serializable: " + (obj instanceof Serializable));
            CalculatorConnectionFactory fact = (CalculatorConnectionFactory) ctx.lookup("java:comp/env/jca/CalculatorAdapter");
            Object o = fact.getConnection(calcType);
            System.out.println("Connection: " + o) ;
            ret = (CalculatorConnection)o;
            System.out.println("JcaTestServlet retrieved connection of type '" + calcType +
                    "' to JCA Adapter: " + ret);
        } catch (Exception ex) {
            throw new ServletException("Could not initialise due to JCA adapter not being found", ex);
        }
        return ret;
    }
}
