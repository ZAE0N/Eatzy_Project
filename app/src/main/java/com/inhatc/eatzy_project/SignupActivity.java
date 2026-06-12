package com.inhatc.eatzy_project;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.recaptcha.RecaptchaAction;

import java.util.ArrayList;

public class SignupActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etPasswordConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.signup);
        UiInsets.apply(this);

        TextView btnBack = findViewById(R.id.btn_back);
        Button btnSendCode = findViewById(R.id.btn_send_code);
        Button btnVerify = findViewById(R.id.btn_verify);
        Button btnSignup = findViewById(R.id.btn_signup);
        TextView tvLogin = findViewById(R.id.tv_login);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etPasswordConfirm = findViewById(R.id.et_password_confirm);

        btnBack.setOnClickListener(v -> finish());
        tvLogin.setOnClickListener(v -> finish());

        btnSendCode.setOnClickListener(v ->
                Toast.makeText(this, "인증 메일을 전송했습니다.", Toast.LENGTH_SHORT).show());

        btnVerify.setOnClickListener(v ->
                Toast.makeText(this, "이메일 인증 완료!", Toast.LENGTH_SHORT).show());

        btnSignup.setOnClickListener(v -> signup());
    }

    /** 입력값을 검증하고, 통과하면 User 객체를 Firebase users/ 에 저장한다. */
    private void signup() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String passwordConfirm = etPasswordConfirm.getText().toString();

        // ── 입력 검증 ──
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "이메일을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(passwordConfirm)) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ── 봇 방지: reCAPTCHA 통과해야만 가입 진행 ──
        RecaptchaHelper.verify(this, RecaptchaAction.custom("signup"), pass -> {
            if (!pass) {
                Toast.makeText(this, "봇 검증에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            saveUser(email);
        });
    }

    /** reCAPTCHA 통과 후 실제 User 저장. */
    private void saveUser(String email) {
        // 알레르기·기피메뉴는 빈 상태 → 나중에 니즈설정에서 채움
        User user = new User(email, new ArrayList<>(), new ArrayList<>());

        // 이메일의 '.'은 Realtime DB 키에 못 쓰므로 keyFromEmail()로 치환.
        String userKey = SessionManager.keyFromEmail(email);
        FirebaseHelper.get().users().child(userKey).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
