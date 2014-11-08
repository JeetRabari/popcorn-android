package pct.droid.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileObserver;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;

import butterknife.InjectView;
import pct.droid.R;
import pct.droid.streamer.Status;
import pct.droid.utils.FileUtils;
import pct.droid.utils.LogUtils;

public class StreamLoadingActivity extends BaseActivity {

    private FileObserver mFileObserver;
    private Boolean mStreaming = false;

    @InjectView(R.id.progressIndicator)
    ProgressBar progressIndicator;
    @InjectView(R.id.progressText)
    TextView progressText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_streamloading);

        if(!getIntent().hasExtra("stream_url")) {
            finish();
        }

        String streamUrl = getIntent().getStringExtra("stream_url");

        while(!getApp().isServiceBound()) {
            getApp().startService();
        }

        getApp().startStreamer(streamUrl);

        String directory = getApp().getStreamDir();
        mFileObserver = new FileObserver(directory) {
            @Override
            public void onEvent(int event, String path) {
                if(path == null) return;
                if(path.contains("streamer.json")) {
                    switch (event) {
                        case CREATE:
                            LogUtils.d("StreamLoadingActivity", "Streamer file created");
                            break;
                        case MODIFY:
                            LogUtils.d("StreamLoadingActivity", "Streamer file modified");
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setDataAndType(Uri.parse("http://localhost:9999"), "video/*");
                            startActivity(i);
                            break;
                    }
                } else if(path.contains("status.json")) {
                    switch (event) {
                        case CREATE:
                        case MODIFY:
                            LogUtils.d("StreamLoadingActivity", "Status file changed");
                            updateStatus();
                            break;
                    }
                }
            }
        };

        mFileObserver.startWatching();
    }

    private void updateStatus() {
        try {
            final Status status = Status.parseJSON(FileUtils.getContentsAsString(getApp().getStreamDir() + "/status.json"));
            LogUtils.d("StreamLoadingActivity", status.toString());
            final int progress = (int)Math.floor(status.progress);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressIndicator.setProgress(progress);
                    progressText.setText(status.progress + "%");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ask user if he wants to stop streaming or open video player
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFileObserver.stopWatching();
        getApp().stopStreamer();
    }
}
