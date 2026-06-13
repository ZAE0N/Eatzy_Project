package com.inhatc.eatzy_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
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
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RestaurantMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    // 위치를 못 잡을 때(권한 거부, 에뮬레이터 등) 사용할 기본 좌표 (인하공업전문대학)
    private static final LatLng DEFAULT_LOCATION = new LatLng(37.4516, 126.7015);
    private static final int REQ_LOCATION = 1000;
    private static final float ZOOM = 15f;
    private static final double RADIUS_METERS = 500;     // 반경 500m
    private static final double WALK_METERS_PER_MIN = 67; // 도보 약 4km/h ≈ 분당 67m
    private static final double PLACES_RADIUS = 1500;     // 실제 식당 검색 반경(Places)

    private GoogleMap map;
    private FusedLocationProviderClient fusedClient;
    private LinearLayout listContainer;
    private TextView listTitle;
    private EditText etSearch;
    private String userKey;
    private PlacesClient placesClient;                    // 실제 식당 검색(없으면 샘플 폴백)
    private BottomSheetBehavior<View> sheetBehavior;

    // 룰렛에서 넘어온 당첨 메뉴 카테고리(예: "한식"). 없으면 전체 표시.
    private String filterCategory;

    // 카테고리/반경으로 1차 필터된 식당 + 지도 중심(검색어 필터의 원본)
    private final List<Restaurant> matchedAll = new ArrayList<>();
    private LatLng currentCenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.restaurant_map);
        UiInsets.apply(this);

        filterCategory = getIntent().getStringExtra("menu");
        userKey = new SessionManager(this).getCurrentUserKey();
        listContainer = findViewById(R.id.list_container);
        listTitle = findViewById(R.id.list_title);
        etSearch = findViewById(R.id.et_search);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 룰렛에서 카테고리가 넘어왔으면 검색 힌트로 안내
        if (!TextUtils.isEmpty(filterCategory)) {
            etSearch.setHint("🔍 '" + filterCategory + "' 식당 검색");
        }

        // 검색: 버튼 + 키보드 검색키
        findViewById(R.id.btn_search).setOnClickListener(v -> applySearch());
        etSearch.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                applySearch();
                return true;
            }
            return false;
        });

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        initPlaces();
        setupBottomSheet();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true); // +/- 줌 버튼(핀치 줌은 기본 동작)
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
        currentCenter = center;
        loadNearbyReal(center);
    }

    /** Places SDK 초기화(키는 매니페스트 meta-data 재사용). 실패하면 샘플 폴백. */
    private void initPlaces() {
        try {
            ApplicationInfo ai = getPackageManager()
                    .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            String key = (ai.metaData != null)
                    ? ai.metaData.getString("com.google.android.geo.API_KEY") : null;
            if (key != null && !Places.isInitialized()) {
                Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), key);
            }
            if (Places.isInitialized()) placesClient = Places.createClient(this);
        } catch (Throwable t) {
            placesClient = null;   // Places 미사용 → Firebase 샘플로 동작
        }
    }

    /**
     * 실제 주변 식당을 Places 로 검색해 표시.
     * Places 미초기화/오류/결과없음이면 Firebase 샘플 데이터로 폴백한다.
     */
    private void loadNearbyReal(LatLng center) {
        if (placesClient == null) {
            SampleData.seedRestaurantsIfEmpty(() -> loadFirebaseRestaurants(center));
            return;
        }
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                Place.Field.ADDRESS, Place.Field.RATING);
        SearchNearbyRequest request = SearchNearbyRequest
                .builder(CircularBounds.newInstance(center, PLACES_RADIUS), fields)
                .setIncludedTypes(Arrays.asList("restaurant"))
                .setMaxResultCount(20)
                .build();

        placesClient.searchNearby(request)
                .addOnSuccessListener(response -> {
                    List<Restaurant> list = new ArrayList<>();
                    for (Place p : response.getPlaces()) {
                        if (p.getLatLng() == null) continue;
                        double rating = (p.getRating() != null) ? p.getRating() : 0.0;
                        list.add(new Restaurant(p.getName(),
                                p.getLatLng().latitude, p.getLatLng().longitude,
                                "맛집", p.getAddress(), rating));
                    }
                    if (list.isEmpty()) {
                        SampleData.seedRestaurantsIfEmpty(() -> loadFirebaseRestaurants(center));
                        return;
                    }
                    Collections.sort(list, (a, b) ->
                            Double.compare(distanceMeters(center, a), distanceMeters(center, b)));
                    matchedAll.clear();
                    matchedAll.addAll(list);
                    renderFiltered();
                })
                .addOnFailureListener(e ->
                        SampleData.seedRestaurantsIfEmpty(() -> loadFirebaseRestaurants(center)));
    }

    /** (폴백) Firebase 에서 식당을 읽어 카테고리/반경으로 거른 뒤 지도 마커 + 리스트를 그린다. */
    private void loadFirebaseRestaurants(LatLng center) {
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

                // 검색어 필터의 원본으로 캐시 후, 현재 검색어 기준으로 렌더
                matchedAll.clear();
                matchedAll.addAll(matched);
                currentCenter = center;
                renderFiltered();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RestaurantMapActivity.this,
                        "식당 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 검색 버튼/엔터 시:
     *  - 검색어를 지오코딩해 장소가 잡히면 그 위치로 지도 중심 이동 + 그 주변 맛집 표시
     *  - 장소가 안 잡히면(식당명/메뉴 등) 현재 결과 안에서 텍스트 필터
     */
    private void applySearch() {
        String q = etSearch.getText().toString().trim();
        if (!q.isEmpty()) {
            LatLng geo = geocode(q);
            if (geo != null) {
                Toast.makeText(this, "'" + q + "' 주변 맛집을 표시합니다.", Toast.LENGTH_SHORT).show();
                etSearch.setText("");      // 위치 기준 전체 표시(텍스트 필터 해제)
                useCenter(geo);            // 카메라 이동 + 그 위치 기준 재로딩
                return;
            }
        }
        renderFiltered();                   // 지오코딩 실패/빈 검색 → 텍스트 필터
    }

    /** 바텀시트(맛집 목록) 3단계 + 단계 조절 버튼 설정. */
    private void setupBottomSheet() {
        View sheet = findViewById(R.id.bottom_sheet);
        sheetBehavior = BottomSheetBehavior.from(sheet);
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED); // 처음 디폴트(접힘)

        findViewById(R.id.btn_sheet_up).setOnClickListener(v -> stepSheet(+1));
        findViewById(R.id.btn_sheet_down).setOnClickListener(v -> stepSheet(-1));
    }

    /** +1: 접힘→중간→최상단, -1: 최상단→중간→접힘. */
    private void stepSheet(int dir) {
        int state = sheetBehavior.getState();
        if (dir > 0) {
            if (state == BottomSheetBehavior.STATE_COLLAPSED)
                sheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
            else
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            if (state == BottomSheetBehavior.STATE_EXPANDED)
                sheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
            else
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    /** 검색어를 좌표로 변환. 실패하면 null. */
    private LatLng geocode(String query) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.KOREA);
            List<Address> list = geocoder.getFromLocationName(query, 1);
            if (list != null && !list.isEmpty()) {
                Address a = list.get(0);
                return new LatLng(a.getLatitude(), a.getLongitude());
            }
        } catch (Exception ignored) { }
        return null;
    }

    /** 검색어(식당명/카테고리/주소 부분일치)로 거른 결과를 마커+리스트에 반영. */
    private void renderFiltered() {
        if (currentCenter == null) return;
        String q = etSearch.getText().toString().trim().toLowerCase();

        List<Restaurant> filtered = new ArrayList<>();
        for (Restaurant r : matchedAll) {
            if (q.isEmpty() || matchesQuery(r, q)) filtered.add(r);
        }
        renderMarkers(filtered);
        renderList(currentCenter, filtered);
        listTitle.setText("주변 맛집 " + filtered.size() + "곳");
    }

    private boolean matchesQuery(Restaurant r, String q) {
        return (r.name != null && r.name.toLowerCase().contains(q))
                || (r.menuCategory != null && r.menuCategory.toLowerCase().contains(q))
                || (r.address != null && r.address.toLowerCase().contains(q));
    }

    /** 거른 식당들을 지도에 마커로 찍는다. */
    private void renderMarkers(List<Restaurant> restaurants) {
        if (map == null) return;
        map.clear();
        for (Restaurant r : restaurants) {
            map.addMarker(new MarkerOptions()
                    .position(new LatLng(r.lat, r.lng))
                    .title(r.name)
                    .snippet(r.menuCategory + " · ⭐ " + r.rating)
                    .icon(MapMarker.blueLogo(this)));
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

            // 🎯 룰렛: 이 맛집을 룰렛 후보로 추가
            item.findViewById(R.id.rest_add_roulette)
                    .setOnClickListener(v -> addToRoulette(r));

            listContainer.addView(item);
        }
    }

    /** 이 맛집을 사용자 룰렛 후보(rouletteItems)에 추가. 룰렛판에 맛집명으로 노출된다. */
    private void addToRoulette(Restaurant r) {
        RouletteItem item = new RouletteItem(r.name, r.menuCategory);
        FirebaseHelper.get().rouletteItems(userKey).push().setValue(item)
                .addOnSuccessListener(a -> Toast.makeText(this,
                        "'" + r.name + "' 을(를) 룰렛에 추가했어요!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this,
                        "추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
