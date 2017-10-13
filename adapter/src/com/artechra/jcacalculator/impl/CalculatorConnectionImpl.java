package com.artechra.jcacalculator.impl;

import com.artechra.jcacalculator.CalculatorConnection;
import com.artechra.jcacalculator.ResultsCallback;

import javax.resource.ResourceException;
import java.util.List;


/**
 * Implementation class for the connection to the calculator resource
 * adapter.  The connection is a lightweight object which can be
 * created and destroyed at will (in contrast to the Managed Connection
 * which is the "heavy weight" object that should be pooled).
 *
 * This particular implementation maintains a 1:1 mapping between
 * managed connections and connections.
 *
 * @author Eoin Woods
 */
class CalculatorConnectionImpl implements CalculatorConnection {
    static final int OPERATION_TIMEOUT_MSEC = 5000;
    private CalculatorManagedConnectionImpl owner;
    private boolean isOpen;

    public CalculatorConnectionImpl(CalculatorManagedConnectionImpl owner) {
        System.out.println("New CalculatorConnection(owner=" + owner + ")");
        this.owner = owner;
        this.isOpen = true;
    }

    public long performOperationWhileIWait(List<Integer> operands)
            throws IllegalStateException, IllegalArgumentException, ResourceException {
        System.out.println("performOperationWhileIWait(operands=" + operands + ")");

        if (!this.isOpen) {
            throw new IllegalStateException("Cannot call operation on closed connection");
        }
        if (operands == null) {
            throw new IllegalArgumentException("Operands list cannot be null");
        }

        OperationCallback callback = new OperationCallback();
        this.owner.performOperationOnResource(operands, callback);
        
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < OPERATION_TIMEOUT_MSEC && !callback.isCompleted()) {
            System.out.println("No result ready for cbid:" + callback.hashCode());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Thread interrupted waiting for result", e);
            }
        }
        if (!callback.isCompleted()) {
            throw new ResourceException("Failed to receive result from Simple Resource Adapter within " +
                    OPERATION_TIMEOUT_MSEC / 1000 + " seconds");
        }

        System.out.println("Result received for cbid:" + callback.hashCode() +
                            " (ex=" + callback.getException() +
                            ", result=" + callback.getResult() + ")");
        if (callback.getException() != null) {
            throw new ResourceException("Failed to complete SimpleResource operation due to exception",
                    callback.getException());
        }
        return callback.getResult();
    }

    public void performOperationAndCallMeBack(List<Integer> operands, ResultsCallback callback)
            throws IllegalStateException, IllegalArgumentException, ResourceException {
        if (!this.isOpen) {
            throw new IllegalStateException("Cannot call operation on closed connection");
        }
        if (operands == null || callback == null) {
            throw new IllegalArgumentException("Operands list and callback cannot be null");
        }
        this.owner.performOperationOnResource(operands, callback);
    }

    public void close() {
        this.isOpen = false;
        this.owner.closeConnection(this);
    }

    public void setOwner(CalculatorManagedConnectionImpl owner) {
        this.owner.disassociateConnection(this);
        this.owner = owner;
    }

    private static class OperationCallback implements ResultsCallback {
        private Long result = null;
        private Exception failureException = null;

        public void onSuccessfulCalculation(long result) throws IllegalArgumentException {
            this.result = result;
        }

        public void onFailedCalculation(Exception failure) throws IllegalArgumentException {
            if (failure == null) {
                throw new IllegalArgumentException("Cannot call Operation Callback with a null failure");
            }
            this.failureException = failure;
        }

        public boolean isCompleted() {
            boolean ret = (this.result != null) || (this.failureException != null);
            return ret;
        }

        public long getResult() {
            return this.result;
        }

        public Exception getException() {
            return this.failureException;
        }
    }
}
