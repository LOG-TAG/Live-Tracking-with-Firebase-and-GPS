package com.hiteshsahu.livetracker;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.util.Date;

public class TrackerActivity extends FragmentActivity implements OnMapReadyCallback,
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final long INTERVAL = 30000; //1 minute
    private static final long FASTEST_INTERVAL = 30000; // 1 minute

    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mCurrentLocation;
    String mLastUpdateTime;

    protected static final String FIREBASE_DB = "https://livetracking-144307.firebaseio.com/";
    private GoogleMap mMap;
    private String TAG = TrackerActivity.class.getSimpleName();
    private MarkerOptions carMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_tracker);

        //Set Map
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Set location
        //show error dialog if GoolglePlayServices not available
        if (!isGooglePlayServicesAvailable()) {
            finish();
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


        //Set DB
        Firebase.setAndroidContext(this);

        Firebase myFirebaseRef = new Firebase(FIREBASE_DB);

        myFirebaseRef.child("UserID").setValue(Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID));

        myFirebaseRef.child("UserID").addValueEventListener(new ValueEventListener() {

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildKey) {
                String title = (String) snapshot.child("title").getValue();
                System.out.println("The updated post title is " + title);
            }

            @Override
            public void onDataChange(DataSnapshot snapshot) {

                System.out.println("There are " + snapshot.getChildrenCount() + " blog posts");

                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    String post = postSnapshot.getValue(String.class);

                    System.out.println(snapshot.getValue());  //prints "Do you have data? You'll love Firebase."

                    Toast.makeText(TrackerActivity.this, "USerID" + snapshot.getValue().toString(), Toast.LENGTH_SHORT).show();
                }
            }


            @Override
            public void onCancelled(FirebaseError error) {
            }
        });

        myFirebaseRef.child("UserID").addChildEventListener(new ChildEventListener() {
            // Retrieve new posts as they are added to the database
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
                BlogPost newPost = snapshot.getValue(BlogPost.class);
                System.out.println("Author: " + newPost.getAuthor());
                System.out.println("Title: " + newPost.getTitle());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
            //... ChildEventListener also defines onChildChanged, onChildRemoved,
            //    onChildMoved and onCanceled, covered in later sections.
        });
        //add location change listener
        createLocationRequest();

    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a carMarker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected - isConnected ...............: " + mGoogleApiClient.isConnected());
        startLocationUpdates();
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        Log.d(TAG, "Location update started ..............: ");
    }

    private void doZoom() {
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(18.520430, 73.856744), 17));
        }
    }

    private void changeMapMode(int mapMode) {

        if (mMap != null) {
            switch (mapMode) {
                case 0:
                    mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                    break;

                case 1:
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    break;

                case 2:
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    break;

                case 3:
                    mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                    break;

                case 4:
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    break;

                default:
                    break;
            }
        }
    }

    private void createRandomMarker(double latitude, double longitude) {

        // lets place some 10 random markers
        for (int i = 0; i < 10; i++) {
            // random latitude and logitude
            double[] randomLocation = Utils.createRandLocation(latitude, longitude);

            // Adding a carMarker
            MarkerOptions marker = new MarkerOptions().position(
                    new LatLng(randomLocation[0], randomLocation[1])).title(
                    "Hello Maps " + i);

            Log.e("Random", "> " + randomLocation[0] + ", " + randomLocation[1]);

            // changing carMarker color
            if (i == 0)
                marker.icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            if (i == 1)
                marker.icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            if (i == 2)
                marker.icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
            if (i == 3)
                marker.icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            if (i == 4)
                marker.icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
            if (i == 5)
                marker.icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            if (i == 6)
                marker.icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_RED));
            if (i == 7)
                marker.icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
            if (i == 8)
                marker.icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
            if (i == 9)
                marker.icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));

            mMap.addMarker(marker);

            // Move the camera to last position with a zoom level
            if (i == 9) {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(randomLocation[0], randomLocation[1]))
                        .zoom(15).build();

                mMap.animateCamera(CameraUpdateFactory
                        .newCameraPosition(cameraPosition));
            }
        }

    }


    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Firing onLocationChanged..............................................");
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        // To get lattitude value from location object
        double latti = location.getLatitude();
        // To get longitude value from location object
        double longi = location.getLongitude();

        addCustomMarker(latti, longi);

        // To hold lattitude and longitude values
        LatLng position = new LatLng(latti, longi);

        //createRandomMarker(latti, longi);

//        // Creating object to pass our current location to the map
//        MarkerOptions markerOptions = new MarkerOptions();
//
//        // To store current location in the markeroptions object
//        markerOptions.position(position);
//
//        // adding markeroptions class object to the map to show our current
//        // location in the map with help of default carMarker
//        mMap.addMarker(markerOptions);

        // Zooming to our current location with zoom level 17.0f
        mMap.animateCamera(CameraUpdateFactory
                .newLatLngZoom(position, 17f));
    }


    private void addCustomMarker(double latitude, double longitude) {

        Log.d(TAG, "addCustomMarker()");
        if (mMap == null) {
            return;
        }

        if (null == carMarker) {
            carMarker =
                    new MarkerOptions().position(
                            new LatLng(latitude, longitude))
                            .title("Hello Maps ")
                            .icon(Utils.getMarkerBitmapFromView(getApplicationContext(), R.drawable.car));

            mMap.addMarker(carMarker);
        } else {
            carMarker.position(new LatLng(latitude, longitude));
        }

//        // adding a carMarker on map with image from  drawable
//        mMap.addMarker(new MarkerOptions()
//                .position(new LatLng(latitude, longitude))
//                .icon(Utils.getMarkerBitmapFromView(getApplicationContext(), R.drawable.car)));
//
//        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
//
//            @Override
//            public void onMarkerDragStart(Marker marker) {
//            }
//
//            @Override
//            public void onMarkerDragEnd(Marker marker) {
//                Log.d(TAG, "latitude : " + marker.getPosition().latitude);
//                marker.setSnippet("" + marker.getPosition().latitude);
//                mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
//
//            }
//
//            @Override
//            public void onMarkerDrag(Marker marker) {
//            }
//
//        });
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart fired ..............");
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop fired ..............");
        mGoogleApiClient.disconnect();
        Log.d(TAG, "isConnected ...............: " + mGoogleApiClient.isConnected());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected())
            stopLocationUpdates();
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);

        Log.d(TAG, "Location update stopped .......................");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();

            Log.d(TAG, "Location update resumed .....................");
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed: " + connectionResult.toString());
    }

    public void onDisconnected() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConnectionSuspended(int arg0) {
        // TODO Auto-generated method stub

    }
}
