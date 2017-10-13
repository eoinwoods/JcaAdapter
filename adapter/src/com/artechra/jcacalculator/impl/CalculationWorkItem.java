package com.artechra.jcacalculator.impl;

import com.artechra.calculator.CancellableCalculator;
import com.artechra.calculator.Operation;
import com.artechra.jcacalculator.CalculationType;

import javax.resource.spi.work.Work;
import java.util.List;

/**
 * This class acts as an adapter (of sorts) between the Cancellable Calculator
 * which the adapter is wrapping and the JCA Work Manager, used to run long
 * running operations.
 * <p/>
 * The class extends the calculator, adding the methods required to be a Work
 * item that the Work Manager can run.  Part of this is to implement cancellation
 * (which relies on the caalculator being cancellable too, which this one is but
 * is perhaps somewhat artificial to make the problem a little simpler).
 *
 * @author Eoin Woods
 */
class CalculationWorkItem extends CancellableCalculator implements Work {
    private CalculationType calcType;
    List<Integer> operands;
    long result;

    /**
     * Create an initialised Calculation Work Item, ready to run
     * @param type the operator type to use
     * @param operands the list of operands to run the operator on
     */
    public CalculationWorkItem(CalculationType type, List<Integer> operands) {
        super() ;
        this.calcType = type;
        this.operands = operands;
        this.result = 0;
    }

    /**
     * Implementation of Work#run(), which is called by the Work Manager to
     * run this piece of processing.
     */
    public void run() {
        System.out.println("CalculationWorkItem Item " + this.hashCode() + " started");

        Long result = this.calculate(calculationTypeToOperation(this.calcType), this.operands);

        if (this.wasCancelled()) {
            System.out.println("CalculationWorkItem Item " + this.hashCode() +
                    " exiting due to cancelation");
            this.result = 0;
        } else {
            assert result != null;
            this.result = result;
            System.out.println("CalculationWorkItem Item " + this.hashCode() +
                    " completed (result=" + this.result + ")");
        }
    }

    /**
     * Implementation of Work#release() which is called by the Work Manager to
     * indicate that the processing should complete as soon as possible.
     */
    public void release() {
        System.out.println("Work Item " + this.hashCode() + " cancelled");
        this.cancelCalculation();
    }

    /**
     * Return the result of the calculation
     * @return the result
     */
    public long getResult() {
        return this.result;
    }

    /**
     * Override of java.lang.Object#toString()
     * @return a human readable representation of the object
     */
    public String toString() {
        return "CalculationWorkItem[id=" + this.hashCode() + " calcType=" + this.calcType +
                " OpListLen=" + this.operands.size() + "]";
    }

    /**
     * A private helper to convert between the Adapter's calculation type constant
     * and the constants used by the Calculator
     * @param type Adapter calculation type
     * @return the corresponding Calculator operation type
     * @throws IllegalArgumentException if the type is unknown
     */
    private Operation calculationTypeToOperation(CalculationType type)
            throws IllegalArgumentException {
        Operation ret = null;
        switch (type) {
            case ADD:
                ret = Operation.ADDITION;
                break;
            case MULTIPLY:
                ret = Operation.MULTIPLICATION;
                break;
            default:
                throw new IllegalArgumentException("Unknown calculation type " + type);
        }
        return ret;
    }
}
