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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.danxx.brisktvlauncher.R;
import com.danxx.brisktvlauncher.adapter.BaseRecyclerViewAdapter;
import com.danxx.brisktvlauncher.adapter.BaseRecyclerViewHolder;
import com.danxx.brisktvlauncher.model.VideoBean;
import com.danxx.brisktvlauncher.module.Settings;
import com.danxx.brisktvlauncher.utils.FileUtils;
import com.danxx.brisktvlauncher.utils.SimpleM3UParser;
import com.danxx.brisktvlauncher.widget.media.CustomMediaController;
import com.danxx.brisktvlauncher.widget.media.IjkVideoView;
import com.danxx.brisktvlauncher.widget.media.MeasureHelper;
import com.open.androidtvwidget.bridge.RecyclerViewBridge;
import com.open.androidtvwidget.recycle.LinearLayoutManagerTV;
import com.open.androidtvwidget.recycle.OnChildSelectedListener;
import com.open.androidtvwidget.recycle.RecyclerViewTV;
import com.open.androidtvwidget.view.MainUpView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.misc.ITrackInfo;

/**
 * 直播播放器
 */
public class LiveVideoActivity extends AppCompatActivity implements TracksFragment.ITrackHolder ,View.OnFocusChangeListener {
    private static final String TAG = "LiveVideoActivity";
    private static final String TV_CHANNEL_LIST_URL = "https://www.mybacc.com/tv.m3u";
    private static final String LOCAL_TV_CHANNEL_FILE = "tv.m3u";

    private static final String LAST_SELECTED_CATEGORY = "LAST_SELECTED_CATEGORY";
    private static final String LAST_SELECTED_CHANNEL_NAME = "LAST_SELECTED_CHANNEL_NAME";


    private String mVideoPath;
    private Uri mVideoUri;

    private HashMap<String,List> mChannelMap;

    private CustomMediaController customMediaController;
    private IjkVideoView mVideoView;
    private RecyclerViewTV videoList;
    private RecyclerViewTV videoList2;

    private ViewGroup mRightDrawer;
    private TextView tips,liveName;
    /**播放指示器**/
    private int playIndex = 0;
    private View oldView;
    private View oldView2;
    MyCategoryAdapter myAdapter;
    MyAdapter myAdapter2;

    MainUpView mainUpView1;
    MainUpView mMainUpView2;
    RecyclerViewBridge mRecyclerViewBridge;
    RecyclerViewBridge mRecyclerViewBridge2;


    private Settings mSettings;
    private boolean mBackPressed;
    private List<VideoBean> datas = new ArrayList<>();

    private long TV_CHANNEL_UPDATE_INTERVAL=30*60*1000;
    private String selectedCategory;
    private ArrayList categoryList;
    private TextView categoryView;
    private LinearLayout categoryLayout;

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

        if(mVideoPath==null){
            mVideoPath=mSettings.getLastVideoPath();
            if (mVideoPath==null){
                mVideoPath=getDefaultChannel();
            }
            selectedCategory=mSettings.getPrefs(LAST_SELECTED_CATEGORY);
        }

        mVideoPath="http://39.135.36.153:18890/000000001000/1000000001000009115/1.m3u8?channel-id=ystenlive&Contentid=1000000001000009115&livemode=1&stbId=005203FF000360100001001A34C0CD33&userToken=bd8bb70bdb2b54bd84b587dffa024f7621vv&usergroup=g21077200000&version=1.0&owaccmark=1000000001000009115&owchid=ystenlive&owsid=1106497909461209970&AuthInfo=yOLXJswzZFfV3FvB8MhHuElKGJKLbU5H0jB3qAhfSE7AORAoVDZDWbFnJ0sXJEaRJ1HPTMtmQf%2bVwcp8RojByB2Rhtn7waHVWUQ9gcJ0mHLEp3xuYtoWp3K%2bdNVn%2bMR4";

        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        tips = (TextView) findViewById(R.id.tips);
        liveName = (TextView) findViewById(R.id.liveName);
        mVideoView = (IjkVideoView) findViewById(R.id.video_view);

        // prefer mVideoPath
        playVideo(mVideoPath ,playIndex);
    }

    public String getDefaultChannel(){
        if(mChannelMap==null){
            return "";
        }

        Iterator<String> keyIterator=mChannelMap.keySet().iterator();
        if(keyIterator.hasNext()){
            String cateName=keyIterator.next();
            VideoBean videoBean= (VideoBean) mChannelMap.get(cateName).get(0);
            if (videoBean!=null){
                return videoBean.getTvUrl();
            }
        }

        return "";

    }
    public void initTiemData()
    {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

                List<SimpleM3UParser.M3U_Entry> videoList=null;
                File tvChannel = new File(LiveVideoActivity.this.getFilesDir(), LOCAL_TV_CHANNEL_FILE);
//                if(tvChannel.exists()){
//                    String content=FileUtils.read(LiveVideoActivity.this, LOCAL_TV_CHANNEL_FILE);
//
//                    String[] videolist=content.split("\r\n");
//                    videoList= new ArrayList<>();
//                    for(SimpleM3UParser.M3U_Entry video:videolist){
//                        videoList.add(video);
//                    }
//                }

                if (!tvChannel.exists() || mSettings.getChannelUpdate().getTime()-(new Date().getTime())>TV_CHANNEL_UPDATE_INTERVAL){ //30min update from server
                    String m3uContent= FileUtils.readFileFromUrl(TV_CHANNEL_LIST_URL);

                    try {
                        FileUtils.write(tvChannel,m3uContent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mSettings.setChannelUpdate(new Date());

//                    String content= TextUtils.join("\r\n",videoList);
//                    if(videoList!=null && videoList.size()>0) {
//                        FileUtils.write(LiveVideoActivity.this, LOCAL_TV_CHANNEL_FILE,content);
//
//                        mSettings.setChannelUpdate(new Date());
//                    }
                }

                SimpleM3UParser parser=new SimpleM3UParser();
                try {
                    videoList=parser.parse(tvChannel.getAbsolutePath());

                } catch (IOException e) {
                    e.printStackTrace();
                }


//                String cateName="";

                mChannelMap=new HashMap<>();

                for(SimpleM3UParser.M3U_Entry entry:videoList){
                    List<VideoBean> list=mChannelMap.get(entry.getGroupTitle());
                    if(list==null){
                        list=new ArrayList<>();
                        mChannelMap.put(entry.getGroupTitle(),list);
                    }

                    VideoBean videoBean = new VideoBean();
                    videoBean.setTvName(entry.getName());
                    videoBean.setTvUrl(entry.getUrl());

                    list.add(videoBean);

                }


//                for (String line:videoList){
//                    if (line.contains(",")){
//                        String[] content=line.split(",");
//                        VideoBean videoBean = new VideoBean();
//                        videoBean.setTvName(content[0]);
//                        videoBean.setTvUrl(content[1]);
//                        datas.add(videoBean);
//                    }
//                    else{
//                        if (datas.size()==0){
//                            cateName=line;
//                        }
//                        else{
//                            List list=mChannelMap.get(cateName);
//                            if(list!=null){
//                                datas.addAll(list);
//                            }
//
//                            mChannelMap.put(cateName,datas);
//                            datas=new ArrayList<>();
//                            cateName=line;
//                        }
//
//                    }
//                }

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

        categoryLayout = (LinearLayout)findViewById(R.id.layoutcategory);

        categoryView = (TextView)findViewById(R.id.txt_category);
        videoList2 = (RecyclerViewTV) findViewById(R.id.videoList2);
        categoryList = new ArrayList();
        categoryList.addAll(mChannelMap.keySet());

        mMainUpView2=new MainUpView(this);
        mMainUpView2.attach2Window(this);
        mMainUpView2.setEffectBridge(new RecyclerViewBridge());
        mRecyclerViewBridge2 = (RecyclerViewBridge) mMainUpView2.getEffectBridge();
        mRecyclerViewBridge2.setUpRectResource(R.drawable.item_rectangle);
        mRecyclerViewBridge2.setTranDurAnimTime(200);
        mRecyclerViewBridge2.setShadowResource(R.drawable.item_shadow);

        LinearLayoutManagerTV linearLayoutManager2 = new LinearLayoutManagerTV(LiveVideoActivity.this);
        linearLayoutManager2.setOrientation(LinearLayoutManager.VERTICAL);
        linearLayoutManager2.setOnChildSelectedListener(new OnChildSelectedListener() {
            @Override
            public void onChildSelected(RecyclerView parent, View focusview, int position, int dy) {
                focusview.bringToFront();
                if (oldView2 == null) {
                    Log.d("danxx", "oldView == null");
                }
                mRecyclerViewBridge2.setFocusView(focusview, oldView2, 1.1f);
                oldView2 = focusview;
            }
        });

        videoList2.setLayoutManager(linearLayoutManager2);
        myAdapter2 = new MyAdapter();
        videoList2.setAdapter(myAdapter2);
        myAdapter2.setOnItemClickListener(new BaseRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, Object data) {
                String url = ((VideoBean)data).getTvUrl();

                playVideo(url,position);

                showMenu(false);

            }

            @Override
            public void onItemLongClick(int position, Object data) {

            }
        });



        if(mVideoPath.isEmpty()){
            selectedCategory=(String) categoryList.get(0);
            playVideo(getDefaultChannel(),0);
        }
        else{
//            selectedCategory=
        }
    }

    private void showMenu(final boolean visable){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(visable){

                    if (selectedCategory==null ||selectedCategory.isEmpty()){
                        selectedCategory=(String) categoryList.get(0);
                    }
                    showChannelList(selectedCategory);
                    categoryLayout.setVisibility(View.VISIBLE);
//
//                    videoList2.setVisibility(View.VISIBLE);
//                    tips.setVisibility(View.INVISIBLE);
//                    mRecyclerViewBridge2.setWidgetVisible(true);
//                    videoList2.requestFocus();

                    videoList2.scrollToPosition(0);
                    myAdapter2.toggleSelection(0);

                    View view= videoList2.getChildAt(0);
                    if (view !=null){
                        view.requestFocus();

                        mRecyclerViewBridge2.setFocusView(view,1.1f);
                    }
                }
                else{
                    videoList2.setVisibility(View.INVISIBLE);
                    tips.setVisibility(View.VISIBLE);
                    mRecyclerViewBridge2.setWidgetVisible(false);
                    categoryLayout.setVisibility(View.INVISIBLE);
                }
            }
        });

    }


    public void showChannelList(String cateName){

        mRecyclerViewBridge2.setUnFocusView(oldView2);
        mRecyclerViewBridge2.setWidgetVisible(false);

        categoryView.setText(cateName);

        selectedCategory=cateName;

        videoList2.setVisibility(View.VISIBLE);

        List channelList=mChannelMap.get(cateName);

        myAdapter2.setData(channelList);
        myAdapter2.notifyDataSetChanged();

        videoList2.requestFocus();
        mRecyclerViewBridge2.setWidgetVisible(true);

    }

    /**
     * 播放视频
     * @param url 直播地址
     */
    private void playVideo(String url ,int index){

        playIndex = index;

        if (myAdapter2!=null){

            VideoBean bean=myAdapter2.getItemData(playIndex);
            if (bean!=null) {
                liveName.setVisibility(View.VISIBLE);
                liveName.setText(bean.getTvName());
                mSettings.setPrefs(LAST_SELECTED_CHANNEL_NAME,bean.getTvName());
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
        mSettings.setPrefs(LAST_SELECTED_CATEGORY,selectedCategory);

        mVideoView.start();
    }

    @Override
    public void onBackPressed() {
        mBackPressed = true;
        finish();
        super.onBackPressed();
    }

    private void showPreviousCategory(){
        int index = categoryList.indexOf(selectedCategory);
        String newCategory = "";
        if (index > 0) {
            newCategory = (String) categoryList.get(index - 1);
        } else {
            newCategory = (String) categoryList.get(0);
        }

        showChannelList(newCategory);
    }

    private void showNextCategory(){
        int index = categoryList.indexOf(selectedCategory);
        String newCategory = "";
        if (index < categoryList.size()-1) {
            newCategory = (String) categoryList.get(index +1);
        } else {
            newCategory = (String) categoryList.get(0);
        }

        showChannelList(newCategory);
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_DPAD_LEFT){
            if(videoList2.isVisible()) {
                showPreviousCategory();
            }
            else{
                showMenu(true);
            }

            return true;
        }else if(keyCode==KeyEvent.KEYCODE_DPAD_RIGHT){

            if(videoList2.isVisible()) {
                showNextCategory();
            }
            return true;
        }

        else
        if(KeyEvent.KEYCODE_DPAD_CENTER == keyCode || KeyEvent.KEYCODE_ENTER == keyCode){

            if (!videoList2.isVisible()){
                showMenu(true);
            }
            return true;

        }else if(KeyEvent.KEYCODE_BACK == keyCode){

            if (videoList2.isVisible()){
                showMenu(false);
            }
            return true;

        }else if(KeyEvent.KEYCODE_MENU == keyCode){

            showMenu(!videoList2.isVisible());
            return true;

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
//        mRecyclerViewBridge2.setFocusView(v, oldView2, 1.0f);
//        oldView2 = v;

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
