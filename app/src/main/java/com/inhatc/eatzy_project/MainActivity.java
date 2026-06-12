package com.inhatc.eatzy_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        UiInsets.apply(this);

        TextView btnMenu = findViewById(R.id.btn_menu);
        View cardRecommend = findViewById(R.id.card_recommend);
        View cardNeeds = findViewById(R.id.card_needs);
        View cardCommunity = findViewById(R.id.card_community);
        View fabRoulette = findViewById(R.id.fab_roulette);

        btnMenu.setOnClickListener(v ->
                startActivity(new Intent(this, SidebarActivity.class)));

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
