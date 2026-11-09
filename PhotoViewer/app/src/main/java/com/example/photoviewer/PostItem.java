package com.example.photoviewer;

import android.graphics.Bitmap;

public class PostItem {

    private final String title;
    private final Bitmap bitmap;

    public PostItem(String title, Bitmap bitmap) {
        this.title = title;
        this.bitmap = bitmap;
    }

    public String getTitle() {
        return title;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
}
