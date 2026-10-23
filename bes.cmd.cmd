<?xml version="1.0" encoding="UTF-8"?>
<bes:request xmlns:bes="http://xml.opendap.org/ns/bes/1.0#" reqID="http-nio-8080-exec-7_24" reqUUID="95db121f-b4fd-4aeb-9403-6a1be05408d9" clientId="/-0" clientCmdCount="966">
  <bes:setContext name="bes_timeout">300</bes:setContext>
  <bes:setContext name="xdap_accept">3.2</bes:setContext>
  <bes:setContext name="dap_explicit_containers">no</bes:setContext>
  <bes:setContext name="errors">xml</bes:setContext>
  <bes:setContext name="olfsLog">0:0:0:0:0:0:0:1|&amp;|curl#8#7#1|&amp;|-|&amp;|-|&amp;|1781269555503|&amp;|http-nio-8080-exec-7_24-95db121f-b4fd-4aeb-9403-6a1be05408d9|&amp;|HTTP-GET|&amp;|/opendap/hyrax/data/nc/fnoc1.nc.dds|&amp;|error#2#</bes:setContext>
  <bes:setContext name="max_response_size">0</bes:setContext>
  <bes:setContext name="max_variable_size">0</bes:setContext>
  <bes:setContext name="uid">not_logged_in</bes:setContext>
  <bes:setContainer name="catalogContainer" space="catalog">/data/nc/fnoc1.nc</bes:setContainer>
  <bes:define name="d1" space="default">
    <bes:container name="catalogContainer">
      <bes:constraint>error(2)</bes:constraint>
    </bes:container>
  </bes:define>
  <bes:get type="dds" definition="d1" />
</bes:request>
