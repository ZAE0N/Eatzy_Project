package com.inhatc.eatzy_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CommunityListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.community_list);

        TextView btnBack = findViewById(R.id.btn_back);
        TextView btnWrite = findViewById(R.id.btn_write);

        btnBack.setOnClickListener(v -> finish());
        btnWrite.setOnClickListener(v ->
                Toast.makeText(this, "글쓰기", Toast.LENGTH_SHORT).show());

        View.OnClickListener openDetail = v ->
                startActivity(new Intent(this, CommunityDetailActivity.class));

        findViewById(R.id.post_1).setOnClickListener(openDetail);
        findViewById(R.id.post_2).setOnClickListener(openDetail);
        findViewById(R.id.post_3).setOnClickListener(openDetail);
        findViewById(R.id.post_4).setOnClickListener(openDetail);
    }
}
