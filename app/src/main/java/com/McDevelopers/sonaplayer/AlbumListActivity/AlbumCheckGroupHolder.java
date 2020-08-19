package com.McDevelopers.sonaplayer.AlbumListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.McDevelopers.sonaplayer.ApplicationContextProvider;
import com.McDevelopers.sonaplayer.GlideApp;
import com.McDevelopers.sonaplayer.MusicLibrary;
import com.McDevelopers.sonaplayer.R;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory;
import com.bumptech.glide.signature.ObjectKey;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;
import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder;

import java.io.File;
import java.util.Random;
import static com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions.withCrossFade;

public class AlbumCheckGroupHolder extends GroupViewHolder {

    private TextView genreName;
    private ImageView arrow;
    private ImageView icon;
    private TextView mNumTextView;
    private static final float INITIAL_POSITION = 0.0f;
    private static final float ROTATED_POSITION = 180f;

    private static boolean isCustomArt = false;
    private static Random rand;
    private static int currentArtIndex;
    private DrawableCrossFadeFactory factory ;


    public AlbumCheckGroupHolder(View itemView) {
        super(itemView);
        genreName = (TextView) itemView.findViewById(R.id.tv_album_category);
        arrow = (ImageView) itemView.findViewById(R.id.iv_arrow_expand);
        icon = (ImageView) itemView.findViewById(R.id.album_imageView);
        mNumTextView=itemView.findViewById(R.id.song_num);

        SharedPreferences currentState = ApplicationContextProvider.getContext().getSharedPreferences("com.McDevelopers.sonaplayer", Context.MODE_PRIVATE);
        isCustomArt = currentState.getBoolean("customAlbum", false);
        factory = new DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build();

    }

    public void setGenreTitle(ExpandableGroup genre) {

            genreName.setText(genre.getTitle());
            mNumTextView.setText(((AlbumCategoryExpanded) genre).getSongCount());

    }

    public  void setGroupIcon(String artId ,boolean checkFlag,String songdata,String songAlbumId){

        if (isCustomArt && MusicLibrary.listOfAllImages.size() != 0 && !checkFlag) {


            GlideApp
                    .with(icon.getContext())
                    .asBitmap()
                    .thumbnail(0.1f)
                    .load(new File(randomArt()))
                    .transition(withCrossFade(factory))
                    .placeholder(R.drawable.artist)
                    .error(R.drawable.artist)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(icon);
        } else {

            try {

                if(new File(artId).exists()) {
                    Log.e("AlbumArtFoundMediaStore", "setGroupIcon: Successfull"+artId);
                    GlideApp
                            .with(icon.getContext())
                            .asBitmap()
                            .transition(withCrossFade(factory))
                            .signature(new ObjectKey(new File(artId).lastModified()))
                            .placeholder(R.drawable.artist)
                            .error(R.drawable.artist)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .load(new File(artId))
                            .into(icon);
                }else {

                    Log.e("AlbumArtFromSongAlbumId", "setGroupIcon: Successfull");

                    GlideApp
                            .with(icon.getContext())
                            .asBitmap()
                            .load(getAlbumUri(artId))
                            .transition(withCrossFade(factory))
                            .placeholder(R.drawable.artist)
                            .error(R.drawable.artist)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .into(icon);
                }

            }catch (Throwable e){
                try {

                    Log.e("AlbumLoadingFromSongData", "setGroupIcon: SongData:"+songdata );
                    GlideApp
                            .with(icon.getContext())
                            .asBitmap()
                            .load(songdata)
                            .transition(withCrossFade(factory))
                            .placeholder(R.drawable.artist)
                            .signature(new ObjectKey(new File(songdata).lastModified()))
                            .error(R.drawable.artist)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .into(icon);
                }catch (Throwable eX){
                    icon.setImageResource(R.drawable.artist);
                    eX.printStackTrace();
                }
            }
        }

    }

    @Override
    public void expand() {

            expansionToggled(false);
            arrow.setRotation(ROTATED_POSITION);

        Log.d("expandAnimation", "expand: Invoked");
    }

    @Override
    public void collapse() {

            expansionToggled(true);
            arrow.setRotation(INITIAL_POSITION);

        Log.d("CollapseAnimation", "collapse: Rotation:" + arrow.getRotation());
    }

    public void expansionToggled(boolean expanded) {

        RotateAnimation rotateAnimation;
        if (expanded) { // rotate clockwise
            rotateAnimation = new RotateAnimation(ROTATED_POSITION,
                    INITIAL_POSITION,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f);

            Log.d("AnimationExpand", " Rotation: Invoked" + arrow.getRotation());

        } else { // rotate counterclockwise
            rotateAnimation = new RotateAnimation(-1 * ROTATED_POSITION,
                    INITIAL_POSITION,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f);
            Log.d("AnimationCollapse", " Rotation:" + arrow.getRotation());

        }

        rotateAnimation.setDuration(200);
        rotateAnimation.setFillAfter(true);
        arrow.startAnimation(rotateAnimation);



    }


    private static String randomArt() {

        int max = MusicLibrary.listOfAllImages.size();
        rand = new Random();
        int newAlbumIndex = currentArtIndex;

        try {
            if (max > 1) {

                if (max > 50) {
                    while (newAlbumIndex == currentArtIndex) {
                        newAlbumIndex = rand.nextInt(max);
                    }
                } else {

                    newAlbumIndex = rand.nextInt(max);
                }
                String newAlbum = MusicLibrary.listOfAllImages.get(newAlbumIndex);
                currentArtIndex = newAlbumIndex;
                return newAlbum;
            } else
                currentArtIndex = -1;
            return "";
        } catch (Throwable e) {

            Log.e("ExceptionRaised", "randomArt: " + e);
            currentArtIndex = -1;
            return "";
        }


    }
    private static Uri getAlbumUri(String mediaID) {

        String mediaPath= "-1";


        if(mediaID!=null && mediaID.equals("")){
            mediaPath="-1";
        }else if(mediaID!=null && !mediaID.isEmpty() ){

            mediaPath=mediaID;
        }

        try {
            Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
            Log.d("AlbumArtUri", ContentUris.withAppendedId(sArtworkUri, Long.parseLong(mediaPath)).toString());
            return ContentUris.withAppendedId(sArtworkUri, Long.parseLong(mediaPath));
        }catch (Throwable e){

            return null;
        }

    }
}