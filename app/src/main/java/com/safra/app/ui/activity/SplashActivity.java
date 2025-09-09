// java/com/safra/app/ui/activity/SplashActivity.java
package com.safra.app.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.safra.app.R; // Ensure this import is correct for your project

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DISPLAY_LENGTH = 2500; // 2.5 seconds total display time
    private static final long ANIMATION_DELAY_LOGO = 0;
    private static final long ANIMATION_DELAY_APP_NAME = 300;
    private static final long ANIMATION_DELAY_TAGLINE = 600;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize views
        ImageView logoImage = findViewById(R.id.logo_image);
        TextView appNameText = findViewById(R.id.app_name_text);
        TextView taglineText = findViewById(R.id.tagline_text);

        // Load animations
        Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.logo_scale_fade);
        Animation appNameAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation taglineAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        // Apply animations with delays
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            logoImage.setVisibility(View.VISIBLE);
            logoImage.startAnimation(logoAnimation);
        }, ANIMATION_DELAY_LOGO);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            appNameText.setVisibility(View.VISIBLE);
            appNameText.startAnimation(appNameAnimation);
        }, ANIMATION_DELAY_APP_NAME);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            taglineText.setVisibility(View.VISIBLE);
            taglineText.startAnimation(taglineAnimation);
        }, ANIMATION_DELAY_TAGLINE);


        // Handler to start the next activity after a delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginRegisterActivity.class));
            }
            finish();
        }, SPLASH_DISPLAY_LENGTH);
    }
}