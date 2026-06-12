package com.inhatc.eatzy_project;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.recaptcha.RecaptchaAction;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class CommunityListActivity extends AppCompatActivity {

    public static final String EXTRA_POST_KEY = "post_key";

    private LinearLayout listContainer;
    private String userKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.community_list);
        UiInsets.apply(this);

        userKey = new SessionManager(this).getCurrentUserKey();
        listContainer = findViewById(R.id.list_container);

        TextView btnBack = findViewById(R.id.btn_back);
        TextView btnWrite = findViewById(R.id.btn_write);

        btnBack.setOnClickListener(v -> finish());
        btnWrite.setOnClickListener(v -> showWriteDialog());

        loadPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 상세에서 댓글을 달거나 돌아왔을 때 목록 갱신
        loadPosts();
    }

    /** posts 시딩 → 최신순으로 목록 동적 생성. */
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

                for (DataSnapshot child : children) {
                    Post post = child.getValue(Post.class);
                    if (post == null) continue;
                    addPostView(inflater, child.getKey(), post);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CommunityListActivity.this,
                        "게시글을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void addPostView(LayoutInflater inflater, String postKey, Post post) {
        View item = inflater.inflate(R.layout.item_post, listContainer, false);

        String author = TextUtils.isEmpty(post.author) ? "익명" : post.author;
        ((TextView) item.findViewById(R.id.post_avatar)).setText(author.substring(0, 1));
        ((TextView) item.findViewById(R.id.post_author)).setText(author);
        ((TextView) item.findViewById(R.id.post_date)).setText(TimeUtil.relative(post.createdAt));
        ((TextView) item.findViewById(R.id.post_title)).setText(post.title);
        ((TextView) item.findViewById(R.id.post_preview)).setText(post.content);

        item.findViewById(R.id.post_card).setOnClickListener(v -> {
            Intent intent = new Intent(this, CommunityDetailActivity.class);
            intent.putExtra(EXTRA_POST_KEY, postKey);
            startActivity(intent);
        });

        listContainer.addView(item);
    }

    /** 제목 + 내용을 입력받아 새 게시글을 posts 에 push. */
    private void showWriteDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);

        EditText etTitle = new EditText(this);
        etTitle.setHint("제목");
        etTitle.setSingleLine(true);
        box.addView(etTitle);

        EditText etContent = new EditText(this);
        etContent.setHint("내용을 입력하세요");
        etContent.setMinLines(3);
        etContent.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        box.addView(etContent);

        new AlertDialog.Builder(this)
                .setTitle("새 글 쓰기")
                .setView(box)
                .setPositiveButton("등록", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    String content = etContent.getText().toString().trim();
                    if (title.isEmpty() || content.isEmpty()) {
                        Toast.makeText(this, "제목과 내용을 입력하세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 봇 방지: reCAPTCHA 통과해야만 글 등록 (매크로 도배 차단)
                    RecaptchaHelper.verify(this, RecaptchaAction.custom("post_write"), pass -> {
                        if (!pass) {
                            Toast.makeText(this, "봇 검증에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        submitPost(title, content);
                    });
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /** reCAPTCHA 통과 후 실제 게시글 등록. */
    private void submitPost(String title, String content) {
        Post post = new Post(title, content, userKey);
        FirebaseHelper.get().posts().push().setValue(post)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "게시글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                    loadPosts();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "등록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
