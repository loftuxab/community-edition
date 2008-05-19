<#import "global/alfresco-template.ftl" as template />
<@template.header>
   <link rel="stylesheet" type="text/css" href="${url.context}/templates/documentlibrary/documentlibrary.css" />
   <script type="text/javascript" src="${url.context}/templates/documentlibrary/documentlibrary.js"></script>
</@>
<@template.body>
<div id="doc3">
   <div id="hd">
      <@region id="header" scope="global" protected=true />
      <@region id="title" scope="page" protected=true />
      <@region id="navigation" scope="page" protected=true />
   </div>
   <div id="bd">
      <div class="yui-t1" id="divDoclibWrapper">
         <div id="yui-main">
            <div class="yui-b" id="divDoclibDocs">
               <@region id="documentlist" scope="template" protected=true />
            </div>
         </div>
         <div class="yui-b" id="divDoclibFilters">
            <@region id="filter" scope="template" protected=true />
            <@region id="tree" scope="template" protected=true />
         </div>
      </div>
   </div>
   <div id="ft">
      <@region id="footer" scope="global" protected=true />
   </div>
   <div class="hiddenComponents">
      <@region id="file-upload" scope="template" protected=true />
   </div>
</div>
</@>