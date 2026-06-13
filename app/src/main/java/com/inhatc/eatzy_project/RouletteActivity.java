package com.inhatc.eatzy_project;

import android.animation.ObjectAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RouletteActivity extends AppCompatActivity {

    private static final int WEDGES = 6;   // 룰렛판 고정 6칸
    // 후보가 하나도 없을 때 쓰는 최후 폴백(기피/알레르기로 전부 걸러진 경우)
    private static final String[] FALLBACK = {"한식", "중식", "일식", "양식", "분식", "치킨"};

    /** 룰렛판 한 칸: label 은 화면 표시(카테고리 or 맛집명), filter 는 당첨 후 맛집 검색용 카테고리. */
    private static class WheelOption {
        final String label;
        final String filter;
        WheelOption(String label, String filter) { this.label = label; this.filter = filter; }
    }

    // 6칸에 표시되는 라벨/필터(후보를 순환해 채움). 칸 i 의 결과는 wheelLabels[i].
    private final String[] wheelLabels = new String[WEDGES];
    private final String[] wheelFilters = new String[WEDGES];

    private final TextView[] labelViews = new TextView[WEDGES];

    private float currentRotation = 0f;
    private final Random random = new Random();
    private boolean spinning = false;
    private boolean ready = false;       // 후보 로드 완료 전엔 스핀 금지
    private String resultMenu = null;    // 당첨 칸의 filter(맛집 검색용 카테고리)

    private FrameLayout wheelGroup;
    private Button btnSpin;
    private Button btnFind;
    private TextView txtResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.roulette);
        UiInsets.apply(this);

        wheelGroup = findViewById(R.id.wheel_group);
        btnSpin = findViewById(R.id.btn_spin);
        btnFind = findViewById(R.id.btn_find);
        txtResult = findViewById(R.id.txt_result);
        TextView btnProfile = findViewById(R.id.btn_profile);

        // 로고 "Eatzy" 의 마지막 'y' 만 포인트색으로 (HTML 시안 재현)
        TextView logo = findViewById(R.id.logo_eatzy);
        android.text.SpannableString logoText = new android.text.SpannableString("Eatzy");
        logoText.setSpan(new android.text.style.ForegroundColorSpan(getColor(R.color.roulette_primary)),
                4, 5, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        logo.setText(logoText);

        labelViews[0] = findViewById(R.id.label_0);
        labelViews[1] = findViewById(R.id.label_1);
        labelViews[2] = findViewById(R.id.label_2);
        labelViews[3] = findViewById(R.id.label_3);
        labelViews[4] = findViewById(R.id.label_4);
        labelViews[5] = findViewById(R.id.label_5);

        // 화면 폭에 맞춰 룰렛판/중심축/라벨을 비례 조정(반응형)
        scaleWheelToScreen();

        btnProfile.setOnClickListener(v ->
                Toast.makeText(this, "프로필", Toast.LENGTH_SHORT).show());

        // 사용자가 룰렛에 직접 메뉴 추가
        findViewById(R.id.btn_add_menu).setOnClickListener(v -> showAddMenuDialog());

        btnSpin.setOnClickListener(v -> spinWheel());

        // 당첨 메뉴를 들고 맛집 추천(지도)으로 이동
        btnFind.setOnClickListener(v -> {
            Intent intent = new Intent(this, RestaurantMapActivity.class);
            intent.putExtra("menu", resultMenu);
            startActivity(intent);
        });

        // 로드 전엔 스핀 비활성화 후, 후보 계산되면 활성화
        btnSpin.setEnabled(false);
        txtResult.setText("메뉴 불러오는 중…");
        loadCandidates();
    }

    /**
     * 룰렛판을 화면 폭에 비례해 리사이즈한다(반응형).
     * 원래 디자인 기준값(휠 280dp / 허브 44dp·아래여백 118dp / 라벨 패딩 38dp)을
     * 실제 휠 크기 비율(scale)로 곱해 작은 폰에선 넘치지 않고, 큰 폰에선 답답하지 않게 한다.
     */
    private void scaleWheelToScreen() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density;

        // 좌우 패딩(24dp×2)을 뺀 사용 가능 폭. 너무 커지지 않도록 360dp 로 상한.
        int sidePaddingPx = Math.round(24 * 2 * density);
        int maxPx = Math.round(360 * density);
        int sizePx = Math.min(dm.widthPixels - sidePaddingPx, maxPx);
        if (sizePx <= 0) return;   // 측정 실패 시 XML 기본값(280dp) 유지

        float baseWheelPx = 280f * density;   // 디자인 기준 휠 크기
        float scale = sizePx / baseWheelPx;

        // 회전하는 휠 그룹(룰렛판 + 라벨)
        setSize(wheelGroup, sizePx, sizePx);

        // 바깥 컨테이너: 위쪽에 화살표(▼) 공간을 둔다(기존 300-280=20dp 도 비례).
        FrameLayout container = findViewById(R.id.wheel_container);
        int headroomPx = Math.round(20 * density * scale);
        setSize(container, sizePx, sizePx + headroomPx);

        // 중심 허브: 크기·아래여백을 휠 비율로 스케일 → 항상 휠 정중앙에 위치
        View hub = findViewById(R.id.wheel_hub);
        int hubPx = Math.round(44 * density * scale);
        FrameLayout.LayoutParams hubLp = (FrameLayout.LayoutParams) hub.getLayoutParams();
        hubLp.width = hubPx;
        hubLp.height = hubPx;
        hubLp.bottomMargin = Math.round(118 * density * scale);
        hub.setLayoutParams(hubLp);

        // 라벨: 위쪽 패딩(중심에서의 반지름)을 비율로 스케일
        int labelTopPx = Math.round(38 * density * scale);
        for (TextView label : labelViews) {
            label.setPadding(0, labelTopPx, 0, 0);
        }
    }

    private static void setSize(View v, int widthPx, int heightPx) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        lp.width = widthPx;
        lp.height = heightPx;
        v.setLayoutParams(lp);
    }

    /** 사용자가 직접 메뉴(이름 + 카테고리)를 룰렛에 추가하는 다이얼로그. */
    private void showAddMenuDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);

        final android.widget.EditText etName = new android.widget.EditText(this);
        etName.setHint("메뉴 이름 (예: 마라탕)");
        etName.setSingleLine(true);
        box.addView(etName);

        // 카테고리 선택(당첨 후 맛집 검색에 사용). FALLBACK 6종 재사용.
        final android.widget.Spinner spCategory = new android.widget.Spinner(this);
        spCategory.setAdapter(new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, FALLBACK));
        box.addView(spCategory);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("룰렛에 메뉴 추가")
                .setView(box)
                .setPositiveButton("추가", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "메뉴 이름을 입력하세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String category = (String) spCategory.getSelectedItem();
                    addCustomMenu(name, category);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    /** 입력한 메뉴를 rouletteItems 에 저장하고 룰렛을 다시 구성. */
    private void addCustomMenu(String name, String category) {
        String userKey = new SessionManager(this).getCurrentUserKey();
        FirebaseHelper.get().rouletteItems(userKey).push()
                .setValue(new RouletteItem(name, category))
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "'" + name + "' 추가됨", Toast.LENGTH_SHORT).show();
                    btnSpin.setEnabled(false);
                    ready = false;
                    txtResult.setText("메뉴 불러오는 중…");
                    loadCandidates();   // 새 후보 반영해 휠 갱신
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** menus 시딩 → 메뉴/사용자 읽어 후보 카테고리 계산 → 휠 라벨 채움. */
    private void loadCandidates() {
        SampleData.seedMenusIfEmpty(() ->
                FirebaseHelper.get().menus().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot menuSnap) {
                List<Menu> menus = new ArrayList<>();
                for (DataSnapshot child : menuSnap.getChildren()) {
                    Menu m = child.getValue(Menu.class);
                    if (m != null) menus.add(m);
                }
                loadUserThenBuild(menus);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                applyCandidates(null);   // 못 읽으면 폴백
            }
        }));
    }

    private void loadUserThenBuild(List<Menu> menus) {
        String userKey = new SessionManager(this).getCurrentUserKey();
        FirebaseHelper.get().users().child(userKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnap) {
                User user = userSnap.getValue(User.class);
                loadRouletteItemsThenBuild(menus, user, userKey);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadRouletteItemsThenBuild(menus, null, userKey);
            }
        });
    }

    /** 사용자가 맛집 추천에서 추가한 맛집(rouletteItems)을 읽어 후보에 합친다. */
    private void loadRouletteItemsThenBuild(List<Menu> menus, User user, String userKey) {
        FirebaseHelper.get().rouletteItems(userKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                List<RouletteItem> items = new ArrayList<>();
                for (DataSnapshot child : snap.getChildren()) {
                    RouletteItem it = child.getValue(RouletteItem.class);
                    if (it != null && it.name != null) items.add(it);
                }
                applyCandidates(buildOptions(menus, user, items));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                applyCandidates(buildOptions(menus, user, null));
            }
        });
    }

    /** 카테고리 후보 + 사용자가 추가한 맛집을 합쳐 룰렛 칸 옵션 목록을 만든다. */
    private List<WheelOption> buildOptions(List<Menu> menus, User user, List<RouletteItem> items) {
        List<WheelOption> options = new ArrayList<>();
        // 사용자가 추가한 맛집(맛집명 표시, 카테고리로 검색)을 우선 배치
        if (items != null) {
            for (RouletteItem it : items) {
                options.add(new WheelOption(it.name,
                        it.category != null ? it.category : it.name));
            }
        }
        // 카테고리 후보(표시=검색 모두 카테고리)
        for (String cat : buildCandidates(menus, user)) {
            options.add(new WheelOption(cat, cat));
        }
        return options;
    }

    /**
     * 후보 카테고리 계산:
     *  - 사용자 선호 카테고리가 있으면 그 안에서만(없으면 전체 카테고리)
     *  - 알레르기 안전 메뉴(알레르기 성분이 하나도 안 겹치는 메뉴)가 1개 이상 있는 카테고리만
     */
    private List<String> buildCandidates(List<Menu> menus, User user) {
        List<String> prefer = (user != null) ? user.preferredCategories : null;
        List<String> allergies = (user != null) ? user.allergies : null;

        // 카테고리 등장 순서를 유지하기 위해 LinkedHashSet 사용
        Set<String> result = new LinkedHashSet<>();
        for (Menu m : menus) {
            if (m == null || m.category == null) continue;
            // 선호 카테고리 필터
            if (prefer != null && !prefer.isEmpty() && !prefer.contains(m.category)) continue;
            // 알레르기 안전 메뉴인지
            if (hasAllergen(m, allergies)) continue;
            result.add(m.category);
        }
        return new ArrayList<>(result);
    }

    /** 메뉴의 알레르기 성분 중 사용자 알레르기와 겹치는 게 하나라도 있으면 true. */
    private boolean hasAllergen(Menu m, List<String> userAllergies) {
        if (userAllergies == null || userAllergies.isEmpty()) return false;
        if (m.allergens == null) return false;
        for (String a : m.allergens) {
            if (userAllergies.contains(a)) return true;
        }
        return false;
    }

    /** 후보로 6칸 라벨/필터를 채우고 스핀을 활성화. */
    private void applyCandidates(List<WheelOption> options) {
        if (options == null || options.isEmpty()) {
            // 전부 걸러졌으면 폴백(빈 룰렛 방지)
            for (int i = 0; i < WEDGES; i++) {
                wheelLabels[i] = FALLBACK[i];
                wheelFilters[i] = FALLBACK[i];
            }
            Toast.makeText(this, "조건에 맞는 메뉴가 없어 전체 메뉴로 돌려요.", Toast.LENGTH_SHORT).show();
        } else {
            // 후보를 순환시켜 6칸을 채움 (후보가 6개 미만이어도 빈칸 없음)
            for (int i = 0; i < WEDGES; i++) {
                WheelOption o = options.get(i % options.size());
                wheelLabels[i] = o.label;
                wheelFilters[i] = o.filter;
            }
        }
        for (int i = 0; i < WEDGES; i++) {
            labelViews[i].setText(wheelLabels[i]);
        }
        ready = true;
        btnSpin.setEnabled(true);
        txtResult.setText("버튼을 눌러 메뉴를 골라보세요!");
    }

    private void spinWheel() {
        if (spinning || !ready) return;
        spinning = true;

        // 다시 돌릴 때를 위해 결과 UI 초기화
        btnFind.setVisibility(View.GONE);
        txtResult.setText("두구두구…");
        txtResult.setTextColor(getColor(R.color.gray_500));

        // 최소 4바퀴(1440도) 이상 무작위 회전
        float target = currentRotation + 1440 + random.nextInt(360) + 1;

        ObjectAnimator anim = ObjectAnimator.ofFloat(wheelGroup, "rotation", currentRotation, target);
        anim.setDuration(3000);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentRotation = target % 360;
                showResult();
                spinning = false;
            }
        });
        anim.start();
    }

    /** 핀(12시, 0도) 아래에 걸린 칸을 계산해 당첨 메뉴를 표시한다. */
    private void showResult() {
        // 룰렛판이 시계방향으로 rot 만큼 돌면, 지금 12시에 온 지점의 '원래(로컬) 각도'는 (360 - rot).
        float top = (360f - (currentRotation % 360f)) % 360f;
        int index = ((int) (top / 60f)) % WEDGES;
        resultMenu = wheelFilters[index];   // 맛집 검색용 카테고리

        txtResult.setText("🎉 오늘 점심은  " + wheelLabels[index] + " !");
        txtResult.setTextColor(getColor(R.color.text_navy));
        btnFind.setVisibility(View.VISIBLE);
    }
}
