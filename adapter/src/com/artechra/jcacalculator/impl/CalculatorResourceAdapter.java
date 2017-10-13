package com.artechra.jcacalculator.impl;

import com.artechra.jcacalculator.CalculationType;

import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.work.*;
import javax.resource.ResourceException;
import javax.transaction.xa.XAResource;
import java.util.*;
import java.io.Serializable;

/**
 * This class implements the main body of the Calculator Resource Adapter.
 * The only things that this class has to do are to handle start() and
 * stop() lifecycle methods and the endpointActivation/Deactivation()
 * methods (which are only used in inbound adapters).
 *
 * This this case though, the Adapter class is used to implement the
 * interface with the Work Manager, allowing a work object to be
 * executed and handling the associated lifecycle methods.
 *
 * @author Eoin Woods
 */
public class CalculatorResourceAdapter implements ResourceAdapter, Serializable {

    private static final long serialVersionUID = -8570564690131485528L;

    // Setting this timeout to anything shorter than INDEFINITE (e.g. 5000) results
    // in "javax.resource.spi.work.WorkRejectedException: error code: 1" exceptions.
    // Error code 1 turns out to mean START_TIMED_OUT and it appears that the BEA
    // work manager sometimes pauses for a number of seconds before starting work.
    static final long WM_START_TIMEOUT_MSEC = WorkManager.INDEFINITE ;

    private CalculationType calculationType ;    

    private WorkManager workManager; // The WM used to run asynchronous work

    private Map<Work, Date> runningWorkObjects = new HashMap<Work, Date>();

    /**
     * Implementation of ResourceAdapter#start(), called when the adapter
     * is started by the container
     * @param context a context object that allows a Work Manager to be retrieved
     */
    public void start(BootstrapContext context) {
        log("CalculatorResourceAdapter.start()'ing");
        this.workManager = context.getWorkManager();
        log("CalculatorResourceAdapter.started");
    }

    /**
     * Implementaiton of ResourceAdapter#stop(), called when the adapter
     * is stopped by the container.  As part of stopping, the adapter
     * checks if there are any work objects still running in the Work
     * Manager and if there are, they are stopped.
     */
    public void stop() {
        log("CalculatorResourceAdapter.stop()'ing");
        log("Adapter has " + this.runningWorkObjects.size() + " running work items");
        for (Work item : this.runningWorkObjects.keySet()) {
            long startTime = this.runningWorkObjects.get(item).getTime();
            long now = System.currentTimeMillis();
            log("Work item " + item + " has been running for " + (now - startTime) + " milliseconds - stopping");
            item.release();
        }
        log("CalculatorResourceAdapter stopped");
    }

    /**
     * Implementation of ResourceAdapter#endpointActivation(), unused in this adapter.
     * Used in inbound adapters to register a new inbound message endpoint
     * @param messageEndpointFactory the factory to use to create the endpoint
     * @param activationSpec the activation spec defining the endpoint's parameters
     * @throws ResourceException if the process fails
     */
    public void endpointActivation(MessageEndpointFactory messageEndpointFactory, ActivationSpec activationSpec) throws ResourceException {
        throw new UnsupportedOperationException("CalculatorResourceAdapter does not support the message inflow contract");
    }

    /**
     * Implementation of ResourceAdapter#endpointDeactivation(), unused in this adapter.
     * Used in inbound adapters to shutdown a new inbound message endpoint
     * @param messageEndpointFactory the factory that created the endpoint
     * @param activationSpec the activation spec that defined the endpoints' parameters
     */
    public void endpointDeactivation(MessageEndpointFactory messageEndpointFactory, ActivationSpec activationSpec) {
        throw new UnsupportedOperationException("CalculatorResourceAdapter does not support the message inflow contract");
    }

    /**
     * Implementation of ResourceAdapter#getXAResources() called by the container
     * during crash recovery to retrieve XA resource managers for each of the active
     * message (inbound) endpoints, so tha the container can perform transaction recovery
     * @param activationSpecs the specifications of the active endpoints that a resource manager is needed for
     * @return a list of resource managers.  This adapter always returns an empty list.
     */
    public XAResource[] getXAResources(ActivationSpec[] activationSpecs) {
        log("getXAResources() - returning empty list");
        // called for recovery information, so we return none
        return new XAResource[0];
    }

    /**
     * Set the value of the Calculation Type property (the calculation operator to use
     * for this adapter).
     * @param type the calculation type
     */
    public void setCalculationType(String type) {
        this.calculationType = CalculationType.valueOf(type) ;
    }

    /**
     * Return the current Calculation Type property for this adapter
     * @return the calculation type in use
     */
    public String getCalculationType() {
        return this.calculationType.toString() ;
    }

    /**
     * Override of java.lang.Object#equals() implemented as part of Java Bean
     * compliance
     * @param o the object to compare
     * @return true if the objects are "equal"
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CalculatorResourceAdapter that = (CalculatorResourceAdapter) o;

        if (runningWorkObjects != null ? !runningWorkObjects.equals(that.runningWorkObjects) : that.runningWorkObjects != null)
            return false;
        if (workManager != null ? !workManager.equals(that.workManager) : that.workManager != null) return false;

        return true;
    }

    /**
     * Override of java.lang.Object#hashCode() implemented as part of Java Bean
     * compliance
     * @return a hash value for the object
     */
    public int hashCode() {
        int result;
        result = (workManager != null ? workManager.hashCode() : 0);
        result = 31 * result + (runningWorkObjects != null ? runningWorkObjects.hashCode() : 0);
        return result;
    }

    /**
     * Package scope method, used by the Managed Connection to execute asynchronous work
     * @param workObject the object to run in the Work Manager
     * @param callback the object to call when the workObject is complete
     * @throws WorkException if the work object cannot be executed
     */
    void runWorkObject(Work workObject, WorkCompletionCallback callback) throws WorkException {
        log("Running my workObject=" + workObject) ;
        runningWorkObjects.put(workObject, new Date());
        this.workManager.startWork(workObject, WM_START_TIMEOUT_MSEC, null, new WorkListenerCallback(callback));
    }

    /**
     * Private helper to make writing to whatever log is in use standardised
     * @param message the message to write
     */
    private void log(String message) {
        System.out.println(message) ;
    }

    /**
     * A Work Listener nested class which is used to receive lifecycle events for
     * the outstanding work objects which the Work Manager is running for the
     * adapter
     *
     * @author Eoin Woods
     */
    private class WorkListenerCallback implements WorkListener {
        WorkCompletionCallback callback;

        /**
         * Create the callback with a completion callback object to
         * allow us to notify our callers that work has completed
         * @param completionCallback the callback to call when the work is done
         */
        public WorkListenerCallback(WorkCompletionCallback completionCallback) {
            this.callback = completionCallback;
        }

        /**
         * A method called to indicate that the work has been accepted for
         * execution but hasn't yet started
         * @param event the event defining the work item and state
         */
        public void workAccepted(WorkEvent event) {
            log("Work item " + event.getWork() + " successfully accepted");
            Work workObject = event.getWork() ;
            System.out.println("workAccepted for workObject=" + workObject) ;
        }

        /**
         * A method called to indicate that the work was rejected and won't
         * be run
         * @param event the event defining the work item and state
         */
        public void workRejected(WorkEvent event) {
            log("Work item " + event.getWork() + " rejected");
            processCompletion(event);
        }

        /**
         * A method called to indicate that the work has started
         * execution and is in progress
         * @param event the event defining the work item and state
         */
        public void workStarted(WorkEvent event) {
            log("Work item " + event.getWork() + " started");
            Work workObject = event.getWork() ;
            System.out.println("workStarted for workObject=" + workObject) ;
        }

        /**
         * A method called to indicate that the work has finished
         * @param event the event defining the work item and state
         */
        public void workCompleted(WorkEvent event) {
            log("Work item " + event.getWork() + " completed");
            processCompletion(event);
        }

        /**
         * Process successful and unsuccessful work completion
         * @param event defining the work item and state
         */
        private void processCompletion(WorkEvent event) {
            // This is all a little bit complicated than you'd imagine.  The reason
            // is that the event object contains a Work object that appears to be
            // ours (it's equal() to it).  However it's actually a WebLogic wrapper
            // around our object that doesn't let us get to the original.  We
            // can't pass this back to our caller as they may cast to our specific
            // Work class and so we need to use the Work object we're passed to look
            // up the one we started with.
            // Naturally BEA don't document this!
            List<Work> workObjects = new ArrayList<Work>(runningWorkObjects.keySet()) ;
            int workItemIndex = workObjects.indexOf(event.getWork()) ;
            if (workItemIndex == -1) {
                log("Warning: completion event " + event + " received for nonexistent work item") ;
                return ;
            }
            Work object = workObjects.get(workItemIndex);  // NB crucial point is to use OUR object not WLS's one
            assert object != null ;
            long startTime = runningWorkObjects.get(object).getTime();
            long now = System.currentTimeMillis();
            log("Work object " + object + " completed in " + (now - startTime) + " milliseconds");
            runningWorkObjects.remove(object);
            if (this.callback != null) {
                this.callback.onWorkCompletion(object, event.getType(), event.getException());
            }
        }
    }

}
