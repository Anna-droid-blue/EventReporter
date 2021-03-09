package com.laioffer.eventreporter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EventMapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EventMapFragment extends Fragment implements OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener {
    private MapView mMapView;
    private View mView;
    private DatabaseReference database;
    private List<Event> events;
    private GoogleMap mGoogleMap;
    private Marker lastClicked;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public EventMapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EventMapFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EventMapFragment newInstance(String param1, String param2) {
        EventMapFragment fragment = new EventMapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_event_map, container, false);
        database = FirebaseDatabase.getInstance().getReference();
        events = new ArrayList<Event>();
        return mView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMapView = (MapView) mView.findViewById(R.id.event_map_view);
        if (mMapView != null) {
            mMapView.onCreate(null);
            mMapView.onResume(); // needed to get the map to display immediately
            mMapView.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(getContext());
        mGoogleMap = googleMap;
        mGoogleMap.setOnInfoWindowClickListener(this);
        mGoogleMap.setOnMarkerClickListener(this);

        final LocationTracker locationTracker = new LocationTracker(getActivity());
        locationTracker.getLocation();
        double curLatitude = locationTracker.getLatitude();
        double curLongitude = locationTracker.getLongitude();

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(curLatitude, curLongitude)).zoom(12).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        setUpMarkersCloseToCurLocation(googleMap, curLatitude, curLongitude);
    }

    private void setUpMarkersCloseToCurLocation(final GoogleMap googleMap, final double curLatitude,
                                                final double curLongitude) {
        events.clear();
        database.child("events").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get all available events
                for (DataSnapshot noteDataSnapshot : dataSnapshot.getChildren()) {
                    Event event = noteDataSnapshot.getValue(Event.class);
                    double destLatitude = event.getLatitude();
                    double destLongitude = event.getLongitude();
                    int distance = Utils.distanceBetweenTwoLocations(curLatitude, curLongitude,
                            destLatitude, destLongitude);
                    if (distance <= 10) {
                        events.add(event);
                    }
                }
                // Set up every event
                for (Event event : events) {
                    // create marker
                    MarkerOptions marker = new MarkerOptions().position(
                            new LatLng(event.getLatitude(), event.getLongitude())).title(event.getTitle());
                    // changing marker icon
                    marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
                    // adding marker
                    Marker mker = googleMap.addMarker(marker);
                    mker.setTag(event);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Event event = (Event) marker.getTag();
        Intent intent = new Intent(getContext(), CommentActivity.class);
        String eventId = event.getId();
        intent.putExtra("EventID", eventId);
        getContext().startActivity(intent);
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        final Event event = (Event) marker.getTag();
        if (lastClicked != null && lastClicked.equals(marker)) {
            lastClicked = null;
            marker.hideInfoWindow();
            marker.setIcon(null);
            return true;
        } else {
            lastClicked = marker;
            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... voids) {
                    Bitmap bitmap = Utils.getBitmapFromURL(event.getImgUri());
                    return bitmap;
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    super.onPostExecute(bitmap);
                    if (bitmap != null) {
                        marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
                        marker.setTitle(event.getTitle());
                    }
                }
            }.execute();
            return false;
        }
    }
}