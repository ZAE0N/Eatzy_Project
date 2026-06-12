package com.inhatc.eatzy_project;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * 시스템 바(상태바/네비게이션 바) 인셋을 화면 루트 뷰에 패딩으로 적용하는 공통 헬퍼.
 *
 * targetSdk 35+ 부터는 edge-to-edge 가 강제돼 콘텐츠가 시스템 바 뒤로 그려진다.
 * 인셋 처리를 안 하면 상단바가 상태바에, 하단 버튼이 네비게이션 바에 가려진다.
 * 모든 액티비티가 setContentView 직후 {@link #apply(Activity)} 를 호출해 겹침을 막는다.
 *
 * 레이아웃 루트 뷰 자체에 패딩을 주므로(루트의 배경은 화면 끝까지 유지),
 * 각 레이아웃에 별도 id 를 추가할 필요가 없다.
 */
public final class UiInsets {

    private UiInsets() {}

    public static void apply(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;
        View root = content.getChildAt(0);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
