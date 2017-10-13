package com.artechra.calculator;

import java.util.List;

/**
 * A simple, but slightly odd, calculator class, which performs a
 * specified calculation on a list of operands, but can be cancelled
 * while this happens.
 *
 * @author Eoin Woods
 */
public class CancellableCalculator {
    private volatile boolean exit ;
    private long calculationDelayMsec ;

    /**
     * Create an initialised calculator ready for use
     */
    public CancellableCalculator() {
        this.calculationDelayMsec = 0 ;
    }

    /**
     * Perform the specified calculation operation on the supplied operands
     * @param calculationType the operation to perform
     * @param operands the list of numbers to perform the operation on
     * @return the result of performing the operation on the operands
     * @throws IllegalStateException if the calculator is interrupted during a sleep
     * @throws IllegalArgumentException if the parameters can't be used
     */
    public Long calculate(Operation calculationType, List<Integer> operands)
            throws IllegalStateException, IllegalArgumentException {
        Long result = null ;
        this.exit = false ;
        for (int operand : operands) {
            if (this.calculationDelayMsec > 0) {
                try {
                    Thread.sleep(this.calculationDelayMsec) ;
                } catch(InterruptedException ie) {
                    throw new IllegalStateException("Calculation delay interrupted", ie) ;
                }
            }
            if (result == null) {
                result = (long)operand ;
            } else if (calculationType == Operation.ADDITION) {
                result += operand;
            } else if (calculationType == Operation.MULTIPLICATION) {
                result *= operand;
            } else {
                throw new IllegalArgumentException("Unexpected calculation type found: " + calculationType);
            }
            if (this.exit) {
                result = null ;
                break ;
            }
        }
        return result ;
    }

    /**
     * Indicate that this calculator should complete processing as quickly
     * as possible and exit, assuming a calculate() call is in progress.
     */
    public void cancelCalculation() {
        this.exit = true ;
    }

    /**
     * Is the cancellation flag set?
     * @return the value of the cancellation flag
     */
    public boolean wasCancelled() {
        return this.exit ;
    }
}
