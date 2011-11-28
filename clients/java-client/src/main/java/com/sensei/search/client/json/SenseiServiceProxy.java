package com.sensei.search.client.json;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

import com.sensei.search.client.json.req.SenseiClientRequest;
import com.sensei.search.client.json.res.SenseiResult;

public class SenseiServiceProxy {
    private static Log LOG = LogFactory.getLog(SenseiServiceProxy.class);
	public SenseiResult sendRequest(String urlStr, SenseiClientRequest request) throws IOException, JSONException {
    	String requestStr = JsonSerializer.serialize(request).toString();
        String output = sendPost(urlStr, requestStr);
        //System.out.println("Output from Server = " + output);
        return JsonDeserializer.deserialize(SenseiResult.class, jsonResponse(output));
    }
	private String sendPost(String urlStr, String requestStr)
			throws MalformedURLException, IOException, ProtocolException,
			UnsupportedEncodingException {
		HttpURLConnection conn = null;
        try {
        LOG.info("Sending a post request to the server - " + urlStr);
        LOG.debug("The request is - " + requestStr);
        URL url = new URL(urlStr);
         conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
     
        conn.setRequestProperty("Accept-Encoding", "gzip");
		String string = requestStr;
        byte[] requestBytes = string.getBytes("UTF-8");
        conn.setRequestProperty("Content-Length", String.valueOf(requestBytes.length));
        //GZIPOutputStream zippedOutputStream = new GZIPOutputStream(conn.getOutputStream());
        OutputStream os = new BufferedOutputStream( conn.getOutputStream());
        os.write(requestBytes);
        os.flush();
        os.close();
        int responseCode = conn.getResponseCode();
		LOG.info("The http response code is " + responseCode);
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed : HTTP error code : "
                + responseCode);
        }
        byte[] bytes = drain(new GZIPInputStream(new BufferedInputStream( conn.getInputStream())));
        String output = new String(bytes, "UTF-8");
        LOG.debug("The response from the server is - " + output);
        return output;
        } finally {
        	if (conn != null) conn.disconnect();
        }
	}
    private JSONObject jsonResponse(String output) throws JSONException {
        return new JSONObject(output);
    }
    byte[] drain(InputStream inputStream) throws IOException {
        try {
        byte[] buf = new byte[1024];
        int len;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                while ((len = inputStream.read(buf)) > 0) {
                    byteArrayOutputStream.write(buf, 0, len);
                }      
        return byteArrayOutputStream.toByteArray();
        } finally {
            inputStream.close();
        }
    }
}