<#if form.mode == "view">
<div class="viewmode-field">
   <span class="viewmode-label">${field.label?html}:</span>
   <span class="viewmode-value">${field.value?html}</span>
</div>
<#else>
<label for="${args.htmlid}_${field.id}">${field.label?html}:<#if field.mandatory><span class="mandatory-indicator">${msg("form.required.fields.marker")}</span></#if></label>
<input id="${args.htmlid}_${field.id}" type="text" name="${field.name}" 
       <#if field.control.params.styleClass?exists>class="${field.control.params.styleClass}"</#if>
       <#if field.value?is_number>value="${field.value?c}"<#else>value="${field.value}"</#if>
       <#if field.description?exists>title="${field.description}"</#if>
       <#if field.control.params.maxLength?exists>maxlength="${field.control.params.maxLength}"</#if> 
       <#if field.control.params.size?exists>size="${field.control.params.size}"</#if> 
       <#if field.disabled>disabled="true"</#if> />
</#if>