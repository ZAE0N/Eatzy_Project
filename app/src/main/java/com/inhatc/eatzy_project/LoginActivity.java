package com.inhatc.eatzy_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etId;
    private EditText etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        etId = findViewById(R.id.et_id);
        etPassword = findViewById(R.id.et_password);

        Button btnLogin = findViewById(R.id.btn_login);
        Button btnGoogle = findViewById(R.id.btn_google);
        Button btnGuest = findViewById(R.id.btn_guest);
        TextView tvFindPw = findViewById(R.id.tv_find_pw);
        TextView tvSignup = findViewById(R.id.tv_signup);

        btnLogin.setOnClickListener(v -> {
            String id = etId.getText().toString().trim();
            String pw = etPassword.getText().toString().trim();
            if (id.isEmpty() || pw.isEmpty()) {
                Toast.makeText(this, "ID와 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            goHome();
        });

        btnGoogle.setOnClickListener(v -> {
            Toast.makeText(this, "Google 로그인", Toast.LENGTH_SHORT).show();
            goHome();
        });

        btnGuest.setOnClickListener(v -> goHome());

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
