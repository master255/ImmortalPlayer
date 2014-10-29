package com.immortalplayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import com.immortalplayer.HttpParser.ProxyRequest;
import com.immortalplayer.HttpParser.ProxyResponse;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

public class HttpGetProxy
{
	final static public String TAG = "HttpGetProxy";
	final static public String LOCAL_IP_ADDRESS = "127.0.0.1";
	final static public int HTTP_PORT = 80;
	private int remotePort = -1;
	private long urlsize = 0;
	private int localPort;
	private ServerSocket localServer = null;
	private SocketAddress serverAddress;
	private String mUrl, ftplogin, ftppass, remoteHost, remotePath;
	private String mMediaFilePath, newPath, newPath1, file2, cachefolder;
	private File file, file1, file4;
	private Proxy proxy = null;
	private ArrayList<range> ranges = new ArrayList<range>();
	private boolean startProxy, error = false, ftpenable;
	private Thread prox;
	public boolean seek = false, close;
	private Context ctx;
	private FTPClient mFTPClient;
	private InputStream ftp = null;

	/**
	 * Initialize the proxy server, and start the proxy server
	 */
	public HttpGetProxy()
	{
		try
		{
			// Initialize proxy server
			localServer = new ServerSocket(0, 1,
					InetAddress.getByName(LOCAL_IP_ADDRESS));
			localPort = localServer.getLocalPort();// There ServerSocket
													// automatically assigned
													// port
			startProxy();
		} catch (Exception e)
		{e.printStackTrace();}
	}

	public class range
	{
		long start = 0;
		long end = 0;

		public void setstart(long star)
		{
			start = star;
		}

		public void setend(long en)
		{
			end = en;
		}
	}

	/**
	 * Get playing link
	 */
	public String getLocalURL()
	{
		File file = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + cachefolder + "/" + file2);
		if (file.exists() == true)
		{
			return file.getAbsolutePath();
		} else
		{
			Uri originalURI = Uri.parse(mUrl);
			remoteHost = originalURI.getHost();
			remotePath = originalURI.getPath();
			String localUrl = mUrl.replace(remoteHost,
					LOCAL_IP_ADDRESS + ":" + localPort).toLowerCase();
			if (localUrl.contains("ftp://") == true)
			{
				localUrl = localUrl.replaceFirst("ftp://", "http://");
				ftpenable = true;
			} else
			{
				ftpenable = false;
			}
			return localUrl;
		}
	}

	public void stopProxy()
	{
		startProxy = false;
		try
		{
			if (localServer != null)
			{
				localServer.close();
				localServer = null;
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void setPaths(String dirPath, String url, int MaxSize, int maxnum,
			Context ctxx, boolean deltemp, String loginftp, String pasftp,
			boolean ftpclose)
	{
		close = ftpclose;
		cachefolder = dirPath;
		ctx = ctxx;
		ftplogin = loginftp;
		ftppass = pasftp;
		dirPath = Environment.getExternalStorageDirectory().getAbsolutePath()
				+ dirPath;
		new File(dirPath).mkdirs();
		long maxsize1 = MaxSize * 1024L * 1024L;
		Utils.asynRemoveBufferFile(dirPath, maxnum, maxsize1);
		mUrl = url;
		file2 = Uri.decode(mUrl.substring(mUrl.lastIndexOf("/") + 1));
		mMediaFilePath = dirPath + "/" + file2;
		file = new File(mMediaFilePath);
		file1 = new File(dirPath + "/" + file2 + "-");
		error = false;
		if (newPath1 != null)
		{
			file4 = new File(newPath1);
			if ((mMediaFilePath != newPath) && (deltemp == true)
					&& (file4.exists()))
			{
				file4.delete();
			}
		}
	}

	public void startProxy()
	{
		startProxy = true;
		prox = new Thread()
		{
			public void run()
			{
				while (startProxy)
				{
					// --------------------------------------
					// MediaPlayer's request listening, MediaPlayer-> proxy
					// server
					// --------------------------------------
					try
					{
						if (proxy != null)
						{
							proxy.closeSockets();
						}
						Socket s = localServer.accept();
						if (startProxy == false) break;
						proxy = new Proxy(s);
					} catch (IOException e)
					{e.printStackTrace();}
				}
			}
		};
		prox.start();
	}

	public void scan(Uri url)
	{
		Intent scanFileIntent = new Intent(
				Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, url);// scan for other
															// players
		ctx.sendBroadcast(scanFileIntent);
	}

	private class Proxy
	{
		/** Socket receive requests Media Player */
		private Socket sckPlayer = null;
		/** Socket transceiver Media Server requests */
		private Socket sckServer = null;
		private HttpParser httpParser = null;
		private HttpGetProxyUtils utils = null;
		private int bytes_read;
		private byte[] file_buffer = new byte[1448];
		private File file2, file3;
		private byte[] local_request = new byte[1024];
		private byte[] remote_reply = new byte[1448 * 50];
		private ProxyRequest request = null;
		private ProxyResponse proxyResponse = null;
		private boolean sentResponseHeader = false;
		private boolean isExists = false;
		private String header = "";
		private RandomAccessFile os = null, fInputStream = null;
		private long sendByte = 0;
		private FTPFile[] files = null;

		public Proxy(Socket sckPlayer)
		{
			this.sckPlayer = sckPlayer;
			run();
		}

		/**
		 * Shut down the existing links
		 */
		public void closeSockets()
		{
			try
			{// Before starting a new request to close the past Sockets
				if (sckPlayer != null)
				{
					sckPlayer.close();
					sckPlayer = null;
				}
				if ((ftp != null) && (close == true))
				{
					try
					{
						mFTPClient.abort();
					} catch (IOException e)
					{
						e.printStackTrace();
						mFTPClient.disconnect();
					}
					if (mFTPClient.getReplyCode() == 426
							|| mFTPClient.getReplyCode() == 226)
					{
						mFTPClient.completePendingCommand();
					} else
					{
						ftp.close();
					}
					ftp = null;
				}
			} catch (IOException e1)
			{e1.printStackTrace();}
		}

		private boolean connect()
		{
			boolean rezult = false;
			try
			{
				if ((mFTPClient != null) && (mFTPClient.isConnected() == true))
				{
					mFTPClient.disconnect();
				}
				mFTPClient = new FTPClient();
				// mFTPClient.addProtocolCommandListener(new PrintCommandListener(
				// new PrintWriter(System.out)));
				mFTPClient.setControlEncoding("UTF-8");
				mFTPClient.setAutodetectUTF8(true);
				mFTPClient.setBufferSize(1048576);
				mFTPClient.connect(remoteHost, 21);
				if (FTPReply.isPositiveCompletion(mFTPClient.getReplyCode()))
				{
					mFTPClient.setFileType(FTP.BINARY_FILE_TYPE); // for support
					mFTPClient.enterLocalPassiveMode();
					rezult = mFTPClient.login(ftplogin, ftppass);
					mFTPClient.setFileType(FTP.BINARY_FILE_TYPE); // for user
				}
				mFTPClient.setSoTimeout(1500);
			} catch (Exception e)
			{e.printStackTrace();}
			return rezult;
		}

		private void sendToFile()
		{
			if (file1.exists())
			{// Send pre-loaded file to MediaPlayer
				try
				{
					fInputStream = new RandomAccessFile(file1, "r");
					if ((request._rangePosition > 0)
							|| (request._overRange == true))
					{
						header = "HTTP/1.1 206 Partial Content\r\nAccept-Ranges: bytes\r\nContent-Length: "
								+ Long.toString(file1.length()
										- request._rangePosition)
								+ "\r\nContent-Range: bytes "
								+ Long.toString(request._rangePosition)
								+ "-"
								+ Long.toString(file1.length() - 1)
								+ "/"
								+ file1.length()
								+ "\r\nContent-Type: application/octet-stream\r\n\r\n";
						fInputStream.seek(request._rangePosition);
					} else
					{
						header = "HTTP/1.1 200 OK\r\nAccept-Ranges: bytes\r\nContent-Length: "
								+ Long.toString(file1.length())
								+ "\r\nContent-Disposition: attachment\r\nContent-Type: application/octet-stream\r\n\r\n";
					}
					sckPlayer.setSoTimeout(1500); // need experiment
					sckPlayer.getOutputStream().write(header.getBytes(), 0,
							header.length());
					while (((bytes_read = fInputStream.read(file_buffer)) != -1)
							&& (sckPlayer.isClosed() == false)
							&& (mMediaFilePath == newPath))
					{
						sckPlayer.getOutputStream().write(file_buffer, 0,
								bytes_read);
					}
					sckPlayer.getOutputStream().flush();
				} catch (Exception ex)
				{ex.printStackTrace();}
				if (fInputStream != null)
				{
					try
					{
						fInputStream.close();
					} catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}
			return;
		}

		public void run()
		{
			if (mMediaFilePath != newPath)
			{
				error = false;
				urlsize = 0;
				ranges.clear();
				ranges.trimToSize();
				newPath = mMediaFilePath;
				newPath1 = file1.getAbsolutePath();
			}
			// Read player request
			try
			{
				httpParser = new HttpParser(remoteHost, remotePort,
						LOCAL_IP_ADDRESS, localPort);
				while (((bytes_read = sckPlayer.getInputStream().read(
						local_request)) != -1)
						&& (mMediaFilePath == newPath))
				{
					byte[] buffer = httpParser.getRequestBody(local_request,
							bytes_read);
					if (buffer != null)
					{
						request = httpParser.getProxyRequest(buffer, urlsize);
						break;
					}
				}
				isExists = file.exists();
				// Read from file///////////////////////////////
				if (((isExists) || ((file1.exists()) && (error == true)))
						&& (mMediaFilePath == newPath))
				{
					try
					{
						if ((file1.exists()) && (error == true)
								&& (isExists == false))
						{
							fInputStream = new RandomAccessFile(file1, "r");
							if ((request._rangePosition > 0)
									|| (request._overRange == true))
							{
								header = "HTTP/1.1 206 Partial Content\r\nAccept-Ranges: bytes\r\nContent-Length: "
										+ Long.toString(file1.length()
												- request._rangePosition)
										+ "\r\nContent-Range: bytes "
										+ Long.toString(request._rangePosition)
										+ "-"
										+ Long.toString(file1.length() - 1)
										+ "/"
										+ file1.length()
										+ "\r\nContent-Type: application/octet-stream\r\n\r\n";
								fInputStream.seek(request._rangePosition);
							} else
							{
								header = "HTTP/1.1 200 OK\r\nAccept-Ranges: bytes\r\nContent-Length: "
										+ Long.toString(file1.length())
										+ "\r\nContent-Disposition: attachment\r\nContent-Type: application/octet-stream\r\n\r\n";
							}
						} else
						{
							fInputStream = new RandomAccessFile(file, "r");
							if ((request._rangePosition > 0)
									|| (request._overRange == true))
							{
								header = "HTTP/1.1 206 Partial Content\r\nAccept-Ranges: bytes\r\nContent-Length: "
										+ Long.toString(file.length()
												- request._rangePosition)
										+ "\r\nContent-Range: bytes "
										+ Long.toString(request._rangePosition)
										+ "-"
										+ Long.toString(file.length() - 1)
										+ "/"
										+ file.length()
										+ "\r\nContent-Type: application/octet-stream\r\n\r\n";
								fInputStream.seek(request._rangePosition);
							} else
							{
								header = "HTTP/1.1 200 OK\r\nAccept-Ranges: bytes\r\nContent-Length: "
										+ Long.toString(file.length())
										+ "\r\nContent-Disposition: attachment\r\nContent-Type: application/octet-stream\r\n\r\n";
							}
						}
						error = false;
						sckPlayer.getOutputStream().write(header.getBytes(), 0,
								header.length());
						while (((bytes_read = fInputStream.read(file_buffer)) != -1)
								&& (sckPlayer.isClosed() == false)
								&& (mMediaFilePath == newPath))
						{
							sckPlayer.getOutputStream().write(file_buffer, 0,
									bytes_read);
						}
						sckPlayer.getOutputStream().flush();
					} catch (Exception ex)
					{ex.printStackTrace();} finally
					{
						if (fInputStream != null) fInputStream.close();
					}
					return;
				}
			} catch (Exception e1)
			{
				e1.printStackTrace();
			}
			// Read from FTP internet///////////////////////
			if (ftpenable == true)
			{
				if ((mFTPClient == null) || (mFTPClient.isConnected() != true))
				{
					if (connect() == false)
					{
						error = true;
						sendToFile();
					}
				}
				try
				{
					os = new RandomAccessFile(file1, "rwd");
				} catch (FileNotFoundException e2)
				{e2.printStackTrace();}
				try
				{
					if (urlsize == 0)
					{
						files = mFTPClient.listFiles(remotePath);
						if (files.length == 1 && files[0].isFile())
						{
							urlsize = files[0].getSize();
						}
						if (urlsize == 0)
						{
							mFTPClient.sendCommand("SIZE " + remotePath);
							if (mFTPClient.getReplyString().startsWith("213 ") == true)
							{
								urlsize = Long.parseLong(mFTPClient
										.getReplyString()
										.replaceFirst("213 ", "")
										.replaceAll("\r\n", ""));
							}
						}
					}
					if (request._rangePosition > 0)
					{
						mFTPClient.setRestartOffset(request._rangePosition);
						os.seek(request._rangePosition);
					}
					ftp = mFTPClient.retrieveFileStream(remotePath);
				} catch (Exception e)
				{
					e.printStackTrace();
					ftp = null;
				}
				try
				{
					if ((ftp == null) || (urlsize == 0))
					{
						if (connect() == false)
						{
							error = true;
							sendToFile();
						} else
						{
							if (urlsize == 0)
							{
								files = mFTPClient.listFiles(remotePath);
								if (files.length == 1 && files[0].isFile())
								{
									urlsize = files[0].getSize();
								}
								if (urlsize == 0)
								{
									mFTPClient
											.sendCommand("SIZE " + remotePath);
									if (mFTPClient.getReplyString().startsWith(
											"213 ") == true)
									{
										urlsize = Long.parseLong(mFTPClient
												.getReplyString()
												.replaceFirst("213 ", "")
												.replaceAll("\r\n", ""));
									}
								}
							}
							if (request._rangePosition > 0)
							{
								mFTPClient
										.setRestartOffset(request._rangePosition);
								os.seek(request._rangePosition);
							}
							ftp = mFTPClient.retrieveFileStream(remotePath);
							if ((ftp == null) || (urlsize == 0))
							{
								error = true;
								sendToFile();
							}
						}
					}
				} catch (Exception e1)
				{
					e1.printStackTrace();
					error = true;
					sendToFile();
				}
				error = false;
				try
				{
					if ((request._rangePosition > 0)
							|| (request._overRange == true))
					{
						header = "HTTP/1.1 206 Partial Content\r\nAccept-Ranges: bytes\r\nContent-Length: "
								+ Long.toString(urlsize
										- request._rangePosition)
								+ "\r\nContent-Range: bytes "
								+ Long.toString(request._rangePosition)
								+ "-"
								+ Long.toString(urlsize - 1)
								+ "/"
								+ urlsize
								+ "\r\nContent-Type: application/octet-stream\r\n\r\n";
					} else
					{
						header = "HTTP/1.1 200 OK\r\nAccept-Ranges: bytes\r\nContent-Length: "
								+ Long.toString(urlsize)
								+ "\r\nContent-Disposition: attachment\r\nContent-Type: application/octet-stream\r\n\r\n";
					}
					sckPlayer.getOutputStream().write(header.getBytes(), 0,
							header.length());
					seek = false;
					while ((ftp != null)
							&& ((bytes_read = ftp.read(remote_reply)) != -1)
							&& (seek == false) && (mMediaFilePath == newPath))
					{
						os.write(remote_reply, 0, bytes_read);
						sendByte += bytes_read;
						sckPlayer.getOutputStream().write(remote_reply, 0,
								bytes_read);
					}
					sckPlayer.getOutputStream().flush();
				} catch (IOException e)
				{}
			} else
			{
				// Read from HTTP Internet///////////////////////
				try
				{
					if ((request != null) && (isExists == false)
							&& (mMediaFilePath == newPath))
					{
						try
						{
							serverAddress = new InetSocketAddress(remoteHost,
									HTTP_PORT);
							utils = new HttpGetProxyUtils(sckPlayer,
									serverAddress);
							sckServer = utils.sentToServer(request._body);
							// Send MediaPlayer's request to server
						} catch (Exception e)
						{
							e.printStackTrace();
							error = true;
							sendToFile();
						}
						sckPlayer.setSoTimeout(1500);
						sckServer.setSoTimeout(1500); // without this flac not work.
						error = false;
						os = new RandomAccessFile(file1, "rwd");
					} else
					{
						// MediaPlayer's request is invalid
						closeSockets();
						return;
					}
					// ------------------------------------------------------
					// The feedback network server sent to the MediaPlayer,
					// network server -> Proxy -> MediaPlayer
					// ------------------------------------------------------
					seek = false;
					while ((sckServer != null)
							&& ((bytes_read = sckServer.getInputStream().read(
									remote_reply)) != -1) && (seek == false)
							&& (mMediaFilePath == newPath))
					{
						if (sentResponseHeader)
						{
							try
							{
								// When you drag the progress bar, easy this
								// exception, to disconnect and reconnect
								os.write(remote_reply, 0, bytes_read);
								sendByte += bytes_read;
								utils.sendToMP(remote_reply, bytes_read);
							} catch (Exception e)
							{
								break;// Send abnormal (normal) exit while
							}
							continue;// Exit this while
						}
						proxyResponse = httpParser.getProxyResponse(
								remote_reply, bytes_read);
						if (proxyResponse._duration > 0)
						{
							urlsize = proxyResponse._duration;
						}
						sentResponseHeader = true;
						// send http header to mediaplayer
						utils.sendToMP(proxyResponse._body);
						// Send the binary data
						if (proxyResponse._other != null)
						{
							utils.sendToMP(proxyResponse._other);
							os.seek(proxyResponse._currentPosition);
							os.write(proxyResponse._other, 0,
									proxyResponse._other.length);
							sendByte += proxyResponse._other.length;
						}
					}
				} catch (Exception e)
				{e.printStackTrace();}
			}
			closeSockets();
			try
			{
				if (os != null)
				{
					os.close();
					file2 = new File(newPath);
					file3 = new File(newPath1);
					if ((file2.exists() == false) && (request != null)
							&& (sendByte > 0))
					{
						range r = new range();
						r.setstart(request._rangePosition);
						r.setend(request._rangePosition + sendByte);
						ranges.add(r);
						if (urlsize > 0)
						{
							long h = 0;
							for (int i = 0; i < ranges.size(); i++)
							{
								for (int i1 = 0; i1 < ranges.size(); i1++)
								{
									if (ranges.get(i1).start <= h)
									{
										if (ranges.get(i1).end > h)
											h = ranges.get(i1).end;
									}
								}
							}
							if (((ftpenable == true) && (urlsize == h))
									|| ((ftpenable == false) && (urlsize == h - 1)))
							{
								file3.renameTo(file2);
								scan(Uri.fromFile(file2));
							}
						}
					}
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
