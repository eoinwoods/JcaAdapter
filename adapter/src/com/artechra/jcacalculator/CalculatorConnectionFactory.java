package com.artechra.jcacalculator;

import javax.resource.ResourceException;

/**
 *  This interface defines the of the connection factory implemented by the
 *  Calculator Adapter, which is used by clients of the adapter to retrieve
 *  a connection to the adapter.
 *
 * @author Eoin Woods
 */
public interface CalculatorConnectionFactory {

    /**
     * Return a connection object which can be used to access the adapter, that
     * is configured to perform the specified type of calculation
     * @param type the type of calculation that this connection should perform
     * @return a connection object connected to the Calculator JCA Adapter
     * @throws ResourceException if the connection cannot be made
     */
    CalculatorConnection getConnection(CalculationType type) throws ResourceException;
}
