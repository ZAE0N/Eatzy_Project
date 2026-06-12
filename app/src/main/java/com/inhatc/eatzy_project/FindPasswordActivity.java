package com.inhatc.eatzy_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class FindPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.find_password);
        UiInsets.apply(this);

        TextView btnBack = findViewById(R.id.btn_back);
        Button btnSend = findViewById(R.id.btn_send);
        Button btnVerify = findViewById(R.id.btn_verify);

        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v ->
                Toast.makeText(this, "인증 코드를 이메일로 발송했습니다.", Toast.LENGTH_SHORT).show());

        btnVerify.setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));
    }
}
