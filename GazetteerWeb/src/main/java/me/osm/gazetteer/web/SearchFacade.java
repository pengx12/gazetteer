package me.osm.gazetteer.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Servlet implementation class SearchFacade
 */
@WebServlet("/api")
public class SearchFacade extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String result = "{\"result\" : \"error\", \"error\" : \"Undefined api method request.\"}";
		
		if(request.getParameter("search") != null) {
			result = doSearch(request);
		}

		if(request.getParameter("feature") != null) {
			result = doGetFeature(request);
		}

		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json");
		
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
		response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
		response.setDateHeader("Expires", 0); // Proxies.
		
		response.getWriter().print(result);
		response.getWriter().flush();
	}

	private String doGetFeature(HttpServletRequest request) {
		return null;
	}

	private String doSearch(HttpServletRequest request) {
		String querry = request.getParameter("search");
		
		Client client = ESNodeHodel.getClient();
		SearchResponse searchResponse = client.prepareSearch("gazetteer")
			.setSearchType(SearchType.QUERY_AND_FETCH).setSize(10)
			.setQuery(QueryBuilders.simpleQueryString(querry))
			.execute().actionGet();

		return encodeSearchResult(searchResponse, 
				request.getParameter("pretty") != null && "true".equals(request.getParameter("pretty")),
				request.getParameter("full_geometry") != null && "true".equals(request.getParameter("full_geometry")));
	}

	private String encodeSearchResult(SearchResponse searchResponse, boolean preaty, boolean fullGeometry) {
		JSONObject result = new JSONObject();
		result.put("result", "success");
		
		JSONArray features = new JSONArray();
		result.put("features", features);
		
		for(SearchHit hit : searchResponse.getHits().getHits()) {
			JSONObject feature = new JSONObject(hit.getSource());
			
			if(!fullGeometry) {
				feature.remove("full_geometry");
			}
			
			features.put(feature);
		}
		
		return result.toString(preaty ? 2 : 0);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
