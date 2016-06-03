package org.alfresco.service.cmr.transfer;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * TransferEvents are produced by the Transfer service during an in flight 
 * transfer.
 * 
 * <p>
 * The TransferCallback presents TransferEvents for processing. 
 * 
 * @see TransferCallback
 * 
 * @author davidc
 */
public interface TransferEndEvent extends TransferEvent
{
    /**
     * Gets the report generated by the transfer source repository
     * 
     * @return source transfer report
     */
    public NodeRef getSourceReport();

    /**
     * Gets the report generated by the transfer destination repository
     * 
     * @return destination transfer report
     */
    public NodeRef getDestinationReport();
}
