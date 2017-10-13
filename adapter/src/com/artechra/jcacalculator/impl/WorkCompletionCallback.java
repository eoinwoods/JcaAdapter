package com.artechra.jcacalculator.impl;

import javax.resource.spi.work.Work;

/**
 * The interface a class must implement if it is to be notified of
 * Calculation work items completing
 */
interface WorkCompletionCallback {
    
    /**
     * Called when a calculation work item is completed
     * @param completedWorkItem the work item that is completed
     * @param status a WorkEvent.status value
     * @param workItemException an exception if the work item failed or null if there
     *        was no exception
     */
    public void onWorkCompletion(Work completedWorkItem, int status, Exception workItemException);
}
