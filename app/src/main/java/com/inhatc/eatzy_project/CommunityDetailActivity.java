package com.inhatc.eatzy_project;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CommunityDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.community_detail);

        TextView btnBack = findViewById(R.id.btn_back);
        EditText etComment = findViewById(R.id.et_comment);
        TextView btnSend = findViewById(R.id.btn_send);

        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String text = etComment.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "댓글을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            etComment.setText("");
            Toast.makeText(this, "댓글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
        });
    }
}
