package com.inhatc.eatzy_project;

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
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class CommunityDetailActivity extends AppCompatActivity {

    private String postKey;
    private String userKey;

    private TextView postTitle, postAvatar, postMeta, postContent, commentCount;
    private LinearLayout commentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.community_detail);
        UiInsets.apply(this);

        postKey = getIntent().getStringExtra(CommunityListActivity.EXTRA_POST_KEY);
        userKey = new SessionManager(this).getCurrentUserKey();

        TextView btnBack = findViewById(R.id.btn_back);
        EditText etComment = findViewById(R.id.et_comment);
        TextView btnSend = findViewById(R.id.btn_send);

        postTitle = findViewById(R.id.post_title);
        postAvatar = findViewById(R.id.post_avatar);
        postMeta = findViewById(R.id.post_meta);
        postContent = findViewById(R.id.post_content);
        commentCount = findViewById(R.id.comment_count);
        commentContainer = findViewById(R.id.comment_container);

        btnBack.setOnClickListener(v -> finish());

        if (TextUtils.isEmpty(postKey)) {
            Toast.makeText(this, "게시글을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPost();
        loadComments();

        btnSend.setOnClickListener(v -> {
            String text = etComment.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "댓글을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            sendComment(text, etComment);
        });
    }

    private DatabaseReference postRef() {
        return FirebaseHelper.get().posts().child(postKey);
    }

    /** 게시글 본문 로드. */
    private void loadPost() {
        postRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Post post = snapshot.getValue(Post.class);
                if (post == null) return;
                String author = TextUtils.isEmpty(post.author) ? "익명" : post.author;
                postTitle.setText(post.title);
                postContent.setText(post.content);
                postAvatar.setText(author.substring(0, 1));
                postMeta.setText(author + " · " + TimeUtil.relative(post.createdAt));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    /** 댓글 목록 로드(오래된→최신 순) + 개수 표시. */
    private void loadComments() {
        postRef().child("comments").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentContainer.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(CommunityDetailActivity.this);
                long count = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Comment comment = child.getValue(Comment.class);
                    if (comment == null) continue;
                    addCommentView(inflater, comment);
                    count++;
                }
                commentCount.setText("댓글 " + count + "개");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void addCommentView(LayoutInflater inflater, Comment comment) {
        View item = inflater.inflate(R.layout.item_comment, commentContainer, false);
        String author = TextUtils.isEmpty(comment.author) ? "익명" : comment.author;
        ((TextView) item.findViewById(R.id.c_avatar)).setText(author.substring(0, 1));
        ((TextView) item.findViewById(R.id.c_author))
                .setText(author + "  " + TimeUtil.relative(comment.createdAt));
        ((TextView) item.findViewById(R.id.c_text)).setText(comment.text);
        commentContainer.addView(item);
    }

    /** 댓글을 posts/{postKey}/comments 에 push 후 목록 갱신. */
    private void sendComment(String text, EditText input) {
        Comment comment = new Comment(userKey, text);
        postRef().child("comments").push().setValue(comment)
                .addOnSuccessListener(a -> {
                    input.setText("");
                    Toast.makeText(this, "댓글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                    loadComments();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "등록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
