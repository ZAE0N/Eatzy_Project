package com.inhatc.eatzy_project;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/** 사용자가 '저장'한 게시글(위시리스트) 목록 화면. */
public class WishlistActivity extends AppCompatActivity {

    private LinearLayout listContainer;
    private String userKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.wishlist);
        UiInsets.apply(this);

        userKey = new SessionManager(this).getCurrentUserKey();
        listContainer = findViewById(R.id.list_container);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWishlist();   // 상세에서 저장/해제하고 돌아왔을 때 갱신
    }

    /** wishlists/{userKey} 의 key 들을 읽고, 해당 게시글을 posts 에서 찾아 표시. */
    private void loadWishlist() {
        FirebaseHelper.get().wishlist(userKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot wishSnap) {
                List<String> keys = new ArrayList<>();
                for (DataSnapshot c : wishSnap.getChildren()) keys.add(c.getKey());

                if (keys.isEmpty()) {
                    showEmpty();
                    return;
                }
                renderPosts(keys);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { showEmpty(); }
        });
    }

    /** 저장된 key 목록에 해당하는 게시글을 posts 에서 1회 읽어 그린다. */
    private void renderPosts(List<String> keys) {
        FirebaseHelper.get().posts().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot postsSnap) {
                listContainer.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(WishlistActivity.this);
                int shown = 0;
                // 저장 역순(최근 저장 우선)
                for (int i = keys.size() - 1; i >= 0; i--) {
                    String key = keys.get(i);
                    DataSnapshot child = postsSnap.child(key);
                    Post post = child.getValue(Post.class);
                    if (post == null) continue;   // 삭제된 글은 건너뜀
                    addPostView(inflater, key, post);
                    shown++;
                }
                if (shown == 0) showEmpty();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { showEmpty(); }
        });
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
            intent.putExtra(CommunityListActivity.EXTRA_POST_KEY, postKey);
            startActivity(intent);
        });

        listContainer.addView(item);
    }

    private void showEmpty() {
        listContainer.removeAllViews();
        TextView empty = new TextView(this);
        empty.setText("저장한 게시글이 없습니다.\n게시글에서 '🔖 저장'을 눌러보세요!");
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(48, 80, 48, 48);
        empty.setTextColor(ContextCompat.getColor(this, R.color.gray_400));
        empty.setTextSize(13);
        listContainer.addView(empty);
    }
}
