package com.videoplayer;
import java.io.File;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;

public class MainActivity extends Activity {
	static View frame1;
	static player textureView;
	static boolean pause=false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}
	@Override
    protected void onDestroy() {
		pause=false;
		super.onDestroy();
	}
	@Override
	public void onPause() {
		pause=true;
		super.onPause();
	} 
	@Override
	public void onResume() {
		if ((player.sf!=null)&&(pause==true)&&(textureView.getSurfaceTexture()==null)) {
		    textureView.setSurfaceTexture(player.sf);}
		super.onResume();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	public static class PlaceholderFragment extends Fragment  {
		static private final long BUFFER_SIZE= 200*1024*1024;
		private HttpGetProxy proxy;
		private String videoUrl ="http://master255.org/res/%d0%9a%d0%bb%d0%b8%d0%bf%d1%8b/S/SKRILLEX/Skrillex%20-%20Summit%20(feat.%20Ellie%20Goulding)%20%5bVideo%20by%20Pilerats%5d.mp4";
		private String file1="";

		public PlaceholderFragment() {}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			frame1=rootView.findViewById(R.id.frame1);
			textureView = (player)rootView.findViewById(R.id.textureView1);
			//Create a pre-loaded video file storage folder
			new File( getBufferDir()).mkdirs();
			file1=Uri.decode(videoUrl.substring(videoUrl.lastIndexOf("/")));
			// Initialize and start proxy server in new thread
			proxy = new HttpGetProxy(getBufferDir(), BUFFER_SIZE, 1, file1,videoUrl);
			//start player
			String proxyUrl = proxy.getLocalURL();
			textureView.setVideoPath(proxyUrl);
			
			textureView.setMediaController(new mediac(getActivity(), frame1));
	        if ((player.sf != null)&&(pause==false)&&(textureView.getSurfaceTexture()==null)) {
	        textureView.setSurfaceTexture(player.sf);}
			textureView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
	            @Override
	            public void onPrepared(final MediaPlayer mp) {
	                textureView.start();
	            }
	        });
			return rootView;
		}
		
		static public String getBufferDir(){
			String bufferDir = Environment.getExternalStorageDirectory()
			.getAbsolutePath() + "/ProxyBuffer";
			return bufferDir;
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
