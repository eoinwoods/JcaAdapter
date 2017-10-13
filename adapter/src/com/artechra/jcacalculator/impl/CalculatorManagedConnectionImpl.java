package com.artechra.jcacalculator.impl;

import com.artechra.jcacalculator.*;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.*;
import javax.resource.ResourceException;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import java.io.PrintWriter;
import java.lang.IllegalStateException;
import java.util.List;
import java.util.ArrayList;

/**
 * The implementation of the Calculator JCA Adapter's managed connection.  The managed
 * connection is the "heavyweight" underlying connection to the resource the adapter
 * connects to (as opposed to the regular connection that is meant to be lightweight
 * so that it can be created and thrown away cheaply).
 *
 * The managed connection manages the connection contract with the container and in
 * this case provides the calculation ability too.
 *
 * @author Eoin Woods
 */
class CalculatorManagedConnectionImpl implements ManagedConnection, WorkCompletionCallback {

    CalculatorResourceAdapter owningAdapter;
    PrintWriter log;
    private boolean isOpen;
    CalculationType calcType;
    CalculatorConnectionImpl connHandle;
    ResultsCallback connHandleCallback;
    List<ConnectionEventListener> listeners;

    /**
     * Constructor to create an initialised managed connnection
     * @param resourceAdapter the resource adapter this belongs to
     */
    public CalculatorManagedConnectionImpl(ResourceAdapter resourceAdapter) {
        if (!(resourceAdapter instanceof CalculatorResourceAdapter)) {
            throw new IllegalArgumentException("CalculatorManagedConnectionImpl must be used with CalculatorResourceAdapter (found " +
                    resourceAdapter.getClass().getName() + ")");
        }
        this.owningAdapter = (CalculatorResourceAdapter) resourceAdapter;
        this.isOpen = true;
        this.listeners = new ArrayList<ConnectionEventListener>();
    }

    /**
     * Implementation of ManagedConnection#getConnection()
     * Called by the container's Connection Manager to retrieve a managed connection
     * @param subject the security principal (subject, contains "n" principals) in use
     * @param connectionRequestInfo the request info defining the request (from the connection factory originally)
     * @return a connection for this adapter matching the request info
     * @throws ResourceException if the connection can't be created
     * @throws IllegalStateException if the managed connection should not be used
     */
    public Object getConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo)
            throws ResourceException, IllegalStateException {
        log("getConnection(" + subject + ", " + connectionRequestInfo + ")");

        if (this.connHandle != null) {
            throw new IllegalStateException("ManagedConnection " + this.hashCode() + " already in use");
        }

        if (!this.isOpen) {
            throw new IllegalStateException("Cannot retrieve connection from closed managed connection");
        }
        if (!(connectionRequestInfo instanceof SimpleConnectionRequestInfo)) {
            throw new IllegalArgumentException("Connection request info object is of the wrong type (was " +
                    connectionRequestInfo.getClass().getName() + ")");
        }
        this.calcType = ((SimpleConnectionRequestInfo) connectionRequestInfo).getCalculationType();

        this.connHandle = new CalculatorConnectionImpl(this);

        return this.connHandle;
    }

    /**
     * Implementation of ManagedConnection#destroy() called when the container wants to
     * dispose of this connection entirely
     * @throws ResourceException if the connection can't be destroyed
     */
    public void destroy() throws ResourceException {
        log("Managed connection " + this.toString() + " destroyed");
        cleanup();
        this.isOpen = false;
    }

    /**
     * Implementation of ManagedConnection#cleanup() called when the container wants to
     * reset a managed connection to its unused state so it can be returned to the
     * connection pool
     */
    public void cleanup() {
        log("Managed connection " + this.toString() + " cleaned up");
        if (this.connHandle != null) {
            this.connHandle.close();
            notifyListenersOfClose(this.connHandle);
        }
    }

    /**
     * Implementation of ManagedConnection#associateConnection(), called by the
     * container to indicate that the specified connection should be associated
     * with this managed connection
     * @param o the connection to associate (should be CalculatorConnectionImpl in this case)
     * @throws ResourceException if the wrong sort of connection is specified
     */
    public void associateConnection(Object o) throws ResourceException {
        if (!(o instanceof CalculatorConnectionImpl)) {
            throw new ResourceException("Calculator Managed Connection can only associate with Calculator Connections "+
                    "(found " + o.getClass().getName() + ")") ;
        }
        CalculatorConnectionImpl conn = (CalculatorConnectionImpl) o;
        conn.setOwner(this);
        this.connHandle = conn;
    }

    /**
     * Used to break the connection with this managed connection as part of its close operation
     * @param conn the connection that wishes to disassocate
     */
    public void disassociateConnection(CalculatorConnection conn) {
        if (this.connHandle != conn) {
            throw new IllegalArgumentException("Connection " + conn + " is not associated with this managed connection");
        }
        this.connHandle = null;
    }

    /**
     * Used by the Calculator Connection when it is closing, to notify the managed connection.
     * An important part of this is calling the registered listeners (which come from the container)
     * so that usage can be tracked and this reused if possible
     * @param conn the connection that is closing
     */
    public void closeConnection(CalculatorConnection conn) {
        log("Managed connection closing connection " + conn);
        this.disassociateConnection(conn);
        this.notifyListenersOfClose(conn);
    }

    /**
     * Implementation of ManagedConnection#addConnectionEventListener(), called by the container
     * to register a listener so that it can keep track of who is using what
     * @param connectionEventListener the listener to call back when connections close
     */
    public void addConnectionEventListener(ConnectionEventListener connectionEventListener) {
        this.listeners.add(connectionEventListener);
    }

    /**
     * Implementation of ManagedConnection#removeConnectionEventListener(), called by the container
     * to deregister a listener
     * @param connectionEventListener the listener to deregister
     */
    public void removeConnectionEventListener(ConnectionEventListener connectionEventListener) {
        this.listeners.remove(connectionEventListener);
    }

    /**
     * Implementation of ManagedConnection#getXAResource() called by the container during
     * recovery to get the associated XA Resource Manager (if any) for transaction recovery
     * @return null from this class
     */
    public XAResource getXAResource() {
        throw new UnsupportedOperationException("No transaction support implemented");
    }

    /**
     * Implementation of ManagedConnection#getLocalTransaction() called by the container
     * when it wants to try to use local transactions with the adapter
     * @return  null for this implementation
     */
    public LocalTransaction getLocalTransaction() {
        throw new UnsupportedOperationException("No transaction support implemented");
    }

    /**
     * Implementation of ManagedConnection#getMetaData() called by the container to find
     * out what sort of managed connection this is
     * @return a meta data object to describe this connection
     */
    public ManagedConnectionMetaData getMetaData() {
        return new CalculatorManagedConnectionMetaData();
    }

    /**
     * Implementation of ManagedConnection#setLogWriter() called by the container to
     * provide the connection with a log to write to
     * @param printWriter the log destination to use
     */
    public void setLogWriter(PrintWriter printWriter) {
        this.log = printWriter;
    }

    /**
     * Implementation of ManagedConnection#setLogWriter(), no idea why this would be needed
     * (except perhaps for other adapter classes to use)
     * @return the log writer in use
     */
    public PrintWriter getLogWriter() {
        return this.log;
    }

    /**
     * Implementation of our WorkCompletionCallback#onWorkCompletion, called by the resource
     * adapter when Work Manager threads which were started on our behalf complete.  In response to
     * this, this managed connection calls back in turn to the Calculator Connection who initiated
     * the request.
     * @param completedWorkItem the item that completed
     * @param status the WorkEvent status indicator (WORK_COMPLETED, WORK_REJECTED)
     * @param workItemException the exception that the work item threw, if it failed, otherwise null
     */
    public void onWorkCompletion(Work completedWorkItem, int status, Exception workItemException) {
        System.out.println("SimpleManagedConnection.onWorkCompletion(" + completedWorkItem + ", " +
                status + ", " + workItemException + ")");

        if (status != WorkEvent.WORK_REJECTED && status != WorkEvent.WORK_COMPLETED) {
            throw new IllegalStateException("Unexpected work status of " + status + " received by managed connection");
        }
        if (!(completedWorkItem instanceof CalculationWorkItem)) {
            throw new IllegalStateException("Unexpected work item type received by managed connection (found " +
                    completedWorkItem.getClass().getName() + ")");
        }

        if (workItemException != null) {
            this.connHandleCallback.onFailedCalculation(workItemException);
        } else {
            CalculationWorkItem calcItem = (CalculationWorkItem) completedWorkItem;
            this.connHandleCallback.onSuccessfulCalculation(calcItem.getResult());
        }
        this.connHandleCallback = null;
    }

    /**
     * Override of java.lang.Object#equals() as part of Java Bean requirements
     * @param o the object to compare this one to
     * @return true if they can be considered equal, false otherwise
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CalculatorManagedConnectionImpl that = (CalculatorManagedConnectionImpl) o;

        if (isOpen != that.isOpen) return false;
        if (calcType != that.calcType) return false;
        if (connHandle != null ? !connHandle.equals(that.connHandle) : that.connHandle != null) return false;
        if (connHandleCallback != null ? !connHandleCallback.equals(that.connHandleCallback) : that.connHandleCallback != null)
            return false;
        if (listeners != null ? !listeners.equals(that.listeners) : that.listeners != null) return false;
        if (log != null ? !log.equals(that.log) : that.log != null) return false;
        if (owningAdapter != null ? !owningAdapter.equals(that.owningAdapter) : that.owningAdapter != null)
            return false;

        return true;
    }

    /**
     * Override of java.lang.Object#hashCode() as part of Java Bean requirements
     * @return a hash value for this object
     */
    public int hashCode() {
        int result;
        result = (owningAdapter != null ? owningAdapter.hashCode() : 0);
        result = 31 * result + (log != null ? log.hashCode() : 0);
        result = 31 * result + (isOpen ? 1 : 0);
        result = 31 * result + (calcType != null ? calcType.hashCode() : 0);
        result = 31 * result + (connHandle != null ? connHandle.hashCode() : 0);
        result = 31 * result + (connHandleCallback != null ? connHandleCallback.hashCode() : 0);
        result = 31 * result + (listeners != null ? listeners.hashCode() : 0);
        return result;
    }

    /**
     * Protected method used by the Calculator Connection to ask for a calculation to be
     * performed
     * @param operands the operands to calculate
     * @param completionCallback the object to call when done
     * @throws ResourceException if the operation can't be run (probably a Work Manager problem)
     * @throws IllegalStateException if this connection doesn't have a connection associated with it
     */
    void performOperationOnResource(List<Integer> operands, ResultsCallback completionCallback)
            throws ResourceException, IllegalStateException {
        if (this.connHandleCallback != null) {
            throw new IllegalStateException("SimpleManagedConnection.performOperationOnResource() called " +
                    "again when outstanding request is pending");
        }
        Work item = new CalculationWorkItem(this.calcType, operands);
        this.connHandleCallback = completionCallback;
        try {
            this.owningAdapter.runWorkObject(item, this);
        } catch (WorkException e) {
            this.connHandleCallback = null;
            throw new ResourceException("Could not run work item", e);
        }
    }

    /**
     * Helper method used when connections are closed to notify all of the
     * registered listeners of the event
     * @param conn the connection that is closing
     */
    private void notifyListenersOfClose(CalculatorConnection conn) {
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(conn);
        for (ConnectionEventListener listener : this.listeners) {
            listener.connectionClosed(event);
        }
    }

    /**
     * Helper method to allow easy logging of simple messages
     * @param message the message to write
     */
    private void log(String message) {
        if (this.log == null) {
            this.log = new PrintWriter(System.out);
        }
        this.log.println(message);
    }


}
