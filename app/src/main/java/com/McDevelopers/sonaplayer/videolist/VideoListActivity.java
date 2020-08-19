package com.McDevelopers.sonaplayer.videolist;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.McDevelopers.sonaplayer.Animations;
import com.McDevelopers.sonaplayer.ApplicationContextProvider;
import com.McDevelopers.sonaplayer.ClickListener;
import com.McDevelopers.sonaplayer.CustomRVItemTouchListener;
import com.McDevelopers.sonaplayer.GlideApp;
import com.McDevelopers.sonaplayer.InfoTabDialog;

import com.McDevelopers.sonaplayer.MediaDataId;
import com.McDevelopers.sonaplayer.MusicLibrary;
import com.McDevelopers.sonaplayer.R;
import com.McDevelopers.sonaplayer.SonaToast;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import xyz.danoz.recyclerviewfastscroller.vertical.VerticalRecyclerViewFastScroller;

import static com.McDevelopers.sonaplayer.ApplicationContextProvider.getContext;
import static com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions.withCrossFade;

public class VideoListActivity extends AppCompatActivity  implements InfoTabDialog.InfoTabResult {

    Video_Recycler_View_Adapter adapter;
    Video_View_Adapter_Large adapterLarge;
    List<VideoData> data= Collections.emptyList();
    RecyclerView recyclerView;
    private  TextView listHeader;
    VerticalRecyclerViewFastScroller fastScroller;
    boolean focus;
   // String scrollId;
   private static boolean checkFlags=false;
    private ImageButton layoutSwitcher;
    private SearchView searchView;
    private static int listStyle=1;
    SharedPreferences currentState;
    SharedPreferences.Editor editor;
    private static int lastPosition=0;
    private FrameLayout parentLayout;
    private RelativeLayout optionDialog;
    private CheckBox markAllChecker;
    TextView markCountText;
    Button closeDialogBtn;
    int lastMarkPos=-1;
    LinearLayout sendBtnLayout;
    LinearLayout deleteBtnLayout;
    LinearLayout playlistBtnLayout;
    LinearLayout queueBtnLayout;
    private InputMethodManager imm;
    private static Uri sdCardUri;
    AlertDialog.Builder builder=null;
    AlertDialog alertDialog=null;
    ProgressDialog progressDialog;
    private boolean supressUpdate=false;
    private PopupWindow mPopupWindow;
    private Handler marqeeHandler =new Handler();
    private static boolean marqueeFlag=true;
    private boolean deleteSingleFlag=false;
    private  String deletePath=null;
    private String deleteMediaId=null;
    private InfoTabDialog infoTabDialog;
    private static boolean TagEditFlag=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);
        getWindow().setBackgroundDrawable(null);
        Intent i = getIntent();
        parentLayout = findViewById(R.id.recycler_view_wrapper);
        fastScroller = findViewById(R.id.fast_scroller);
        listHeader=findViewById(R.id.video_list_header);
        currentState = getSharedPreferences("com.McDevelopers.sonaplayer", Context.MODE_PRIVATE);
        listStyle=currentState.getInt("videoListStyle",1);
        marqueeFlag=currentState.getBoolean("marqueeFlag",true);

        Log.d("VideoListActivity", "onCreate: Invoked");

        String UriRaw=currentState.getString("SDCardUri",null);
        if(!TextUtils.isEmpty(UriRaw)) {
            sdCardUri = Uri.parse(UriRaw);
            Log.d("MediaListActivity", "onCreate: SdcardUri:" + UriRaw);
        }

        data= MusicLibrary.datav;
        checkFlags=false;
        supressUpdate=false;
       // data = (List<VideoData>) i.getSerializableExtra("LIST");
         focus= i.getBooleanExtra("FOCUS",false);
        listHeader.setText("Videos ( "+data.size()+" )");
        // scrollId=i.getStringExtra("scrollId");

        layoutSwitcher=findViewById(R.id.layout_style);
         recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(-1);

        if(listStyle==0){
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapterLarge = new Video_View_Adapter_Large(data, getApplication());
            adapterLarge.setHasStableIds(true);
            recyclerView.setAdapter(adapterLarge);
            layoutSwitcher.setImageResource(R.drawable.list_grid_icon);

        }else if(listStyle==1){
            recyclerView.setLayoutManager(new GridLayoutManager(VideoListActivity.this, 2));
            adapter = new Video_Recycler_View_Adapter(data, getApplication());
            adapter.setHasStableIds(true);
            recyclerView.setAdapter(adapter);
            layoutSwitcher.setImageResource(R.drawable.list_large_icon);
        }


        if(data.size()==0){
            fastScroller.setEnabled(false);
            recyclerView.setVisibility(View.GONE);
            final ViewStub viewStubNoMusic=findViewById(R.id.viewStub_no_music);
            View viewGroup= viewStubNoMusic.inflate();
            TextView noMusicText =viewGroup.findViewById(R.id.no_music_text);
            noMusicText.setText("No Videos Found!");
            viewStubNoMusic.setVisibility(View.VISIBLE);
            layoutSwitcher.setEnabled(false);
        }
        recyclerView.addOnScrollListener(fastScroller.getOnScrollListener());
        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setAddDuration(1000);
        itemAnimator.setRemoveDuration(1000);
        recyclerView.setItemAnimator(itemAnimator);
        fastScroller.setRecyclerView(recyclerView);
      //  fastScroller.setSectionIndicator(sectionTitleIndicator);
        getWindow().setBackgroundDrawable(null);
        setupSearch();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);


        if(marqueeFlag) {
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    marqeeHandler.removeCallbacks(mMarqeeRunnable);
                    marqeeHandler.postDelayed(mMarqeeRunnable, 3000);
                    Log.d("OnScroll", "onScrolled: Invoked");

                }

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);

                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {

                        if (listStyle == 0) {
                            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                            int visibleFirstPos = layoutManager.findFirstVisibleItemPosition();
                            int visibleLastPos = layoutManager.findLastVisibleItemPosition();
                            Log.e("FirstVisiblePos", "onScrollStateChanged: " + visibleFirstPos);
                            Log.e("lastVisiblePos", "onScrollStateChanged: " + visibleLastPos);

                            int loop = visibleFirstPos;

                            try {
                                while (loop <= (visibleLastPos)) {
                                    View video_holder_large = layoutManager.findViewByPosition(loop);
                                    TextView titleView = video_holder_large.findViewById(R.id.title);
                                    titleView.setSelected(false);
                                    loop++;

                                    Log.d("loopValue", "onScrollStateChanged: " + loop);

                                }
                            }catch (Throwable e){

                                e.printStackTrace();
                            }
                        } else if (listStyle == 1) {

                            GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                            int visibleFirstPos = layoutManager.findFirstVisibleItemPosition();
                            int visibleLastPos = layoutManager.findLastVisibleItemPosition();
                            Log.e("FirstVisiblePos", "onScrollStateChanged: " + visibleFirstPos);
                            Log.e("lastVisiblePos", "onScrollStateChanged: " + visibleLastPos);

                            int loop = visibleFirstPos;

                            try {
                                while (loop <= (visibleLastPos)) {
                                    View video_holder_large = layoutManager.findViewByPosition(loop);
                                    TextView titleView = video_holder_large.findViewById(R.id.video_title);
                                    titleView.setSelected(false);
                                    loop++;

                                    Log.d("loopValue", "onScrollStateChanged: " + loop);

                                }
                            }catch (Throwable e){

                                e.printStackTrace();
                            }
                        }
                    } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {

                        Log.e("ScrollStateIdle", "onScrollStateChanged: Invoked");
                    }

                }
            });
        }


        recyclerView.addOnItemTouchListener(new CustomRVItemTouchListener(this, recyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {

                if(marqueeFlag)
                    marqeeHandler.removeCallbacks(mMarqeeRunnable);

                if (view instanceof AppCompatImageButton || listStyle==1 && view instanceof AppCompatTextView) {

                    if(checkFlags)
                        removeMarkDialog();

                    String songTitle="";
                    String songSubTitle="";
                    String songMeta="";
                    String songImageId="";
                    String songData="";
                    String songMediaId="";

                    if (listStyle == 1) {

                        if ((adapter.list.get(position).title).length() > 15) {
                            songTitle=adapter.list.get(position).title;
                        } else {
                            songTitle=adapter.list.get(position).title+" - "+adapter.list.get(position).description;
                        }

                        songSubTitle=adapter.list.get(position).description;
                        songMeta= timeConvert(adapter.list.get(position).duration) + " | " + getSongFormat(adapter.list.get(position).imageId)+" | "+adapter.list.get(position).size;
                        songImageId=adapter.list.get(position).imageId;
                        songData=adapter.list.get(position).imageId;
                        songMediaId=adapter.list.get(position).mediaId;

                    } else if (listStyle == 0) {

                        if ((adapterLarge.list.get(position).title).length() > 15) {
                            songTitle=adapterLarge.list.get(position).title;
                        } else {
                            songTitle=adapterLarge.list.get(position).title+" - "+adapterLarge.list.get(position).description;
                        }

                        songSubTitle=adapterLarge.list.get(position).description;
                        songMeta= timeConvert(adapterLarge.list.get(position).duration) + " | " + getSongFormat(adapterLarge.list.get(position).imageId)+" | "+adapterLarge.list.get(position).size;
                        songImageId=adapterLarge.list.get(position).imageId;
                        songData=adapterLarge.list.get(position).imageId;
                        songMediaId=adapterLarge.list.get(position).mediaId;

                        Log.d("RecyclerItemTouch", "onClick:VideoData "+songData);
                        Log.d("RecyclerItemTouch", "onClick:VideoFilename "+songSubTitle);


                    }

                    Vibrator vb = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                    if(vb!=null)
                        vb.vibrate(30);
                    inflateOptionMenu(getApplicationContext(),songTitle,songSubTitle,songMeta,songData,songImageId,position,songMediaId);
                    // Toast.makeText(getApplicationContext(),adapterLarge.list.get(position).title,Toast.LENGTH_SHORT).show();

                }else {
                
                
                if(!checkFlags) {


                    imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                    Intent intent = new Intent();
                    if (listStyle == 0) {
                        intent.putExtra("mediaId", adapterLarge.list.get(position).mediaId);


                    } else if (listStyle == 1) {

                        intent.putExtra("mediaId", adapter.list.get(position).mediaId);

                    }

                    setResult(RESULT_OK, intent);
                    finish();
                }else {

                    if (listStyle == 0) {
                        adapterLarge.setMarkBox(position);
                        markCountText.setText(adapterLarge.getMarkCount());

                    } else if (listStyle == 1) {

                        adapter.setMarkBox(position);
                        markCountText.setText(adapter.getMarkCount());
                    }

                    lastMarkPos=position;
                    if(markAllChecker.isChecked()) {
                        markAllChecker.setChecked(false);
                    }
                }
                }
            }

            @Override
            public void onLongClick(View view, int position) {

                Vibrator vb = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                vb.vibrate(50);
                if(!checkFlags) {
                    checkFlags=true;

                    if (listStyle == 0) {

                        adapterLarge.setCheckFlag(true);

                    } else if (listStyle == 1) {

                        adapter.setCheckFlag(true);

                    }


                    inflateMarkDialog();
                }

                if(listStyle==0) {

                    if(lastMarkPos!=-1 && lastMarkPos<position){
                        for(int i=lastMarkPos+1;i<=position;i++){
                            adapterLarge.setMarkBox(i);
                        }
                    }else if(lastMarkPos!=-1 && lastMarkPos>position){
                        for(int i=lastMarkPos-1;i>=position;i--){
                            adapterLarge.setMarkBox(i);
                        }

                    } else {
                        adapterLarge.setMarkBox(position);

                    }
                    if(markAllChecker.isChecked()) {
                        markAllChecker.setChecked(false);
                    }
                    lastMarkPos=position;
                    adapterLarge.notifyDataSetChanged();
                    markCountText.setText(adapterLarge.getMarkCount());

                }else if(listStyle==1){
                    if(lastMarkPos!=-1 && lastMarkPos<position){
                        for(int i=lastMarkPos+1;i<=position;i++){
                            adapter.setMarkBox(i);
                        }
                    }else if(lastMarkPos!=-1 && lastMarkPos>position){
                        for(int i=lastMarkPos-1;i>=position;i--){
                            adapter.setMarkBox(i);
                        }

                    }else {
                        adapter.setMarkBox(position);

                    }
                    if(markAllChecker.isChecked()) {
                        markAllChecker.setChecked(false);
                    }
                    lastMarkPos=position;
                    adapter.notifyDataSetChanged();
                    markCountText.setText(adapter.getMarkCount());
                }

            }
        }));



        layoutSwitcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Vibrator vb = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                vb.vibrate(20);

                if(checkFlags)
                    removeMarkDialog();


                lastPosition= ((LinearLayoutManager)recyclerView.getLayoutManager())
                        .findFirstVisibleItemPosition();

                switch (listStyle) {


                    case 0:
                        recyclerView.setLayoutManager(new GridLayoutManager(VideoListActivity.this, 2));
                        adapter = new Video_Recycler_View_Adapter(data, getApplication());
                        recyclerView.setAdapter(adapter);
                        recyclerView.scrollToPosition(lastPosition);
                        currentState = getSharedPreferences("com.McDevelopers.sonaplayer", Context.MODE_PRIVATE);
                        editor = currentState.edit();
                        editor.putInt("videoListStyle", 1);
                        editor.commit();
                        listStyle = 1;
                        layoutSwitcher.setImageResource(R.drawable.list_large_icon);

                        break;

                    case 1:

                        recyclerView.setLayoutManager(new LinearLayoutManager(VideoListActivity.this));
                        adapterLarge = new Video_View_Adapter_Large(data, getApplication());
                        recyclerView.setAdapter(adapterLarge);
                        recyclerView.scrollToPosition(lastPosition);

                        currentState = getSharedPreferences("com.McDevelopers.sonaplayer", Context.MODE_PRIVATE);
                        editor = currentState.edit();
                        editor.putInt("videoListStyle", 0);
                        editor.commit();
                        listStyle = 0;
                        layoutSwitcher.setImageResource(R.drawable.list_grid_icon);
                        break;
                }}


    });
    }

    final Runnable mMarqeeRunnable = new Runnable() {
        @Override
        public void run() {
            marqeeHandler.removeCallbacksAndMessages(null);
            Log.d("MarqueeRunnableInvoked", "run: Invoked");

            try {
                if (listStyle == 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    int visibleFirstPos = layoutManager.findFirstVisibleItemPosition();
                    int visibleLastPos = layoutManager.findLastVisibleItemPosition();
                    Log.e("FirstVisiblePosH", "onScrollStateChanged: " + visibleFirstPos);
                    Log.e("lastVisiblePosH", "onScrollStateChanged: " + visibleLastPos);
                    int loop = visibleFirstPos;

                    try {
                        while (loop <= (visibleLastPos + 1)) {
                            View video_holder_large = layoutManager.findViewByPosition(loop);
                            TextView titleView = video_holder_large.findViewById(R.id.title);
                            titleView.setSelected(true);
                            loop++;
                            Log.d("loopValueH", "onScrollStateChanged: " + loop);


                        }
                    } catch (Throwable e) {

                        e.printStackTrace();
                    }
                } else if (listStyle == 1) {

                    GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                    int visibleFirstPos = layoutManager.findFirstVisibleItemPosition();
                    int visibleLastPos = layoutManager.findLastVisibleItemPosition();
                    Log.e("FirstVisiblePos", "onScrollStateChanged: " + visibleFirstPos);
                    Log.e("lastVisiblePos", "onScrollStateChanged: " + visibleLastPos);

                    int loop = visibleFirstPos;
                    try {
                        while (loop <= visibleLastPos) {
                            View video_holder_large = layoutManager.findViewByPosition(loop);
                            TextView titleView = video_holder_large.findViewById(R.id.video_title);
                            titleView.setSelected(true);
                            loop++;

                            Log.d("loopValue", "onScrollStateChanged: " + loop);

                        }
                    } catch (Throwable e) {

                        e.printStackTrace();
                    }
                }
            }catch (Throwable e){
                e.printStackTrace();
            }

        }};

    @Override
    public void onBackPressed() {

        if(marqueeFlag)
            marqeeHandler.removeCallbacks(mMarqeeRunnable);
        if(checkFlags)
            removeMarkDialog();
        else
            super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                if (resultCode == Activity.RESULT_OK) {
                    sdCardUri = data.getData();
                    if(sdCardUri!=null) {
                        final int takeFlags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION| Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(sdCardUri, takeFlags);

                        Log.d("SDcardUri", "onActivityResult: Uri:  " + sdCardUri.toString());
                        currentState = getSharedPreferences("com.McDevelopers.sonaplayer", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = currentState.edit();
                        editor.putString("SDCardUri", sdCardUri.toString());
                        editor.putBoolean("canReadSD",true);
                        editor.commit();

                        if(TagEditFlag){
                            SonaToast sonaToast=new SonaToast();
                            sonaToast.setToast(getApplicationContext(),"Permission Granted",0);
                            TagEditFlag=false;

                        }else {


                            if (!deleteSingleFlag) {
                                showProgressDialog();
                            }
                            new Handler().postDelayed(new Runnable() {
                                public void run() {
                                    if (deleteSingleFlag) {
                                        deleteItem(deletePath, deleteMediaId);
                                    } else {
                                        proceedToDelete();
                                    }
                                }
                            }, 500);
                        }
                    }
                }
                break;
        }
    }


    private void setupSearch() {
       searchView = findViewById(R.id.song_search);


        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                listHeader.setVisibility(View.GONE);
                //Toast.makeText(getApplicationContext(),"SearchView Clicked",Toast.LENGTH_LONG).show();
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                processQuery(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                processQuery(newText);
                return false;
            }

        });


        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {

                listHeader.setVisibility(View.VISIBLE);


                if(listStyle==0) {
                    adapterLarge.updateList(data);
                    if(checkFlags){
                        markCountText.setText(adapterLarge.getMarkCount());
                    }

                }else if(listStyle==1){

                    adapter.updateList(data);
                    if(checkFlags){
                        markCountText.setText(adapter.getMarkCount());
                    }

                }
                if(marqueeFlag) {
                    marqeeHandler.removeCallbacks(mMarqeeRunnable);
                    marqeeHandler.postDelayed(mMarqeeRunnable, 3000);
                }
                return false;
            }
        });


    }



    private void processQuery(String query) {
        // in real app you'd have it instantiated just once
        List<VideoData> result = new ArrayList<>();
        String searchRaw;
        query=query.toLowerCase();
        // case insensitive search
        for (VideoData song : data) {

            searchRaw=(song.title)+(song.filename);
            searchRaw=searchRaw.toLowerCase();

            if (searchRaw.contains(query)) {
                result.add(song);
            }
        }

        if(listStyle==0) {
            adapterLarge.updateList(result);
            if(checkFlags){
                markCountText.setText(adapterLarge.getMarkCount());
            }

        }else if(listStyle==1){

            adapter.updateList(result);
            if(checkFlags){
                markCountText.setText(adapter.getMarkCount());
            }
        }
        if(result.size()==0){

            fastScroller.setEnabled(false);
        }else {

            fastScroller.setEnabled(true);
        }
        if(marqueeFlag) {
            marqeeHandler.removeCallbacks(mMarqeeRunnable);
            marqeeHandler.postDelayed(mMarqeeRunnable, 3000);
        }
    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public  void onPause(){
        super.onPause();


    }

    @Override
    public  void onDestroy() {
        recyclerView.setAdapter(null);
        recyclerView=null;
        alertDialog=null;
        builder=null;
        Runtime.getRuntime().gc();
        System.gc();
        super.onDestroy();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    @Override
    public void onResume(){

        super.onResume();

    }


    private static String getSongFormat(String data){

        return  ((data).substring((data.lastIndexOf(".") + 1)).toUpperCase());
    }

    private String timeConvert(long durationX) {
        int duration=(int)(durationX/1000);

        if(duration<3600000) {

            return String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(duration),
                    TimeUnit.MILLISECONDS.toSeconds(duration) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
        }else {

            return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(duration),
                    TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration)),
                    TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));

        }

    }



    public void slideDown(View view){
        try {
            TranslateAnimation animate = new TranslateAnimation(
                    0,                 // fromXDelta
                    0,                 // toXDelta
                    0,                 // fromYDelta
                    view.getHeight()); // toYDelta
            animate.setDuration(400);
            animate.setFillAfter(true);
            view.startAnimation(animate);
            view.setVisibility(View.GONE);
        }catch (Throwable e){

            Log.e("slideDownException", "slideDown:  "+e);
        }
    }

    private void inflateMarkDialog(){

        parentLayout = findViewById(R.id.recycler_view_wrapper);
        View v = getLayoutInflater().inflate(R.layout.list_option_dialog, parentLayout, false);
        FrameLayout.LayoutParams lp =
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT);
        //TransitionManager.beginDelayedTransition(parentLayout);
        lp.gravity=Gravity.BOTTOM;
        lp.bottomMargin=8;
        v.setVisibility(View.INVISIBLE);
        parentLayout.addView(v, lp);

        Animation bottomUp = AnimationUtils.loadAnimation(getContext(),
                R.anim.bottom_up);
        ViewGroup hiddenPanel = findViewById(R.id.list_option_parent);
        hiddenPanel.startAnimation(bottomUp);
        hiddenPanel.setVisibility(View.VISIBLE);
        initMarkDialog();
    }

    private void initMarkDialog(){
        markAllChecker=findViewById(R.id.mark_all);
        markCountText=findViewById(R.id.mark_count);
        closeDialogBtn=findViewById(R.id.close_list_option);
        sendBtnLayout=findViewById(R.id.option_send_parent);
        deleteBtnLayout=findViewById(R.id.option_delete_parent);
        playlistBtnLayout=findViewById(R.id.add_playlist_parent);
        queueBtnLayout=findViewById(R.id.add_queue_parent);

        if(listStyle==0) {
            markCountText.setText(adapterLarge.getMarkCount());
        }else if(listStyle==1){
            markCountText.setText(adapter.getMarkCount());
        }


        markAllChecker.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Vibrator vb = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                vb.vibrate(50);
                lastMarkPos=-1;
                // you might keep a reference to the CheckBox to avoid this class cast
                boolean checked = ((CheckBox)v).isChecked();
                if(listStyle==0) {
                    adapterLarge.setMarkAll(checked);
                    markCountText.setText(adapterLarge.getMarkCount());

                }else if(listStyle==1){
                    adapter.setMarkAll(checked);
                    markCountText.setText(adapter.getMarkCount());

                }
            }

        });

        markAllChecker.setLongClickable(true);
        markAllChecker.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                Vibrator vb = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                vb.vibrate(50);
                lastMarkPos=-1;
                // you might keep a reference to the CheckBox to avoid this class cast
                markAllChecker.setChecked(false);
                if(listStyle==0) {
                    adapterLarge.setMarkAll(false);
                    markCountText.setText(adapterLarge.getMarkCount());
                }else if(listStyle==1){
                    adapter.setMarkAll(false);
                    markCountText.setText(adapter.getMarkCount());

                }

                return true;
            }});

        closeDialogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Vibrator vb = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                vb.vibrate(50);
                removeMarkDialog();
            }
        });

        playlistBtnLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Vibrator vb = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                vb.vibrate(50);
                SonaToast sonaToast=new SonaToast();
                sonaToast.setToast(getApplicationContext(),"Currently Not Available",0);
            }
        });

        queueBtnLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToQueue(false);

            }
        });

        queueBtnLayout.setLongClickable(true);
        queueBtnLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                addToQueue(true);
                return true;
            }
        });



        sendBtnLayout.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                if(getMarkedCount()>0)
                    shareMedia();
                else {
                    Vibrator vb = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                    vb.vibrate(20);
                    SonaToast sonaToast=new SonaToast();
                    sonaToast.setToast(getApplicationContext(),"Please Select media First",0);
                }

            }});

        deleteBtnLayout.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(getMarkedCount()==0){
                    Vibrator vb = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                    vb.vibrate(20);
                    SonaToast sonaToast=new SonaToast();
                    sonaToast.setToast(getApplicationContext(),"Please Select media First",0);
                    return;
                }
                Log.e("DeleteButtonClicked", "onClick: Invoked");
                AlertDialog.Builder builder1 = new AlertDialog.Builder(new ContextThemeWrapper(VideoListActivity.this, R.style.myDialog));
                builder1.setTitle("Confirm Delete ?");
                builder1.setMessage("         Selected: "+getMarkedCount());
                builder1.setCancelable(true);
                builder1.setIcon(R.drawable.delete_icons);
                builder1.setPositiveButton(
                        "DELETE",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                showProgressDialog();
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        proceedToDelete();
                                    }}, 500);

                            }
                        });

                builder1.setNegativeButton(
                        "CANCEL",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog  alertDialog1 = builder1.create();
                alertDialog1.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
                alertDialog1.show();
            }});
    }

    private void addToQueue(boolean ActivityFlag){

        Vibrator vb = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        vb.vibrate(50);
        if(getMarkedCount()==0){
            if(ActivityFlag){
                Intent intent =new Intent("QueueToggle");
                intent.putExtra("StartActivity",true);
                intent.putExtra("StartQueue",false);
                intent.putExtra("QueueSwitch",false);
                sendBroadcast(intent);
                finish();
            }else {
                SonaToast.setToast(getApplicationContext(), "Please Select media First", 0);

            }
            return;
        }


        if(listStyle==0) {
            SonaToast.setToast(getApplicationContext(),adapterLarge.getmarkedNumCount()+" Items Added",0);

            MusicLibrary.queueItems.addAll(adapterLarge.getMarkedIds());
            MusicLibrary.buildQueue();
            adapterLarge.setMarkAll(false);
            markCountText.setText(adapterLarge.getMarkCount());


        }else if(listStyle==1){
            SonaToast.setToast(getApplicationContext(),adapter.getmarkedNumCount()+" Items Added",0);

            MusicLibrary.queueItems.addAll(adapter.getMarkedIds());
            MusicLibrary.buildQueue();
            adapter.setMarkAll(false);
            markCountText.setText(adapter.getMarkCount());

        }
        Intent intent =new Intent("QueueToggle");
        intent.putExtra("StartActivity",ActivityFlag);
        intent.putExtra("StartQueue",true);
        intent.putExtra("QueueSwitch",true);
        sendBroadcast(intent);
        if(ActivityFlag)
            finish();
    }



    private void removeMarkDialog(){
        checkFlags=false;
        optionDialog=findViewById(R.id.list_option_parent);
        slideDown(optionDialog);
        parentLayout.removeView(optionDialog);
        markAllChecker=null;
        markCountText=null;
        closeDialogBtn=null;
        lastMarkPos=-1;

        if(listStyle==0) {
            adapterLarge.setCheckFlag(false);
            adapterLarge.notifyDataSetChanged();

        }else if(listStyle==1){
            adapter.setCheckFlag(false);
            adapter.notifyDataSetChanged();

        }
    }



    private void shareMedia(){
        List<MediaDataId> markedMediaList=new ArrayList<>();
        Log.d("Entered In ShareMedia", "shareMedia: Current ListStyle : "+listStyle);

        if(listStyle==0) {
            markedMediaList=adapterLarge.getMarkedFileList();
            Log.d("Entered In adapter", "shareMedia: ListStyle 1 Invoked ");

        }else if(listStyle==1){

            markedMediaList=adapter.getMarkedFileList();
            Log.d("Entered In adapter", "shareMedia: ListStyle 1 Invoked ");

        }
        if(markedMediaList==null || markedMediaList.size()==0) {
            Log.e("Empty Media List", "shareMedia: Returned!!" );
            SonaToast sonaToast=new SonaToast();
            sonaToast.setToast(getApplicationContext(),"Operation Failed! Something Went Wrong",1);
            return;
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("*/*");
        ArrayList<Uri> uriFileList = new ArrayList<Uri>();
        Uri uri=null;
        for(MediaDataId mediaDataId : markedMediaList /* List of the files you want to send */) {
            String path=mediaDataId.data;
            File file = new File(path);
            Log.d("FilePacked", "path:"+path);
            uri = FileProvider.getUriForFile(getApplicationContext(), VideoListActivity.this.getPackageName(), file);
            VideoListActivity.this.grantUriPermission(VideoListActivity.this.getPackageManager().toString(), uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            uriFileList.add(uri);
        }
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriFileList);
        VideoListActivity.this.startActivity(Intent.createChooser(intent, "Share Media via"));


    }


    private void getSdCardUri(){

        if(builder==null) {
            builder = new AlertDialog.Builder(VideoListActivity.this);
            builder.setTitle("Need SDCard Write Permission");
            builder.setMessage("Open File Manager > Choose SDCard > Tap SELECT to Grant Permission");
            builder.setPositiveButton("Open File Manager", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                    startActivityForResult(intent, 1);
                }
            });

            alertDialog = builder.create();
            alertDialog.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
            alertDialog.show();
        }else {

            if(!alertDialog.isShowing()){
                //if its visibility is not showing then show here
                alertDialog.show();
            }
        }
    }

    private void proceedToDelete(){
        if(deleteSingleFlag) {
            deleteSingleFlag = false;
            deleteMediaId = null;
            deletePath = null;
        }

        List<MediaDataId> markedMediaList=new ArrayList<>();
        Log.d("Entered In ShareMedia", "shareMedia: Current ListStyle : "+listStyle);
        if(listStyle==0) {
            markedMediaList=adapterLarge.getMarkedFileList();
            Log.d("Entered In adapter", "shareMedia: ListStyle 1 Invoked ");

        }else if(listStyle==1){

            markedMediaList=adapter.getMarkedFileList();
            Log.d("Entered In adapter", "shareMedia: ListStyle 1 Invoked ");

        }

        if(markedMediaList==null || markedMediaList.size()==0) {
            Log.e("Empty Media List", "shareMedia: Returned!!" );
            SonaToast sonaToast=new SonaToast();
            sonaToast.setToast(getApplicationContext(),"Operation Failed! Something Went Wrong",1);
            return;
        }

        for(MediaDataId mediaDataId : markedMediaList /* List of the files you want to send */) {
            String path =mediaDataId.data;
            String mediaId=mediaDataId.mediaId;
            File file = new File(path);
            boolean isSuccess = file.delete();
            if(isSuccess){
                long longMediaId = Long.valueOf(mediaId);

                Uri mediaContentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        longMediaId);
                getContentResolver().delete(mediaContentUri, null, null);

            } else {
                Log.d("FilePacked", "path:" + path);
                if (!deleteMedia(sdCardUri, file, mediaId)) {
                    supressUpdate = true;
                    break;
                }
            }

            supressUpdate=false;
        }

        if(!supressUpdate)
            updateLibrary();
        else
            supressUpdate=false;

    }

    private boolean deleteMedia(Uri sdCardUri ,File file, String mediaId){
        DocumentFile documentFile=null;
        try {


            if( sdCardUri!=null)
                documentFile = DocumentFile.fromTreeUri(this, sdCardUri);



            String[] parts = (file.getPath()).split("\\/");

            // findFile method will search documentFile for the first file
            // with the expected `DisplayName`

            // We skip first three items because we are already on it.(sdCardUri = /storage/extSdCard)
            for (int i = 3; i < parts.length; i++) {
                if (documentFile != null) {
                    documentFile = documentFile.findFile(parts[i]);
                }
            }

            if (documentFile == null) {

                // Toast.makeText(getApplicationContext(),"Operation Failed! Please Select Correct SDCard",Toast.LENGTH_LONG);
                progressDialog.dismiss();
                getSdCardUri();
                // File not found on tree search
                // User selected a wrong directory as the sd-card
                // Here must inform user about how to get the correct sd-card
                // and invoke file chooser dialog again.

                return false;

            } else {

                if (documentFile.delete()) {// if delete file succeed
                    // Remove information related to your media from ContentResolver,
                    // which documentFile.delete() didn't do the trick for me.
                    // Must do it otherwise you will end up with showing an empty
                    // ImageView if you are getting your URLs from MediaStore.
                    //

                    long longMediaId = Long.valueOf(mediaId);

                    Uri mediaContentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            longMediaId);
                    getContentResolver().delete(mediaContentUri, null, null);

                    if(deleteSingleFlag) {
                        deleteSingleFlag = false;
                        deleteMediaId = null;
                        deletePath = null;
                    }

                }


            }
        }catch (Throwable e) {
            e.printStackTrace();
            progressDialog.dismiss();
            getSdCardUri();
            return false;
            // Log.e("Exception Raised", "deleteMedia: "+e);
        }
        return true;
    }

    private int getMarkedCount(){

        if(listStyle==0) {
            return adapterLarge.getmarkedNumCount();

        }else if(listStyle==1){

            return adapter.getmarkedNumCount();

        }

        return 0;
    }

    private void updateLibrary(){
        progressDialog.setMessage("Updating Library...");
        sendBroadcast(new Intent("refreshLibrary"));
        new Handler().postDelayed(new Runnable() {
            public void run() {
                removeMarkDialog();

                if (!searchView.isIconified()) {
                    searchView.onActionViewCollapsed();
                }

                if(listStyle==0) {
                    adapterLarge.updateList(data);

                }else if(listStyle==1){

                    adapter.updateList(data);

                }
                progressDialog.dismiss();
                Vibrator vb = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                vb.vibrate(20);

                listHeader.setText("Videos ( "+data.size()+" )");
                listHeader.setVisibility(View.VISIBLE);
                SonaToast sonaToast=new SonaToast();
                sonaToast.setToast(getApplicationContext(),"Media Files Deleted",1);
            }}, 2000);

    }

    private void showProgressDialog(){

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Deleting Media Files");
        progressDialog.setMessage("Please Wait...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(true);
        if( progressDialog.getWindow()!=null)
            //progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.DKGRAY));
            progressDialog.show();

    }



    private  void  inflateOptionMenu(final Context context, String title, String subtitle, String meta, final String songData,final String imageId, final int position, final String mediaId) {

        Typeface currentTypeface=getTypeface();

        parentLayout = findViewById(R.id.recycler_view_wrapper);
        // Initialize a new instance of LayoutInflater service
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.popup_window_layout,null);
       final TextView headerTitle=customView.findViewById(R.id.popup_header_title);
       final TextView headerSubTitle=customView.findViewById(R.id.popup_header_subtitle);
        TextView headerMeta=customView.findViewById(R.id.popup_header_meta);
        TextView popup_delete=customView.findViewById(R.id.popup_delete_text);
        TextView popup_send=customView.findViewById(R.id.popup_send_text);
        TextView popup_mark=customView.findViewById(R.id.popup_mark_text);
        TextView popup_info=customView.findViewById(R.id.popup_info_text);
        TextView popup_ringtone=customView.findViewById(R.id.popup_ringtone_text);
        ImageView headerIcon=customView.findViewById(R.id.popup_header_icon);

        headerTitle.setTypeface(currentTypeface);
        headerSubTitle.setTypeface(currentTypeface);
        headerMeta.setTypeface(currentTypeface);
        popup_delete.setTypeface(currentTypeface);
        popup_send.setTypeface(currentTypeface);
        popup_mark.setTypeface(currentTypeface);
        popup_info.setTypeface(currentTypeface);
        popup_ringtone.setTypeface(currentTypeface);

        headerTitle.setText(title);
        headerSubTitle.setText(subtitle);
        headerMeta.setText(meta);
        headerSubTitle.setSelected(false);
        headerTitle.setSelected(false);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                headerSubTitle.setSelected(true);
                headerTitle.setSelected(true);
            }
        }, 2000);
        // Initialize a new instance of popup window
        mPopupWindow = new PopupWindow(
                customView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        mPopupWindow.setElevation(5.0f);
        mPopupWindow.setFocusable(true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setAnimationStyle(R.style.dialog_animation);
        mPopupWindow.showAtLocation(parentLayout, Gravity.CENTER,0,0);

            popup_ringtone.setTextColor(Color.parseColor("#004D68"));

         DrawableCrossFadeFactory factory = new DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build();
            GlideApp
                    .with(headerIcon.getContext())
                    .asBitmap()
                    .load(Uri.fromFile(new File(songData)))
                    .transition(withCrossFade(factory))
                    .placeholder(R.drawable.loading_art)
                    .error(R.drawable.default_album)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(headerIcon);




        popup_mark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
                performMark(position);
            }
        });

        popup_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Vibrator vb = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                vb.vibrate(20);
                mPopupWindow.dismiss();
                sendItem(songData);
            }
        });

        popup_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Vibrator vb = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                vb.vibrate(20);
                mPopupWindow.dismiss();
                deleteItem(songData,mediaId);
            }
        });

        popup_ringtone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    SonaToast sonaToast = new SonaToast();
                    sonaToast.setToast(getApplicationContext(), "Unavailable for Videos", 0);

            }

        });

        popup_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] mediaTags = getVideoList(mediaId);

                Bundle bundleMeta = getInfoBundleMeta(songData);
                Bundle bundleTags =getInfoBundleTags(songData,imageId,mediaTags[0],mediaTags[1],mediaTags[2],mediaTags[3],mediaTags[4]);
                Vibrator vb = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                vb.vibrate(20);
                inflateInfoWindow(getApplicationContext(),bundleMeta,songData,true,true,mediaId,bundleTags,false,null,null);
                mPopupWindow.dismiss();
            }
        });
    }

    private Typeface getTypeface(){
        Typeface face;
        if(!ApplicationContextProvider.systemFont) {
            face = Typeface.createFromAsset(getAssets(),
                    ApplicationContextProvider.getFontPath());
        }else {

            face=Typeface.DEFAULT;
        }

        return face;
    }


    private void performMark(int position) {

        Vibrator vb = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        vb.vibrate(50);
        if (!checkFlags) {
            checkFlags = true;

            if (listStyle == 1) {
                adapter.setCheckFlag(true);
            } else if (listStyle == 0) {
                adapterLarge.setCheckFlag(true);
            }
            inflateMarkDialog();
        }
        if (listStyle == 1) {
            adapter.setMarkBox(position);
            lastMarkPos = position;
            adapter.notifyDataSetChanged();
            markCountText.setText(adapter.getMarkCount());
        } else if (listStyle == 0) {
            adapterLarge.setMarkBox(position);
            lastMarkPos = position;
            adapterLarge.notifyDataSetChanged();
            markCountText.setText(adapterLarge.getMarkCount());
        }
    }

    private void sendItem( String path){

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("*/*");
        Uri uri=null;
        File file = new File(path);
        Log.d("FilePacked", "path:"+path);
        uri = FileProvider.getUriForFile(getApplicationContext(), VideoListActivity.this.getPackageName(), file);
        VideoListActivity.this.grantUriPermission(VideoListActivity.this.getPackageManager().toString(), uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        VideoListActivity.this.startActivity(Intent.createChooser(intent, "Share Media via"));
    }

    private void deleteItem(final String path,final String mediaId) {

        Vibrator vb = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        vb.vibrate(20);

        AlertDialog.Builder builder1 = new AlertDialog.Builder(new ContextThemeWrapper(VideoListActivity.this, R.style.myDialog));
        builder1.setTitle("Confirm Delete ?");
        builder1.setMessage("         Selected: " + 1);
        builder1.setCancelable(true);
        builder1.setIcon(R.drawable.delete_icons);
        builder1.setPositiveButton(
                "DELETE",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        showProgressDialog();
                        new Handler().postDelayed(new Runnable() {
                            public void run() {


                                File file = new File(path);
                                boolean isSuccess = file.delete();
                                if(isSuccess){
                                    long longMediaId = Long.valueOf(mediaId);

                                    Uri mediaContentUri = ContentUris.withAppendedId(
                                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                            longMediaId);
                                    getContentResolver().delete(mediaContentUri, null, null);

                                    updateLibrary();

                                } else {
                                    deleteSingleFlag=true;
                                    deleteMediaId=mediaId;
                                    deletePath=path;
                                    Log.d("FilePacked", "path:" + path);
                                    if (deleteMedia(sdCardUri, file, mediaId)) {
                                        updateLibrary();
                                    }
                                }
                            }
                        }, 500);

                    }
                });

        builder1.setNegativeButton(
                "CANCEL",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog1 = builder1.create();
        alertDialog1.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
        alertDialog1.show();
    }

    @Override
    public void onSaveTagResult(final String mediaNewId, final Bundle newTagsBundle){
        SonaToast sonaToast=new SonaToast();
        sonaToast.setToast(getApplicationContext(),"Updating...",1);
        Log.e("renameID", "onSaveTagResult: ID:"+mediaNewId );

        new Handler().postDelayed(new Runnable() {
            public void run() {

                Intent intent = new Intent("saveTagResult");
                intent.putExtra("renameId", mediaNewId);
                intent.putExtra("albumImage", "");
                intent.putExtra("BUNDLE",newTagsBundle);
                sendBroadcast(intent);
            }
        }, 500);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (!searchView.isIconified()) {
                    searchView.onActionViewCollapsed();
                }

                if(listStyle==1){

                    adapter.updateList(data);

                }else if(listStyle==0){
                    adapterLarge.updateList(data);
                }
                recyclerView.scrollToPosition(0);
                listHeader.setText("Videos ( "+data.size()+" )");
                listHeader.setVisibility(View.VISIBLE);

            }}, 2500);
    }
    @Override
    public  void onSdCardUriResult(){
        TagEditFlag=true;
        getSdCardUri();

    }

    @Override
    public void onChooseAlbumArt(Bundle bundleMetaTemp, String songDataTemp, boolean isVideoFileTemp,String mediaId, Bundle bundleTagsTemp,Bundle restoreText){

        Log.d("ChooseAlbumArt", "onChooseAlbumArt: AlbumrArtUnavailableForVideos");
    }


    private void inflateInfoWindow(Context context, final Bundle bundleMeta, final String songPath, final boolean isVideoFile, boolean isFirstTab, String mediaId, Bundle bundleTags, boolean isScrollEnd, Bitmap albumCropped, Bundle bundleRestoreText){

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        FragmentManager fm = getSupportFragmentManager();
        infoTabDialog = InfoTabDialog.newInstance("Info Tag Dialog",context,bundleMeta,songPath,isVideoFile,isFirstTab,imm,mediaId,bundleTags,isScrollEnd,albumCropped,bundleRestoreText);
        infoTabDialog.show(fm, "Sona Fonts");

    }

    private Bundle getInfoBundleTags(String songPath, String imageId,String titleMedia,String albumMedia,String artistMedia,String composerMedia,String yearMedia){

        String genre;
        try {

            MediaMetadataRetriever mRetriever = new MediaMetadataRetriever();
            mRetriever.setDataSource(songPath);

            if (titleMedia == null || TextUtils.isEmpty(titleMedia))
                titleMedia = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

            if (albumMedia == null || TextUtils.isEmpty(albumMedia))
                albumMedia = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

            if (artistMedia == null || TextUtils.isEmpty(artistMedia))
                artistMedia = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

            if (composerMedia == null || TextUtils.isEmpty(composerMedia))
                composerMedia = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER);

            if (yearMedia == null || TextUtils.isEmpty(yearMedia))
                yearMedia = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);

            genre = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);


        }catch (Throwable e){

            e.printStackTrace();
            return null;
        }

        Bundle bundleTags = new Bundle();
        bundleTags.putString("TITLE",titleMedia);
        bundleTags.putString("ALBUM",albumMedia);
        bundleTags.putString("ARTIST",artistMedia);
        bundleTags.putString("COMPOSER",composerMedia);
        bundleTags.putString("YEAR",yearMedia);
        bundleTags.putString("GENRE",genre);
        bundleTags.putString("ALBUM_ID",imageId);
        return bundleTags;
    }

    private Bundle getInfoBundleMeta( String songPath){

        try {
            Bundle bundleData = new Bundle();
            MediaFormat mf ;
            float samplerate =48;
            String duration;
            int framerate=24;
            String channel_text = "Stereo";
            try {
                MediaExtractor mediaExtractor = new MediaExtractor();
                mediaExtractor.setDataSource(songPath);

                    mf = mediaExtractor.getTrackFormat(0);
                    framerate=mf.getInteger(MediaFormat.KEY_FRAME_RATE);
                    mf = mediaExtractor.getTrackFormat(1);

                samplerate = (float) mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                samplerate = (samplerate / 1000);

                int channel = mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

                if (channel < 2)
                    channel_text = "Mono";
                else
                    channel_text = "Stereo";

            } catch (Throwable e) {


                e.printStackTrace();
            }

            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(songPath);
            duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String fileName = songPath.substring(songPath.lastIndexOf("/") + 1);

            try {
                    Bitmap frame = mmr.getFrameAtTime();
                    int width = frame.getWidth();
                    int height = frame.getHeight();
                    bundleData.putString("RESOLUTION", width + "x" + height);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            bundleData.putString("FILENAME", fileName);
            bundleData.putString("FRAMERATE",framerate+"FPS");
            bundleData.putString("BITRATE",getBitRate(songPath) + "Kbps");
            bundleData.putString("SAMPLERATE", samplerate + "KHz");
            bundleData.putString("FORMAT", ((songPath).substring((songPath.lastIndexOf(".") + 1)).toUpperCase()));
            bundleData.putString("CHANNEL", channel_text);
            bundleData.putString("SIZE", getStringSizeLengthFile(songPath));
            bundleData.putInt("DURATION", Integer.valueOf(duration));

            mmr.release();
            return bundleData;
        }catch (Throwable e){
            e.printStackTrace();
            return null;
        }

    }

    private  String getBitRate(String data) {

        int Bitrate;
            try {

                MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
                metadataRetriever.setDataSource(data);
                Bitrate = Integer.parseInt(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
                Bitrate = Bitrate / 1000;
                Log.d("Bitrate:", String.valueOf(Bitrate));

            } catch (Throwable e) {
                Bitrate = 128;
                Log.d("BitRate Exception: " + e, String.valueOf(Bitrate));
            }


        return String.valueOf(Bitrate);

    }

    public  String getStringSizeLengthFile(String data) {


        File file = new File(data);

// Get the number of bytes in the file
        long size = file.length();
        DecimalFormat df = new DecimalFormat("0.00");

        float sizeKb = 1024.0f;
        float sizeMo = sizeKb * sizeKb;
        float sizeGo = sizeMo * sizeKb;
        float sizeTerra = sizeGo * sizeKb;


        if(size < sizeMo)
            return df.format(size / sizeKb)+ " KB";
        else if(size < sizeGo)
            return df.format(size / sizeMo) + " MB";
        else if(size < sizeTerra)
            return df.format(size / sizeGo) + " GB";

        return "";
    }

    private  String[] getVideoList(String mediaId){

        long mediaIdL=Long.parseLong(mediaId);
        String[] mediaStoreTags=new String[8];

        String  selection ="_id = " + mediaIdL;

        String[] projection = {
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.ALBUM,
                MediaStore.Video.Media.ARTIST,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DISPLAY_NAME,

        };

        Cursor cursor = null;

        try {
            Uri uri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            cursor =ApplicationContextProvider.getContext().getContentResolver().query(uri, projection, selection, null, null);
            if (cursor != null) {
                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {

                    mediaStoreTags[0]=cursor.getString(0);
                    mediaStoreTags[1]=cursor.getString(1);
                    mediaStoreTags[2]=cursor.getString(2);
                    mediaStoreTags[3]="";
                    mediaStoreTags[4]="";
                    mediaStoreTags[5]=cursor.getString(3);
                    mediaStoreTags[6]=cursor.getString(4);
                    mediaStoreTags[7]=cursor.getString(5);

                    Log.d("MediaStore Retrieved", "Title:"+cursor.getString(0));
                    Log.d("MediaStore Retrieved", "Album:"+cursor.getString(1));
                    Log.d("MediaStore Retrieved", "Artist:"+cursor.getString(2));
                    Log.d("MediaStore Retrieved", "Filename:"+cursor.getString(5));

                    cursor.moveToNext();
                }

            }
        } catch (Exception e) {
            Log.e("Media", e.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return mediaStoreTags;

    }
}

