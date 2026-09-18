package com.ikramkheshgi.popupdetector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.animation.AccelerateDecelerateInterpolator;

public class SplashActivity extends Activity {

    private static final long SPLASH_DURATION = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        ImageView introImage = findViewById(R.id.introImage);
        TextView appName = findViewById(R.id.appName);

        // Initial photo state
        introImage.setAlpha(0f);
        introImage.setScaleX(0.92f);
        introImage.setScaleY(0.92f);

        // Initial app-name state
        appName.setAlpha(0f);
        appName.setTranslationY(40f);

        // Photo animation
        introImage.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // App-name animation
        appName.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(700)
                .setDuration(800)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Open main screen after 3 seconds
        introImage.postDelayed(() -> {
            Intent intent = new Intent(
                    SplashActivity.this,
                    MainActivity.class
            );

            startActivity(intent);

            overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
            );

            finish();
        }, SPLASH_DURATION);
    }
}
