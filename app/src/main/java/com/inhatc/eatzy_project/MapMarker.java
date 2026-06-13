package com.inhatc.eatzy_project;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

/**
 * Eatzy 로고("E")를 넣은 파란색 핀 모양 커스텀 마커를 그려서 BitmapDescriptor 로 돌려준다.
 * 모든 지도 화면(식당/게시글/위치선택)이 동일 마커를 쓰도록 한 곳에 모았다.
 */
public final class MapMarker {

    private MapMarker() { }

    /** 흰 테두리의 파란 원형 핀 + 가운데 흰색 "E" 로고. */
    public static BitmapDescriptor blueLogo(Context ctx) {
        float d = ctx.getResources().getDisplayMetrics().density;
        int w = Math.round(40 * d);
        int h = Math.round(52 * d);   // 아래쪽 뾰족한 꼬리 포함

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        int blue = ContextCompat.getColor(ctx, R.color.primary);
        float cx = w / 2f;
        float r = w / 2f - 2 * d;     // 흰 테두리 여백 2dp
        float cy = r + 2 * d;

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 꼬리(아래 삼각형) — 흰 테두리 → 파란 채움
        Path tail = new Path();
        tail.moveTo(cx - 8 * d, cy);
        tail.lineTo(cx + 8 * d, cy);
        tail.lineTo(cx, h);
        tail.close();
        p.setColor(Color.WHITE);
        c.drawPath(tail, p);

        // 흰 테두리 원
        p.setColor(Color.WHITE);
        c.drawCircle(cx, cy, r, p);
        // 파란 원
        p.setColor(blue);
        c.drawCircle(cx, cy, r - 3 * d, p);

        // 꼬리 안쪽 파란 채움(테두리 살짝 남기기)
        Path tailIn = new Path();
        tailIn.moveTo(cx - 5.5f * d, cy);
        tailIn.lineTo(cx + 5.5f * d, cy);
        tailIn.lineTo(cx, h - 3 * d);
        tailIn.close();
        c.drawPath(tailIn, p);

        // 가운데 "E" 로고
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(Color.WHITE);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setTextSize(20 * d);
        try {
            Typeface outfit = ResourcesCompat.getFont(ctx, R.font.outfit);
            tp.setTypeface(Typeface.create(outfit, Typeface.BOLD));
        } catch (Exception e) {
            tp.setFakeBoldText(true);
        }
        Rect b = new Rect();
        tp.getTextBounds("E", 0, 1, b);
        c.drawText("E", cx, cy + b.height() / 2f, tp);

        return BitmapDescriptorFactory.fromBitmap(bmp);
    }
}
