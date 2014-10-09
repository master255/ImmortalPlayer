package com.immortalplayer;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

/**
 * Http packet processing class
 *
 */
public class HttpParser {
	final static public String TAG = "HttpParser";
	final static private String RANGE_PARAMS="Range: bytes=";
	final static private String CONTENT_RANGE_PARAMS="Content-Range: bytes ";
	final static private String CONTENT_LENGTH_PARAMS="Content-Length: ";
	final static public String HTTP_BODY_END = "\r\n\r\n";
	final static public String HTTP_RESPONSE_BEGIN = "HTTP/";
	final static public String HTTP_DOCUMENT_BEGIN = "<html>";
	final static public String HTTP_REQUEST_BEGIN = "GET ";
	final static public String HTTP_REQUEST_LINE1_END = " HTTP/";
	
	private static final  int HEADER_BUFFER_LENGTH_MAX = 1448*50;
	private byte[] headerBuffer = new byte[HEADER_BUFFER_LENGTH_MAX];
	private int headerBufferLength=0;
	
	/** Link with the port */
	private int remotePort=-1;
	/** The remote server address */
	private String remoteHost;
	/** Proxy server port */
	private int localPort;
	/** Local server address */
	private String localHost;
	
	public HttpParser(String rHost,int rPort,String lHost,int lPort){
		remoteHost=rHost;
		remotePort =rPort;
		localHost=lHost;
		localPort=lPort;
	}
	
	static public class ProxyRequest{
		//**Http Request Content*//*
		public String _body;
		//**RanageLocation*//*
		public long _rangePosition;
	}
	
	static public class ProxyResponse{
		public byte[] _body;
		public byte[] _other;
		public long _currentPosition;
		public long _duration;
	}
	
	public void clearHttpBody(){
		headerBuffer = new byte[HEADER_BUFFER_LENGTH_MAX];
		headerBufferLength=0;
	}
	
	/**
	 * Get Request packets
	 * @param source
	 * @param length
	 * @return
	 */
	public byte[] getRequestBody(byte[] source,int length){
		List<byte[]> httpRequest=getHttpBody(HTTP_REQUEST_BEGIN, 
				HTTP_BODY_END, 
				source,
				length); 
		if(httpRequest.size()>0){
			return httpRequest.get(0);
		}
		return null;
	}
	
	/**
	 * Request packet parsing conversion ProxyRequest
	 * @param bodyBytes
	 * @param urlsize 
	 * @return
	 */
	public ProxyRequest getProxyRequest(byte[] bodyBytes, long urlsize){
		ProxyRequest result=new ProxyRequest();
		//Get Body
		result._body=new String(bodyBytes);
		
		// The request to the local ip remote ip
		result._body = result._body.replace(localHost, remoteHost);
		// The proxy server port to port the original URL
		if (remotePort ==-1)
			result._body = result._body.replace(":" + localPort, "");
		else
			result._body = result._body.replace(":" + localPort, ":"+ remotePort);
		//Without Range then add the fill, easy to deal with later
		String rangePosition="0";
		if(result._body.contains(RANGE_PARAMS)==false)
		{
			result._rangePosition=0;
		} else {
			rangePosition=Utils.getSubString(result._body,RANGE_PARAMS,"-");
		try {
		Log.i(TAG,"------->rangePosition:"+rangePosition);
		result._rangePosition = Integer.valueOf(rangePosition);
		if ((result._rangePosition>=urlsize) && (urlsize>0)) {
			result._body=result._body.replaceAll(RANGE_PARAMS+rangePosition, RANGE_PARAMS+"0");
			result._rangePosition=0;}
		} catch (Exception e) {
			result._rangePosition=0;
			e.printStackTrace();
		}}
		Log.i(TAG, result._body);
		
		return result;
	}
	
	/**
	 * GetProxyResponse
	 * @param source
	 * @param length
	 */
	public ProxyResponse getProxyResponse(byte[] source,int length)
	{
		List<byte[]> httpResponse=getHttpBody(HTTP_RESPONSE_BEGIN, 
				HTTP_BODY_END, 
				source,
				length);
		
		if (httpResponse.size() == 0)
			return null;
		
		ProxyResponse result=new ProxyResponse();
		
		//Get Response Body
		result._body=httpResponse.get(0);
		String text = new String(result._body);
		
		Log.i(TAG + "<---", text);
		//Get the binary data
		if(httpResponse.size()==2)
			result._other = httpResponse.get(1);
		
		//Sample Content-Range: bytes 2267097-257405191/257405192
		try {
			// Get the starting position
			if(text.contains(CONTENT_RANGE_PARAMS)==false)
			{
				result._currentPosition=0;
				if(text.contains(CONTENT_LENGTH_PARAMS)==true) {
					String duration = Utils.getSubString(text, CONTENT_LENGTH_PARAMS, "\r\n");
					try {
						result._duration = Integer.valueOf(duration)-1;
					} catch (Exception e) {
					result._duration=0;
						e.printStackTrace();
					}
				} else {result._duration=0;}
			} else {
				String currentPosition=Utils.getSubString(text,CONTENT_RANGE_PARAMS, "-");
				try {
				result._currentPosition = Integer.valueOf(currentPosition);
				// Get final position
				String startStr = CONTENT_RANGE_PARAMS + currentPosition + "-";
				String duration = Utils.getSubString(text, startStr, "/");
				result._duration = Integer.valueOf(duration);
			} catch (Exception e) {
			result._currentPosition=0; result._duration=0;
				e.printStackTrace();
			}
			}
			
			
		} catch (Exception ex) {
			Log.e(TAG, Utils.getExceptionMessage(ex));
		}
		return result;
	}
	
	private List<byte[]> getHttpBody(String beginStr,String endStr,byte[] source,int length){
		if((headerBufferLength+length)>=headerBuffer.length){
			clearHttpBody();
		}
		
		System.arraycopy(source, 0, headerBuffer, headerBufferLength, length);
		headerBufferLength+=length;
		
		List<byte[]> result = new ArrayList<byte[]>();
		String responseStr = new String(headerBuffer);
		if (responseStr.contains(beginStr)
				&& responseStr.contains(endStr)) {
			
			int startIndex=responseStr.indexOf(beginStr, 0);
			int endIndex = responseStr.indexOf(endStr, startIndex);
			endIndex+=endStr.length();
			
			byte[] header=new byte[endIndex-startIndex];
			System.arraycopy(headerBuffer, startIndex, header, 0, header.length);
			result.add(header);
			
			if ((headerBufferLength > header.length) && (responseStr.indexOf(HTTP_DOCUMENT_BEGIN, header.length)==-1)) {
				//There are binary data
				byte[] other = new byte[headerBufferLength - header.length];
				System.arraycopy(headerBuffer, header.length, other, 0,other.length);
				result.add(other);
			}
			clearHttpBody();
		}
		
		return result;
	}
	
}
