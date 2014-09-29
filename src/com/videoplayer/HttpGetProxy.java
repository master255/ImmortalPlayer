package com.videoplayer;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Arrays;

import com.videoplayer.HttpGetProxyUtils.ProxyRequest;
import com.videoplayer.HttpGetProxyUtils.ProxyResponse;

import android.os.Environment;
import android.util.Log;

public class HttpGetProxy{
	
	final static public String TAG = "HttpGetProxy";
	private static final int SIZE = 1024*1024;
	final static public String LOCAL_IP_ADDRESS = "127.0.0.1";
	final static public int HTTP_PORT = 80;
	private int remotePort=-1;
	private String remoteHost;
	private int localPort;
	private ServerSocket localServer = null;
	private SocketAddress serverAddress;
	private ProxyResponse proxyResponse=null;
	private String file1,mUrl;
	private String mMediaFilePath;
	private RandomAccessFile os = null;
	private Proxy proxy=null;
	public boolean error=false, error1=false;
	/**
	 * Initialize the proxy server, and start the proxy server
	 * @param dir Path cache folder
	 * @param size The size of the required pre-loaded
	 * @param maximum The maximum number of pre-loaded files
	 */
	public HttpGetProxy(String dirPath,long MaxSize,int maxnum, String file, String url) {
		try {
			//Initialize proxy server
			localServer = new ServerSocket(0, 1,InetAddress.getByName(LOCAL_IP_ADDRESS));
			localPort =localServer.getLocalPort();//There ServerSocket automatically assigned port
			//Clear the cache file past
			long freespace = Environment.getExternalStorageDirectory().getFreeSpace();
			freespace=freespace-200000000;
			MaxSize=freespace>MaxSize?MaxSize:freespace;
			Utils.asynRemoveBufferFile(dirPath, maxnum, MaxSize);
			
			file1=file;
			mUrl=url;
			mMediaFilePath = dirPath + "/" + file1;
			Log.i(TAG, "1 start proxy");
			startProxy();
		} catch (Exception e) {
		}
	}

	/**
	 * Get playing links
	 */
	public String getLocalURL() {
			URI originalURI = URI.create(mUrl);
			remoteHost = originalURI.getHost();
			String localUrl = mUrl.replace(remoteHost, LOCAL_IP_ADDRESS + ":" + localPort);
		return localUrl;
	}
	
	private void startProxy() {
		new Thread() {
			public void run() {
		while (true) {
			// --------------------------------------
			// MediaPlayer's request listening, MediaPlayer-> proxy server
			// --------------------------------------
			try {
				if(proxy!=null){
					proxy.closeSockets();
				} 
				Socket s = localServer.accept();
				proxy = new Proxy(s);
				proxy.run();
			} catch (IOException e) {
				Log.e(TAG, e.toString());
				Log.e(TAG, Utils.getExceptionMessage(e));
			}
		}
			}
		}.start();
	}
	
	private class Proxy{
		/** Socket receive requests Media Player */
		private Socket sckPlayer = null;
		/** Socket transceiver Media Server requests */
		private Socket sckServer = null;
		
		public Proxy(Socket sckPlayer){
			this.sckPlayer=sckPlayer;
		}
		
		/**
		 * Shut down the existing links
		 */
		public void closeSockets(){
			try {// Before starting a new request to close the past Sockets
				if (sckPlayer != null){
					sckPlayer.close();
					sckPlayer=null;
				}
				
				if (sckServer != null){
					sckServer.close();
					sckServer=null;
				}
			} catch (IOException e1) {}
		}
		
		public void run() {
			HttpParser httpParser = null;
			HttpGetProxyUtils utils = null;
			int bytes_read;
			final int MIN_SIZE= 150*1024;
			byte[] file_buffer = new byte[1024];
			File file = new File(mMediaFilePath);
			byte[] local_request = new byte[1024];
			byte[] remote_reply = new byte[1024 * 50];

			boolean sentResponseHeader = false, sentBody=true;
			boolean isExists=false;
			try {
				httpParser = new HttpParser(remoteHost, remotePort, LOCAL_IP_ADDRESS, localPort);
				ProxyRequest request = null;
				while ((bytes_read = sckPlayer.getInputStream().read(
						local_request)) != -1) {
					byte[] buffer = httpParser.getRequestBody(local_request,
							bytes_read);
					if (buffer != null) {
						request = httpParser.getProxyRequest(buffer);
						break;
					}
				}
				serverAddress = new InetSocketAddress(remoteHost, HTTP_PORT);
				utils = new HttpGetProxyUtils(sckPlayer, serverAddress);
				isExists = file.exists();
				int sentBufferSize = 0;
				if (request._rangePosition==0) {error=false;}
				////////////////////File info
				/*if  (request._rangePosition > file.length()) {// Range is too small cache size exceeds a pre-
					Log.i(TAG,">>>Do not read the pre-loaded files range:" + request._rangePosition + ",buffer:" + file.length());
				}

				if  (file.length() < MIN_SIZE) {// Pre-cache available is too small, no need to read and re-issued Request
					Log.i(TAG, ">>>Pre-loaded file is too small, does not read the pre-loaded");
				}*/
				/////////////////////
				
				if ((isExists) && (error==false) && (request._rangePosition < file.length()) && (file.length() > MIN_SIZE)) 
				{//Send pre-loaded file to MediaPlayer
					Log.i(TAG, "Send pre-loaded into the MediaPlayer");
					isExists = false;
					byte[] clear = new byte[1024];

					RandomAccessFile fInputStream = null;
					try {
						fInputStream = new RandomAccessFile(file, "r");
						if (request._rangePosition > 0) {
							fInputStream.seek(request._rangePosition);
							}
							String header = "HTTP/1.1 206 Partial Content\r\nAccept-Ranges: bytes\r\nContent-Length: "+Integer.toString((int)(file.length()-request._rangePosition))+"\r\nContent-Range: bytes "+Integer.toString((int)request._rangePosition)+"-"+Integer.toString((int)(file.length()-1))+"\r\n\r\n";
							
							
						while ((bytes_read = fInputStream.read(file_buffer)) != -1) {
							if ((Arrays.equals(file_buffer,clear)==true) && error1==false) {
								if (sentBufferSize>0) {
								error=true;
								closeSockets();	
								}
								break;}
							if (sentBufferSize==0) {sckPlayer.getOutputStream().write(header.getBytes(), 0, header.length());}
							sckPlayer.getOutputStream().write(file_buffer, 0, bytes_read);
							sentBufferSize += bytes_read;//Was successfully sent computing
						}
						sckPlayer.getOutputStream().flush();
						
					} catch (Exception ex) {
						error=true;
						Log.e(TAG, Utils.getExceptionMessage(ex));
					} finally {
						try {
							if (fInputStream != null)
								fInputStream.close();
						} catch (IOException e) {}
					}
				}
				if ((sentBufferSize > 0) && (error==false)) {
						// modifying Range Request
						request._rangePosition = (int) (sentBufferSize + request._rangePosition);
						request._body = httpParser
								.modifyRequestRange(request._body, (int) request._rangePosition);
						Log.i(TAG, request._body);
						sentBody=false;
				} 
				

				if ((request != null) && !((sentBufferSize > 0) && (error==true))) {
					error=false;
					try {
						sckServer = utils.sentToServer(request._body);// Send MediaPlayer's request to server
					} catch (Exception e) {
						error1=true;
						return;
					}
					error1=false;
					os = new RandomAccessFile(mMediaFilePath, "rwd");
				} else {// MediaPlayer's request is invalid
					closeSockets();
					return;
				}
				
				// ------------------------------------------------------
				// The feedback network server sent to the MediaPlayer, network server -> Proxy -> MediaPlayer
				// ------------------------------------------------------
				
				while ((sckServer != null) && 
						((bytes_read = sckServer.getInputStream().read(remote_reply)) != -1) && 
						(sckPlayer.isClosed()==false))
				{
					if (sentResponseHeader) {
						try {// When you drag the progress bar, easy this exception, to disconnect and reconnect
							os.write(remote_reply, 0, bytes_read);
							utils.sendToMP(remote_reply, bytes_read);
						} catch (Exception e) {
							Log.e(TAG, e.toString());
							Log.e(TAG, Utils.getExceptionMessage(e));
							break;// Send abnormal (normal) exit while
						}
						
						if (proxyResponse == null)
							continue;// No Response Header exit this cycle

						// Has finished reading
						if (proxyResponse._currentPosition > proxyResponse._duration - SIZE) {
							proxyResponse._currentPosition = -1;
						} else if (proxyResponse._currentPosition != -1) {// Did not finish reading the
							proxyResponse._currentPosition += bytes_read;
						}

						continue;// Exit this while
					}
					proxyResponse = httpParser.getProxyResponse(remote_reply,
							bytes_read);
					if (proxyResponse == null)
						continue;// No Response Header exit this cycle

					sentResponseHeader = true; 
					// send http header to mediaplayer
					if (sentBody==true)
						utils.sendToMP(proxyResponse._body);
					// Send the binary data
					if (proxyResponse._other != null) {
						utils.sendToMP(proxyResponse._other);
						os.seek(proxyResponse._currentPosition);
						os.write(proxyResponse._other, 0, proxyResponse._other.length);
					}
				}
				Log.i(TAG, "END WHILE AND CLOSE SOCKETS");
				// Close 2 SOCKETS
				closeSockets();
			} catch (Exception e) {
				Log.e(TAG, e.toString());
				Log.e(TAG, Utils.getExceptionMessage(e));
			}
			finally {
				try {
					if (os != null)
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
