package com.danxx.brisktvlauncher.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.danxx.brisktvlauncher.R;
import com.danxx.brisktvlauncher.adapter.BaseRecyclerViewAdapter;
import com.danxx.brisktvlauncher.adapter.BaseRecyclerViewHolder;
import com.danxx.brisktvlauncher.model.VideoBean;
import com.danxx.brisktvlauncher.module.Settings;
import com.danxx.brisktvlauncher.utils.FileUtils;
import com.danxx.brisktvlauncher.widget.media.CustomMediaController;
import com.danxx.brisktvlauncher.widget.media.IjkVideoView;
import com.danxx.brisktvlauncher.widget.media.MeasureHelper;
import com.open.androidtvwidget.bridge.RecyclerViewBridge;
import com.open.androidtvwidget.recycle.LinearLayoutManagerTV;
import com.open.androidtvwidget.recycle.OnChildSelectedListener;
import com.open.androidtvwidget.recycle.RecyclerViewTV;
import com.open.androidtvwidget.view.MainUpView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;

/**
 * 直播播放器
 */
public class LiveVideoActivity extends AppCompatActivity implements TracksFragment.ITrackHolder ,View.OnFocusChangeListener {
    private static final String TAG = "LiveVideoActivity";
    private static final String TV_CHANNEL_LIST_URL = "http://ssvip.mybacc.com/tv.txt";
    private static final String LOCAL_TV_CHANNEL_FILE = "tv.txt";

    private String mVideoPath;
    private Uri mVideoUri;

    private HashMap<String,List> mChannelMap;

    //    private AndroidMediaController mMediaController;
    private CustomMediaController customMediaController;
    private IjkVideoView mVideoView;
    private RecyclerViewTV videoList;
    private RecyclerViewTV videoList2;
//    private TextView mToastTextView;
//    private TableLayout mHudView;
//    private DrawerLayout mDrawerLayout;
    private ViewGroup mRightDrawer;
    private TextView tips,liveName;
    /**播放指示器**/
    private int playIndex = 0;
    private View oldView;
    MyCategoryAdapter myAdapter;
    MyAdapter myAdapter2;

    MainUpView mainUpView1;
    RecyclerViewBridge mRecyclerViewBridge;

    private Settings mSettings;
    private boolean mBackPressed;
    private List<VideoBean> datas = new ArrayList<>();
//    private String []names = new String[]{
//            "香港电影","综艺频道","高清音乐","动作电影","电影","周星驰","成龙","喜剧","儿歌","LIVE生活"
//    };
//
//    private String []urls = new String[]{
//            "http://live.gslb.letv.com/gslb?stream_id=lb_hkmovie_1300&tag=live&ext=m3u8&sign=live_tv&platid=10&splatid=1009&format=letv&expect=1",
//            "http://live.gslb.letv.com/gslb?stream_id=lb_ent_1300&tag=live&ext=m3u8&sign=live_tv&platid=10&splatid=1009&format=letv&expect=1",
//            "http://live.gslb.letv.com/gslb?stream_id=lb_music_1300&tag=live&ext=m3u8&sign=live_tv&platid=10&splatid=1009&format=letv&expect=1",
//            "http://live.gslb.letv.com/gslb?tag=live&stream_id=lb_dzdy_720p&tag=live&ext=m3u8&sign=live_tv&platid=10&splatid=1009&format=C1S&expect=1",
//            "http://live.gslb.letv.com/gslb?tag=live&stream_id=lb_movie_720p&tag=live&ext=m3u8&sign=live_tv&platid=10&splatid=1009&format=C1S&expect=1",
//            "http://live.gslb.letv.com/gslb?tag=live&stream_id=lb_zxc_720p&tag=live&ext=m3u8&sign=live_tv&platid=10&splatid=1009&format=C1S&expect=1",
//            "http://live.gslb.letv.com/gslb?tag=live&stream_id=lb_cl_720p&tag=live&ext=m3u8&sign=live_tv&platid=10&splatid=1009&format=C1S&expect=1",
//            "http://live.gslb.letv.com/gslb?tag=live&stream_id=lb_comedy_720p&tag=live&ext=m3u8&sign=live_tv&platid=10&splatid=1009&format=C1S&expect=1",
//            "http://live.gslb.letv.com/gslb?tag=live&stream_id=lb_erge_720p&tag=live&ext=m3u8&sign=live_tv&platid=10&splatid=1009&format=C1S&expect=1",
//            "http://live.gslb.letv.com/gslb?tag=live&stream_id=lb_livemusic_720p&tag=live&ext=m3u8&sign=live_tv&platid=10&splatid=1009&format=C1S&expect=1"
//    };
    private long TV_CHANNEL_UPDATE_INTERVAL=30*60*1000;

    public static Intent newIntent(Context context, String videoPath, String videoTitle ,int index) {
        Intent intent = new Intent(context, LiveVideoActivity.class);
        intent.putExtra("videoPath", videoPath);
        intent.putExtra("videoTitle", videoTitle);
        intent.putExtra("index" ,index);
        return intent;
    }

    public static void intentTo(Context context, String videoPath, String videoTitle ,int index) {
        context.startActivity(newIntent(context, videoPath, videoTitle,index));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_video);
        initTiemData();
//        initVideoList();
        mSettings = new Settings(this);

        // handle arguments
        mVideoPath = getIntent().getStringExtra("videoPath");
        playIndex = getIntent().getIntExtra("index",0);
//        mVideoPath = urls[playIndex];

        if(mVideoPath==null){
            mVideoPath=mSettings.getLastVideoPath();
        }

//        Intent intent = getIntent();
//        String intentAction = intent.getAction();
        /**取消对Intent的处理**/
//        if (!TextUtils.isEmpty(intentAction)) {
//            if (intentAction.equals(Intent.ACTION_VIEW)) {
//                mVideoPath = intent.getDataString();
//            } else if (intentAction.equals(Intent.ACTION_SEND)) {
//                mVideoUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
//                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//                    String scheme = mVideoUri.getScheme();
//                    if (TextUtils.isEmpty(scheme)) {
//                        Log.e(TAG, "Null unknown ccheme\n");
//                        finish();
//                        return;
//                    }
//                    if (scheme.equals(ContentResolver.SCHEME_ANDROID_RESOURCE)) {
//                        mVideoPath = mVideoUri.getPath();
//                    } else if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
//                        Log.e(TAG, "Can not resolve content below Android-ICS\n");
//                        finish();
//                        return;
//                    } else {
//                        Log.e(TAG, "Unknown scheme " + scheme + "\n");
//                        finish();
//                        return;
//                    }
//                }
//            }
//        }

//        if (!TextUtils.isEmpty(mVideoPath)) {
//            new RecentMediaStorage(this).saveUrlAsync(mVideoPath);
//        }

//        customMediaController = new CustomMediaController(this, false);
//        customMediaController.setVisibility(View.GONE);
//        customMediaController.setSupportActionBar(actionBar);
//        actionBar.setDisplayHomeAsUpEnabled(true);

//        mToastTextView = (TextView) findViewById(R.id.toast_text_view);
//        mHudView = (TableLayout) findViewById(R.id.hud_view);
//        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
//        mRightDrawer = (ViewGroup) findViewById(R.id.right_drawer);
//        mDrawerLayout.setScrimColor(Color.TRANSPARENT);

        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        tips = (TextView) findViewById(R.id.tips);
        liveName = (TextView) findViewById(R.id.liveName);
        mVideoView = (IjkVideoView) findViewById(R.id.video_view);
//        mVideoView.setMediaController(customMediaController);
//        mVideoView.setHudView(mHudView);
        // prefer mVideoPath
        playVideo(mVideoPath ,playIndex);
    }
    public void initTiemData()
    {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

                List<String> videoList=null;
                File tvChannel = new File(LiveVideoActivity.this.getFilesDir(), LOCAL_TV_CHANNEL_FILE);
                if(tvChannel.exists()){
                    String content=FileUtils.read(LiveVideoActivity.this, LOCAL_TV_CHANNEL_FILE);

                    String[] videolist=content.split("\r\n");
                    videoList= new ArrayList<>();
                    for(String video:videolist){
                        videoList.add(video);
                    }

                }

                if (!tvChannel.exists() || videoList.size()==0 || mSettings.getChannelUpdate().getTime()-(new Date().getTime())>TV_CHANNEL_UPDATE_INTERVAL){ //30min update from server
                    videoList= FileUtils.readFileFromUrl(TV_CHANNEL_LIST_URL);
                    String content= TextUtils.join("\r\n",videoList);
                    if(videoList!=null && videoList.size()>0) {
                        FileUtils.write(LiveVideoActivity.this, LOCAL_TV_CHANNEL_FILE,content);

                        mSettings.setChannelUpdate(new Date());
                    }

                }


                String cateName="";

                mChannelMap=new HashMap<>();
                for (String line:videoList){
                    if (line.contains(",")){
                        String[] content=line.split(",");
                        VideoBean videoBean = new VideoBean();
                        videoBean.setTvName(content[0]);
                        videoBean.setTvUrl(content[1]);
                        datas.add(videoBean);
                    }
                    else{
                        if (datas.size()==0){
                            cateName=line;
                        }
                        else{
                            List list=mChannelMap.get(cateName);
                            if(list!=null){
                                datas.addAll(list);
                            }

                            mChannelMap.put(cateName,datas);
                            datas=new ArrayList<>();
                            cateName=line;
                        }

                    }
                }

                runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {
                                      initVideoList();
                                  }
                              }

                );

            }
        });

    }

    private void initVideoList(){
        videoList = (RecyclerViewTV) findViewById(R.id.videoList);
        videoList2 = (RecyclerViewTV) findViewById(R.id.videoList2);

        mainUpView1 = (MainUpView) findViewById(R.id.mainUpView);
        mainUpView1.setEffectBridge(new RecyclerViewBridge());
        mRecyclerViewBridge = (RecyclerViewBridge) mainUpView1.getEffectBridge();
        mRecyclerViewBridge.setUpRectResource(R.drawable.item_rectangle);
        mRecyclerViewBridge.setTranDurAnimTime(200);
        mRecyclerViewBridge.setShadowResource(R.drawable.item_shadow);

        LinearLayoutManagerTV linearLayoutManager = new LinearLayoutManagerTV(LiveVideoActivity.this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        videoList.setLayoutManager(linearLayoutManager);
        linearLayoutManager.setOnChildSelectedListener(new OnChildSelectedListener() {
            @Override
            public void onChildSelected(RecyclerView parent, View focusview, int position, int dy) {
                focusview.bringToFront();
                if (oldView == null) {
                    Log.d("danxx", "oldView == null");
                }
                mRecyclerViewBridge.setFocusView(focusview, oldView, 1.1f);
                oldView = focusview;
            }
        });
//        findViewById(R.id.videoContent).setOnFocusChangeListener(this);



        ArrayList catelist= new ArrayList();
        catelist.addAll(mChannelMap.keySet());


        myAdapter = new MyCategoryAdapter();
        myAdapter.setData(catelist);
        videoList.setAdapter(myAdapter);
        videoList.setFocusable(false);
        myAdapter.notifyDataSetChanged();
        myAdapter.setOnItemClickListener(new BaseRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, Object data) {

                String catename=data.toString();


                showChannelList(catename);



//                String url = ((VideoBean)data).getTvUrl();
//                playVideo(url,position);
//                if(videoList.getVisibility() == View.VISIBLE) {
//                    videoList.setVisibility(View.INVISIBLE);
////                    tips.setVisibility(View.VISIBLE);
//
//
//                    /**隐藏焦点**/
//                    mRecyclerViewBridge.setVisibleWidget(true);
//                }
            }

            @Override
            public void onItemLongClick(int position, Object data) {

            }
        });


        LinearLayoutManagerTV linearLayoutManager2 = new LinearLayoutManagerTV(LiveVideoActivity.this);
        linearLayoutManager2.setOrientation(LinearLayoutManager.VERTICAL);
        linearLayoutManager2.setOnChildSelectedListener(new OnChildSelectedListener() {
            @Override
            public void onChildSelected(RecyclerView parent, View focusview, int position, int dy) {
                focusview.bringToFront();
                if (oldView == null) {
                    Log.d("danxx", "oldView == null");
                }
                mRecyclerViewBridge.setFocusView(focusview, oldView, 1.1f);
                oldView = focusview;
            }
        });

        videoList2.setLayoutManager(linearLayoutManager2);
        myAdapter2 = new MyAdapter();
//        myAdapter2.setData(catelist);
        videoList2.setAdapter(myAdapter2);
        videoList2.setFocusable(false);
//        myAdapter2.notifyDataSetChanged();
        myAdapter2.setOnItemClickListener(new BaseRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, Object data) {
                String url = ((VideoBean)data).getTvUrl();
                playVideo(url,position);
                if(videoList2.getVisibility() == View.VISIBLE) {
                    videoList2.setVisibility(View.INVISIBLE);
                    videoList.setVisibility(View.INVISIBLE);

//                    tips.setVisibility(View.VISIBLE);


                    /**隐藏焦点**/
                    mRecyclerViewBridge.setVisibleWidget(true);
                }
            }

            @Override
            public void onItemLongClick(int position, Object data) {

            }
        });


    }

    public void showChannelList(String cateName){
        videoList2.setVisibility(View.VISIBLE);

        List channelList=mChannelMap.get(cateName);

        myAdapter2.setData(channelList);
        myAdapter2.notifyDataSetChanged();

//        if(videoList2.getVisibility() == View.VISIBLE) {
//            videoList2.setVisibility(View.INVISIBLE);
////                    tips.setVisibility(View.VISIBLE);
//
//
//            /**隐藏焦点**/
//            mRecyclerViewBridge.setVisibleWidget(true);
//        }
    }

    /**
     * 播放视频
     * @param url 直播地址
     */
    private void playVideo(String url ,int index){

        playIndex = index;

        if (myAdapter!=null){

            VideoBean bean=myAdapter2.getItemData(playIndex);
            if (bean!=null) {
                liveName.setVisibility(View.VISIBLE);
                liveName.setText(bean.getTvName());
            }
            tips.setVisibility(View.VISIBLE);
        }

        Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tips.setVisibility(View.INVISIBLE);
                liveName.setVisibility(View.INVISIBLE);
            }
        },3000);


        if (url != null) {
            mVideoView.pause();
            mVideoView.setVideoPath(url);
        }else if (mVideoUri != null)
            mVideoView.setVideoURI(mVideoUri);
        else {
            Log.e(TAG, "Null Data Source\n");
            finish();
            return;
        }

        mSettings.setLastVideoPath(url);
        mVideoView.start();
    }

    @Override
    public void onBackPressed() {
        mBackPressed = true;
        finish();
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(KeyEvent.KEYCODE_DPAD_CENTER == keyCode || KeyEvent.KEYCODE_ENTER == keyCode){
            if(videoList.getVisibility() != View.VISIBLE){
                videoList.setVisibility(View.VISIBLE);
                tips.setVisibility(View.INVISIBLE);
                mRecyclerViewBridge.setVisibleWidget(false);
                videoList.requestFocus();
            }
        }else if(KeyEvent.KEYCODE_BACK == keyCode){
            if(videoList2.getVisibility() == View.VISIBLE){
                videoList2.setVisibility(View.INVISIBLE);
//                tips.setVisibility(View.VISIBLE);
                videoList.setVisibility(View.VISIBLE);
                mRecyclerViewBridge.setVisibleWidget(true);
                return true;
            }
            else if(videoList.getVisibility() == View.VISIBLE){
                videoList.setVisibility(View.INVISIBLE);
                tips.setVisibility(View.VISIBLE);
                mRecyclerViewBridge.setVisibleWidget(true);
                return true;
            }
        }else if(KeyEvent.KEYCODE_MENU == keyCode){
            if(videoList.getVisibility() != View.VISIBLE){
                videoList.setVisibility(View.VISIBLE);
                tips.setVisibility(View.INVISIBLE);
                videoList.requestFocus();
                mRecyclerViewBridge.setVisibleWidget(false);
            }
        }
        return super.onKeyDown(keyCode, event);
    }



    @Override
    protected void onStop() {
        super.onStop();

        if (mBackPressed || !mVideoView.isBackgroundPlayEnabled()) {
            mVideoView.stopPlayback();
            mVideoView.release(true);
            mVideoView.stopBackgroundPlay();
        } else {
            mVideoView.enterBackground();
        }
        IjkMediaPlayer.native_profileEnd();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_toggle_ratio) {
            int aspectRatio = mVideoView.toggleAspectRatio();
            String aspectRatioText = MeasureHelper.getAspectRatioText(this, aspectRatio);
//            mToastTextView.setText(aspectRatioText);
//            customMediaController.showOnce(mToastTextView);
            return true;
        }
        /**隐藏重播选项**/
//        else if (id == R.id.action_toggle_player) {
//            int player = mVideoView.togglePlayer();
//            String playerText = IjkVideoView.getPlayerText(this, player);
//            mToastTextView.setText(playerText);
//            customMediaController.showOnce(mToastTextView);
//            return true;
//        }
        /**隐藏render选项**/
//        else if (id == R.id.action_toggle_render) {
//            int render = mVideoView.toggleRender();
//            String renderText = IjkVideoView.getRenderText(this, render);
//            mToastTextView.setText(renderText);
//            customMediaController.showOnce(mToastTextView);
//            return true;
//        }
        else if (id == R.id.action_show_info) {
            mVideoView.showMediaInfo();
        }
        /**隐藏tracks选项**/
//        else if (id == R.id.action_show_tracks) {
//            if (mDrawerLayout.isDrawerOpen(mRightDrawer)) {
//                Fragment f = getSupportFragmentManager().findFragmentById(R.id.right_drawer);
//                if (f != null) {
//                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//                    transaction.remove(f);
//                    transaction.commit();
//                }
//                mDrawerLayout.closeDrawer(mRightDrawer);
//            } else {
//                Fragment f = TracksFragment.newInstance();
//                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//                transaction.replace(R.id.right_drawer, f);
//                transaction.commit();
//                mDrawerLayout.openDrawer(mRightDrawer);
//            }
//        }
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public ITrackInfo[] getTrackInfo() {
        if (mVideoView == null)
            return null;

        return mVideoView.getTrackInfo();
    }

    @Override
    public void selectTrack(int stream) {
        mVideoView.selectTrack(stream);
    }

    @Override
    public void deselectTrack(int stream) {
        mVideoView.deselectTrack(stream);
    }

    @Override
    public int getSelectedTrack(int trackType) {
        if (mVideoView == null)
            return -1;

        return mVideoView.getSelectedTrack(trackType);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_up_in, R.anim.activity_up_out);
    }

    /**
     * Called when the focus state of a view has changed.
     *
     * @param v        The view whose state has changed.
     * @param hasFocus The new focus state of v.
     */
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        mRecyclerViewBridge.setFocusView(v, oldView, 1.0f);
        oldView = v;
    }

    class MyAdapter extends BaseRecyclerViewAdapter<VideoBean> {

        @Override
        protected BaseRecyclerViewHolder createItem(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(LiveVideoActivity.this).inflate(R.layout.item_live,null);
            MyViewHolder myViewHolder = new MyViewHolder(view);
            return myViewHolder;
        }

        @Override
        protected void bindData(BaseRecyclerViewHolder holder, int position) {
            ((MyViewHolder)holder).name.setText(getItemData(position).getTvName());
        }
        class MyViewHolder extends BaseRecyclerViewHolder{
            TextView name;
            public MyViewHolder(View itemView) {
                super(itemView);
                name = (TextView) itemView.findViewById(R.id.name);
            }

            @Override
            protected View getView() {
                return null;
            }
        }
    }
    class MyCategoryAdapter extends BaseRecyclerViewAdapter<String> {

        @Override
        protected BaseRecyclerViewHolder createItem(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(LiveVideoActivity.this).inflate(R.layout.item_live,null);
            MyViewHolder myViewHolder = new MyViewHolder(view);
            return myViewHolder;
        }

        @Override
        protected void bindData(BaseRecyclerViewHolder holder, int position) {
            ((MyViewHolder)holder).name.setText(getItemData(position));
        }
        class MyViewHolder extends BaseRecyclerViewHolder{
            TextView name;
            public MyViewHolder(View itemView) {
                super(itemView);
                name = (TextView) itemView.findViewById(R.id.name);
            }

            @Override
            protected View getView() {
                return null;
            }
        }
    }
}
