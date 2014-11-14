package com.immortalplayer;

import java.io.File;
import java.io.IOException;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.Environment;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.MediaController;
import android.widget.Toast;

/**
 * Displays a video file.
 */
public class player extends TextureView implements
		MediaController.MediaPlayerControl
{
	// constants
	private static final int STATE_ERROR = -1;
	private static final int STATE_IDLE = 0;
	private static final int STATE_PREPARING = 1;
	private static final int STATE_PREPARED = 2;
	private static final int STATE_PLAYING = 3;
	private static final int STATE_PAUSED = 4;
	private static final int STATE_PLAYBACK_COMPLETED = 5;
	// variables
	private static SurfaceTexture sf;
	private static Surface sf1;
	private static int mVideoWidth;
	private static int mVideoHeight;
	private static int mCurrentState = STATE_IDLE;
	private static MediaPlayer mMediaPlayer;
	private static String path;
	private static MediaController mMediaController;
	private OnCompletionListener mOnCompletionListener;
	private OnPreparedListener mOnPreparedListener;
	private OnErrorListener mOnErrorListener;
	private OnInfoListener mOnInfoListener;
	private OnBufferingUpdateListener mOnBufferingUpdateListener;
	private static int mCurrentBufferPercentage;
	private static int mSeekWhenPrepared;
	private PlayPauseListener mListener;
	private SeekListener mListener1;
	private static boolean mCanPause;
	private static boolean mCanSeekBack;
	private static boolean mCanSeekForward;
    private int skip, skip1=2000;

	public player(Context context)
	{
		super(context);
	}

	public player(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
		initVideoView(context);
	}

	public player(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	OnVideoSizeChangedListener mSizeChangedListener = new OnVideoSizeChangedListener()
	{
		public void onVideoSizeChanged(MediaPlayer mp, int width, int height)
		{
			mVideoWidth = mp.getVideoWidth();
			mVideoHeight = mp.getVideoHeight();
			requestLayout();
		}
	};
	OnPreparedListener mPreparedListener = new OnPreparedListener()
	{
		public void onPrepared(MediaPlayer mp)
		{
			mCurrentState = STATE_PREPARED;
			mVideoWidth = mp.getVideoWidth();
			mVideoHeight = mp.getVideoHeight();
			mCanPause = mCanSeekBack = mCanSeekForward = true;
			int seekToPosition = mSeekWhenPrepared;
			if (seekToPosition != 0)
			{
				seekTo(seekToPosition);
			} else
			{
				seekTo(0);
			};
			if (mMediaController != null)
			{
				mMediaController.setEnabled(true);
			}
			if (mOnPreparedListener != null)
				mOnPreparedListener.onPrepared(mp);
		}
	};
	TextureView.SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener()
	{
		@Override
		public void onSurfaceTextureSizeChanged(final SurfaceTexture surface,
				final int width, final int height)
		{}

		@Override
		public void onSurfaceTextureAvailable(final SurfaceTexture surface,
				final int width, final int height)
		{
			sf = surface;
			if (mMediaPlayer != null)
			{
				sf1 = new Surface(surface);
				mMediaPlayer.setSurface(sf1);
			}
		}

		@Override
		public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface)
		{
			return false;
		}

		@Override
		public void onSurfaceTextureUpdated(final SurfaceTexture surface)
		{}
	};
	private OnCompletionListener mCompletionListener = new OnCompletionListener()
	{
		public void onCompletion(MediaPlayer mp)
		{
			if (mCurrentState == STATE_PLAYBACK_COMPLETED) { return; }
			mCurrentState = STATE_PLAYBACK_COMPLETED;
			if (mMediaController != null) mMediaController.hide();
			if (mOnCompletionListener != null)
				mOnCompletionListener.onCompletion(mMediaPlayer);
		}
	};
	private OnErrorListener mErrorListener = new OnErrorListener()
	{
		public boolean onError(MediaPlayer mp, int framework_err, int impl_err)
		{
			mCurrentState = STATE_ERROR;
			if (mOnErrorListener != null)
			{
				if (mOnErrorListener.onError(mMediaPlayer, framework_err,
						impl_err)) return true;
			}
			return true;
		}
	};
	private OnBufferingUpdateListener mBufferingUpdateListener = new OnBufferingUpdateListener()
	{
		public void onBufferingUpdate(MediaPlayer mp, int percent)
		{
			//Only for Demo
            if (mCurrentBufferPercentage==100) {
            	release();
            	Toast toast = Toast.makeText(getContext(), "File save to SDCARD. Start play video from file.", Toast.LENGTH_LONG); 
				toast.show();
				
		        String path1=Environment.getExternalStorageDirectory()
		    			.getAbsolutePath() + "/ProxyBuffer/DJ Snake & Lil Jon - Turn Down for What.mp4";
		        if (new File (path1).exists()==true) {
		        	path=path1;
		        }
		        openVideo();
				 
            }//
            if (mCurrentState == STATE_ERROR) {
            	mSeekWhenPrepared=mp.getCurrentPosition()+1000;
            	if (skip==mSeekWhenPrepared) {mSeekWhenPrepared=mSeekWhenPrepared+skip1;
            	skip1=skip1+1000;
            	} else {skip1=2000;}
            	skip=mSeekWhenPrepared;
            	release();
            	openVideo();
            }
			mCurrentBufferPercentage = percent;
			if (mOnBufferingUpdateListener != null)
				mOnBufferingUpdateListener.onBufferingUpdate(mp, percent);
		}
	};
	private OnInfoListener mInfoListener = new OnInfoListener()
	{
		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra)
		{
			if (mOnInfoListener != null)
			{
				mOnInfoListener.onInfo(mp, what, extra);
			}
			return true;
		}
	};

	public void setDisplay()
	{
		if ((mMediaPlayer != null) && (sf != null)
				&& (getSurfaceTexture() == null))
		{
			setSurfaceTexture(sf);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		// Log.i("@@@@", "onMeasure(" + MeasureSpec.toString(widthMeasureSpec) +
		// ", "
		// + MeasureSpec.toString(heightMeasureSpec) + ")");
		int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
		int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
		if (mVideoWidth > 0 && mVideoHeight > 0)
		{
			int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
			int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
			int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
			int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
			if (widthSpecMode == MeasureSpec.EXACTLY
					&& heightSpecMode == MeasureSpec.EXACTLY)
			{
				// the size is fixed
				width = widthSpecSize;
				height = heightSpecSize;
				// for compatibility, we adjust size based on aspect ratio
				if (mVideoWidth * height < width * mVideoHeight)
				{
					// Log.i("@@@", "image too wide, correcting");
					width = height * mVideoWidth / mVideoHeight;
				} else if (mVideoWidth * height > width * mVideoHeight)
				{
					// Log.i("@@@", "image too tall, correcting");
					height = width * mVideoHeight / mVideoWidth;
				}
			} else if (widthSpecMode == MeasureSpec.EXACTLY)
			{
				// only the width is fixed, adjust the height to match aspect
				// ratio if possible
				width = widthSpecSize;
				height = width * mVideoHeight / mVideoWidth;
				if (heightSpecMode == MeasureSpec.AT_MOST
						&& height > heightSpecSize)
				{
					// couldn't match aspect ratio within the constraints
					height = heightSpecSize;
				}
			} else if (heightSpecMode == MeasureSpec.EXACTLY)
			{
				// only the height is fixed, adjust the width to match aspect
				// ratio if possible
				height = heightSpecSize;
				width = height * mVideoWidth / mVideoHeight;
				if (widthSpecMode == MeasureSpec.AT_MOST
						&& width > widthSpecSize)
				{
					// couldn't match aspect ratio within the constraints
					width = widthSpecSize;
				}
			} else
			{
				// neither the width nor the height are fixed, try to use actual
				// video size
				width = mVideoWidth;
				height = mVideoHeight;
				if (heightSpecMode == MeasureSpec.AT_MOST
						&& height > heightSpecSize)
				{
					// too tall, decrease both width and height
					height = heightSpecSize;
					width = height * mVideoWidth / mVideoHeight;
				}
				if (widthSpecMode == MeasureSpec.AT_MOST
						&& width > widthSpecSize)
				{
					// too wide, decrease both width and height
					width = widthSpecSize;
					height = width * mVideoHeight / mVideoWidth;
				}
			}
		} else
		{
			// no size yet, just adopt the given spec sizes
		}
		setMeasuredDimension(width, height);
	}

	@Override
	public void onInitializeAccessibilityEvent(AccessibilityEvent event)
	{
		super.onInitializeAccessibilityEvent(event);
		event.setClassName(player.class.getName());
	}

	@Override
	public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info)
	{
		super.onInitializeAccessibilityNodeInfo(info);
		info.setClassName(player.class.getName());
	}

	public int resolveAdjustedSize(int desiredSize, int measureSpec)
	{
		return getDefaultSize(desiredSize, measureSpec);
	}

	private void initVideoView(Context ctx)
	{
		setSurfaceTextureListener(mSurfaceTextureListener);
		if ((mMediaPlayer != null) && (sf != null)
				&& (getSurfaceTexture() == null))
		{
			setSurfaceTexture(sf);
		}
		setFocusable(true);
		setFocusableInTouchMode(true);
		requestFocus();
	}

	public void setVideoPath(String path1)
	{
		path = path1;
		mSeekWhenPrepared = 0;
		openVideo();
		requestLayout();
		invalidate();
	}

	private void setListeners()
	{
		mMediaPlayer.setOnPreparedListener(mPreparedListener);
		mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
		mMediaPlayer.setOnCompletionListener(mCompletionListener);
		mMediaPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
		mMediaPlayer.setOnErrorListener(mErrorListener);
		mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
		mMediaPlayer.setOnInfoListener(mInfoListener);
	}

	private void openVideo()
	{
		if (path == null) return;
		if (mMediaPlayer == null)
		{
			try
			{
				mCurrentBufferPercentage = 0;
				mMediaPlayer = new MediaPlayer();
				setListeners();
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				if (sf != null)
				{
					if (sf1 == null)
					{
						sf1 = new Surface(sf);
					}
					mMediaPlayer.setSurface(sf1);
				}
				mMediaPlayer.setWakeMode(getContext(),
						PowerManager.PARTIAL_WAKE_LOCK);
				mMediaPlayer.setDataSource(path);
				mMediaPlayer.prepareAsync();
				mCurrentState = STATE_PREPARING;
			} catch (IOException ex)
			{
				mCurrentState = STATE_ERROR;
				mErrorListener.onError(mMediaPlayer,
						MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
				return;
			}
		} else
		{
			setListeners();
		}
	}

	public void setMediaController(MediaController controller)
	{
		if (mMediaController != null) mMediaController.hide();
		mMediaController = controller;
		attachMediaController();
	}

	private void attachMediaController()
	{
		if (mMediaPlayer != null && mMediaController != null)
		{
			mMediaController.setMediaPlayer(this);
			mMediaController.setEnabled(isInPlaybackState());
		}
	}

	public void setOnPreparedListener(OnPreparedListener l)
	{
		mOnPreparedListener = l;
	}

	public void setOnCompletionListener(OnCompletionListener l)
	{
		mOnCompletionListener = l;
	}

	public void setOnErrorListener(OnErrorListener l)
	{
		mOnErrorListener = l;
	}

	public void setOnBufferingUpdateListener(OnBufferingUpdateListener l)
	{
		mOnBufferingUpdateListener = l;
	}

	public void setOnInfoListener(OnInfoListener l)
	{
		mOnInfoListener = l;
	}

	public void release()
	{
		if (mMediaPlayer != null)
		{
			mMediaPlayer.release();
			mMediaPlayer = null;
			mCurrentState = STATE_IDLE;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		if (mMediaController != null) toggleMediaControlsVisiblity();
		return false;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent ev)
	{
		if (mMediaController != null) toggleMediaControlsVisiblity();
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK
				&& keyCode != KeyEvent.KEYCODE_VOLUME_UP
				&& keyCode != KeyEvent.KEYCODE_VOLUME_DOWN
				&& keyCode != KeyEvent.KEYCODE_MENU
				&& keyCode != KeyEvent.KEYCODE_CALL
				&& keyCode != KeyEvent.KEYCODE_ENDCALL;
		if (isInPlaybackState() && isKeyCodeSupported
				&& mMediaController != null)
		{
			if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
					|| keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
					|| keyCode == KeyEvent.KEYCODE_SPACE)
			{
				if (mMediaPlayer.isPlaying())
				{
					pause();
					mMediaController.show();
				} else
				{
					start();
					mMediaController.hide();
				}
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY)
			{
				if (!mMediaPlayer.isPlaying())
				{
					start();
					mMediaController.hide();
				}
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
					|| keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE)
			{
				if (mMediaPlayer.isPlaying())
				{
					pause();
					mMediaController.show();
				}
				return true;
			} else
			{
				toggleMediaControlsVisiblity();
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void toggleMediaControlsVisiblity()
	{
		if (mMediaController.isShowing())
		{
			mMediaController.hide();
		} else
		{
			mMediaController.show();
		}
	}

	public void setPlayPauseListener(PlayPauseListener listener)
	{
		mListener = listener;
	}

	interface PlayPauseListener
	{
		void onPlay();

		void onPause();
	}

	public void start()
	{
		if (isInPlaybackState())
		{
			mMediaPlayer.start();
			mCurrentState = STATE_PLAYING;
		}
		if (mListener != null)
		{
			mListener.onPlay();
		}
	}

	public void pause()
	{
		if (isInPlaybackState())
		{
			if (mMediaPlayer.isPlaying())
			{
				mMediaPlayer.pause();
				mCurrentState = STATE_PAUSED;
			}
		}
		if (mListener != null)
		{
			mListener.onPause();
		}
	}

	public int getDuration()
	{
		if (isInPlaybackState()) { return mMediaPlayer.getDuration(); }
		return -1;
	}

	public int getCurrentPosition()
	{
		if (isInPlaybackState()) return mMediaPlayer.getCurrentPosition();
		return 0;
	}

	interface SeekListener
	{
		void onSeek(int msec);

		void onSeekComplete(MediaPlayer mp);
	}

	public void setSeekListener(SeekListener listener)
	{
		mListener1 = listener;
	}

	private OnSeekCompleteListener mOnSeekCompleteListener = new OnSeekCompleteListener()
	{
		@Override
		public void onSeekComplete(MediaPlayer mp)
		{
			mListener1.onSeekComplete(mp);
		}
	};

	public void seekTo(int msec)
	{
		if (isInPlaybackState())
		{
			if (mListener1 != null)
			{
				mListener1.onSeek(msec);
			}
			mMediaPlayer.seekTo(msec);
			mSeekWhenPrepared = 0;
		} else
		{
			mSeekWhenPrepared = msec;
		}
	}

	public boolean isPlaying()
	{
		return isInPlaybackState() && mMediaPlayer.isPlaying();
	}

	public int getBufferPercentage()
	{
		if (mMediaPlayer != null) return mCurrentBufferPercentage;
		return 0;
	}

	protected boolean isInPlaybackState()
	{
		return (mMediaPlayer != null && mCurrentState != STATE_ERROR
				&& mCurrentState != STATE_IDLE && mCurrentState != STATE_PREPARING);
	}

	@Override
	public boolean canPause()
	{
		return mCanPause;
	}

	@Override
	public boolean canSeekBackward()
	{
		return mCanSeekBack;
	}

	@Override
	public boolean canSeekForward()
	{
		return mCanSeekForward;
	}

	@Override
	public int getAudioSessionId()
	{
		if (mMediaPlayer != null) return mMediaPlayer.getAudioSessionId();
		return -1;
	}
}
