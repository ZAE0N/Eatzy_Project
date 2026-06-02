package com.inhatc.eatzy_project;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.ChipGroup;

public class NeedsSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.needs_settings);

        TextView btnBack = findViewById(R.id.btn_back);
        Button btnSave = findViewById(R.id.btn_save);
        ChipGroup chipsFood = findViewById(R.id.chips_food);
        ChipGroup chipsIngredient = findViewById(R.id.chips_ingredient);

        btnBack.setOnClickListener(v -> finish());

        // 칩 클릭 시 선택/해제 토글
        setupChipToggle(chipsFood);
        setupChipToggle(chipsIngredient);

        btnSave.setOnClickListener(v -> {
            Toast.makeText(this, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupChipToggle(ChipGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) {
                TextView chip = (TextView) child;
                // 태그에 현재 선택 상태 저장 (배경으로 판단)
                chip.setTag(isOn(chip));
                chip.setOnClickListener(v -> toggleChip(chip));
            }
        }
    }

    private boolean isOn(TextView chip) {
        Object tag = chip.getTag();
        return tag instanceof Boolean && (Boolean) tag;
    }

    private void toggleChip(TextView chip) {
        boolean on = !isOn(chip);
        chip.setTag(on);
        if (on) {
            chip.setBackgroundResource(R.drawable.bg_chip_on);
            chip.setTextColor(ContextCompat.getColor(this, R.color.primary));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip);
            chip.setTextColor(ContextCompat.getColor(this, R.color.gray_500));
        }
        // 패딩이 배경 변경으로 초기화되지 않도록 재설정
        int h = dp(13);
        int v = dp(6);
        chip.setPadding(h, v, h, v);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
