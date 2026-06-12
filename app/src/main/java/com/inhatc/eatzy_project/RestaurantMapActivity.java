package com.inhatc.eatzy_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RestaurantMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    // 위치를 못 잡을 때(권한 거부, 에뮬레이터 등) 사용할 기본 좌표 (인하공업전문대학)
    private static final LatLng DEFAULT_LOCATION = new LatLng(37.4516, 126.7015);
    private static final int REQ_LOCATION = 1000;
    private static final float ZOOM = 15f;
    private static final double RADIUS_METERS = 500;     // 반경 500m
    private static final double WALK_METERS_PER_MIN = 67; // 도보 약 4km/h ≈ 분당 67m

    private GoogleMap map;
    private FusedLocationProviderClient fusedClient;
    private LinearLayout listContainer;

    // 룰렛에서 넘어온 당첨 메뉴 카테고리(예: "한식"). 없으면 전체 표시.
    private String filterCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.restaurant_map);
        UiInsets.apply(this);

        filterCategory = getIntent().getStringExtra("menu");
        listContainer = findViewById(R.id.list_container);

        TextView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // 검색바 힌트로 현재 필터를 안내
        if (!TextUtils.isEmpty(filterCategory)) {
            ((TextView) findViewById(R.id.et_search))
                    .setHint("🔍 '" + filterCategory + "' 식당 검색");
        }

        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        // 권한이 있으면 현재 위치, 없으면 권한 요청
        if (hasLocationPermission()) {
            showCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_LOCATION);
            // 권한 응답을 기다리는 동안 일단 기본 위치를 보여준다
            useCenter(DEFAULT_LOCATION);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** 현재 위치를 가져와 지도 중심으로 사용. 위치가 null이면 기본 좌표로 폴백. */
    @SuppressLint("MissingPermission") // 호출 전에 hasLocationPermission()으로 확인함
    private void showCurrentLocation() {
        map.setMyLocationEnabled(true); // 파란 현재위치 점 표시

        fusedClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        useCenter(new LatLng(location.getLatitude(), location.getLongitude()));
                    } else {
                        // 에뮬레이터 등에서 위치가 없으면 인하공전으로
                        Toast.makeText(this, "현재 위치를 못 찾아 기본 위치로 표시합니다.", Toast.LENGTH_SHORT).show();
                        useCenter(DEFAULT_LOCATION);
                    }
                })
                .addOnFailureListener(e -> useCenter(DEFAULT_LOCATION));
    }

    /** 지도 중심을 옮기고, 그 중심을 기준으로 식당 데이터를 불러온다. */
    private void useCenter(LatLng center) {
        if (map == null) return;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, ZOOM));
        // 샘플 데이터가 없으면 1회 심은 뒤 로드
        SampleData.seedRestaurantsIfEmpty(() -> loadRestaurants(center));
    }

    /** Firebase 에서 식당을 읽어 카테고리/반경으로 거른 뒤 지도 마커 + 리스트를 그린다. */
    private void loadRestaurants(LatLng center) {
        FirebaseHelper.get().restaurants().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Restaurant> matched = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Restaurant r = child.getValue(Restaurant.class);
                    if (r == null) continue;

                    // 1) 카테고리 필터 (룰렛에서 넘어온 메뉴가 있을 때만)
                    if (!TextUtils.isEmpty(filterCategory)
                            && !filterCategory.equals(r.menuCategory)) {
                        continue;
                    }
                    // 2) 반경 500m 필터
                    if (distanceMeters(center, r) > RADIUS_METERS) continue;

                    matched.add(r);
                }
                // 가까운 순으로 정렬
                Collections.sort(matched, (a, b) ->
                        Double.compare(distanceMeters(center, a), distanceMeters(center, b)));

                renderMarkers(matched);
                renderList(center, matched);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RestaurantMapActivity.this,
                        "식당 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** 거른 식당들을 지도에 마커로 찍는다. */
    private void renderMarkers(List<Restaurant> restaurants) {
        if (map == null) return;
        map.clear();
        for (Restaurant r : restaurants) {
            map.addMarker(new MarkerOptions()
                    .position(new LatLng(r.lat, r.lng))
                    .title(r.name)
                    .snippet(r.menuCategory + " · ⭐ " + r.rating));
        }
    }

    /** 거른 식당들을 아래 리스트로 동적 생성한다. */
    private void renderList(LatLng center, List<Restaurant> restaurants) {
        listContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (restaurants.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(TextUtils.isEmpty(filterCategory)
                    ? "주변 500m 내 식당이 없습니다."
                    : "주변 500m 내 '" + filterCategory + "' 식당이 없습니다.");
            empty.setPadding(48, 48, 48, 48);
            empty.setTextColor(ContextCompat.getColor(this, R.color.gray_400));
            listContainer.addView(empty);
            return;
        }

        for (Restaurant r : restaurants) {
            View item = inflater.inflate(R.layout.item_restaurant, listContainer, false);

            int meters = (int) Math.round(distanceMeters(center, r));
            int walkMin = Math.max(1, (int) Math.ceil(meters / WALK_METERS_PER_MIN));

            ((TextView) item.findViewById(R.id.rest_thumb)).setText(categoryEmoji(r.menuCategory));
            ((TextView) item.findViewById(R.id.rest_name))
                    .setText(r.name + "  " + r.menuCategory);
            ((TextView) item.findViewById(R.id.rest_meta1))
                    .setText("⭐ " + r.rating + " · 도보 " + walkMin + "분 (" + meters + "m)");
            ((TextView) item.findViewById(R.id.rest_meta2)).setText(r.address);

            // 항목 클릭 시 해당 마커로 카메라 이동
            item.setOnClickListener(v ->
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(r.lat, r.lng), 17f)));

            listContainer.addView(item);
        }
    }

    /** 두 지점 사이 직선 거리(m). android.location.Location 의 정확한 계산을 사용. */
    private double distanceMeters(LatLng center, Restaurant r) {
        float[] result = new float[1];
        Location.distanceBetween(center.latitude, center.longitude, r.lat, r.lng, result);
        return result[0];
    }

    /** 카테고리에 맞는 썸네일 이모지. */
    private String categoryEmoji(String category) {
        if (category == null) return "🍽️";
        switch (category) {
            case "한식": return "🍚";
            case "중식": return "🍜";
            case "일식": return "🍱";
            case "양식": return "🍝";
            case "분식": return "🥟";
            case "치킨": return "🍗";
            default:     return "🍽️";
        }
    }

    /** 권한 요청 결과 처리 */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showCurrentLocation();   // 허용 → 현재 위치
            } else {
                Toast.makeText(this, "위치 권한이 없어 기본 위치로 표시합니다.", Toast.LENGTH_SHORT).show();
                // 거부 시 이미 기본 위치가 표시돼 있으므로 그대로 둔다
            }
        }
    }
}
