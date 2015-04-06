package com.immortalplayer;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.xml.sax.InputSource;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{
	static View frame1;
	static player textureView;
	static TextView textProgress;
	static boolean pause = false;
	static Activity A;
	private static Thread th;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		A=this;
		th = Thread.currentThread();
		if (savedInstanceState == null)
		{
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	protected void onDestroy()
	{
		pause = false;
		super.onDestroy();
	}

	@Override
	public void onPause()
	{
		pause = true;
		super.onPause();
	}

	@Override
	public void onResume()
	{
		if (textureView != null)
		{
			textureView.setDisplay();
		}
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();
		if (id == R.id.action_settings)
		{
			System.gc();
			System.exit(0);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public static class PlaceholderFragment extends Fragment
	{
		static private final int BUFFER_SIZE = 700;// Mb
		static private final int NUM_FILES = 20;// Count files in cache dir
		private HttpGetProxy proxy;
		private String videoUrl = "ftp://master255.org/%d0%9a%d0%bb%d0%b8%d0%bf%d1%8b/D/DJ%20Snake%20&%20Lil%20Jon/DJ%20Snake%20&%20Lil%20Jon%20-%20Turn%20Down%20for%20What.mp4";
		private String ftplogin = "user", ftppass = "123";

		public PlaceholderFragment()
		{}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState)
		{
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			frame1 = rootView.findViewById(R.id.frame1);
			textureView = (player) rootView.findViewById(R.id.textureView1);
			textProgress = (TextView) rootView.findViewById(R.id.textView1);
			//Loading xml index file
			new Transfer().start();
			synchronized (th)
			{
				try
				{
					th.wait();
				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			
			// Initialize and start proxy server in new thread
			proxy = new HttpGetProxy();
			proxy.setPaths("/ProxyBuffer", videoUrl, BUFFER_SIZE, NUM_FILES, 200,
					A.getApplicationContext(), false, ftplogin,
					ftppass, false, true, A.getExternalCacheDir()
							.getPath(), textProgress,
					A.getString(R.string.delay_dc));
			// "5FQTRE5AIPHN7K5Z4D3HTN653FXLCPH3VDVBU5A");
			// start player
			String proxyUrl = proxy.getLocalURL();
			textureView.setVideoPath(proxyUrl);
			textureView.setMediaController(new mediac(A, frame1));
			Toast toast = Toast
					.makeText(
							A,
							"For save file to SDCARD watch video fully (or rewind forward).",
							Toast.LENGTH_LONG);
			toast.show();
			textureView.setSeekListener(new player.SeekListener()
			{
				@Override
				public void onSeek(int msec)
				{
					if (proxy != null)
					{
						proxy.seek = true;
					} // more speed for seeking
				}

				@Override
				public void onSeekComplete(MediaPlayer mp)
				{}
			});
			textureView
					.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
					{
						@Override
						public void onPrepared(final MediaPlayer mp)
						{
							textureView.start();
						}
					});
			return rootView;
		}

		private class Transfer extends Thread
		{

			public Transfer()
			{
				
			}

			@Override
			public void run()
			{
				try
				{
					//Utility for create xml
					// file:https://github.com/master255/SimplyServer
					URL url = new URL("http://master255.org/res/files.xml.gz");
					URLConnection urlConnection = url.openConnection();
					urlConnection.setConnectTimeout(1500);
					InputStream http = new GZIPInputStream(
							urlConnection.getInputStream());
					InputSource is = new InputSource(http);
					InputStream input = new BufferedInputStream(
							is.getByteStream());
					File xmlFile1 = new File(A.getExternalCacheDir(),
							"master255.org.xml");
					final OutputStream output = new FileOutputStream(
							xmlFile1);
					if (input != null)
					{
						try
						{
							final byte[] buffer = new byte[1024];
							int read;
							while ((read = input.read(buffer)) != -1)
								output.write(buffer, 0, read);
							output.flush();
						} finally
						{
							output.close();
							http.close();
						}
					}
				} catch (Exception e)
				{
					e.printStackTrace();
				}
				
				
				synchronized (th)
				{
					th.notify();
				}
			}


		}
		public class mediac extends MediaController
		{
			public mediac(Context context, View anchor)
			{
				super(context);
				super.setAnchorView(anchor);
			}

			@Override
			public void setAnchorView(View view)
			{}
		}
	}
}
