package com.inhatc.eatzy_project;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        TextView btnBack = findViewById(R.id.btn_back);
        Button btnSendCode = findViewById(R.id.btn_send_code);
        Button btnVerify = findViewById(R.id.btn_verify);
        Button btnSignup = findViewById(R.id.btn_signup);
        TextView tvLogin = findViewById(R.id.tv_login);

        btnBack.setOnClickListener(v -> finish());
        tvLogin.setOnClickListener(v -> finish());

        btnSendCode.setOnClickListener(v ->
                Toast.makeText(this, "인증 메일을 전송했습니다.", Toast.LENGTH_SHORT).show());

        btnVerify.setOnClickListener(v ->
                Toast.makeText(this, "이메일 인증 완료!", Toast.LENGTH_SHORT).show());

        btnSignup.setOnClickListener(v -> {
            Toast.makeText(this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
