package com.inhatc.eatzy_project;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/** 게시글에 등록된 단일 위치를 지도에 마커로 보여주는 화면. */
public class PostMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LNG = "lng";
    public static final String EXTRA_PLACE = "place";

    private double lat, lng;
    private String place;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.post_map);
        UiInsets.apply(this);

        lat = getIntent().getDoubleExtra(EXTRA_LAT, 0);
        lng = getIntent().getDoubleExtra(EXTRA_LNG, 0);
        place = getIntent().getStringExtra(EXTRA_PLACE);

        ((TextView) findViewById(R.id.txt_place))
                .setText(TextUtils.isEmpty(place) ? "위치" : place);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        map.getUiSettings().setZoomControlsEnabled(true); // +/- 줌 버튼(핀치 줌은 기본 동작)
        LatLng pos = new LatLng(lat, lng);
        map.addMarker(new MarkerOptions().position(pos)
                .title(TextUtils.isEmpty(place) ? "등록 위치" : place)
                .icon(MapMarker.blueLogo(this)));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));
    }
}
