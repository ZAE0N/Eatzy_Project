package com.inhatc.eatzy_project;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

public class CommunityDetailActivity extends AppCompatActivity {

    private String postKey;
    private String userKey;

    private TextView postTitle, postAvatar, postMeta, postContent, commentCount;
    private TextView btnLike, btnSave, btnReport, postPlace, btnMap;
    private ImageView postImage;
    private LinearLayout commentContainer, locRow;

    // 작성 후 이 기간이 지나면 좋아요 불가
    private static final long LIKE_WINDOW_MS = 10L * 24 * 60 * 60 * 1000; // 10일

    private boolean liked = false;   // 현재 사용자가 이 글을 좋아요했는지(DB 기준)
    private boolean saved = false;   // 위시리스트 저장 여부(DB 기준)
    private int currentLikes = 0;    // 화면에 표시 중인 좋아요 수
    private long postCreatedAt = 0;  // 게시글 작성 시각(좋아요 기간 제한 판정)

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
        postImage = findViewById(R.id.post_image);
        locRow = findViewById(R.id.loc_row);
        postPlace = findViewById(R.id.post_place);
        btnMap = findViewById(R.id.btn_map);
        btnLike = findViewById(R.id.btn_like);
        btnSave = findViewById(R.id.btn_save);
        btnReport = findViewById(R.id.btn_report);

        btnBack.setOnClickListener(v -> finish());
        btnLike.setOnClickListener(v -> toggleLike());
        btnSave.setOnClickListener(v -> toggleSave());
        btnReport.setOnClickListener(v ->
                Toast.makeText(this, "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show());

        if (TextUtils.isEmpty(postKey)) {
            Toast.makeText(this, "게시글을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPost();
        loadLikeState();
        loadSaveState();
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
                currentLikes = post.likes;
                postCreatedAt = post.createdAt;
                renderLike();

                // 첨부 이미지
                if (!TextUtils.isEmpty(post.imageUri)) {
                    try {
                        postImage.setImageURI(Uri.parse(post.imageUri));
                        postImage.setVisibility(View.VISIBLE);
                    } catch (Exception ignored) { }
                }

                // 위치 + 지도 버튼
                if (post.hasLocation()) {
                    postPlace.setText("📍 " + (TextUtils.isEmpty(post.placeName) ? "등록 위치" : post.placeName));
                    locRow.setVisibility(View.VISIBLE);
                    btnMap.setOnClickListener(v -> {
                        Intent i = new Intent(CommunityDetailActivity.this, PostMapActivity.class);
                        i.putExtra(PostMapActivity.EXTRA_LAT, post.lat);
                        i.putExtra(PostMapActivity.EXTRA_LNG, post.lng);
                        i.putExtra(PostMapActivity.EXTRA_PLACE, post.placeName);
                        startActivity(i);
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    /** 현재 사용자의 좋아요 여부를 DB(posts/{key}/likedBy/{userKey})에서 읽어 버튼 상태 반영. */
    private void loadLikeState() {
        postRef().child("likedBy").child(userKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                liked = snapshot.exists();
                renderLike();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    /**
     * 좋아요 토글. 사용자당 1회만 반영되도록 likedBy/{userKey} 로 중복을 막는다.
     * 누르면 좋아요(+1, 표시), 다시 누르면 취소(-1). likes 증감은 트랜잭션으로 안전 처리.
     */
    private void toggleLike() {
        // 게스트는 좋아요 불가
        if (SessionManager.GUEST_KEY.equals(userKey)) {
            Toast.makeText(this, "게스트는 좋아요를 누를 수 없습니다. 로그인 후 이용하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        // 작성 10일이 지난 글은 좋아요 불가(취소도 막음 — 기간 지나면 고정)
        if (postCreatedAt > 0 && System.currentTimeMillis() - postCreatedAt > LIKE_WINDOW_MS) {
            Toast.makeText(this, "작성 10일이 지난 게시물에는 좋아요를 누를 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        final boolean add = !liked;
        // 사용자별 좋아요 표식 먼저 갱신(중복 방지의 근거)
        if (add) postRef().child("likedBy").child(userKey).setValue(true);
        else postRef().child("likedBy").child(userKey).removeValue();

        postRef().child("likes").runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer cur = currentData.getValue(Integer.class);
                int next = (cur == null ? 0 : cur) + (add ? 1 : -1);
                currentData.setValue(Math.max(0, next));
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot snapshot) {
                if (error != null || !committed || snapshot == null) return;
                liked = add;
                Integer val = snapshot.getValue(Integer.class);
                currentLikes = (val == null ? 0 : val);
                renderLike();
            }
        });
    }

    private void renderLike() {
        btnLike.setText((liked ? "❤️ " : "🤍 ") + currentLikes);
    }

    /** 위시리스트 저장 여부를 DB 에서 읽어 버튼 상태 반영. */
    private void loadSaveState() {
        FirebaseHelper.get().wishlist(userKey).child(postKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                saved = snapshot.exists();
                renderSave();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    /** 저장 토글: wishlists/{userKey}/{postKey} 에 추가/삭제. */
    private void toggleSave() {
        saved = !saved;
        if (saved) {
            FirebaseHelper.get().wishlist(userKey).child(postKey).setValue(true);
            Toast.makeText(this, "위시리스트에 저장했습니다.", Toast.LENGTH_SHORT).show();
        } else {
            FirebaseHelper.get().wishlist(userKey).child(postKey).removeValue();
            Toast.makeText(this, "위시리스트에서 삭제했습니다.", Toast.LENGTH_SHORT).show();
        }
        renderSave();
    }

    private void renderSave() {
        btnSave.setText(saved ? "✅ 저장됨" : "🔖 저장");
        btnSave.setTextColor(ContextCompat.getColor(this, saved ? R.color.primary : R.color.gray_700));
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
