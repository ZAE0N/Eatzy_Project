package com.inhatc.eatzy_project;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class CommunityListActivity extends AppCompatActivity {

    public static final String EXTRA_POST_KEY = "post_key";

    /** 좋아요가 이 수 이상이면 베스트 탭에 노출. */
    public static final int BEST_THRESHOLD = 10;

    private LinearLayout listContainer;
    private TextView tabNormal, tabBest;
    private boolean showBest = false;   // false=일반, true=베스트

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.community_list);
        UiInsets.apply(this);

        listContainer = findViewById(R.id.list_container);
        tabNormal = findViewById(R.id.tab_normal);
        tabBest = findViewById(R.id.tab_best);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_write).setOnClickListener(v ->
                startActivity(new Intent(this, CommunityWriteActivity.class)));

        tabNormal.setOnClickListener(v -> switchTab(false));
        tabBest.setOnClickListener(v -> switchTab(true));

        loadPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 글쓰기/상세에서 돌아왔을 때 목록 갱신
        loadPosts();
    }

    /** 탭 전환 + 강조 표시. */
    private void switchTab(boolean best) {
        showBest = best;
        int on = ContextCompat.getColor(this, R.color.primary);
        int off = ContextCompat.getColor(this, R.color.gray_400);
        tabNormal.setTextColor(best ? off : on);
        tabBest.setTextColor(best ? on : off);
        tabNormal.setTypeface(null, best ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
        tabBest.setTypeface(null, best ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        loadPosts();
    }

    /** posts 시딩 → 최신순으로 목록 동적 생성(탭 필터 적용). */
    private void loadPosts() {
        SampleData.seedPostsIfEmpty(() ->
                FirebaseHelper.get().posts().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listContainer.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(CommunityListActivity.this);

                // 최신글이 위로 오도록 역순 수집
                java.util.List<DataSnapshot> children = new java.util.ArrayList<>();
                for (DataSnapshot c : snapshot.getChildren()) children.add(c);
                java.util.Collections.reverse(children);

                int shown = 0;
                for (DataSnapshot child : children) {
                    Post post = child.getValue(Post.class);
                    if (post == null) continue;
                    // 베스트 탭이면 좋아요 임계치 이상만
                    if (showBest && post.likes < BEST_THRESHOLD) continue;
                    addPostView(inflater, child.getKey(), post);
                    shown++;
                }
                showEmptyIfNeeded(shown);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CommunityListActivity.this,
                        "게시글을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void showEmptyIfNeeded(int shown) {
        if (shown > 0) return;
        TextView empty = new TextView(this);
        empty.setText(showBest
                ? "아직 베스트 게시글이 없습니다.\n좋아요 " + BEST_THRESHOLD + "개 이상이면 등록돼요!"
                : "게시글이 없습니다.");
        empty.setGravity(android.view.Gravity.CENTER);
        empty.setPadding(48, 80, 48, 48);
        empty.setTextColor(ContextCompat.getColor(this, R.color.gray_400));
        empty.setTextSize(13);
        listContainer.addView(empty);
    }

    private void addPostView(LayoutInflater inflater, String postKey, Post post) {
        View item = inflater.inflate(R.layout.item_post, listContainer, false);

        String author = TextUtils.isEmpty(post.author) ? "익명" : post.author;
        ((TextView) item.findViewById(R.id.post_avatar)).setText(author.substring(0, 1));
        ((TextView) item.findViewById(R.id.post_author)).setText(author);
        ((TextView) item.findViewById(R.id.post_date)).setText(TimeUtil.relative(post.createdAt));
        ((TextView) item.findViewById(R.id.post_title)).setText(post.title);
        ((TextView) item.findViewById(R.id.post_preview)).setText(post.content);
        ((TextView) item.findViewById(R.id.post_likes)).setText("❤️ " + post.likes);

        TextView loc = item.findViewById(R.id.post_loc);
        if (post.hasLocation()) {
            loc.setText("📍 " + (TextUtils.isEmpty(post.placeName) ? "위치" : post.placeName));
            loc.setVisibility(View.VISIBLE);
        }

        item.findViewById(R.id.post_card).setOnClickListener(v -> {
            Intent intent = new Intent(this, CommunityDetailActivity.class);
            intent.putExtra(EXTRA_POST_KEY, postKey);
            startActivity(intent);
        });

        listContainer.addView(item);
    }
}
