package com.immortalplayer;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
/**
 * Proxy Tools
 *
 */
public class HttpGetProxyUtils {
	final static public String TAG = "HttpGetProxyUtils";
	
	/** Socket receive requests Media Player */
	private Socket mSckPlayer = null;
	/**Address Server*/
	private SocketAddress mServerAddress;
	
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
	
	public HttpGetProxyUtils(Socket sckPlayer,SocketAddress address){
		mSckPlayer=sckPlayer;
		mServerAddress=address;
	}

	/**
	 * The Response Header remove server
	 * @throws IOException 
	 */
	public ProxyResponse removeResponseHeader(Socket sckServer,HttpParser httpParser)throws IOException {
		ProxyResponse result = null;
		int bytes_read;
		byte[] tmp_buffer = new byte[1024];
		while ((bytes_read = sckServer.getInputStream().read(tmp_buffer)) != -1) {
			result = httpParser.getProxyResponse(tmp_buffer, bytes_read);
			if (result == null)
				continue;// No Header exit this cycle
			// Response received the Header
			if (result._other != null) {// Send the remaining data
				sendToMP(result._other);
			}
			break;
		}
		return result;
	}
	
	public void sendToMP(byte[] bytes, int length) throws IOException {
		mSckPlayer.getOutputStream().write(bytes, 0, length);
		mSckPlayer.getOutputStream().flush();
	}

	public void sendToMP(byte[] bytes) throws IOException{
		if(bytes.length==0)
			return;
		mSckPlayer.getOutputStream().write(bytes);
		mSckPlayer.getOutputStream().flush();	
	}
	
	public Socket sentToServer(String requestStr) throws IOException{
		Socket sckServer = new Socket();
		sckServer.connect(mServerAddress);
		sckServer.getOutputStream().write(requestStr.getBytes());// MediaPlayer's request
		sckServer.getOutputStream().flush();
		return sckServer;
	}
}