package com.inhatc.eatzy_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.recaptcha.RecaptchaAction;

public class LoginActivity extends AppCompatActivity {

    private EditText etId;
    private EditText etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login);
        UiInsets.apply(this);

        etId = findViewById(R.id.et_id);
        etPassword = findViewById(R.id.et_password);

        Button btnLogin = findViewById(R.id.btn_login);
        Button btnGoogle = findViewById(R.id.btn_google);
        Button btnGuest = findViewById(R.id.btn_guest);
        TextView tvFindPw = findViewById(R.id.tv_find_pw);
        TextView tvSignup = findViewById(R.id.tv_signup);

        SessionManager session = new SessionManager(this);

        btnLogin.setOnClickListener(v -> {
            String id = etId.getText().toString().trim();
            String pw = etPassword.getText().toString().trim();
            if (id.isEmpty() || pw.isEmpty()) {
                Toast.makeText(this, "ID와 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            // 봇 방지: reCAPTCHA 통과해야만 로그인 진행 (자동 로그인 시도 차단)
            RecaptchaHelper.verify(this, RecaptchaAction.custom("login"), pass -> {
                if (!pass) {
                    Toast.makeText(this, "봇 검증에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 입력한 ID(이메일)를 현재 사용자로 기억 → 니즈설정/룰렛이 이 사용자를 가리킴
                session.setCurrentUserKey(SessionManager.keyFromEmail(id));
                goHome();
            });
        });

        btnGoogle.setOnClickListener(v -> {
            Toast.makeText(this, "Google 로그인", Toast.LENGTH_SHORT).show();
            session.setCurrentUserKey(SessionManager.GUEST_KEY);
            goHome();
        });

        btnGuest.setOnClickListener(v -> {
            session.setCurrentUserKey(SessionManager.GUEST_KEY);
            goHome();
        });

        tvFindPw.setOnClickListener(v ->
                startActivity(new Intent(this, FindPasswordActivity.class)));

        tvSignup.setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class)));
    }

    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
