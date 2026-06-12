package com.inhatc.eatzy_project;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ResetPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.reset_password);
        UiInsets.apply(this);

        TextView btnBack = findViewById(R.id.btn_back);
        EditText etNew = findViewById(R.id.et_new_password);
        TextView tvStrength = findViewById(R.id.tv_strength);
        Button btnChange = findViewById(R.id.btn_change);

        btnBack.setOnClickListener(v -> finish());

        etNew.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                updateStrength(s.toString(), tvStrength);
            }
        });

        btnChange.setOnClickListener(v -> {
            Toast.makeText(this, "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void updateStrength(String pw, TextView tv) {
        if (pw.isEmpty()) {
            tv.setText("");
            return;
        }
        int score = 0;
        if (pw.length() >= 8) score++;
        if (pw.matches(".*[a-zA-Z].*")) score++;
        if (pw.matches(".*[0-9].*")) score++;
        if (pw.matches(".*[!@#$%^&*].*")) score++;

        if (score <= 2) {
            tv.setText("강도: 약함 ⚠️");
            tv.setTextColor(ContextCompat.getColor(this, R.color.red));
        } else if (score == 3) {
            tv.setText("강도: 보통 ⚡");
            tv.setTextColor(ContextCompat.getColor(this, R.color.amber));
        } else {
            tv.setText("강도: 강함 ✅");
            tv.setTextColor(ContextCompat.getColor(this, R.color.green));
        }
    }
}
