package bz.code.medialistener;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.MediaSessionManager.OnActiveSessionsChangedListener;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import static android.media.MediaMetadata.METADATA_KEY_ALBUM;
import static android.media.MediaMetadata.METADATA_KEY_ALBUM_ART;
import static android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI;
import static android.media.MediaMetadata.METADATA_KEY_ART;
import static android.media.MediaMetadata.METADATA_KEY_ARTIST;
import static android.media.MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE;
import static android.media.MediaMetadata.METADATA_KEY_DISPLAY_TITLE;
import static android.media.MediaMetadata.METADATA_KEY_TITLE;


public class MediaDisplay extends Activity {

    private static final String TAG = "MediaDisplay";

    private static final boolean DEBUG = true;

    private MediaSessionManager mMediaSessionManager;

    private MediaController mMediaController;

    private TextView mPackageName;
    private TextView mTitle;
    private TextView mArtist;
    private TextView mAlbum;
    private TextView mDisplayTitle;
    private TextView mDisplaySubTitle;

    private ImageView mAlbumArt;
    private ImageView mAlbumArtUri;
    private ImageView mArt;

    /**
     * Listen to sessions changed event. Use the first item in the list of
     * controllers to populate the views. This is not always the Session in focus
     * but, it is in most of the cases..
     * <p/>
     * Register callbacks for the MediaController we are interested in.
     * Update views when playback state or metadata changes.
     */
    private OnActiveSessionsChangedListener mOnActiveSessionsChangedListener
            = new OnActiveSessionsChangedListener() {

        @Override
        public void onActiveSessionsChanged(List<MediaController> controllers) {
            if (controllers != null && controllers.size() > 0) {
                mMediaController = controllers.get(0);

                mMediaController.registerCallback(new MediaController.Callback() {
                    @Override
                    public void onPlaybackStateChanged(PlaybackState state) {
                        updateViews();
                    }

                    @Override
                    public void onMetadataChanged(MediaMetadata metadata) {
                        updateViews();
                    }

                });

                updateViews();

                if (DEBUG) {
                    printControllers(controllers);
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (isNotificationServiceEnabled()) {
            findViewById(R.id.settingsButton).setVisibility(View.GONE);
        } else {
            return;
        }

        ComponentName componentName = new ComponentName(this, NotificationsListener.class);

        mMediaSessionManager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
        mMediaSessionManager.addOnActiveSessionsChangedListener(mOnActiveSessionsChangedListener,
                componentName);

        mPackageName = (TextView) findViewById(R.id.packageName);
        mTitle = (TextView) findViewById(R.id.title);
        mArtist = (TextView) findViewById(R.id.artist);
        mAlbum = (TextView) findViewById(R.id.album);
        mDisplayTitle = (TextView) findViewById(R.id.displayTitle);
        mDisplaySubTitle = (TextView) findViewById(R.id.displaySubTitle);

        mAlbumArt = (ImageView) findViewById(R.id.albumArt);
        mAlbumArtUri = (ImageView) findViewById(R.id.albumArtUri);
        mArt = (ImageView) findViewById(R.id.art);

        /*
         * Look up for any active media sessions
         */
        List<MediaController> activeControllers = mMediaSessionManager
                .getActiveSessions(componentName);

        if (activeControllers != null && activeControllers.size() > 0) {
            mMediaController = activeControllers.get(0);
            updateViews();
        }
    }


    private void updateViews() {
        if (mMediaController != null && mMediaController.getMetadata() != null) {

            MediaMetadata metadata = mMediaController.getMetadata();

            mPackageName.setText(mMediaController.getPackageName());
            mTitle.setText("Title: " + metadata.getString(METADATA_KEY_TITLE));
            mAlbum.setText("Album: " + metadata.getString(METADATA_KEY_ALBUM));
            mArtist.setText("Artist: " + metadata.getString(METADATA_KEY_ARTIST));

            mDisplayTitle.setText("Display Title: "
                    + metadata.getString(METADATA_KEY_DISPLAY_TITLE));

            mDisplaySubTitle.setText("Display sub-title: "
                    + metadata.getString(METADATA_KEY_DISPLAY_SUBTITLE));

            mAlbumArt.setImageBitmap(metadata.getBitmap(METADATA_KEY_ALBUM_ART));

            mAlbumArtUri.setImageBitmap(metadata.getBitmap(METADATA_KEY_ALBUM_ART_URI));

            mArt.setImageBitmap(metadata.getBitmap(METADATA_KEY_ART));

            updateControllers();

        }
    }


    private void updateControllers() {

        PlaybackState playbackState = mMediaController.getPlaybackState();
        long validActions = playbackState == null ? 0 : playbackState.getActions();

        if (validActions != 0) {
            findViewById(R.id.controllers).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.controllers).setVisibility(View.GONE);
            return;
        }

        // We can look for play state and hide play or pause. But, it's okay
        View pause = findViewById(R.id.pause);
        if (isPauseSupported(validActions)) {
            pause.setVisibility(View.VISIBLE);
        } else {
            pause.setVisibility(View.GONE);
        }

        View play = findViewById(R.id.play);
        if (isPlaySupported(validActions)) {
            play.setVisibility(View.VISIBLE);
        } else {
            play.setVisibility(View.GONE);
        }

        View skipNext = findViewById(R.id.skipNext);
        if (isSkipToNextSupported(validActions)) {
            skipNext.setVisibility(View.VISIBLE);
        } else {
            skipNext.setVisibility(View.GONE);
        }

        View skipPrev = findViewById(R.id.skipPrev);
        if (isSkipToPreviousSupported(validActions)) {
            skipPrev.setVisibility(View.VISIBLE);
        } else {
            skipPrev.setVisibility(View.GONE);
        }

        View stop = findViewById(R.id.stop);
        if (isStopSupported(validActions)) {
            stop.setVisibility(View.VISIBLE);
        } else {
            stop.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaSessionManager != null) {
            mMediaSessionManager.removeOnActiveSessionsChangedListener(mOnActiveSessionsChangedListener);
        }
    }

    private boolean isStopSupported(long validActions) {
        return (validActions & PlaybackState.ACTION_STOP) != 0;
    }

    private boolean isPlaySupported(long validActions) {
        return (validActions & PlaybackState.ACTION_PLAY) != 0;
    }

    private boolean isPauseSupported(long validActions) {
        return (validActions & PlaybackState.ACTION_PAUSE) != 0;
    }

    private boolean isSkipToNextSupported(long validActions) {
        return (validActions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0;
    }

    private boolean isSkipToPreviousSupported(long validActions) {
        return (validActions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0;
    }


    public void onControlsClicked(View v) {
        if (mMediaController == null) {
            return;
        }

        MediaController.TransportControls transportControls
                = mMediaController.getTransportControls();

        if (transportControls == null) {
            return;
        }

        switch (v.getId()) {
            case R.id.play:
                transportControls.play();
                break;
            case R.id.pause:
                transportControls.pause();
                break;
            case R.id.skipNext:
                transportControls.skipToNext();
                break;
            case R.id.skipPrev:
                transportControls.skipToPrevious();
                break;
            case R.id.stop:
                transportControls.stop();
                break;
            default:

        }
    }

    /**
     * For Debug
     *
     * @param controllers Active controllers
     */
    private void printControllers(List<MediaController> controllers) {
        for (MediaController mediaController : controllers) {
            Log.e(TAG, " Active Controller " + mediaController.getPackageName());
            Log.e(TAG, " PendingIntent " + mediaController.getSessionActivity());

            if (mediaController.getPlaybackState() != null) {
                Log.e(TAG, " Active Controller " + mediaController.getPlaybackState().getState());
            } else {
                Log.e(TAG, " Active Controller null state ");
            }
        }
    }


    private boolean isNotificationServiceEnabled() {

        ComponentName componentName = new ComponentName(this, NotificationsListener.class);

        String enabledNotificationListeners = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");

        return enabledNotificationListeners != null &&
                enabledNotificationListeners.contains(componentName.flattenToString());
    }

    public void launchSettings(View v) {
        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        finish();
    }
}
