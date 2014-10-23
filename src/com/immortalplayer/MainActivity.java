package com.immortalplayer;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.Toast;

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
		if (textureView!=null) {textureView.setDisplay();}
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
			System.gc();
			System.exit(0);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	public static class PlaceholderFragment extends Fragment  {
		static private final int BUFFER_SIZE= 700;//Mb
		static private final int NUM_FILES= 20;//Count files in cache dir
		private HttpGetProxy proxy;
		private String videoUrl ="ftp://master255.org/%d0%9a%d0%bb%d0%b8%d0%bf%d1%8b/D/DJ%20Snake%20&%20Lil%20Jon/DJ%20Snake%20&%20Lil%20Jon%20-%20Turn%20Down%20for%20What.mp4";
		private String ftplogin = "user", ftppass = "123";
		
		public PlaceholderFragment() {}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			frame1=rootView.findViewById(R.id.frame1);
			textureView = (player)rootView.findViewById(R.id.textureView1);
			// Initialize and start proxy server in new thread
			proxy = new HttpGetProxy();
			proxy.setPaths("/ProxyBuffer", videoUrl, BUFFER_SIZE, NUM_FILES, getActivity().getApplicationContext(), false, ftplogin, ftppass, false);
			//start player 
			String proxyUrl = proxy.getLocalURL();
			textureView.setVideoPath(proxyUrl);
			textureView.setMediaController(new mediac(getActivity(), frame1));
            Toast toast = Toast.makeText(getActivity(), "For save file to SDCARD watch video fully (or rewind forward).", Toast.LENGTH_LONG); 
			toast.show();
	        textureView.setSeekListener(new player.SeekListener() {
				@Override
				public void onSeek(int msec) {
					if (proxy!=null) {proxy.seek=true;} //more speed for seeking
				}

				@Override
				public void onSeekComplete(MediaPlayer mp) {

				}
	        	
	        });
			textureView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
	            @Override
	            public void onPrepared(final MediaPlayer mp) {
	                textureView.start();
	            }
	        });
			return rootView;
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
