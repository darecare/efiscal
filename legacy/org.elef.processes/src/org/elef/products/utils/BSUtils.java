package org.elef.products.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.soap.Node;
import org.adempiere.process.rpl.XMLHelper;
//import org.adempiere.process.rpl.XMLHelper;
import org.compiere.model.MProduct;
import org.elef.model.MELF_ApiConn;
import org.elef.model.MELF_ApiField;
import org.elef.model.MELF_ApiTemplate;
import org.json.JSONObject;
//import org.w3c.dom.Document;
//import org.w3c.dom.Document;
//import org.w3c.dom.Node;

public class BSUtils {
	
	public static String getSessionHandle(MELF_ApiConn ElfApiConn, MELF_ApiTemplate ElfApiTemplate, String CompanyID, String Year) throws Exception {
		//HttpClient client = HttpClient.newHttpClient();
		
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body>"
                + "<GetSessionHandle>"
                + "<Language>1</Language>"
                + "<CompanyID>" + CompanyID + "</CompanyID>"
                + "<CompanyYear>" + Year + "</CompanyYear>"
                + "<Username>" + ElfApiConn.getElf_ApiUserName() + "</Username>"
                + "<Password>" + ElfApiConn.getElf_ApiPassword() + "</Password>"
                + "<BSLiveID></BSLiveID>"
                + "<BSLivePassword></BSLivePassword>"
                + "<compress>false</compress>"
                + "</GetSessionHandle>"
                + "</soap:Body>"
                + "</soap:Envelope>";

        HttpRequest request = ElfApiConn.getHttpRequest(null, xml, ElfApiTemplate);
        HttpResponse<String> response = ElfApiConn.sendRequest(request);
		if(response != null)
			return parseSessionHandleResponse(response);
		return null;
	}
	
	private static String parseSessionHandleResponse(HttpResponse<String> response) throws Exception {
		//Document doc = XMLHelper.createDocumentFromString(response.body());
		String expression = "/Envelope/Body/GetSessionHandleResponse";
		//Node SessionHandleResponse = XMLHelper.getNode(expression, doc);
//		String SessionHandle = XMLHelper.getString("./return", SessionHandleResponse);
//		return SessionHandle;
		return null;
	}
	
	public static String parseItemsResponse(HttpResponse<String> response, String itemCallType) throws Exception {
		//Document doc = XMLHelper.createDocumentFromString(response.body());
		// check if the response is a fault
	    String faultExpression = "/Envelope/Body/Fault";
//	    Node faultNode = XMLHelper.getNode(faultExpression, doc);
//	    if (faultNode != null) {
//	        String faultCode = XMLHelper.getString("./faultcode", faultNode);
//	        String faultString = XMLHelper.getString("./faultstring", faultNode);
//	        throw new Exception("SOAP Fault: " + faultCode + " - " + faultString);
//	    }
//		String expression = "/Envelope/Body/"+ itemCallType +"Response";
//		Node ItemsResponse = XMLHelper.getNode(expression, doc);
//		String items = XMLHelper.getString("./return", ItemsResponse);
//		return items;
	    return null;
	}
	
	public static String prepareXMLRequest(String sessionHandle, String itemType, String JSONRequest, String itemCallType) {
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
	    xml.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
	    xml.append("<soap:Body>");
	    xml.append("<" + itemCallType + ">");
	    xml.append("<SessionHandle>" + sessionHandle + "</SessionHandle>");
	    xml.append("<sItemType>" + itemType + "</sItemType>");
	    xml.append("<JSON" + itemCallType + "Request>" + JSONRequest + "</JSON" + itemCallType + "Request>");
		xml.append("<limit>2</limit>");
		xml.append("</" + itemCallType + ">");
		xml.append("</soap:Body>");
		xml.append("</soap:Envelope>");
		
		return xml.toString();
	}
	
	public static HttpResponse<String> sendRequest(String xml) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(new URI("http://biznisoft.com:28805/soap/IBSWebService"))
				.header("Content-Type", "text/xml; charset=utf-8")
				.POST(HttpRequest.BodyPublishers.ofString(xml, StandardCharsets.UTF_8)).build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		return response;
	}
	
	public static void setProductFields(MProduct mproduct,List<MELF_ApiField> MELFApiField, JSONObject product) {
		//loop through the fields and set the values to the product
		for (MELF_ApiField apiField : MELFApiField) {
			Object value = product.get(apiField.getElf_ExtName());
			if (value != null) {
				mproduct.set_ValueOfColumn(apiField.getName(), value);
			}
		}
	}
	
}
