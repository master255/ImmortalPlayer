package com.immortalplayer;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;

import com.immortalplayer.HttpParser.ProxyRequest;
import com.immortalplayer.HttpParser.ProxyResponse;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class HttpGetProxy{
	
	final static public String TAG = "HttpGetProxy";
	final static public String LOCAL_IP_ADDRESS = "127.0.0.1";
	final static public int HTTP_PORT = 80;
	private int remotePort=-1;
	private long urlsize=0;
	private String remoteHost;
	private int localPort;
	private ServerSocket localServer = null;
	private SocketAddress serverAddress;
	private String mUrl;
	private String mMediaFilePath, newPath, newPath1, file2, cachefolder;
	private File file, file1;
	private Proxy proxy=null;
	private ArrayList<range> ranges = new ArrayList<range>();
	private boolean startProxy, error=false;
	private Thread prox;
	public boolean seek=false;
	private Context ctx;
	
	/**
	 * Initialize the proxy server, and start the proxy server
	 */
	public HttpGetProxy() {
		try {
			//Initialize proxy server
			localServer = new ServerSocket(0, 1,InetAddress.getByName(LOCAL_IP_ADDRESS));
			localPort =localServer.getLocalPort();//There ServerSocket automatically assigned port
			startProxy();
		} catch (Exception e) {
		}
	}

	public class range 
	{
		long start=0;
		long end=0;
		public void setstart(long star)
		{start=star;}
		public void setend(long en)
		{end=en;}
	}
	/**
	 * Get playing link
	 */
	public String getLocalURL() {
		File file=new File (Environment.getExternalStorageDirectory()
				.getAbsolutePath() + cachefolder +"/"+file2);
		if (file.exists()==true){return file.getAbsolutePath();} else 
		{
			Uri originalURI = Uri.parse(mUrl);
			remoteHost = originalURI.getHost();
			String localUrl = mUrl.replace(remoteHost, LOCAL_IP_ADDRESS + ":" + localPort);
		return localUrl;
		}
	}
	
	public void stopProxy ()
	{
			startProxy=false;
			try {
				if (localServer!=null)
				{localServer.close();
				localServer=null;}
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	public void setPaths (String dirPath, String url, int MaxSize,int maxnum, Context ctxx)
	{
		cachefolder=dirPath;
		ctx=ctxx;
		dirPath=Environment.getExternalStorageDirectory().getAbsolutePath()+dirPath;
		new File(dirPath).mkdirs();
		long maxsize1=MaxSize*1024*1024;
		Utils.asynRemoveBufferFile(dirPath, maxnum, maxsize1);
		mUrl=url;
		file2=Uri.decode(mUrl.substring(mUrl.lastIndexOf("/")+1));
		mMediaFilePath = dirPath + "/" + file2;
		file = new File(mMediaFilePath);
		file1 = new File(dirPath + "/" + file2+"-");
		error=false;
	}
	
	public void startProxy() 
	{
		startProxy=true; 
		
		prox=new Thread() {
			public void run() {
		while (startProxy) {
			// --------------------------------------
			// MediaPlayer's request listening, MediaPlayer-> proxy server
			// --------------------------------------
			Log.i("111", "1 start proxy");
			try {
				if(proxy!=null){
					proxy.closeSockets();
				} 
				Socket s = localServer.accept();
				if (startProxy==false) break;
				proxy = new Proxy(s);
			} catch (IOException e) {
				Log.e(TAG, e.toString());
				Log.e(TAG, Utils.getExceptionMessage(e));
			}
		}
			}
		};
		prox.start();
	}
	
	public void scan(Uri url)
	{
		Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, url);//scan for other players
	    ctx.sendBroadcast(scanFileIntent);
	    
	}
	
	private class Proxy
	{
		/** Socket receive requests Media Player */
		private Socket sckPlayer = null;
		/** Socket transceiver Media Server requests */
		private Socket sckServer = null;

		
		public Proxy(Socket sckPlayer){
			this.sckPlayer=sckPlayer;
			run();
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
			byte[] file_buffer = new byte[1448];
			File file2, file3;
			byte[] local_request = new byte[1024];
			byte[] remote_reply = new byte[1448*50];
			ProxyRequest request = null;
			ProxyResponse proxyResponse=null;
			boolean sentResponseHeader = false;
			boolean isExists=false;
			String header="";
			RandomAccessFile os = null, fInputStream = null;
			if (mMediaFilePath!=newPath) {
				error=false; urlsize=0; ranges.clear(); ranges.trimToSize(); newPath=mMediaFilePath; newPath1=file1.getAbsolutePath();
				}
				try {
					httpParser = new HttpParser(remoteHost, remotePort, LOCAL_IP_ADDRESS, localPort);
					while (((bytes_read = sckPlayer.getInputStream().read(
							local_request)) != -1) && (mMediaFilePath==newPath)) {
						byte[] buffer = httpParser.getRequestBody(local_request,
								bytes_read);
						if (buffer != null) {
							request = httpParser.getProxyRequest(buffer, urlsize);
							break;
						}
					}
					serverAddress = new InetSocketAddress(remoteHost, HTTP_PORT);
					utils = new HttpGetProxyUtils(sckPlayer, serverAddress);
					isExists = file.exists();

					//Read from file
					if (((isExists) || ((file1.exists()) && (error==true))) && (mMediaFilePath==newPath)) 
					{//Send pre-loaded file to MediaPlayer
						Log.i(TAG, "Send cache to the MediaPlayer");
						try {
							if ((file1.exists()) && (error==true) && (isExists==false))
							{
								fInputStream = new RandomAccessFile(file1, "r");
								header = "HTTP/1.1 206 Partial Content\r\nAccept-Ranges: bytes\r\nContent-Length: "+Integer.toString((int)(file1.length()-request._rangePosition))+"\r\nContent-Range: bytes "+Integer.toString((int)request._rangePosition)+"-"+Integer.toString((int)(file1.length()-1))+"/"+file1.length()+"\r\nContent-Type: application/octet-stream\r\n\r\n";
							} else {
								fInputStream = new RandomAccessFile(file, "r");
								header = "HTTP/1.1 206 Partial Content\r\nAccept-Ranges: bytes\r\nContent-Length: "+Integer.toString((int)(file.length()-request._rangePosition))+"\r\nContent-Range: bytes "+Integer.toString((int)request._rangePosition)+"-"+Integer.toString((int)(file.length()-1))+"/"+file.length()+"\r\nContent-Type: application/octet-stream\r\n\r\n";
							}
							if (request._rangePosition > 0) {
								fInputStream.seek(request._rangePosition);
								}
							error=false;
							sckPlayer.getOutputStream().write(header.getBytes(), 0, header.length());
							while (((bytes_read = fInputStream.read(file_buffer)) != -1)  && 
									(sckPlayer.isClosed()==false) && (mMediaFilePath==newPath)) {
								sckPlayer.getOutputStream().write(file_buffer, 0, bytes_read);
							}
							sckPlayer.getOutputStream().flush();
						} catch (Exception ex) {
							Log.e(TAG, Utils.getExceptionMessage(ex));
						} finally {
								if (fInputStream != null)
									fInputStream.close();
								
						}
						return;
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				//Read from Internet
				try {
				if ((request != null) && (isExists==false) && (mMediaFilePath==newPath))
				{
					try {
						sckServer = utils.sentToServer(request._body);// Send MediaPlayer's request to server
					} catch (Exception e) {
						error=true;
						if (file1.exists())
						{//Send pre-loaded file to MediaPlayer
							Log.i(TAG, "Send cache to the MediaPlayer");
							try {
								fInputStream = new RandomAccessFile(file1, "r");
								if (request._rangePosition > 0) {
									fInputStream.seek(request._rangePosition);
									}
								header = "HTTP/1.1 206 Partial Content\r\nAccept-Ranges: bytes\r\nContent-Length: "+Integer.toString((int)(file1.length()-request._rangePosition))+"\r\nContent-Range: bytes "+Integer.toString((int)request._rangePosition)+"-"+Integer.toString((int)(file1.length()-1))+"/"+file1.length()+"\r\nContent-Type: application/octet-stream\r\n\r\n";
								sckPlayer.getOutputStream().write(header.getBytes(), 0, header.length());
								while (((bytes_read = fInputStream.read(file_buffer)) != -1)  && 
										(sckPlayer.isClosed()==false) && (mMediaFilePath==newPath)) {
									sckPlayer.getOutputStream().write(file_buffer, 0, bytes_read);
								}
								sckPlayer.getOutputStream().flush();
							} catch (Exception ex) {
								Log.e(TAG, Utils.getExceptionMessage(ex));
							} finally {
									if (fInputStream != null)
										fInputStream.close();
							}
						}
						return;
					}
					sckServer.setSoTimeout(1000); // without this flac not work.
					sckServer.setSoLinger(true, 500); //for correct to close sockets
					sckPlayer.setSoLinger(true, 500);
					error=false; 
					os = new RandomAccessFile(file1, "rwd");
				} else {// MediaPlayer's request is invalid
					closeSockets();
					return;
				}
				
				// ------------------------------------------------------
				// The feedback network server sent to the MediaPlayer, network server -> Proxy -> MediaPlayer
				// ------------------------------------------------------
				seek=false;
				while ((sckServer != null) && 
						((bytes_read = sckServer.getInputStream().read(remote_reply)) != -1) && 
						(seek==false) && (mMediaFilePath==newPath))
				{
					if (sentResponseHeader) {
						try {// When you drag the progress bar, easy this exception, to disconnect and reconnect
							os.write(remote_reply, 0, bytes_read);
							proxyResponse._currentPosition += bytes_read;
						utils.sendToMP(remote_reply, bytes_read);
						} catch (Exception e) {
							Log.e(TAG, e.toString());
							Log.e(TAG, Utils.getExceptionMessage(e));
							break;// Send abnormal (normal) exit while
						}
						continue;// Exit this while
					}
					proxyResponse = httpParser.getProxyResponse(remote_reply,
							bytes_read);
					if (proxyResponse._duration>0) {urlsize=proxyResponse._duration;}
					sentResponseHeader = true; 
					// send http header to mediaplayer
					utils.sendToMP(proxyResponse._body);

					// Send the binary data
					if (proxyResponse._other != null) {
						utils.sendToMP(proxyResponse._other);
						os.seek(proxyResponse._currentPosition);
						os.write(proxyResponse._other, 0, proxyResponse._other.length);
						proxyResponse._currentPosition += proxyResponse._other.length;
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
					{
					os.close();
					file2=new File (newPath);
					file3=new File (newPath1);
					if (file2.exists()==false) {
						range r=new range();
						r.setstart(request._rangePosition);
						r.setend(proxyResponse._currentPosition);
						ranges.add (r);
						if (urlsize>0) {
							long h=0;
							for (int i=0; i<ranges.size(); i++)
							{
								for (int i1=0; i1<ranges.size(); i1++)
								{
									if (ranges.get(i1).start<=h) {
										if (ranges.get(i1).end>h) h=ranges.get(i1).end;
									}
								
								}
							}
							if (urlsize==(h-1)) {
								file3.renameTo(file2);
								scan(Uri.fromFile(file2));
							}
						}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}