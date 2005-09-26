/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.webservice.repository;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import javax.transaction.UserTransaction;

import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.transaction.TransactionUtil;
import org.alfresco.repo.webservice.AbstractWebService;
import org.alfresco.repo.webservice.CMLUtil;
import org.alfresco.repo.webservice.Utils;
import org.alfresco.repo.webservice.types.AssociationDefinition;
import org.alfresco.repo.webservice.types.CML;
import org.alfresco.repo.webservice.types.Cardinality;
import org.alfresco.repo.webservice.types.ClassDefinition;
import org.alfresco.repo.webservice.types.NodeDefinition;
import org.alfresco.repo.webservice.types.Predicate;
import org.alfresco.repo.webservice.types.PropertyDefinition;
import org.alfresco.repo.webservice.types.Query;
import org.alfresco.repo.webservice.types.QueryLanguageEnum;
import org.alfresco.repo.webservice.types.Reference;
import org.alfresco.repo.webservice.types.RoleDefinition;
import org.alfresco.repo.webservice.types.Store;
import org.alfresco.repo.webservice.types.StoreEnum;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.axis.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Web service implementation of the RepositoryService. The WSDL for this
 * service can be accessed from
 * http://localhost:8080/alfresco/wsdl/repository-service.wsdl
 * 
 * @author gavinc
 */
public class RepositoryWebService extends AbstractWebService implements
        RepositoryServiceSoapPort
{
    private static Log logger = LogFactory.getLog(RepositoryWebService.class);

    private DictionaryService dictionaryService;

    private CMLUtil cmlUtil;

    private SimpleCache<String, QuerySession> querySessionCache;

    /**
     * Sets the instance of the DictionaryService to be used
     * 
     * @param dictionaryService
     *            The DictionaryService
     */
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    /**
     * Sets the CML Util
     * 
     * @param cmlUtil   CML util object
     */
    public void setCmlUtil(CMLUtil cmlUtil)
    {
        this.cmlUtil = cmlUtil;
    }

    /**
     * Sets the instance of the SimpleCache to be used
     * 
     * @param querySessionCache
     *            The SimpleCache
     */
    public void setQuerySessionCache(
            SimpleCache<String, QuerySession> querySessionCache)
    {
        this.querySessionCache = querySessionCache;
    }

    /**
     * @see org.alfresco.repo.webservice.repository.RepositoryServiceSoapPort#getStores()
     */
    public Store[] getStores() throws RemoteException, RepositoryFault
    {
        UserTransaction tx = null;

        try
        {
            tx = Utils.getUserTransaction(MessageContext.getCurrentContext());
            tx.begin();

            List<StoreRef> stores = this.nodeService.getStores();
            Store[] returnStores = new Store[stores.size()];
            for (int x = 0; x < stores.size(); x++)
            {
                StoreRef storeRef = stores.get(x);
                StoreEnum storeEnum = StoreEnum.fromString(storeRef
                        .getProtocol());
                Store store = new Store(storeEnum, storeRef.getIdentifier());
                returnStores[x] = store;
            }

            // commit the transaction
            tx.commit();

            return returnStores;
        } catch (Throwable e)
        {
            // rollback the transaction
            try
            {
                if (tx != null)
                {
                    tx.rollback();
                }
            } catch (Exception ex)
            {
            }

            if (logger.isDebugEnabled())
            {
                logger.error("Unexpected error occurred", e);
            }

            throw new RepositoryFault(0, e.getMessage());
        }
    }

    /**
     * @see org.alfresco.repo.webservice.repository.RepositoryServiceSoapPort#query(org.alfresco.repo.webservice.types.Store,
     *      org.alfresco.repo.webservice.types.Query, boolean)
     */
    public QueryResult query(Store store, Query query, boolean includeMetaData)
            throws RemoteException, RepositoryFault
    {
        QueryLanguageEnum langEnum = query.getLanguage();

        if (langEnum.equals(QueryLanguageEnum.cql)
                || langEnum.equals(QueryLanguageEnum.xpath))
        {
            throw new RepositoryFault(110, "Only '"
                    + QueryLanguageEnum.lucene.getValue()
                    + "' queries are currently supported!");
        }

        UserTransaction tx = null;
        MessageContext msgContext = MessageContext.getCurrentContext();

        try
        {
            tx = Utils.getUserTransaction(msgContext);
            tx.begin();

            // setup a query session and get the first batch of results
            QuerySession querySession = new ResultSetQuerySession(Utils
                    .getBatchSize(msgContext), store, query, includeMetaData);
            QueryResult queryResult = querySession
                    .getNextResultsBatch(this.searchService, this.nodeService,
                            this.namespaceService);

            // add the session to the cache if there are more results to come
            if (queryResult.getQuerySession() != null)
            {
                // this.querySessionCache.putQuerySession(querySession);
                this.querySessionCache.put(queryResult.getQuerySession(),
                        querySession);
            }

            // commit the transaction
            tx.commit();

            return queryResult;
        } catch (Throwable e)
        {
            // rollback the transaction
            try
            {
                if (tx != null)
                {
                    tx.rollback();
                }
            } catch (Exception ex)
            {
            }

            if (logger.isDebugEnabled())
            {
                logger.error("Unexpected error occurred", e);
            }

            e.printStackTrace();

            throw new RepositoryFault(0, e.getMessage());
        }
    }

    /**
     * @see org.alfresco.repo.webservice.repository.RepositoryServiceSoapPort#queryChildren(org.alfresco.repo.webservice.types.Reference)
     */
    public QueryResult queryChildren(Reference node) throws RemoteException,
            RepositoryFault
    {
        UserTransaction tx = null;

        try
        {
            tx = Utils.getUserTransaction(MessageContext.getCurrentContext());
            tx.begin();

            // setup a query session and get the first batch of results
            QuerySession querySession = new ChildrenQuerySession(Utils
                    .getBatchSize(MessageContext.getCurrentContext()), node);
            QueryResult queryResult = querySession
                    .getNextResultsBatch(this.searchService, this.nodeService,
                            this.namespaceService);

            // add the session to the cache if there are more results to come
            if (queryResult.getQuerySession() != null)
            {
                // this.querySessionCache.putQuerySession(querySession);
                this.querySessionCache.put(queryResult.getQuerySession(),
                        querySession);
            }

            // commit the transaction
            tx.commit();

            return queryResult;
        } catch (Throwable e)
        {
            // rollback the transaction
            try
            {
                if (tx != null)
                {
                    tx.rollback();
                }
            } catch (Exception ex)
            {
            }

            if (logger.isDebugEnabled())
            {
                logger.error("Unexpected error occurred", e);
            }

            throw new RepositoryFault(0, e.getMessage());
        }
    }

    /**
     * @see org.alfresco.repo.webservice.repository.RepositoryServiceSoapPort#queryParents(org.alfresco.repo.webservice.types.Reference)
     */
    public QueryResult queryParents(Reference node) throws RemoteException,
            RepositoryFault
    {
        UserTransaction tx = null;

        try
        {
            tx = Utils.getUserTransaction(MessageContext.getCurrentContext());
            tx.begin();

            // setup a query session and get the first batch of results
            QuerySession querySession = new ParentsQuerySession(Utils
                    .getBatchSize(MessageContext.getCurrentContext()), node);
            QueryResult queryResult = querySession
                    .getNextResultsBatch(this.searchService, this.nodeService,
                            this.namespaceService);

            // add the session to the cache if there are more results to come
            if (queryResult.getQuerySession() != null)
            {
                // this.querySessionCache.putQuerySession(querySession);
                this.querySessionCache.put(queryResult.getQuerySession(),
                        querySession);
            }

            // commit the transaction
            tx.commit();

            return queryResult;
        } catch (Throwable e)
        {
            // rollback the transaction
            try
            {
                if (tx != null)
                {
                    tx.rollback();
                }
            } catch (Exception ex)
            {
            }

            if (logger.isDebugEnabled())
            {
                logger.error("Unexpected error occurred", e);
            }

            throw new RepositoryFault(0, e.getMessage());
        }
    }

    /**
     * @see org.alfresco.repo.webservice.repository.RepositoryServiceSoapPort#queryAssociated(org.alfresco.repo.webservice.types.Reference,
     *      org.alfresco.repo.webservice.repository.Association[])
     */
    public QueryResult queryAssociated(Reference node, Association[] association)
            throws RemoteException, RepositoryFault
    {
        throw new RepositoryFault(1,
                "queryAssociated() is not implemented yet!");
    }

    /**
     * @see org.alfresco.repo.webservice.repository.RepositoryServiceSoapPort#fetchMore(java.lang.String)
     */
    public QueryResult fetchMore(String querySession) throws RemoteException,
            RepositoryFault
    {
        QueryResult queryResult = null;

        UserTransaction tx = null;

        try
        {
            tx = Utils.getUserTransaction(MessageContext.getCurrentContext());
            tx.begin();

            // try and get the QuerySession with the given id from the cache
            QuerySession session = this.querySessionCache.get(querySession);

            if (session == null)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Invalid querySession id requested: "
                            + querySession);

                throw new RepositoryFault(4, "querySession with id '"
                        + querySession + "' is invalid");
            }

            // get the next batch of results
            queryResult = session.getNextResultsBatch(this.searchService,
                    this.nodeService, this.namespaceService);

            // remove the QuerySession from the cache if there are no more
            // results to come
            if (queryResult.getQuerySession() == null)
            {
                this.querySessionCache.remove(querySession);
            }

            // commit the transaction
            tx.commit();

            return queryResult;
        } catch (Throwable e)
        {
            // rollback the transaction
            try
            {
                if (tx != null)
                {
                    tx.rollback();
                }
            } catch (Exception ex)
            {
            }

            if (e instanceof RepositoryFault)
            {
                throw (RepositoryFault) e;
            } else
            {
                if (logger.isDebugEnabled())
                {
                    logger.error("Unexpected error occurred", e);
                }

                throw new RepositoryFault(0, e.getMessage());
            }
        }
    }

    /**
     * @see org.alfresco.repo.webservice.repository.RepositoryServiceSoapPort#update(org.alfresco.repo.webservice.types.CML)
     */
    public UpdateResult[] update(CML statements) throws RemoteException,
            RepositoryFault
    {
        UpdateResult[] result = null;
        UserTransaction tx = null;

        try
        {
            tx = Utils.getUserTransaction(MessageContext.getCurrentContext());
            tx.begin();

            result = this.cmlUtil.executeCML(statements);

            // commit the transaction
            tx.commit();

            return result;
        } 
        catch (Throwable e)
        {
            // rollback the transaction
            try
            {
                if (tx != null)
                {
                    tx.rollback();
                }
            } catch (Exception ex)
            {
            }

            if (logger.isDebugEnabled())
            {
                logger.error("Unexpected error occurred", e);
            }

            throw new RepositoryFault(0, e.getMessage());
        }
    }

    /**
     * @see org.alfresco.repo.webservice.repository.RepositoryServiceSoapPort#describe(org.alfresco.repo.webservice.types.Predicate)
     */
    public NodeDefinition[] describe(Predicate items) throws RemoteException,
            RepositoryFault
    {
        NodeDefinition[] nodeDefs = null;
        UserTransaction tx = null;

        try
        {
            tx = Utils.getUserTransaction(MessageContext.getCurrentContext());
            tx.begin();

            List<NodeRef> nodes = Utils
                    .resolvePredicate(items, this.nodeService,
                            this.searchService, this.namespaceService);
            nodeDefs = new NodeDefinition[nodes.size()];

            for (int x = 0; x < nodes.size(); x++)
            {
                nodeDefs[x] = setupNodeDefObject(nodes.get(x));
            }

            // commit the transaction
            tx.commit();

            return nodeDefs;
        } catch (Throwable e)
        {
            // rollback the transaction
            try
            {
                if (tx != null)
                {
                    tx.rollback();
                }
            } catch (Exception ex)
            {
            }

            if (logger.isDebugEnabled())
            {
                logger.error("Unexpected error occurred", e);
            }

            throw new RepositoryFault(0, e.getMessage());
        }
    }

    /**
     * Creates a NodeDefinition web service type object for the given 
     * repository NodeRef instance
     * 
     * @param nodeRef The NodeRef to generate the NodeDefinition for
     * @return The NodeDefinition representation of nodeRef
     */
    private NodeDefinition setupNodeDefObject(NodeRef nodeRef)
    {
        if (logger.isDebugEnabled())
            logger.debug("Building NodeDefinition for node: " + nodeRef);

        TypeDefinition ddTypeDef = this.dictionaryService
                .getType(this.nodeService.getType(nodeRef));

        // create the web service ClassDefinition type from the data dictionary TypeDefinition
        ClassDefinition typeDef = Utils.setupClassDefObject(ddTypeDef);

        // create the web service ClassDefinition types to represent the aspects
        ClassDefinition[] aspectDefs = null;
        List<AspectDefinition> aspects = ddTypeDef.getDefaultAspects();
        if (aspects != null)
        {
            aspectDefs = new ClassDefinition[aspects.size()];
            int pos = 0;
            for (AspectDefinition ddAspectDef : aspects)
            {
                aspectDefs[pos] = Utils.setupClassDefObject(ddAspectDef);
                pos++;
            }
        }

        return new NodeDefinition(typeDef, aspectDefs);
    }

}
