package com.inhatc.eatzy_project;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.recaptcha.RecaptchaAction;

/** 제목·내용·이미지·위치를 입력해 새 게시글을 작성하는 화면. */
public class CommunityWriteActivity extends AppCompatActivity {

    private android.widget.EditText etTitle, etContent;
    private ImageView imgPreview;
    private TextView txtPlace;
    private String userKey;

    private Uri imageUri;          // 선택한 이미지(없으면 null)
    private double lat, lng;       // 등록 위치(0이면 없음)
    private String placeName;      // 위치 이름/주소

    // 갤러리 이미지 선택 런처 (OpenDocument: 재실행 후에도 읽도록 영구 권한 확보 가능)
    private final ActivityResultLauncher<String[]> pickImage =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) { }
                imageUri = uri;
                imgPreview.setImageURI(uri);
                imgPreview.setVisibility(android.view.View.VISIBLE);
            });

    // 위치 지정 화면 결과 수신
    private final ActivityResultLauncher<Intent> pickLocation =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                Intent data = result.getData();
                lat = data.getDoubleExtra(LocationPickerActivity.EXTRA_LAT, 0);
                lng = data.getDoubleExtra(LocationPickerActivity.EXTRA_LNG, 0);
                placeName = data.getStringExtra(LocationPickerActivity.EXTRA_PLACE);
                txtPlace.setText("📍 " + (placeName == null ? "등록됨" : placeName));
                txtPlace.setVisibility(android.view.View.VISIBLE);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.community_write);
        UiInsets.apply(this);

        userKey = new SessionManager(this).getCurrentUserKey();

        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        imgPreview = findViewById(R.id.img_preview);
        txtPlace = findViewById(R.id.txt_place);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_add_image).setOnClickListener(v -> pickImage.launch(new String[]{"image/*"}));
        findViewById(R.id.btn_add_location).setOnClickListener(v ->
                pickLocation.launch(new Intent(this, LocationPickerActivity.class)));
        findViewById(R.id.btn_submit).setOnClickListener(v -> trySubmit());
    }

    /** 입력 검증 → reCAPTCHA → 등록. */
    private void trySubmit() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        // 봇 방지: reCAPTCHA 통과해야만 등록 (매크로 도배 차단)
        RecaptchaHelper.verify(this, RecaptchaAction.custom("post_write"), pass -> {
            if (!pass) {
                Toast.makeText(this, "봇 검증에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            submitPost(title, content);
        });
    }

    private void submitPost(String title, String content) {
        Post post = new Post(title, content, userKey);
        if (imageUri != null) post.imageUri = imageUri.toString();
        if (lat != 0 || lng != 0) {
            post.lat = lat;
            post.lng = lng;
            post.placeName = placeName;
        }
        FirebaseHelper.get().posts().push().setValue(post)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "게시글이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "등록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
