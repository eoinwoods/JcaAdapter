package com.artechra.jcacalculator ;
/**
 * This interface should be implemented by the class(es) used as callbacks
 * called by the CalculatorAdapter to return asynchronous results.
 *
 * @author Eoin Woods
 */
public interface ResultsCallback {

    /**
     * Called when the calculation completes successfully
     * @param result the result of the calculation
     */
    void onSuccessfulCalculation(long result);

    /**
     * Called when the calculation fails to complete
     * @param failure the exception that caused the calculation to fail
     */
    void onFailedCalculation(Exception failure) ;
}
