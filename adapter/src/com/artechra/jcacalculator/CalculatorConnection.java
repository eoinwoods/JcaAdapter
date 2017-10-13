package com.artechra.jcacalculator;

import javax.resource.ResourceException;
import java.util.List;

/**
 * This interface defines the interface between a client of the JCA adapter and
 * the adapter's calculation facilities.  The operation that the methods 
 * perform is defined when the connection is retrieved.
 *
 * @author Eoin Woods
 */
public interface CalculatorConnection {

    /**
     * Run the connection's operation on the specified list of operands and return
     * the result synchronously.
     * @param operands the list of integers to process
     * @return the result of running the operation on the operands
     * @throws ResourceException if the operation fails
     */
    public long performOperationWhileIWait(List<Integer> operands) throws ResourceException;

    /**
     * Run the connection's operation on the specified list of operands and return
     * the result asynchronously.
     * @param operands the list of integers to process
     * @param callback the object to call when the result is available
     * @throws ResourceException if the operation fails to start
     */
    public void performOperationAndCallMeBack(List<Integer> operands, ResultsCallback callback) throws ResourceException;

    /**
     * Close this connection and return it to the application server
     * for reuse if possible.  The caller must not use the connection object
     * again after calling this method.
     */
    public void close();

}
