package com.artechra.jcacalculator.impl;

import com.artechra.jcacalculator.*;

import javax.resource.ResourceException;
import javax.resource.Referenceable;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.naming.Reference;
import javax.naming.NamingException;
import java.io.Serializable;

/**
 * The factory class that the client uses to create connections to the
 * adapter.  Instances of this class are bound into the JNDI directory
 * to allow clients to find them.  Note that this is a "connection factory"
 * rather than a "managed connection factory" and so most of the real
 * work goes on in the associated managed factory.
 *
 * The class must implement Serializable and Referenceable in order to
 * meet its obligations under the JCA connection contract.  These allow
 * the container to associate objects with references and store them
 * in the directory.
 *
 * @author Eoin Woods
 */
public class CalculatorConnectionFactoryImpl
        implements CalculatorConnectionFactory, Serializable, Referenceable {
    private static final long serialVersionUID = -2500385499999221634L;

    private ConnectionManager connManager;
    private CalculatorManagedConnectionFactoryImpl owner;
    private Reference myReference ;

    /**
     * Create an initialised factory
     * @param owningFactory the managed factory that this factory belongs to
     * @param cm the connection manager (that came from the container) that is used to
     *           greate connections
     */
    public CalculatorConnectionFactoryImpl(CalculatorManagedConnectionFactoryImpl owningFactory, ConnectionManager cm) {
        this.owner = owningFactory;
        this.connManager = cm;
    }

    /**
     * Implementation of CalculationConnectionFactory#getConnection().  Creates a connection
     * of the specified type for the caller.
     * @param type the calculation type the caller wants to use
     * @return a connection to the adapter
     * @throws ResourceException if the connection could not be created
     */
    public CalculatorConnection getConnection(CalculationType type) throws ResourceException {
        // This object doesn't do much itself, it just calls the Connection Manager
        // that does the real work, interacting with the Managed Connection Factory to
        // create the underlying connection if needed
        ConnectionRequestInfo connRequestInfo = new SimpleConnectionRequestInfo(type);
        return (CalculatorConnection) this.connManager.allocateConnection(this.owner, connRequestInfo);
    }

    /**
     * Implemenation of Referenceable#setReference()
     * @param reference the reference to associate with this object
     */
    public void setReference(Reference reference) {
        this.myReference = reference ;
    }

    /**
     * Implemenation of Referenceable#getReference()
     * @return the reference previously associated with this object
     */
    public Reference getReference() throws NamingException {
        return this.myReference ;
    }
}
