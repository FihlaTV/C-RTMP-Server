/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.android.media;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.VideoView;
import android.content.res.Configuration;
import android.content.res.Resources;
import com.rtmpd.*;

public class VideoViewDemo extends Activity {
    private static final String TAG = "VideoViewDemo";

    private VideoView mVideoView;
    private ImageButton mPlay;
    private ImageButton mStop;
    private ImageButton mQuit;
//    private String mCurrentPath;
    private Thread _thread;
    private int mContextId;
    private CommandsInterface _ci;

    private RetryHandler mRetryHandler = new RetryHandler();

    @Override
    public void onCreate(Bundle icicle) {
	super.onCreate(icicle);

	mContextId = -1;

	_ci = new CommandsInterface();
		
	_thread = new Thread() {
		public void run() {
		    _ci.EnvRun("0.0.0.0", 5544);
		}
	    };
	_thread.start();

	setupUI();

	playVideo();

// 	runOnUiThread(new Runnable(){
// 		public void run() {
// 		    //playVideo();
		
// 		}
// 	    });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
	Toast.makeText(getApplicationContext(), "Configuration changed", Toast.LENGTH_SHORT).show();

        super.onConfigurationChanged(newConfig);
	stopPlayback();

	setupUI();

	startPlayback();
    }

    private void setupUI() {
	setContentView(R.layout.main);
	mVideoView = (VideoView) findViewById(R.id.surface_view);

	mPlay = (ImageButton) findViewById(R.id.play);
	mStop = (ImageButton) findViewById(R.id.stop);
	mQuit = (ImageButton) findViewById(R.id.quit);

	mPlay.setOnClickListener(new OnClickListener() {
		public void onClick(View view) {
		    playVideo();
		}
	    });
	mStop.setOnClickListener(new OnClickListener() {
		public void onClick(View view) {
		    stopVideo();
		}
	    });
	mQuit.setOnClickListener(new OnClickListener() {
		public void onClick(View view) {
		    _ci.EnvStop();
		    System.exit(0);
		}
	    });
    }

    private void stopVideo() { 
	stopPlayback();

	if (mContextId == -1)
	    return;
	MessageBase result=new MessageBase(_ci.ContextClose( mContextId ));
	Log.v(TAG, "ContextClose: " + result.toString());
	mContextId = -1;
    }

    private void stopPlayback() {
	if (mVideoView != null) {
	    //mCurrentPath = null;
	    mVideoView.stopPlayback();
	}
    }

    private void cmdPlayVideo(String url) {
	if (mContextId == -1)
	    return;

	Toast.makeText(getApplicationContext(), "Opening: " + url, Toast.LENGTH_LONG).show();

	MessageBase result;
	result=new MessageBase(_ci.CommandPlay( mContextId, 
						url, "", ""));
	Log.v(TAG, "CommandPlay: " + result.toString());
    }

    private void playVideo() {
	if (mContextId == -1)
	{
	    MessageContextCreate msgContextCreate=new MessageContextCreate(_ci.ContextCreate());
	    Log.v(TAG, "ContextCreate: " + msgContextCreate.toString());
	    mContextId = msgContextCreate.getCreatedContextId();

	    if (mContextId == -1)
		return;

	    cmdPlayVideo("http://mediadownloads.mlb.com/mlbam/2010/07/26/10292767_m3u8/master_mobile.m3u8");
//"http://mediadownloads.mlb.com/mlbam/2010/06/29/9505835_m3u8/128/dropf_9505835_100m_128K.m3u8");

	    mRetryHandler.retryInMsec( 1000 );
	}
	else
	    startPlayback();
    }

    class RetryHandler extends Handler {
	@Override
	public void handleMessage(Message msg) {
	    VideoViewDemo.this.playVideo();
	}

	public void retryInMsec(long delayMillis) {
	    this.removeMessages(0);
	    sendMessageDelayed(obtainMessage(0), delayMillis);
	}
    }

    private void startPlayback() {
	try {
	    MessageInfoListStreams msgInfoListStreams = new MessageInfoListStreams( _ci.InfoListStreams(mContextId) );
	    Log.v(TAG, "InfoListStreams: " + msgInfoListStreams.toString());
	    if (msgInfoListStreams.getStreamNamesLength() == 0)
	    {
		Log.v(TAG, "Waiting for video...");
		Toast.makeText(getApplicationContext(), "Waiting for video...", Toast.LENGTH_SHORT).show();

		mRetryHandler.retryInMsec( 500 );
		
		return;
	    }

	    final String path = "rtsp://localhost:5544/" + msgInfoListStreams.getStreamName(0);
	    Log.v(TAG, "path: " + path);
	    if (path == null || path.length() == 0) {
		Toast.makeText(VideoViewDemo.this, "File URL/path is empty",
			       Toast.LENGTH_LONG).show();

	    } else {
		mVideoView.setVideoURI(Uri.parse( path ));
		Toast.makeText(VideoViewDemo.this, "Local URL: " + path,
			       Toast.LENGTH_LONG).show();
		mVideoView.start();
		mVideoView.requestFocus();
	    }
	} catch (Exception e) {
	    Log.e(TAG, "error: " + e.getMessage(), e);
	    if (mVideoView != null) {
		mVideoView.stopPlayback();
	    }
	}
    }

    static {
        System.loadLibrary("crtmpserver_dynamic");
    }
}

