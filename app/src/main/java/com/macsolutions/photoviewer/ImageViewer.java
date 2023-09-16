package com.macsolutions.photoviewer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class ImageViewer extends AppCompatActivity {
    ImageView imgview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        imgview = findViewById(R.id.imgview);
        String intentUrl = getIntent().getStringExtra("id");

        Glide.with(this)
                .load(intentUrl)
                .thumbnail(0.5f)
                .into(imgview);
    }
}