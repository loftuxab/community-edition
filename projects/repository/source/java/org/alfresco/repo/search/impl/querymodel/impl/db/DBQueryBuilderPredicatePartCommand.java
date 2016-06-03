package org.alfresco.repo.search.impl.querymodel.impl.db;

import org.alfresco.repo.search.adaptor.lucene.LuceneFunction;

/**
 * @author Andy
 */
public class DBQueryBuilderPredicatePartCommand
{
    DBQueryBuilderPredicatePartCommandType type;

    String fieldName;

    Object value;

    Object[] values;

    String alias;
    
    private DBQueryBuilderJoinCommandType joinCommandType;

    private LuceneFunction function;
    
    private Long qnameId;

    /**
     * @return the qnameId
     */
    public Long getQnameId()
    {
        return qnameId;
    }

    /**
     * @param qnameId the qnameId to set
     */
    public void setQnameId(Long qnameId)
    {
        this.qnameId = qnameId;
    }
    
    /**
     * @return the joinCommandType
     */
    public String getJoinCommandType()
    {
        return joinCommandType.toString();
    }

    /**
     * @param joinCommandType the joinCommandType to set
     */
    public void setJoinCommandType(DBQueryBuilderJoinCommandType joinCommandType)
    {
        this.joinCommandType = joinCommandType;
    }

    /**
     * @return the type
     */
    public String getType()
    {
        return type.toString();
    }

    /**
     * @param type the type to set
     */
    public void setType(DBQueryBuilderPredicatePartCommandType type)
    {
        this.type = type;
    }

    /**
     * @return the fieldName
     */
    public String getFieldName()
    {
        return fieldName;
    }

    /**
     * @param fieldName the fieldName to set
     */
    public void setFieldName(String fieldName)
    {
        this.fieldName = fieldName;
    }

    /**
     * @return the value
     */
    public Object getValue()
    {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(Object value)
    {
        this.value = value;
    }

    /**
     * @return the values
     */
    public Object[] getValues()
    {
        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(Object[] values)
    {
        this.values = values;
    }

    /**
     * @return the joinAlias
     */
    public String getAlias()
    {
        return alias;
    }

    /**
     * @param alias the joinAlias to set
     */
    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    /**
     * @return the function
     */
    public LuceneFunction getFunction()
    {
        return function;
    }

    /**
     * @param function the function to set
     */
    public void setFunction(LuceneFunction function)
    {
        this.function = function;
    }

    public String getFieldAndFunction()
    {
        if(function != null)
        {
            if(function == LuceneFunction.LOWER)
            {
                return "LOWER( "+alias +"." +fieldName+") ";
            }
            else if(function == LuceneFunction.UPPER)
            {
                return "UPPER( "+alias +"." +fieldName+") ";
            }
            else
            {
                return alias +"." +fieldName;
            }
        }
        else
        {
            return alias +"." +fieldName;
        }
    }
    
}
