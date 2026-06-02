package com.inhatc.eatzy_project;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class RouletteActivity extends AppCompatActivity {

    private float currentRotation = 0f;
    private final Random random = new Random();
    private boolean spinning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.roulette);

        ImageView wheel = findViewById(R.id.wheel);
        Button btnSpin = findViewById(R.id.btn_spin);
        TextView btnProfile = findViewById(R.id.btn_profile);

        btnProfile.setOnClickListener(v ->
                Toast.makeText(this, "프로필", Toast.LENGTH_SHORT).show());

        btnSpin.setOnClickListener(v -> spinWheel(wheel));
    }

    private void spinWheel(ImageView wheel) {
        if (spinning) return;
        spinning = true;

        // 최소 4바퀴(1440도) 이상 무작위 회전
        float target = currentRotation + 1440 + random.nextInt(360) + 1;

        ObjectAnimator anim = ObjectAnimator.ofFloat(wheel, "rotation", currentRotation, target);
        anim.setDuration(3000);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();

        currentRotation = target % 360;
        wheel.postDelayed(() -> spinning = false, 3000);
    }
}
