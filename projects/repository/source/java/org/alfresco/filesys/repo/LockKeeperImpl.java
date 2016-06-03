package org.alfresco.filesys.repo;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import org.alfresco.repo.cache.TransactionalCache;
import org.alfresco.repo.lock.mem.Lifetime;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * AlfrescoLockKeeperImpl
 * <p>
 * Repository level locking for CIFS, prevents files open via CIFS/FTP/JLAN being interfered with by the alfresco "back end".
 * 
 * Delegates ephemeral locking requests to the lockService.
 * 
 * @author mrogers
 *
 */
public class LockKeeperImpl implements LockKeeper 
{
	private String LOCK_KEEPER_KEY = "AlfrescoLockKeeperImpl";
	
	private LockService lockService;
	private TransactionService transactionService;
	
    private TransactionalCache<NodeRef, KeeperInfo> lockKeeperTransactionalCache;
	
	private int timeToExpire = 3600 * 2;  // 2 Hours
	private boolean lockEnabled = true;
	
    private static final Log logger = LogFactory.getLog(LockKeeperImpl.class);
    
    public void init()
    {
        PropertyCheck.mandatory(this, "lockService", getLockService());
        PropertyCheck.mandatory(this, "lockKeeperTransactionalCache", getLockKeeperTransactionalCache());
        PropertyCheck.mandatory(this, "transactionService", getTransactionService());
    }
 

	@Override
	public void addLock(NodeRef nodeRef) 
	{
		if(lockEnabled)
		{
			if(logger.isDebugEnabled())
			{
				logger.debug("lock nodeRef:" + nodeRef);
			}
		    getLockService().lock(nodeRef, LockType.WRITE_LOCK, getTimeToExpire(), Lifetime.EPHEMERAL, LOCK_KEEPER_KEY);
		    lockKeeperTransactionalCache.put(nodeRef, new KeeperInfo(AuthenticationUtil.getFullyAuthenticatedUser()));
		}
	}

	@Override
	public void removeLock(NodeRef nodeRef) 
	{
		if(lockEnabled)
		{
		    logger.trace("removeLock nodeRef:" + nodeRef);
		    getLockService().unlock(nodeRef);
		    lockKeeperTransactionalCache.remove(nodeRef);
		}
	}

	@Override
	public void refreshAllLocks() 
	{
		Collection<NodeRef> nodes = lockKeeperTransactionalCache.getKeys();
		if(logger.isTraceEnabled())
		{
		    logger.trace("RefreshAllLocks called for #locks, " + nodes.size());
		}
		
		if(!transactionService.getAllowWrite())
		{
			if(logger.isTraceEnabled())
			{
			    logger.trace("Repo is read only - do nothing");
			    return;
			}
		}
		for(NodeRef nodeRef : nodes)
		{
			final NodeRef nodeRefToRefresh = nodeRef;
			final KeeperInfo keeperInfo = lockKeeperTransactionalCache.get(nodeRefToRefresh);
			final String additionalInfo = lockService.getAdditionalInfo(nodeRefToRefresh);
			
		    transactionService.getRetryingTransactionHelper().doInTransaction(
			new RetryingTransactionCallback<Void>()
			{	
			    @Override
        	    public Void execute() throws Throwable 
			    {
					if(LOCK_KEEPER_KEY.equalsIgnoreCase(additionalInfo))
					{
						// Its one of this class's locks
					    AuthenticationUtil.setFullyAuthenticatedUser(keeperInfo.getOwner());
				
					    //TODO What about node does not exist?
			    	    switch (lockService.getLockStatus(nodeRefToRefresh))
			    	    {
			    	        case LOCK_OWNER:
			    	        	if(logger.isDebugEnabled())
			    	        	{
			    	        	logger.debug("refresh ephemeral lock nodeRef: " + nodeRefToRefresh);
			    	        	}
			    	        	// Expect to go here - refresh the lock
			    	            getLockService().lock(nodeRefToRefresh, LockType.WRITE_LOCK, getTimeToExpire(), Lifetime.EPHEMERAL, LOCK_KEEPER_KEY);
			    	            break;
			    	        case LOCKED:
			    	        	// Locked by somebody else? Something has gone wrong here
			    	        case LOCK_EXPIRED:
			    	        default:
			    	        	if(logger.isDebugEnabled())
			    	        	{
			    	        	    logger.debug("remove lock from lock keeper cache, nodeRef: " + nodeRefToRefresh);
			    	        	}
			    	        	lockKeeperTransactionalCache.remove(nodeRefToRefresh);
			    	    }
					}
					else
					{
	    	        	if(logger.isDebugEnabled())
	    	        	{
	    	        	    logger.debug("not a lock keeper lock, remove lock from lock keeper cache, nodeRef: " + nodeRefToRefresh);
	    	        	}
						lockKeeperTransactionalCache.remove(nodeRefToRefresh);
					}
			    	return null;
			    }
						
			}, false, true);
		}
	}

	public void setLockEnabled(boolean lockEnabled) {
		this.lockEnabled = lockEnabled;
	}

	public boolean isLockEnabled() {
		return lockEnabled;
	}


	public void setLockService(LockService lockService) {
		this.lockService = lockService;
	}


	public LockService getLockService() {
		return lockService;
	}


	public void setLockKeeperTransactionalCache(
			TransactionalCache<NodeRef, KeeperInfo> lockKeeperTransactionalCache) 
	{
		this.lockKeeperTransactionalCache = lockKeeperTransactionalCache;
	}


	public TransactionalCache<NodeRef, KeeperInfo> getLockKeeperTransactionalCache() 
	{
		return lockKeeperTransactionalCache;
	}


	public void setTransactionService(TransactionService transactionHelper) 
	{
		this.transactionService = transactionHelper;
	}


	public TransactionService getTransactionService() 
	{
		return transactionService;
	}
	
	public void setTimeToExpire(int timeToExpire) {
		this.timeToExpire = timeToExpire;
	}


	public int getTimeToExpire() {
		return timeToExpire;
	}

	private class KeeperInfo implements Serializable
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -4200553975218699638L;
		/**
		 * 
		 */
		KeeperInfo(String owner)
		{
		    this.setOwner(owner);
		    lockTime = new Date();
		}
		public void setOwner(String owner) {
			this.owner = owner;
		}
		public String getOwner() {
			return owner;
		}
		private String owner;
		Date lockTime;
	}

}
