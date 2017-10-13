package com.artechra.jcacalculator.impl;

import javax.resource.spi.*;
import javax.resource.ResourceException;
import javax.security.auth.Subject;
import java.util.Set;
import java.io.PrintWriter;

/**
 * The factory class for the Calculator Adapter's managed connection objects.
 *
 * @author Eoin Woods
 */
public class CalculatorManagedConnectionFactoryImpl implements ManagedConnectionFactory, ResourceAdapterAssociation {
    private static final long serialVersionUID = 8251336603953078348L;

    private ResourceAdapter resourceAdapter;
    private PrintWriter log;

    /**
     * Implementation of ManagedConnectionFactory#createConnectionFactory().  Called by the
     * container to retrieve a Calculator Connection Factory associated with this managed
     * connection factory
     * @param connectionManager the CM to use to allocate connections
     * @return a new Connection Factory
     */
    public Object createConnectionFactory(ConnectionManager connectionManager) {
        return new CalculatorConnectionFactoryImpl(this, connectionManager);
    }

    /**
     * Implementation of a ManagedConnectionFactory#createConnectionFactory().  Called
     * in unmanaged environments (i.e. outside a container) to create a connection
     * factory.
     * @return none
     * @throws ResourceException doesn't do this
     * @throws IllegalArgumentException because we don't support unmanaged mode
     */
    public Object createConnectionFactory() throws ResourceException, IllegalArgumentException {
        throw new IllegalArgumentException("Cannot create a simple connection factory without a connection manager");
    }

    /**
     * Main method called by container when a new managed connection is required.
     * @param subject the security subject this is on behalf of (i.e. the user)
     * @param connectionRequestInfo the connection request details
     * @return a new managed connection, initialed from the connection request info (if relevant)
     * @throws ResourceException if the connection can't be created
     */
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        return new CalculatorManagedConnectionImpl(this.resourceAdapter);
    }

    /**
     * Implemenation of ManagedConnectionFactory#matchManagedConnections() called by the container to
     * implement pooling.  This method should check whether there is an entry in the supplied set of
     * managed connections that is compatible with the specified connection request info for the
     * specified subject and if so, return it.  This implementation always returns null, effectively
     * disabling pooling
     * @param set the set of connections to check
     * @param subject the subject the new connection is for
     * @param connectionRequestInfo the conn request info describing the attributes of the new connection
     * @return a connection from the set or null if no matching one was found
     * @throws ResourceException if the comparison couldn't be made
     */
    public ManagedConnection matchManagedConnections(Set set, Subject subject, ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        // returning null effectively disables connection pooling, so a new managed connection is always
        // allocated as we don't return one to use
        return null;
    }

    /**
     * Implementation of ManagedConnectionFactory#setLogWriter() called by the container to supply
     * a log destination
     * @param printWriter  the destination to use
     */
    public void setLogWriter(PrintWriter printWriter) {
        this.log = printWriter;
    }

    /**
     * Implementation of ManagedConnectionFactory#getLogWriter() which can be called by other
     * classes to get the log writer
     * @return the log writer in use or null if there isn't one
     */
    public PrintWriter getLogWriter() {
        return this.log;
    }

    /**
     * Implementation of ResourceAdapterAssociation#getResourceAdapter() to return the adapter
     * associated with this factory
     * @return a Calculator JCA Adapter
     */
    public ResourceAdapter getResourceAdapter() {
        return this.resourceAdapter;
    }

    /**
     * Implementation of ResourceAdapterAssociation#setResourceAdapter() to set the adapter
     * associated with this factory
     */
    public void setResourceAdapter(ResourceAdapter resourceAdapter) {
        this.resourceAdapter = resourceAdapter;
    }

    /**
     * Override of java.lang.Object#equals() as part of the Java Bean requirements
     * @param o the object to compare with this one
     * @return true if they can be considered equal, false otherwise
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CalculatorManagedConnectionFactoryImpl that = (CalculatorManagedConnectionFactoryImpl) o;

        if (log != null ? !log.equals(that.log) : that.log != null) return false;
        if (resourceAdapter != null ? !resourceAdapter.equals(that.resourceAdapter) : that.resourceAdapter != null)
            return false;

        return true;
    }

    /**
     * Override of java.lang.Object#hashCode() as part of the Java Bean requirements
     * @return a hash value for this object
     */
    public int hashCode() {
        int result;
        result = (resourceAdapter != null ? resourceAdapter.hashCode() : 0);
        result = 31 * result + (log != null ? log.hashCode() : 0);
        return result;
    }
}
