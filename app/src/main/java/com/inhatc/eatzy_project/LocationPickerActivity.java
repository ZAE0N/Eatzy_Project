package com.inhatc.eatzy_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 위치 지정 화면: 현재 위치를 기준으로 시작하고,
 * (1) 지도를 탭해 마커를 찍거나 (2) 주소를 검색하면 그 위치로 이동/선택한다.
 * 확정 시 lat/lng/placeName 을 결과로 돌려준다.
 */
public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LNG = "lng";
    public static final String EXTRA_PLACE = "place";

    // 위치를 못 잡을 때 폴백(인하공업전문대학)
    private static final LatLng DEFAULT_LOCATION = new LatLng(37.4516, 126.7015);
    private static final int REQ_LOCATION = 3000;
    // 탭한 지점이 이 거리(m) 안의 식당이면 식당명을 라벨로 사용
    private static final double SNAP_METERS = 40;

    // 식당명 매칭용 캐시(Firebase 1회 로드)
    private final List<Restaurant> restaurants = new ArrayList<>();

    private GoogleMap map;
    private FusedLocationProviderClient fusedClient;
    private Marker marker;
    private EditText etAddress;
    private TextView txtSelected;

    private double selLat, selLng;
    private String selPlace;
    private boolean hasSelection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.location_picker);
        UiInsets.apply(this);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        etAddress = findViewById(R.id.et_address);
        txtSelected = findViewById(R.id.txt_selected);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_search).setOnClickListener(v -> searchAddress());
        etAddress.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchAddress();
                return true;
            }
            return false;
        });
        findViewById(R.id.btn_confirm).setOnClickListener(v -> confirm());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true); // +/- 줌 버튼(핀치 줌은 기본 동작)
        // 지도 탭 → 그 지점을 위치로 선택
        map.setOnMapClickListener(latLng -> select(latLng, null, false));
        loadRestaurants();
        moveToCurrentLocation();
    }

    /** 식당명 매칭에 쓸 식당 목록을 1회 로드. */
    private void loadRestaurants() {
        SampleData.seedRestaurantsIfEmpty(() ->
                FirebaseHelper.get().restaurants().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                restaurants.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Restaurant r = child.getValue(Restaurant.class);
                    if (r != null) restaurants.add(r);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        }));
    }

    /** 탭한 지점 근처(SNAP_METERS 이내) 가장 가까운 식당명. 없으면 null. */
    private String nearbyRestaurantName(double lat, double lng) {
        String best = null;
        double bestDist = SNAP_METERS;
        float[] out = new float[1];
        for (Restaurant r : restaurants) {
            Location.distanceBetween(lat, lng, r.lat, r.lng, out);
            if (out[0] <= bestDist) {
                bestDist = out[0];
                best = r.name;
            }
        }
        return best;
    }

    /** 현재 위치를 지도 중심으로(권한 없으면 요청, 실패 시 폴백). */
    private void moveToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            centerOnly(DEFAULT_LOCATION);
            return;
        }
        fetchCurrentLocation();
    }

    @SuppressLint("MissingPermission") // 호출 전 권한 확인함
    private void fetchCurrentLocation() {
        map.setMyLocationEnabled(true);
        fusedClient.getLastLocation()
                .addOnSuccessListener(loc -> centerOnly(loc != null
                        ? new LatLng(loc.getLatitude(), loc.getLongitude())
                        : DEFAULT_LOCATION))
                .addOnFailureListener(e -> centerOnly(DEFAULT_LOCATION));
    }

    /** 마커 없이 카메라만 이동(초기 기준점). */
    private void centerOnly(LatLng center) {
        if (map != null) map.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 16f));
    }

    /** 주소 입력 → 지오코딩 → 그 위치로 이동/선택. */
    private void searchAddress() {
        String query = etAddress.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "주소를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Geocoder geocoder = new Geocoder(this, Locale.KOREA);
            List<Address> list = geocoder.getFromLocationName(query, 1);
            if (list == null || list.isEmpty()) {
                Toast.makeText(this, "주소를 찾지 못했습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            Address a = list.get(0);
            LatLng pos = new LatLng(a.getLatitude(), a.getLongitude());
            String label = a.getAddressLine(0);
            select(pos, TextUtils.isEmpty(label) ? query : label, true);
        } catch (Exception e) {
            Toast.makeText(this, "주소 검색 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** 위치 선택: 마커 표시 + 카메라 이동 + 주소 라벨 갱신. */
    private void select(LatLng pos, String label, boolean animate) {
        selLat = pos.latitude;
        selLng = pos.longitude;
        hasSelection = true;

        if (marker == null) {
            marker = map.addMarker(new MarkerOptions().position(pos)
                    .icon(MapMarker.blueLogo(this)));
        } else {
            marker.setPosition(pos);
        }
        if (animate) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 17f));
        }
        if (!TextUtils.isEmpty(label)) {
            // 주소 검색 등에서 라벨이 넘어온 경우
            selPlace = label;
        } else {
            // 지도 탭: 근처에 식당이 있으면 식당명, 없으면 역지오코딩 주소
            String rest = nearbyRestaurantName(selLat, selLng);
            selPlace = rest != null ? rest : reverseGeocode(selLat, selLng);
        }
        txtSelected.setText("📍 " + selPlace);
    }

    /** 좌표 → 주소(실패 시 좌표 문자열). */
    private String reverseGeocode(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.KOREA);
            List<Address> list = geocoder.getFromLocation(lat, lng, 1);
            if (list != null && !list.isEmpty()) {
                String line = list.get(0).getAddressLine(0);
                if (!TextUtils.isEmpty(line)) return line;
            }
        } catch (Exception ignored) { }
        return String.format(Locale.US, "%.5f, %.5f", lat, lng);
    }

    /** 선택 위치를 결과로 돌려주고 종료. */
    private void confirm() {
        if (!hasSelection) {
            Toast.makeText(this, "지도를 탭하거나 주소를 검색해 위치를 지정하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent result = new Intent();
        result.putExtra(EXTRA_LAT, selLat);
        result.putExtra(EXTRA_LNG, selLng);
        result.putExtra(EXTRA_PLACE, selPlace);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
        }
    }
}
