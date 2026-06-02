package com.inhatc.eatzy_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView btnMenu = findViewById(R.id.btn_menu);
        TextView btnNoti = findViewById(R.id.btn_noti);
        View cardRecommend = findViewById(R.id.card_recommend);
        View cardNeeds = findViewById(R.id.card_needs);
        View cardCommunity = findViewById(R.id.card_community);
        View fabRoulette = findViewById(R.id.fab_roulette);

        btnMenu.setOnClickListener(v ->
                startActivity(new Intent(this, SidebarActivity.class)));

        btnNoti.setOnClickListener(v ->
                Toast.makeText(this, "알림", Toast.LENGTH_SHORT).show());

        cardRecommend.setOnClickListener(v ->
                startActivity(new Intent(this, RestaurantMapActivity.class)));

        cardNeeds.setOnClickListener(v ->
                startActivity(new Intent(this, NeedsSettingsActivity.class)));

        cardCommunity.setOnClickListener(v ->
                startActivity(new Intent(this, CommunityListActivity.class)));

        fabRoulette.setOnClickListener(v ->
                startActivity(new Intent(this, RouletteActivity.class)));
    }
}
