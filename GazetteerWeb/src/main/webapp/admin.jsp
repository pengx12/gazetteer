<%@page import="org.elasticsearch.action.support.IndicesOptions"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.elasticsearch.cluster.metadata.MappingMetaData"%>
<%@page import="org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse"%>
<%@page import="org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest"%>
<%@page import="me.osm.gazetteer.web.Importer"%>
<%@page import="java.nio.charset.Charset"%>
<%@page import="org.apache.commons.codec.binary.Base64"%>
<%@page import="org.apache.commons.codec.digest.DigestUtils"%>
<%@page import="org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse"%>
<%@page import="java.util.Map"%>
<%@page import="org.elasticsearch.action.admin.indices.status.IndexStatus"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="org.elasticsearch.action.admin.indices.status.IndicesStatusResponse"%>
<%@page import="org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder"%>
<%@page import="org.elasticsearch.client.AdminClient"%>
<%@page import="me.osm.gazetteer.web.ESNodeHodel"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
	
	<%
	final String authorization = request.getHeader("Authorization");
    if (authorization != null && authorization.startsWith("Basic")) {
       
		// Authorization: Basic base64credentials
        String base64Credentials = authorization.substring("Basic".length()).trim();
        String credentials = new String(Base64.decodeBase64(base64Credentials),
                Charset.forName("UTF-8"));
        
        // credentials = username:password
        final String[] values = credentials.split(":",2);
        
        if(DigestUtils.md5Hex(values[1]).equals("21232f297a57a5a743894a0e4a801fc3")){
        	request.getSession().setAttribute("user", values[0]);
        }

	}
	
	String user = (String)request.getSession().getAttribute("user");
    if(user == null || !user.equals("admin")) {
		response.addHeader("WWW-Authenticate", "Basic");
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You are not authorized");
	}
	
	Map<String, String[]> requesParams = request.getParameterMap(); 
	if(requesParams.containsKey("dropindex")) {
		AdminClient adminClient = ESNodeHodel.getClient().admin();
		DeleteIndexResponse delResponse = adminClient.indices().prepareDelete(requesParams.get("dropindex")).get();
		
		response.sendRedirect("admin.jsp");
	}
	
	if(requesParams.containsKey("import")) {
		String[] importPathParam = requesParams.get("import");
		Importer importer = new Importer(importPathParam[0]);
		importer.run();
		response.sendRedirect("admin.jsp");
	}
	
	JSONObject clusterInfo = new JSONObject(
			ESNodeHodel.getClient().admin().cluster()
				.prepareClusterStats().get().toString());
	
	JSONObject nodes = clusterInfo.getJSONObject("nodes");
	%>
	
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Gazetter elasticsearch administration page</title>
</head>
<body>
	<h1>Gazetter elasticsearch administration page</h1>
	<h3>Versions: <%=nodes.getJSONArray("versions").toString() %></h3>
	<form>
		<input name="import" type="text"></input>
		<input type="submit" value="Import"></input>
	</form>
	<br><br>
	<table>
	<% 
	
	IndicesStatusResponse indecesResponse = ESNodeHodel.getClient().admin().indices().prepareStatus().execute().actionGet();
	for (Entry<String, IndexStatus> entry : indecesResponse.getIndices().entrySet()) { 
		long indexSize = entry.getValue().getStoreSize().getMb();
		String indexName = entry.getKey();
	%>
		<tr>
			<td><%=indexName %></td>
			<td><%=indexSize %> mb</td>
			<td><a href="admin.jsp?dropindex=<%=indexName%>">drop</a></td>
		</tr>
	<%}	%>
	</table>
	<h3>mapping</h3>
	<pre><% 
	if(indecesResponse.getIndices().get("gazetteer") != null) {
		GetMappingsResponse getMappingsResponse = 
			ESNodeHodel.getClient().admin().indices().getMappings(
					new GetMappingsRequest().indices("gazetteer")).get();
		
		MappingMetaData placeMapping = getMappingsResponse.getMappings().get("gazetteer").get(Importer.TYPE_NAME);
		
		if(placeMapping != null) {
			String mapping = new JSONObject(placeMapping.source().toString()).toString(2);
			out.println(mapping);
		}
	}
	%>
	</pre>	
</body>
</html>