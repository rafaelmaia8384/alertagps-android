package com.smarttools.alertagps;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.arsy.maps_library.MapRadar;
import com.github.angads25.toggle.LabeledSwitch;
import com.github.angads25.toggle.interfaces.OnToggledListener;
import com.github.pengrad.mapscaleview.MapScaleView;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.snatik.storage.Storage;

import java.io.File;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    public static final String STORAGE_NOT_FIRST_ACCESS = "not-first-access.data";

    private static final int UX_MAXDELAY_TOGGLE = 2500;
    private static final int UX_MINDELAY_TOGGLE = 500;
    private static final int UX_MAPDELAY = 1500;

    public static final String CONFIG_VIBRAR_APARELHO = "config_vibrar_aparelho.data";
    public static final String CONFIG_LOCALIZACAO_APROXIMADA = "config_localizacao_aproximada.data";
    public static final String CONFIG_MOSTRAR_ICONE = "config_mostrar_icone.data";
    public static final String CONFIG_SEEKBAR_DISTANCIA = "config_seekbar_distancia.data";

    public DialogHelper dialogHelper;
    private SupportMapFragment mapFragment;
    private DrawerLayout drawer;
    private LatLng currentLocation;
    private DisplayMetrics metrics;
    private LabeledSwitch toggleAtivo;
    private LabeledSwitch toggleVibrar;
    private LabeledSwitch toggleIconePosicao;
    private LabeledSwitch toggleLocalizacaoAproximada;
    private SeekBar seekDistancia;
    private TextView textLatitude;
    private TextView textLongitude;
    private PopupMenu pm;
    private Storage storage;
    private MapRadar mapRadar;
    private Bitmap iconMarker;
    private Marker homeMarker;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        storage = new Storage(getApplicationContext());

        if (!storage.isFileExist(storage.getInternalFilesDirectory() + File.separator + STORAGE_NOT_FIRST_ACCESS)) {

            Intent i = new Intent(MainActivity.this, IntroActivity.class);

            startActivity(i);
            finish();
        }

        setContentView(R.layout.activity_main);

        dialogHelper = new DialogHelper(MainActivity.this);

        currentLocation = new LatLng(-10.443150, -49.238243); //obter isso do GPS

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        LayoutInflater inflator = (LayoutInflater) this .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_layout, null);

        metrics = getResources().getDisplayMetrics();

        actionBar.setCustomView(v);

        getSupportActionBar().setTitle("");

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        drawer = findViewById(R.id.drawer_layout);

        final DrawerLayout finalDrawer = drawer;


        int width = (int)(metrics.density * 25f);
        int height = (int)(metrics.density * 25f);
        iconMarker = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.icon_marker_home), width, height, false);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(new OnMapReadyCallback() {

            @Override
            public void onMapReady(final GoogleMap googleMap) {

                final MapScaleView scaleView = findViewById(R.id.scaleView);

                scaleView.metersOnly();

                googleMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {

                    @Override
                    public void onCameraMove() {

                        CameraPosition cp = googleMap.getCameraPosition();

                        LatLng latLng = cp.target;

                        textLatitude.setText("Latitude: " + String.format("%.6f", latLng.latitude));
                        textLongitude.setText("Longitude: " + String.format("%.6f", latLng.longitude));

                        scaleView.update(cp.zoom, cp.target.latitude);
                    }
                });

                googleMap.getUiSettings().setScrollGesturesEnabled(true);
                googleMap.getUiSettings().setRotateGesturesEnabled(false);
                googleMap.getUiSettings().setZoomGesturesEnabled(true);
                googleMap.getUiSettings().setMapToolbarEnabled(false);

                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(MainActivity.this, R.raw.style_padrao));
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                CameraUpdate center = CameraUpdateFactory.newLatLngZoom(currentLocation, getZoomLevel(getDistanciaRadar()));
                googleMap.animateCamera(center);

                findViewById(R.id.progress).setVisibility(View.GONE);

                ((TextView)findViewById(R.id.textStatus)).setText("Aguardando novos alertas.");

                googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {

                    @Override
                    public void onMapLoaded() {

                        mapRadar = new MapRadar(googleMap, currentLocation, MainActivity.this);
                        mapRadar.withRadarColors(getResources().getColor(R.color.colorRadarBegin), getResources().getColor(R.color.colorRadarEnd));
                        mapRadar.withDistance(getDistanciaRadar());
                        mapRadar.withOuterCircleStrokewidth(0);
                        mapRadar.withOuterCircleStrokeColor(0x00000000);
                        mapRadar.withRadarSpeed(1);
                        mapRadar.startRadarAnimation();

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                hideProgress();
                            }
                        }, UX_MAPDELAY);

                        homeMarker = googleMap.addMarker(new MarkerOptions()
                                .position(currentLocation)
                                .title("Minha posição atual")
                                .draggable(false)
                                .visible(toggleIconePosicao.isOn())
                                .icon(BitmapDescriptorFactory.fromBitmap(iconMarker)));
                    }
                });
            }
        });

        toggleAtivo = findViewById(R.id.toggleAtivo);

        NavigationView navigationView = findViewById(R.id.nav_view);

        toggleVibrar = navigationView.getHeaderView(0).findViewById(R.id.toggleVibrar);
        toggleIconePosicao = navigationView.getHeaderView(0).findViewById(R.id.toggleIconePosicao);
        toggleLocalizacaoAproximada = navigationView.getHeaderView(0).findViewById(R.id.toggleLocalizacaoAproximada);
        seekDistancia = navigationView.getHeaderView(0).findViewById(R.id.seekDistancia);

        loadConfigFiles();

        textLatitude = findViewById(R.id.textLatitude);
        textLongitude = findViewById(R.id.textLongitude);

        pm = new PopupMenu(MainActivity.this, findViewById(R.id.buttonEstilos));
        pm.inflate(R.menu.menu_estilos);

        toggleAtivo.setOnToggledListener(new OnToggledListener() {

            @Override
            public void onSwitched(LabeledSwitch labeledSwitch, boolean isOn) {

                int rand = new Random().nextInt(UX_MAXDELAY_TOGGLE) + UX_MINDELAY_TOGGLE;

                if (isOn) {

                    seekDistancia.setEnabled(true);

                    ((TextView)findViewById(R.id.textStatus)).setText("Processando...");

                    showProgress();

                    mapFragment.getMapAsync(new OnMapReadyCallback() {

                        @Override
                        public void onMapReady(final GoogleMap googleMap) {

                            googleMap.clear();

                            CameraUpdate center = CameraUpdateFactory.newLatLngZoom(currentLocation, getZoomLevel(getDistanciaRadar()));
                            googleMap.animateCamera(center);

                            googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {

                                @Override
                                public void onMapLoaded() {

                                    mapRadar = new MapRadar(googleMap, currentLocation, MainActivity.this);
                                    mapRadar.withRadarColors(getResources().getColor(R.color.colorRadarBegin), getResources().getColor(R.color.colorRadarEnd));
                                    mapRadar.withDistance(getDistanciaRadar());
                                    mapRadar.withOuterCircleStrokewidth(0);
                                    mapRadar.withOuterCircleStrokeColor(0x00000000);
                                    mapRadar.withRadarSpeed(1);
                                    mapRadar.startRadarAnimation();

                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {

                                            ((TextView)findViewById(R.id.textStatus)).setText("Aguardando novos alertas.");

                                            hideProgress();
                                        }
                                    }, UX_MAPDELAY);
                                }
                            });
                        }
                    });
                }
                else {

                    seekDistancia.setEnabled(false);

                    ((TextView)findViewById(R.id.textStatus)).setText("Processando...");

                    showProgress();

                    mapRadar.stopRadarAnimation();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            ((TextView)findViewById(R.id.textStatus)).setText("Monitoramento inativo.");

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    findViewById(R.id.buttonHome).setVisibility(View.GONE);
                                    hideProgress();
                                }
                            });
                        }
                    }, rand);
                }
            }
        });

        toggleVibrar.setOnToggledListener(new OnToggledListener() {

            @Override
            public void onSwitched(LabeledSwitch labeledSwitch, boolean isOn) {

                int rand = new Random().nextInt(UX_MAXDELAY_TOGGLE) + UX_MINDELAY_TOGGLE;

                String path = storage.getInternalFilesDirectory() + File.separator + CONFIG_VIBRAR_APARELHO;
                storage.createFile(path, isOn ? "1" : "0");

                dialogHelper.showProgressDelayed(rand, null);
            }
        });

        toggleVibrar.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {

                int action = event.getAction();

                switch (action) {

                    case MotionEvent.ACTION_DOWN:

                        view.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:

                        view.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                view.onTouchEvent(event);

                return true;
            }
        });

        toggleLocalizacaoAproximada.setOnToggledListener(new OnToggledListener() {

            @Override
            public void onSwitched(LabeledSwitch labeledSwitch, boolean isOn) {

                int rand = new Random().nextInt(UX_MAXDELAY_TOGGLE) + UX_MINDELAY_TOGGLE;

                String path = storage.getInternalFilesDirectory() + File.separator + CONFIG_LOCALIZACAO_APROXIMADA;
                storage.createFile(path, isOn ? "1" : "0");

                dialogHelper.showProgressDelayed(rand, null);
            }
        });

        toggleLocalizacaoAproximada.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {

                int action = event.getAction();

                switch (action) {

                    case MotionEvent.ACTION_DOWN:

                        view.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:

                        view.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                view.onTouchEvent(event);

                return true;
            }
        });

        toggleIconePosicao.setOnToggledListener(new OnToggledListener() {

            @Override
            public void onSwitched(LabeledSwitch labeledSwitch, final boolean isOn) {

                int rand = new Random().nextInt(UX_MAXDELAY_TOGGLE) + UX_MINDELAY_TOGGLE;

                String path = storage.getInternalFilesDirectory() + File.separator + CONFIG_MOSTRAR_ICONE;
                storage.createFile(path, isOn ? "1" : "0");

                mapFragment.getMapAsync(new OnMapReadyCallback() {

                    @Override
                    public void onMapReady(GoogleMap googleMap) {

                        homeMarker.setVisible(isOn);
                    }
                });

                dialogHelper.showProgressDelayed(rand, null);
            }
        });

        toggleIconePosicao.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {

                int action = event.getAction();

                switch (action) {

                    case MotionEvent.ACTION_DOWN:

                        view.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:

                        view.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                view.onTouchEvent(event);

                return true;
            }
        });

        /*toggleMostrarSatelite.setOnToggledListener(new OnToggledListener() {

            @Override
            public void onSwitched(LabeledSwitch labeledSwitch, final boolean isOn) {

                ((TextView)findViewById(R.id.textStatus)).setText("Processando...");

                String path = storage.getInternalFilesDirectory() + File.separator + CONFIG_MOSTRAR_IMAGENS_SATELITE;
                storage.createFile(path, isOn ? "1" : "0");

                showProgress();

                if (mapRadar.isAnimationRunning()) {

                    mapRadar.stopRadarAnimation();
                }

                mapFragment.getMapAsync(new OnMapReadyCallback() {

                    @Override
                    public void onMapReady(final GoogleMap googleMap) {

                        if (isOn) {

                            googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        }
                        else {

                            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        }

                        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {

                            @Override
                            public void onMapLoaded() {

                                mapRadar = new MapRadar(googleMap, currentLocation, MainActivity.this);
                                mapRadar.withRadarColors(getResources().getColor(R.color.colorRadarBegin), getResources().getColor(R.color.colorRadarEnd));
                                mapRadar.withDistance(getDistanciaRadar());
                                mapRadar.withOuterCircleStrokewidth(0);
                                mapRadar.withOuterCircleStrokeColor(0x00000000);
                                mapRadar.withRadarSpeed(1);
                                mapRadar.startRadarAnimation();

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {

                                        ((TextView)findViewById(R.id.textStatus)).setText("Aguardando novos alertas.");

                                        hideProgress();
                                    }
                                }, UX_MAPDELAY);
                            }
                        });
                    }
                });
            }
        });*/


        seekDistancia.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {

                int action = event.getAction();

                switch (action) {

                    case MotionEvent.ACTION_DOWN:

                        view.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:

                        view.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                view.onTouchEvent(event);

                return true;
            }
        });

        seekDistancia.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                ((TextView)findViewById(R.id.textStatus)).setText("Processando...");

                String path = storage.getInternalFilesDirectory() + File.separator + CONFIG_SEEKBAR_DISTANCIA;
                storage.createFile(path, Integer.toString(seekBar.getProgress()));

                mapRadar.stopRadarAnimation();

                finalDrawer.closeDrawer(GravityCompat.START);

                showProgress();

                mapFragment.getMapAsync(new OnMapReadyCallback() {

                    @Override
                    public void onMapReady(final GoogleMap googleMap) {

                        CameraUpdate center = CameraUpdateFactory.newLatLngZoom(currentLocation, getZoomLevel(getDistanciaRadar()));

                        googleMap.animateCamera(center);

                        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {

                            @Override
                            public void onMapLoaded() {

                                mapRadar = new MapRadar(googleMap, currentLocation, MainActivity.this);
                                mapRadar.withRadarColors(getResources().getColor(R.color.colorRadarBegin), getResources().getColor(R.color.colorRadarEnd));
                                mapRadar.withDistance(getDistanciaRadar());
                                mapRadar.withOuterCircleStrokewidth(0);
                                mapRadar.withOuterCircleStrokeColor(0x00000000);
                                mapRadar.withRadarSpeed(1);
                                mapRadar.startRadarAnimation();

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {

                                        ((TextView)findViewById(R.id.textStatus)).setText("Aguardando novos alertas.");

                                        hideProgress();
                                    }
                                }, UX_MAPDELAY);
                            }
                        });
                    }
                });
            }
        });

        showProgress();
    }

    public void buttonHome(View view) {

        mapFragment.getMapAsync(new OnMapReadyCallback() {

            @Override
            public void onMapReady(GoogleMap googleMap) {

                CameraUpdate center = CameraUpdateFactory.newLatLngZoom(currentLocation, getZoomLevel(getDistanciaRadar()));
                
                googleMap.animateCamera(center);

                ((TextView)findViewById(R.id.textStatus)).setText("Aguardando novos alertas.");

                googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {

                    @Override
                    public void onMapLoaded() {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                hideProgress();
                            }
                        }, UX_MAPDELAY);
                    }
                });
            }
        });
    }

    public void buttonEstilos(View view) {

        pm.show();
    }

    public void menuMapaEstilo(MenuItem item) {

        final int style;

        switch (item.getOrder()) {

            case 1:

                style = R.raw.style_alto_contraste;

                break;

            case 2:

                style = R.raw.style_deserto;

                break;

            case 3:

                style = R.raw.style_essencia_branca;

                break;

            case 4:

                style = R.raw.style_geleira;

                break;

            case 5:

                style = R.raw.style_meia_noite;

                break;

            case 6:

                style = R.raw.style_mundo_verde;

                break;

            case 7:

                style = R.raw.style_padrao;

                break;

            case 8:

                style = R.raw.style_redes_conectadas;

                break;

            case 9:

                style = 0;

                break;

            case 10:

                style = R.raw.style_tons_de_cinza;

                break;

            case 11:

                style = R.raw.style_vintage;

                break;

            case 12:

                style = R.raw.style_visao_noturna;

                break;

            default:

                return;
        }

        mapFragment.getMapAsync(new OnMapReadyCallback() {

            @Override
            public void onMapReady(GoogleMap googleMap) {

                if (style != 0) {

                    googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(MainActivity.this, style));
                }
                else {

                    googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START)) {

            drawer.closeDrawer(GravityCompat.START);
        }
        else {

            super.onBackPressed();
        }
    }

    private void showProgress() {

        findViewById(R.id.progress).setVisibility(View.VISIBLE);
    }

    private void hideProgress() {

        findViewById(R.id.progress).setVisibility(View.GONE);
    }

    private void loadConfigFiles() {

        String path;

        path = storage.getInternalFilesDirectory() + File.separator + CONFIG_VIBRAR_APARELHO;

        if (storage.isFileExist(path)) {

            toggleVibrar.setOn(storage.readTextFile(path).equals("1"));
        }
        else {

            storage.createFile(path, "1");
        }

        path = storage.getInternalFilesDirectory() + File.separator + CONFIG_LOCALIZACAO_APROXIMADA;

        if (storage.isFileExist(path)) {

            toggleLocalizacaoAproximada.setOn(storage.readTextFile(path).equals("1"));
        }
        else {

            storage.createFile(path, "1");
        }

        path = storage.getInternalFilesDirectory() + File.separator + CONFIG_MOSTRAR_ICONE;

        if (storage.isFileExist(path)) {

            toggleIconePosicao.setOn(storage.readTextFile(path).equals("1"));
        }
        else {

            storage.createFile(path, "1");
        }

        path = storage.getInternalFilesDirectory() + File.separator + CONFIG_SEEKBAR_DISTANCIA;

        if (storage.isFileExist(path)) {

            seekDistancia.setProgress(Integer.parseInt(storage.readTextFile(path)));
        }
        else {

            storage.createFile(path, "3");
        }
    }

    private int getZoomLevel(int meters) {

        double scale = meters / 900;

        return (int) (16 - Math.log(scale) / Math.log(2));
    }

    private int getDistanciaRadar() {

        int distancia = seekDistancia.getProgress();

        if (distancia == 0) {

            distancia = 10000;
        }
        else if (distancia == 1) {

            distancia = 25000;
        }
        else if (distancia == 2) {

            distancia = 50000;
        }
        else if (distancia == 3) {

            distancia = 100000;
        }
        else if (distancia == 4) {

            distancia = 250000;
        }
        else {

            distancia = 500000;
        }

        return distancia * 2;
    }
}
