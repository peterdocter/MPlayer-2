package com.stang.mplayer;

import android.app.Application;
import android.media.Image;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

/**
 * Created by Stanislav on 14.11.2016.
 */

public class MediaPlayerApplication extends Application {

    DisplayImageOptions options = new DisplayImageOptions.Builder()
            .showImageOnFail(R.drawable.ipod_player_icon_small)
            .showImageForEmptyUri(R.drawable.music_box_black)
            //.imageScaleType(ImageScaleType.EXACTLY_STRETCHED)
            //.resetViewBeforeLoading()
            //.cacheInMemory()
            //.cacheOnDisc()
            //.decodingType(ImageScaleType.EXACTLY)
            .build();


    @Override
    public void onCreate() {
        super.onCreate();
        // todo init all global libraries
        initImageLoader();
    }


    private void initImageLoader() {
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                //.memoryCacheExtraOptions(480, 800) // width, height
                //.discCacheExtraOptions(480, 800, Bitmap.CompressFormat.JPEG, 75) // width, height, compress format, quality
                .threadPoolSize(5)
                .threadPriority(Thread.MIN_PRIORITY + 2)
                .denyCacheImageMultipleSizesInMemory()
                .memoryCache(new UsingFreqLimitedMemoryCache(2 * 1024 * 1024)) // 2 Mb
                //.discCache(new UnlimitedDiscCache(cacheDir))
                //.discCacheFileNameGenerator(new HashCodeFileNameGenerator())
                //.imageDownloader(new BaseImageDownloader(5 * 1000, 30 * 1000)) // connectTimeout (5 s), readTimeout (30 s)
                .defaultDisplayImageOptions(options)
                //.enableLogging()
                .build();
        ImageLoader.getInstance().init(config);
    }
}
