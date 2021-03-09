package com.laioffer.eventreporter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
//import android.app.Fragment;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class EventActivity extends AppCompatActivity {
    private Fragment mEventsFragment;
    private Fragment mEventMapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        if (mEventsFragment == null) {
            mEventsFragment = new EventsFragment();
        }

        BottomNavigationView bottomNavigationView = (BottomNavigationView)
                findViewById(R.id.navigation);
        getSupportFragmentManager().beginTransaction().
                add(R.id.relativelayout_event, mEventsFragment).commit();

        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_event_list:
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.relativelayout_event,
                                                mEventsFragment).commit();
                                break;
                            case R.id.action_event_map:
                                if (mEventMapFragment == null) {
                                    mEventMapFragment = new EventMapFragment();
                                }
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.relativelayout_event,
                                                mEventMapFragment).commit();
                        }
                        return false;
                    }
                });
    }
}