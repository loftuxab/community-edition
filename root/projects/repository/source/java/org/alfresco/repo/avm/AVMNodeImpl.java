/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * and Open Source Software ("FLOSS") applications as described in Alfresco's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * http://www.alfresco.com/legal/licensing" */

package org.alfresco.repo.avm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.avm.util.RawServices;
import org.alfresco.repo.domain.DbAccessControlList;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.repo.security.permissions.ACLCopyMode;
import org.alfresco.service.cmr.avm.AVMReadOnlyException;
import org.alfresco.util.GUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for all repository file system like objects.
 * @author britt
 */
public abstract class AVMNodeImpl implements AVMNode, Serializable
{
    private static Log    fgLogger = LogFactory.getLog(AVMNodeImpl.class);
    
    protected static final boolean DEBUG = fgLogger.isDebugEnabled();
    
    /**
     * The Object ID.
     */
    private long fID;
    
    /**
     * The Version ID.
     */
    private int fVersionID;
    
    /**
     * The basic attributes of this.  Owner, creator, mod time, etc.
     */
    private BasicAttributes fBasicAttributes;
    
    /**
     * The version number (for concurrency control).
     */
    private long fVers;
    
    /**
     * The rootness of this node.
     */
    private boolean fIsRoot;

    /**
     * The ACL on this node.
     */
    private DbAccessControlList fACL;
    
    /**
     * The Store that we're new in.
     */
    private AVMStore fStoreNew;
    
    /**
     * The GUID for this version.
     */
    private String fGUID;
    
    /**
     * The Aspects that belong to this node.
     */
    private Set<Long> fAspects;
    
    private Map<Long, PropertyValue> fProperties;
    
    /**
     * Default constructor.
     */
    protected AVMNodeImpl()
    {
        fAspects = new HashSet<Long>();
        fProperties = new HashMap<Long, PropertyValue>();
    }

    /**
     * Constructor used when creating a new concrete subclass instance.
     * @param store The AVMStore that owns this.
     */
    protected AVMNodeImpl(AVMStore store)
    {
        fAspects = new HashSet<Long>();
        fProperties = new HashMap<Long, PropertyValue>();
        fVersionID = -1;
        fIsRoot = false;
        long time = System.currentTimeMillis();
        String user = 
            RawServices.Instance().getAuthenticationContext().getCurrentUserName();
        if (user == null)
        {
            user = RawServices.Instance().getAuthenticationContext().getSystemUserName();
        }
        fBasicAttributes = new BasicAttributesImpl(user,
                                                   user,
                                                   user,
                                                   time,
                                                   time,
                                                   time);
        fStoreNew = store;
        fGUID = GUID.generate();
    }
    
    /**
     * Set the ancestor of this node.
     * @param ancestor The ancestor to set.
     */
    public void setAncestor(AVMNode ancestor)
    {
        if (ancestor == null)
        {
            return;
        }
        HistoryLinkImpl link = new HistoryLinkImpl();
        link.setAncestor(ancestor);
        link.setDescendent(this);
        AVMDAOs.Instance().fHistoryLinkDAO.save(link);
    }

    /**
     * Change the ancestor of this node.
     * @param ancestor The new ancestor to give it.
     */
    public void changeAncestor(AVMNode ancestor)
    {
        HistoryLink old = AVMDAOs.Instance().fHistoryLinkDAO.getByDescendent(this);
        if (old != null)
        {
            AVMDAOs.Instance().fHistoryLinkDAO.delete(old);
        }
        setAncestor(ancestor);
    }
    
    /**
     * Get the ancestor of this node.
     * @return The ancestor of this node.
     */
    public AVMNode getAncestor()
    {
        return AVMDAOs.Instance().fAVMNodeDAO.getAncestor(this);
    }
    
    /**
     * Set the node that was merged into this.
     * @param mergedFrom The node that was merged into this.
     */
    public void setMergedFrom(AVMNode mergedFrom)
    {
        if (mergedFrom == null)
        {
            return;
        }
        MergeLinkImpl link = new MergeLinkImpl();
        link.setMfrom(mergedFrom);
        link.setMto(this);
        AVMDAOs.Instance().fMergeLinkDAO.save(link);
    }
    
    /**
     * Get the node that was merged into this.
     * @return The node that was merged into this.
     */
    public AVMNode getMergedFrom()
    {
        return AVMDAOs.Instance().fAVMNodeDAO.getMergedFrom(this);
    }
    
    /**
     * Equality based on object ids.
     * @param obj The thing to compare against.
     * @return Equality.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof AVMNode))
        {
            return false;
        }
        return fID == ((AVMNode)obj).getId();
    }

    /**
     * Get a reasonable hash value.
     * @return The hash code.
     */
    @Override
    public int hashCode()
    {
        return (int)fID;
    }
    
    /**
     * Set the object id.  For Hibernate.
     * @param id The id to set.
     */
    protected void setId(long id)
    {
        fID = id;
    }
    
    /**
     * Get the id of this node.
     * @return The object id.
     */
    public long getId()
    {
        return fID;
    }
 
    /**
     * Set the versionID for this node.  
     * @param versionID The id to set.
     */
    public void setVersionID(int versionID)
    {
        fVersionID = versionID;
    }
    
    /**
     * Get the version id of this node.
     * @return The version id.
     */
    public int getVersionID()
    {
        return fVersionID;
    }
    
    /**
     * Set the basic attributes. For Hibernate.
     * @param attrs
     */
    protected void setBasicAttributes(BasicAttributes attrs)
    {
        fBasicAttributes = attrs;
    }
    
    /**
     * Get the basic attributes.
     * @return The basic attributes.
     */
    public BasicAttributes getBasicAttributes()
    {
        return fBasicAttributes;
    }
    
    /**
     * Get whether this is a new node.
     * @return Whether this is new.
     */
    public boolean getIsNew()
    {
        return fStoreNew != null;
    }
 
    /**
     * Set the version for concurrency control
     * @param vers
     */
    protected void setVers(long vers)
    {
        fVers = vers;
    }
    
    /**
     * Get the version for concurrency control.
     * @return The version for optimistic locks.
     */
    protected long getVers()
    {
        return fVers;
    }

    /**
     * Get whether this is a root node.
     * @return Whether this is a root node.
     */
    public boolean getIsRoot()
    {
        return fIsRoot;
    }

    /**
     * @param isRoot
     */
    public void setIsRoot(boolean isRoot)
    {
        fIsRoot = isRoot;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.AVMNode#updateModTime()
     */
    public void updateModTime()
    {
        if (DEBUG)
        {
            checkReadOnly();
        }
        String user = 
            RawServices.Instance().getAuthenticationContext().getCurrentUserName();
        if (user == null)
        {
            user = RawServices.Instance().getAuthenticationContext().getSystemUserName();
        }
        fBasicAttributes.setModDate(System.currentTimeMillis());
        fBasicAttributes.setLastModifier(user);
    }
    
    /**
     * Copy all properties from another node.
     * @param other The other node.
     */
    protected void copyProperties(AVMNode other)
    {
        fProperties = new HashMap<Long, PropertyValue>();
        for (Map.Entry<Long, PropertyValue> entry : other.getProperties().entrySet())
        {
            fProperties.put(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Copy all aspects from another node.
     * @param other The other node.
     */
    protected void copyAspects(AVMNode other)
    {
        fAspects = new HashSet<Long>(other.getAspects());
    }
    
    protected void copyCreationAndOwnerBasicAttributes(AVMNode other)
    {
        fBasicAttributes.setCreateDate(other.getBasicAttributes().getCreateDate());
        fBasicAttributes.setCreator(other.getBasicAttributes().getCreator());
        fBasicAttributes.setOwner(other.getBasicAttributes().getOwner());
    }
    
    protected void copyACLs(AVMNode other, Long parentAcl, ACLCopyMode mode)
    {
        DbAccessControlList acl = other.getAcl();
        if (acl != null)
        {
            setAcl(acl.getCopy(parentAcl, mode));
        }
    }
    
    /**
     * Copy out metadata from another node.
     * @param other The other node.
     */
    public void copyMetaDataFrom(AVMNode other, Long parentAcl)
    {
        copyAspects(other);
        copyACLs(other, parentAcl, ACLCopyMode.COPY);
        copyProperties(other);
        copyCreationAndOwnerBasicAttributes(other);
    }
    
    /**
     * Set a property on a node. Overwrite it if it exists.
     * @param name The name of the property.
     * @param value The value to set.
     */
    public void setProperty(Long qnameEntityId, PropertyValue value)
    {
        if (DEBUG)
        {
            checkReadOnly();
        }
        fProperties.put(qnameEntityId, value);
    }
    
    public void addProperties(Map<Long, PropertyValue> properties)
    {
        for (Map.Entry<Long, PropertyValue> entry : properties.entrySet())
        {
            fProperties.put(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Set a collection of properties on this node.
     * @param properties The Map of QNames to PropertyValues.
     */
    public void setProperties(Map<Long, PropertyValue> properties)
    {
        fProperties = properties;
    }
    
    /**
     * Get a property by name.
     * @param name The name of the property.
     * @return The PropertyValue or null if non-existent.
     */
    public PropertyValue getProperty(Long qnameEntityId)
    {
        return fProperties.get(qnameEntityId);
    }
    
    /**
     * {@inheritDoc}
     */
    public Map<Long, PropertyValue> getProperties()
    {
        return fProperties;
    }

    /**
     * Delete a property from this node.
     * @param name The name of the property.
     */
    public void deleteProperty(Long qnameEntityId)
    {
        if (DEBUG)
        {
            checkReadOnly();
        }
        fProperties.remove(qnameEntityId);
    }
    
    /**
     * Delete all properties from this node.
     */
    public void deleteProperties()
    {
        fProperties.clear();
    }
    
    /**
     * Set the ACL on this node.
     * @param acl The ACL to set.
     */
    public void setAcl(DbAccessControlList acl)
    {
        fACL = acl;
    }
    
    /**
     * Get the ACL on this node.
     * @return The ACL on this node.
     */
    public DbAccessControlList getAcl()
    {
        return fACL;
    }
    
    /**
     * Set the store we are new in.
     * @param store The store we are new in.
     */
    public void setStoreNew(AVMStore store)
    {
        fStoreNew = store;
    }
    
    /**
     * Get the possibly null store we are new in.
     * @return The store we are new in.
     */
    public AVMStore getStoreNew()
    {
        return fStoreNew;
    }
    
    protected void checkReadOnly()
    {
        if (getStoreNew() == null)
        {
            throw new AVMReadOnlyException("Write Operation on R/O Node.");
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.AVMNode#getGuid()
     */
    public String getGuid() 
    {
        return fGUID;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.AVMNode#setGuid(java.lang.String)
     */
    public void setGuid(String guid) 
    {
        fGUID = guid;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.avm.AVMNode#getAspects()
     */
    public Set<Long> getAspects()
    {
        return fAspects;
    }
    
    /**
     * Set the aspects on this node.
     * @param aspects
     */
    public void setAspects(Set<Long> aspects)
    {
        fAspects = aspects;
    }
}
