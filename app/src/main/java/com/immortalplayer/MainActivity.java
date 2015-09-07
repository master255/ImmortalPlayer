package com.immortalplayer;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.immortalplayer.proxy.HttpGetProxy;

public class MainActivity extends Activity ///This is DEMO Project
{
    static View frame1;
    static player textureView;
    static TextView textProgress;
    static Activity A;
    private static Thread th;
    static boolean pause = false, gprs = false, skip10, useDC = false, play;
    public static ArrayList<String> loads = new ArrayList<String>();
    public static ArrayList<Long> loadsByte = new ArrayList<Long>();
    static ConnectivityManager cm;
    static NetworkInfo mMobile;
    static Method method = null;
    static String loginftp = "user", pasftp = "123", site = "ftp://master255.org/",
            site2 = "ftp://master255.no-ip.org/", //DoubleDomain technology
            cachefolder1 = "/ProxyBuffer";
    static NotificationManager mNotificationManager;
    static Notification.Builder mBuilder;
    static Integer checkNum = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        A = this;
        th = Thread.currentThread();
        if (savedInstanceState == null)
        {
            try
            {
                method = ConnectivityManager.class.getDeclaredMethod(
                        "setMobileDataEnabled", Boolean.TYPE);
            } catch (NoSuchMethodException e)
            {
                e.printStackTrace();
            }
            cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new Notification.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("Media Library").setOngoing(true);
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
            mNotificationManager.cancelAll();
            System.gc();
            System.exit(1);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class PlaceholderFragment extends Fragment
    {
        static private final int BUFFER_SIZE = 700;// Mb
        static private final int NUM_FILES = 20;// Count files in cache dir
        static private final int MAX_FILE = 200;
        private HttpGetProxy proxy;
        private String videoUrl = "ftp://master255.org/%d0%9a%d0%bb%d0%b8%d0%bf%d1%8b/%d0%9f/%d0%9f%d0%b0%d0%b2%d0%b5%d0%bb%20%d0%92%d0%be%d0%bb%d1%8f/%d0%9f%d0%b0%d0%b2%d0%b5%d0%bb%20%d0%92%d0%be%d0%bb%d1%8f%20-%20%d0%9d%d0%be%d0%b2%d0%be%d0%b5.mp4";
        private mediac mc;

        public PlaceholderFragment()
        {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState)
        {
            View rootView = inflater.inflate(R.layout.fragment_main, container,
                    false);
            frame1 = rootView.findViewById(R.id.frame1);
            textureView = (player) rootView.findViewById(R.id.textureView1);
            textProgress = (TextView) rootView.findViewById(R.id.textView1);
            Button play1 = (Button) rootView.findViewById(R.id.button2);
            Button download1 = (Button) rootView.findViewById(R.id.button);
            skip10 = true; //delete incomplete files
            if (savedInstanceState == null)
            {
                if (useDC)
                {
                    new Transfer("", false, true, "", true, true).start();
                    synchronized (th)//Loading xml index file
                    {
                        try
                        {
                            th.wait();
                        } catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
            mc = new mediac(A, frame1);
            textureView.setMediaController(mc);
            mc.setMediaPlayer(textureView);
            play1.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if ((gprs) && (!play))
                    {
                        play = true;
                        NetworkInfo wifi = cm
                                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                        if (!wifi.isConnected())
                        {
                            do
                            {
                            } while (!gprsoff(true));
                        }
                    }
                    // Initialize and start proxy server in new thread
                    proxy = new HttpGetProxy();
                    try
                    {
                        proxy.setPaths(cachefolder1, URLDecoder.decode(videoUrl, "UTF-8"), BUFFER_SIZE, NUM_FILES, MAX_FILE,
                                A.getApplicationContext(), skip10, loginftp,
                                pasftp, false, useDC, A.getExternalCacheDir()
                                        .getPath(), textProgress,
                                A.getString(R.string.delay_dc));
                    } catch (UnsupportedEncodingException e)
                    {
                        e.printStackTrace();
                    }
                    // start player
                    String proxyUrl = proxy.getLocalURL();
                    textureView.setVideoPath(proxyUrl);

                    Toast toast = Toast
                            .makeText(
                                    A,
                                    "For save file to SDCARD watch video fully (or rewind forward).",
                                    Toast.LENGTH_LONG);
                    toast.show();
                }
            });
            download1.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Uri urr = null;
                    try
                    {
                        urr = Uri.parse(URLDecoder.decode(videoUrl, "UTF-8"));
                    } catch (UnsupportedEncodingException e)
                    {
                        e.printStackTrace();
                    }
                    new Transfer(urr.toString(), true, false,
                            Environment.getExternalStorageDirectory()
                                    + cachefolder1 + "/"
                                    + urr.getLastPathSegment(), urr.getScheme()
                            .equals("ftp"), false).start();
                }
            });
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
                {
                }
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
            textureView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    play = false;
                    if (gprs)
                    {
                        NetworkInfo wifi = cm
                                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                        if (!wifi.isConnected())
                        {
                            gprsoff(false);
                        }
                    }
                }
            });
            return rootView;
        }

        public static boolean gprsoff(boolean enab)
        {
            try
            {
                method.invoke(cm, enab);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            mMobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            int i = 0;
            do
            {
                mMobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                i++;
            } while ((mMobile.isConnected() == (!enab)) || (i < 600));
            return true;
        }

        private class Transfer extends Thread
        {
            private String t1, file1, t2;
            private FTPClient mFTPClient;
            private boolean isfile, loadIndex, protocol, first;
            private Uri ur;
            private int numNot;

            public Transfer(String t, boolean b, boolean c, String path1,
                            boolean protocol1, boolean first1)
            {
                if (path1.length() > 0)
                {
                    t1 = Uri.parse(t).getPath();
                    t2 = t;
                } else
                {
                    t1 = t;
                }
                isfile = b;
                loadIndex = c;
                file1 = path1;
                protocol = protocol1;
                first = first1;
            }

            @Override
            public void run()
            {
                if ((gprs) && (!play))   ///Dynamic Network technology.
                // For work need to gprs turn to true;
                {
                    NetworkInfo wifi = cm
                            .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (!wifi.isConnected())
                    {
                        while (!gprsoff(true))
                        {
                        }
                    }
                }
                ur = Uri.parse(site);
                if (protocol)
                {
                    boolean con = ftpConnect(ur.getHost(), loginftp, pasftp,
                            (ur.getPort() == -1 ? 21 : ur.getPort()));
                    if ((!con) && (first)) //DoubleDomain technology
                    {
                        site = site2;
                        ur = Uri.parse(site);
                        con = ftpConnect(ur.getHost(), loginftp, pasftp,
                                (ur.getPort() == -1 ? 21 : ur.getPort()));
                    }
                    if (con)
                    {
                        try
                        {
                            if (!isfile)
                            {
                                mFTPClient
                                        .changeWorkingDirectory((ur.getPath() + t1)
                                                .replace("//", "/"));
                                if (loadIndex)
                                {//Utility for create xml
//					// file:https://github.com/master255/SimplyServer
                                    InputStream ftp = mFTPClient
                                            .retrieveFileStream("/files.xml.gz");
                                    if (ftp != null)
                                    {
                                        ftp = new GZIPInputStream(ftp);
                                        InputSource is = new InputSource(ftp);
                                        InputStream input = new BufferedInputStream(
                                                is.getByteStream());
                                        File xmlFile1 = new File(
                                                A.getExternalCacheDir(),
                                                ur.getHost() + ".xml");
                                        final OutputStream output = new FileOutputStream(
                                                xmlFile1);
                                        if (input != null)
                                        {
                                            try
                                            {
                                                final byte[] buffer = new byte[1024];
                                                int read;
                                                while ((read = input
                                                        .read(buffer)) != -1)
                                                    output.write(buffer, 0,
                                                            read);
                                                output.flush();
                                            } finally
                                            {
                                                output.close();
                                                ftp.close();
                                            }
                                        }
                                    }
                                }
                            } else
                            {
                                if (!t1.startsWith("/"))
                                {
                                    t1 = "/" + t1;
                                }
                                if (file1.length() > 0)
                                {
                                    File newFile = new File(file1);
                                    if (!newFile.exists())
                                    {
                                        if (!loads.contains(file1
                                                + HttpGetProxy.pref))
                                        {
                                            loads.add(file1 + HttpGetProxy.pref);
                                            int numNot = loads.indexOf(file1
                                                    + HttpGetProxy.pref);
                                            loadsByte.add(numNot, -1L);
                                            File downloadFile1 = new File(file1
                                                    + HttpGetProxy.pref);
                                            try
                                            {
                                                byte[] remote_reply = new byte[1448 * 4];
                                                FileOutputStream outputStream1 = new FileOutputStream(
                                                        downloadFile1);
                                                int bytesRead = -1;
                                                RemoteViews contentView1 = new RemoteViews(
                                                        A.getPackageName(),
                                                        R.layout.progress);
                                                contentView1
                                                        .setTextViewText(
                                                                R.id.textView1,
                                                                file1.substring(file1
                                                                        .lastIndexOf("/") + 1));
                                                contentView1.setProgressBar(
                                                        R.id.progressBar1, 100,
                                                        0, false);
                                                Intent notify = new Intent(A,
                                                        notifications.class);
                                                notify.setAction("ML_STOP_DOWNLOAD");
                                                notify.putExtra("numNotif",
                                                        numNot);
                                                PendingIntent butt = PendingIntent
                                                        .getService(
                                                                A,
                                                                numNot,
                                                                notify,
                                                                PendingIntent.FLAG_UPDATE_CURRENT);
                                                contentView1
                                                        .setOnClickPendingIntent(
                                                                R.id.imageButton0,
                                                                butt);
                                                mBuilder.setSmallIcon(R.drawable.ic_download);
                                                Notification not = mBuilder
                                                        .build();
                                                mBuilder.setSmallIcon(R.drawable.ic_launcher);
                                                not.contentView = contentView1;
                                                mNotificationManager.notify(
                                                        numNot + 2, not);
                                                FTPFile[] files = mFTPClient
                                                        .listFiles(t1);
                                                long size = 0;
                                                if (files.length == 1
                                                        && files[0].isFile())
                                                {
                                                    size = files[0].getSize();
                                                }
                                                if (size == 0)
                                                {
                                                    mFTPClient
                                                            .sendCommand("SIZE "
                                                                    + t1);
                                                    if (mFTPClient
                                                            .getReplyString()
                                                            .startsWith("213 "))
                                                    {
                                                        size = Long
                                                                .parseLong(mFTPClient
                                                                        .getReplyString()
                                                                        .replaceFirst(
                                                                                "213 ",
                                                                                "")
                                                                        .replaceAll(
                                                                                "\r\n",
                                                                                ""));
                                                    }
                                                }
                                                InputStream ftp = mFTPClient
                                                        .retrieveFileStream(t1);
                                                long bytesAllRead = size, interval = size;
                                                while ((bytesRead = ftp
                                                        .read(remote_reply)) != -1)
                                                {
                                                    outputStream1.write(
                                                            remote_reply, 0,
                                                            bytesRead);
                                                    if ((interval - 1024000) > size)
                                                    {
                                                        interval = size;
                                                        loadsByte
                                                                .set(numNot,
                                                                        (bytesAllRead - (size - bytesRead)));
                                                        contentView1
                                                                .setProgressBar(
                                                                        R.id.progressBar1,
                                                                        100,
                                                                        (int) (100 * ((float) (bytesAllRead - (size - bytesRead)) / bytesAllRead)),
                                                                        false);
                                                        not.contentView = contentView1;
                                                        mNotificationManager
                                                                .notify(numNot + 2,
                                                                        not);
                                                    }
                                                    if (checkNum == numNot)
                                                    {
                                                        checkNum = -1;
                                                        if ((skip10 == true)
                                                                && ((proxy.file1 == null) || ((proxy.file1
                                                                .getPath()
                                                                .equals(file1
                                                                        + HttpGetProxy.pref)) != true)))
                                                        {
                                                            downloadFile1
                                                                    .delete();
                                                        }
                                                        break;
                                                    }
                                                    size = size - bytesRead;
                                                }
                                                ftp.close();
                                                outputStream1.close();
                                                if ((size == 0)
                                                        && (bytesAllRead > 0))
                                                {
                                                    downloadFile1
                                                            .renameTo(newFile);
                                                    scan(Uri.fromFile(newFile));
                                                } else if ((skip10)
                                                        && ((proxy.file1 == null) || (!(proxy.file1
                                                        .getPath()
                                                        .equals(file1
                                                                + HttpGetProxy.pref)))))
                                                {
                                                    downloadFile1.delete();
                                                }
                                                clearArrays();
                                                mNotificationManager
                                                        .cancel(numNot + 2);
                                            } catch (Exception e)
                                            {
                                                clearArrays();
                                                mNotificationManager
                                                        .cancel(numNot + 2);
                                                if ((skip10)
                                                        && ((proxy.file1 == null) || (!(proxy.file1
                                                        .getPath()
                                                        .equals(file1
                                                                + HttpGetProxy.pref)))))
                                                {
                                                    downloadFile1.delete();
                                                }
                                            }
                                        }
                                    } else
                                    {
                                        A.runOnUiThread(new Runnable()
                                        {
                                            public void run()
                                            {
                                                Toast toast = Toast
                                                        .makeText(
                                                                A,
                                                                A.getResources()
                                                                        .getString(
                                                                                R.string.file_exist),
                                                                Toast.LENGTH_SHORT);
                                                toast.show();
                                            }
                                        });
                                    }
                                } else
                                {
//									InputStream ftp = mFTPClient   ///this is my code for
//                                                        //loading playlists. From another program
//											.retrieveFileStream(t1);
//									BufferedReader reader = new BufferedReader(
//											new InputStreamReader(
//													ftp,
//													t1.toLowerCase()
//															.lastIndexOf("m3u8") != -1 ? "UTF-8"
//															: "windows-1251"));
//									StringBuilder sb = new StringBuilder();
//									String line = null;
//									while ((line = reader.readLine()) != null)
//									{
//										sb.append(line
//												+ System.getProperty("line.separator"));
//									}
//									//strrez = sb.toString();
                                }
                            }
                            mFTPClient.disconnect();
                        } catch (Exception e)
                        {
                        }
                    }
                } else
                {
                    if (file1.length() > 0)
                    {
                        getrec("", Uri.encode(t2, "://"));
                    } else
                    {
                        getrec(site, t1);
                    }
                }
                synchronized (th)
                {
                    th.notify();
                }
                if ((gprs) && (!play))
                {
                    NetworkInfo wifi = cm
                            .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (!wifi.isConnected())
                    {
                        gprsoff(false);
                    }
                }
            }

            private String getrec(String urlsite, String t2)
            {
                String result = "";
                HttpGet get = new HttpGet(urlsite + t2);
                HttpParams httpParameters = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParameters, 1500);
                DefaultHttpClient client = new DefaultHttpClient(httpParameters);
                HttpResponse response = null;
                try
                {
                    response = client.execute(get);
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
                if (file1.length() > 0)
                {
                    File newFile = new File(file1);
                    if (!newFile.exists())
                    {
                        if (!loads.contains(file1 + HttpGetProxy.pref))
                        {
                            loads.add(file1 + HttpGetProxy.pref);
                            numNot = loads.indexOf(file1 + HttpGetProxy.pref);
                            loadsByte.add(numNot, -1L);
                            File downloadFile1 = new File(file1
                                    + HttpGetProxy.pref);
                            try
                            {
                                byte[] remote_reply = new byte[1448 * 4];
                                FileOutputStream outputStream1 = new FileOutputStream(
                                        downloadFile1);
                                int bytesRead = -1;
                                InputStream is = response.getEntity()
                                        .getContent();
                                RemoteViews contentView1 = new RemoteViews(
                                        A.getPackageName(), R.layout.progress);
                                contentView1
                                        .setTextViewText(R.id.textView1, file1
                                                .substring(file1
                                                        .lastIndexOf("/") + 1));
                                contentView1.setProgressBar(R.id.progressBar1,
                                        100, 0, false);
                                Intent notify = new Intent(A,
                                        notifications.class);
                                notify.setAction("ML_STOP_DOWNLOAD");
                                notify.putExtra("numNotif", numNot);
                                PendingIntent butt = PendingIntent.getService(
                                        A, numNot, notify,
                                        PendingIntent.FLAG_UPDATE_CURRENT);
                                contentView1.setOnClickPendingIntent(
                                        R.id.imageButton0, butt);
                                mBuilder.setSmallIcon(R.drawable.ic_download);
                                Notification not = mBuilder.build();
                                mBuilder.setSmallIcon(R.drawable.ic_launcher);
                                not.contentView = contentView1;
                                mNotificationManager.notify(numNot + 2, not);
                                long size = response.getEntity()
                                        .getContentLength();
                                long bytesAllRead = size, interval = size;
                                while ((bytesRead = is.read(remote_reply)) != -1)
                                {
                                    outputStream1.write(remote_reply, 0,
                                            bytesRead);
                                    if ((interval - 1024000) > size)
                                    {
                                        loadsByte
                                                .set(numNot,
                                                        (bytesAllRead - (size - bytesRead)));
                                        interval = size;
                                        contentView1
                                                .setProgressBar(
                                                        R.id.progressBar1,
                                                        100,
                                                        (int) (100 * ((float) (bytesAllRead - (size - bytesRead)) / bytesAllRead)),
                                                        false);
                                        not.contentView = contentView1;
                                        mNotificationManager.notify(numNot + 2,
                                                not);
                                    }
                                    if (checkNum == numNot)
                                    {
                                        checkNum = -1;
                                        outputStream1.close();
                                        clearArrays();
                                        mNotificationManager.cancel(numNot + 2);
                                        if ((skip10)
                                                && ((proxy.file1 == null) || (!(proxy.file1
                                                .getPath().equals(file1
                                                        + HttpGetProxy.pref)))))
                                        {
                                            downloadFile1.delete();
                                        }
                                        return result;
                                    }
                                    size = size - bytesRead;
                                }
                                is.close();
                                outputStream1.close();
                                if ((size == 0) && (bytesAllRead > 0))
                                {
                                    downloadFile1.renameTo(newFile);
                                    scan(Uri.fromFile(newFile));
                                } else if ((skip10)
                                        && ((proxy.file1 == null) || (!(proxy.file1
                                        .getPath().equals(file1
                                                + HttpGetProxy.pref)))))
                                {
                                    downloadFile1.delete();
                                }
                                clearArrays();
                                mNotificationManager.cancel(numNot + 2);
                            } catch (Exception e)
                            {
                                clearArrays();
                                mNotificationManager.cancel(numNot + 2);
                                if ((skip10)
                                        && ((proxy.file1 == null) || (!(proxy.file1
                                        .getPath().equals(file1
                                                + HttpGetProxy.pref)))))
                                {
                                    downloadFile1.delete();
                                }
                            }
                        }
                    } else
                    {
                        A.runOnUiThread(new Runnable()
                        {
                            public void run()
                            {
                                Toast toast = Toast.makeText(
                                        A,
                                        A.getResources().getString(
                                                R.string.file_exist),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        });
                    }
                } else
                {
                    try
                    {
                        if ((first) && ((response == null) || (response.getStatusLine().getStatusCode() != 200)))
                        {
                            site = site2;
                            HttpGet get1 = new HttpGet(site + t2);
                            response = null;
                            try
                            {
                                response = client.execute(get1);
                            } catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.getEntity()
                                        .getContent(), t2.toLowerCase()
                                        .lastIndexOf("m3u8") != -1 ? "UTF-8"
                                        : "windows-1251"));
                        StringBuilder sb = new StringBuilder();
                        String line = null;
                        while ((line = reader.readLine()) != null)
                        {
                            sb.append(line
                                    + System.getProperty("line.separator"));
                        }
                        result = sb.toString();
                    } catch (Exception e)
                    {
                        result = e.toString();
                    }
                }
                if (loadIndex)
                {
                    try
                    {
                        URL url = new URL(urlsite + "/files.xml.gz");
                        URLConnection urlConnection = url.openConnection();
                        urlConnection.setConnectTimeout(1500);
                        InputStream http = new GZIPInputStream(
                                urlConnection.getInputStream());
                        InputSource is = new InputSource(http);
                        InputStream input = new BufferedInputStream(
                                is.getByteStream());
                        File xmlFile1 = new File(A.getExternalCacheDir(),
                                ur.getHost() + ".xml");
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
                }
                return result;
            }

            private void clearArrays()
            {
                loads.set(numNot, null);//complex logic
                loadsByte.set(numNot, null);
                for (int i = loads.size() - 1; i > -1; i--)
                {
                    if (loads.get(i) == null)
                    {
                        loads.remove(i);
                    } else
                    {
                        break;
                    }
                }
                loads.trimToSize();
                for (int i = loadsByte.size() - 1; i > -1; i--)
                {
                    if (loadsByte.get(i) == null)
                    {
                        loadsByte.remove(i);
                    } else
                    {
                        break;
                    }
                }
                loadsByte.trimToSize();
            }

            private void scan(Uri url)
            {
                Intent scanFileIntent = new Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, url);// scan for other
                // players
                A.sendBroadcast(scanFileIntent);
            }

            private boolean ftpConnect(String host, String username,
                                       String password, int port)
            {
                boolean rezult = false;
                try
                {
                    mFTPClient = new FTPClient();
                    mFTPClient.setControlEncoding("UTF-8");
                    mFTPClient.setAutodetectUTF8(true);
                    mFTPClient.setConnectTimeout(1500);
                    mFTPClient.connect(host, port);
                    mFTPClient.setSoTimeout(1500);
                    if (FTPReply
                            .isPositiveCompletion(mFTPClient.getReplyCode()))
                    {
                        mFTPClient.setFileType(FTP.BINARY_FILE_TYPE);
                        mFTPClient.enterLocalPassiveMode();
                        rezult = mFTPClient.login(username, password);
                        mFTPClient.setFileType(FTP.BINARY_FILE_TYPE);
                    }
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
                return rezult;
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
            {
            }
        }
    }
}
