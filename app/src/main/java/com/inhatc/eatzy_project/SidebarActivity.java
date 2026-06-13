package com.inhatc.eatzy_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class SidebarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.sidebar);
        UiInsets.apply(this);

        TextView btnHome = findViewById(R.id.btn_home);
        TextView menuRecommend = findViewById(R.id.menu_recommend);
        TextView menuNeeds = findViewById(R.id.menu_needs);
        TextView menuCommunity = findViewById(R.id.menu_community);
        TextView menuWishlist = findViewById(R.id.menu_wishlist);

        // 홈화면으로 (사이드바 닫기)
        btnHome.setOnClickListener(v -> finish());

        menuRecommend.setOnClickListener(v -> {
            startActivity(new Intent(this, RestaurantMapActivity.class));
            finish();
        });

        menuNeeds.setOnClickListener(v -> {
            startActivity(new Intent(this, NeedsSettingsActivity.class));
            finish();
        });

        menuCommunity.setOnClickListener(v -> {
            startActivity(new Intent(this, CommunityListActivity.class));
            finish();
        });

        menuWishlist.setOnClickListener(v -> {
            startActivity(new Intent(this, WishlistActivity.class));
            finish();
        });
    }
}
