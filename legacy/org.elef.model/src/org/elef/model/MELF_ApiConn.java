package org.elef.model;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.sql.ResultSet;
import java.time.Duration;

import org.compiere.util.DB;
import org.compiere.util.Env;

import java.util.Base64;
import java.util.Properties;

import javax.net.ssl.SSLContext;

public class MELF_ApiConn extends X_ELF_ApiConn{

	private static final long serialVersionUID = -2660569400165048405L;
	HttpRequest request;
	
	public MELF_ApiConn(Properties ctx, int Elf_ApiConn_ID, String trxName) {
		super(ctx, Elf_ApiConn_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MELF_ApiConn(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}
	// want to fetch elf_apiconn_id from database
	public static int GetID(String trxName, Properties ctx, String elf_apisystem) {
		// TODO Auto-generated method stub
		int elf_apiconn_id = DB.getSQLValue(trxName, "select elf_apiconn_id from elf_apiconn where isactive = 'Y' and ad_client_id = ? and elf_apisystem = ?", Env.getAD_Client_ID(ctx), elf_apisystem);
		
		return elf_apiconn_id;
	}
	
	public HttpRequest getHttpRequest(String l_urlExtend, String l_requestBody, MELF_ApiTemplate ElfApiTemplate) {
		String url = this.getElf_ApiURL();
		if (l_urlExtend != null)
			url += l_urlExtend;
		// todo test this
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        		.uri(URI.create(url))
        		.header("Content-Type", ElfApiTemplate.getElf_ApiContentType());
		
		switch(getElf_ApiAuthType()) {
			case "BS":
				requestBuilder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((this.getElf_ApiKey() + ":" + this.getElf_ApiSecret()).getBytes()));
				break;
			case "mTLS":
				requestBuilder.header("PAC", this.getElf_PAC());
		}
		requestBuilder.method(ElfApiTemplate.getElf_ApiRequestType(), (l_requestBody != null ) ? HttpRequest.BodyPublishers.ofString(l_requestBody) : HttpRequest.BodyPublishers.ofString(""));
		HttpRequest request = requestBuilder.build();
		return request;
	}
	
	public HttpResponse<String> sendRequest(HttpRequest request) {
		// TODO Auto-generated method stub
		try {
			HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build().send(request,
					HttpResponse.BodyHandlers.ofString());
			return response;
		} 
		catch (HttpTimeoutException e) {
	        System.err.println("Request timed out: " + e.getMessage());
	        return null;
		}
		catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
    }
	
	public HttpResponse<String> sendRequest(HttpRequest request, SSLContext sslContext) {
		// TODO Auto-generated method stub
		try {
			HttpResponse<String> response = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).sslContext(sslContext).connectTimeout(Duration.ofSeconds(10)).build().send(request,
					HttpResponse.BodyHandlers.ofString());
			return response;
		} 
		catch (HttpTimeoutException e) {
	        System.err.println("Request timed out: " + e.getMessage());
	        return null;
		}
		catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
    }
}
