package com.immortalplayer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

public class notifications extends Service
{
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent != null)
        {
            String action = intent.getAction();
            if (!TextUtils.isEmpty(action))
            {
                if (action.equals("ML_STOP_DOWNLOAD"))
                {
                    if (MainActivity.A != null)
                    {
                        MainActivity.checkNum = intent.getIntExtra("numNotif",
                                1);
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}