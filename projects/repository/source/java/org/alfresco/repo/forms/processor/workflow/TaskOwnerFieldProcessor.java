
package org.alfresco.repo.forms.processor.workflow;

import static org.alfresco.repo.forms.processor.node.FormFieldConstants.PROP_DATA_PREFIX;

import org.alfresco.repo.forms.FieldDefinition;
import org.alfresco.repo.forms.PropertyFieldDefinition;
import org.alfresco.repo.forms.processor.node.TransientFieldProcessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.surf.util.I18NUtil;

/**
 * Transient field processor for the "taskOwner" property.
 * 
 * @since 3.4
 * @author Gavin Cornwell
 */
public class TaskOwnerFieldProcessor extends TransientFieldProcessor
{
    public static final String KEY = "taskOwner";
    public static final String DATA_TYPE = KEY;

    private static final String MSG_LABEL = "form_service.task.owner.label";
    private static final String MSG_DESCRIPTION = "form_service.task.owner.description";

    private static final Log LOGGER = LogFactory.getLog(TaskOwnerFieldProcessor.class);
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.forms.processor.node.TransientFieldProcessor#makeTransientPropertyDefinition()
     */
    @Override
    protected FieldDefinition makeTransientFieldDefinition()
    {
        PropertyFieldDefinition fieldDef = new PropertyFieldDefinition(KEY, DATA_TYPE);
        fieldDef.setRepeating(false);
        fieldDef.setProtectedField(true);
        
        fieldDef.setLabel(I18NUtil.getMessage(MSG_LABEL));
        fieldDef.setDescription(I18NUtil.getMessage(MSG_DESCRIPTION));
        fieldDef.setDataKeyName(PROP_DATA_PREFIX + KEY);
        return fieldDef;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.forms.processor.AbstractFieldProcessor#getLogger()
     */
    @Override
    protected Log getLogger()
    {
        return LOGGER;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.forms.processor.AbstractFieldProcessor#getRegistryKey()
     */
    @Override
    protected String getRegistryKey()
    {
        return KEY;
    }

}
