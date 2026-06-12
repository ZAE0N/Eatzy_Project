package com.inhatc.eatzy_project;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NeedsSettingsActivity extends AppCompatActivity {

    // 저장된 선호가 아직 없는 신규 사용자의 기본 선택(원래 XML ChipOn 과 동일)
    private static final Set<String> DEFAULT_FOOD = new HashSet<>(Arrays.asList("한식", "양식", "분식"));
    private static final Set<String> DEFAULT_INGREDIENT = new HashSet<>(Arrays.asList("밥", "면"));
    private static final int DEFAULT_PRICE = 12000;

    private ChipGroup chipsFood;
    private ChipGroup chipsIngredient;
    private EditText etAllergy;
    private SeekBar seekPrice;

    private String userKey;
    private User currentUser;   // 기존 데이터 보존용 (email/avoidMenus 등)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.needs_settings);
        UiInsets.apply(this);

        TextView btnBack = findViewById(R.id.btn_back);
        Button btnSave = findViewById(R.id.btn_save);
        chipsFood = findViewById(R.id.chips_food);
        chipsIngredient = findViewById(R.id.chips_ingredient);
        etAllergy = findViewById(R.id.et_allergy);
        seekPrice = findViewById(R.id.seek_price);

        btnBack.setOnClickListener(v -> finish());

        // 칩 클릭 토글 핸들러 부착 (상태는 아래 로드 후 적용)
        attachChipToggle(chipsFood);
        attachChipToggle(chipsIngredient);

        userKey = new SessionManager(this).getCurrentUserKey();
        loadUser();   // 저장된 설정 불러와 화면에 채움

        btnSave.setOnClickListener(v -> save());
    }

    /** 현재 사용자 설정을 Firebase 에서 읽어 칩/입력칸/슬라이더에 반영. */
    private void loadUser() {
        FirebaseHelper.get().users().child(userKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = snapshot.getValue(User.class);
                applyUserToUi(currentUser);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                applyUserToUi(null);   // 못 읽으면 기본값으로
            }
        });
    }

    private void applyUserToUi(User user) {
        Set<String> foods = (user != null && notEmpty(user.preferredCategories))
                ? new HashSet<>(user.preferredCategories) : DEFAULT_FOOD;
        Set<String> ingredients = (user != null && notEmpty(user.preferredIngredients))
                ? new HashSet<>(user.preferredIngredients) : DEFAULT_INGREDIENT;

        applyChipSelection(chipsFood, foods);
        applyChipSelection(chipsIngredient, ingredients);

        if (user != null && notEmpty(user.allergies)) {
            etAllergy.setText(TextUtils.join(", ", user.allergies));
        }
        seekPrice.setProgress((user != null && user.maxPrice > 0) ? user.maxPrice : DEFAULT_PRICE);
    }

    /** 저장 버튼: 화면 입력을 모아 User 에 반영하고 Firebase 에 기록. */
    private void save() {
        if (currentUser == null) currentUser = new User();

        currentUser.preferredCategories = collectSelected(chipsFood);
        currentUser.preferredIngredients = collectSelected(chipsIngredient);
        currentUser.allergies = parseCsv(etAllergy.getText().toString());
        currentUser.maxPrice = seekPrice.getProgress();

        FirebaseHelper.get().users().child(userKey).setValue(currentUser)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── 칩 헬퍼 ──

    private void attachChipToggle(ChipGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) {
                TextView chip = (TextView) child;
                chip.setOnClickListener(v -> setChip(chip, !isOn(chip)));
            }
        }
    }

    /** 저장된 선택 집합에 맞춰 각 칩의 on/off 를 설정. */
    private void applyChipSelection(ChipGroup group, Set<String> selectedTexts) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) {
                TextView chip = (TextView) child;
                setChip(chip, selectedTexts.contains(chip.getText().toString()));
            }
        }
    }

    private List<String> collectSelected(ChipGroup group) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView && isOn((TextView) child)) {
                result.add(((TextView) child).getText().toString());
            }
        }
        return result;
    }

    private boolean isOn(TextView chip) {
        Object tag = chip.getTag();
        return tag instanceof Boolean && (Boolean) tag;
    }

    private void setChip(TextView chip, boolean on) {
        chip.setTag(on);
        chip.setBackgroundResource(on ? R.drawable.bg_chip_on : R.drawable.bg_chip);
        chip.setTextColor(ContextCompat.getColor(this, on ? R.color.primary : R.color.gray_500));
        // 배경 변경으로 패딩이 초기화되지 않도록 재설정
        int h = dp(13);
        int v = dp(6);
        chip.setPadding(h, v, h, v);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ── 기타 유틸 ──

    /** "갑각류, 땅콩 , 우유" → ["갑각류","땅콩","우유"] (공백/빈값 제거) */
    private List<String> parseCsv(String text) {
        List<String> result = new ArrayList<>();
        if (TextUtils.isEmpty(text)) return result;
        for (String part : text.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) result.add(t);
        }
        return result;
    }

    private boolean notEmpty(List<String> list) {
        return list != null && !list.isEmpty();
    }
}
