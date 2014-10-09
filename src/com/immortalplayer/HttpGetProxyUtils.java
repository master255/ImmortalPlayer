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

	
	public HttpGetProxyUtils(Socket sckPlayer,SocketAddress address){
		mSckPlayer=sckPlayer;
		mServerAddress=address;
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
