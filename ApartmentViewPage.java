package com.example.shroomies;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.bumptech.glide.Glide;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.make.dots.dotsindicator.DotsIndicator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ApartmentViewPage extends AppCompatActivity implements OnMapReadyCallback {
    ViewPager viewPager;
    ImageButton messageButton;
    DotsIndicator dotsIndicator;
    TextView priceTextView;
    TextView descriptionTextView;
    ViewPagerAdapterApartmentView viewPagerAdapterApartmentView;
    Apartment apartment;
    TextView numberOfRoomMates;
    TextView date;
    TextView username;
    CustomMapView mapView;
    GoogleMap mMap;
    TextView locationAddressTextView;
    Geocoder geocoder;
    Toolbar toolbar;
    ImageView userImageView , maleImageView , femaleImageView , petsImageView , smokeFreeImageView;
    User user;
    FirebaseAuth mAuth;
    boolean[] preferences;

    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apartment_page);
        if(getIntent().getExtras()!=null) {
            apartment = new Apartment();
            apartment = getIntent().getExtras().getParcelable("apartment");
            preferences = getIntent().getExtras().getBooleanArray("apartmentPreferences");


        }
        maleImageView = findViewById(R.id.male_image_view_apartment);
        femaleImageView = findViewById(R.id.female_image_view_apartment);
        petsImageView = findViewById(R.id.pets_allowd_image_view_apartment);
        smokeFreeImageView = findViewById(R.id.non_smoking_image_view_apartment);

        locationAddressTextView = findViewById(R.id.location_address_text_view);
            viewPager = findViewById(R.id.view_pager_apartment_view);
            dotsIndicator = findViewById(R.id.dotsIndicator_apartment_view);
            priceTextView = findViewById(R.id.price_text_view);
            descriptionTextView = findViewById(R.id.user_description_text_view);
            numberOfRoomMates = findViewById(R.id.number_of_roommates_text_view);
            userImageView = findViewById(R.id.user_image_view);
            date = findViewById(R.id.date_of_post_text_view);
            username = findViewById(R.id.name_of_user_text_view);
            mapView = findViewById(R.id.apartment_post_page_map);
            toolbar = (Toolbar) findViewById(R.id.toolbar);
            messageButton = findViewById(R.id.message_user_button);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            initGoogleMap(savedInstanceState);

            mapView.getMapAsync(this);

            //set the desicription
            descriptionTextView.setText(apartment.getDescription());
            //set the no of roommates
            numberOfRoomMates.setText(Integer.toString(apartment.getNumberOfRoommates() ) + " Room mates required ");
            // set the date
            date.setText(apartment.getDate().split(" ")[0]);

            messageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getApplication() , "heree" , Toast.LENGTH_SHORT).show();
                    if(user!=null){

                        chatWithThisUser(user);
                    }
                }
            });

            //set the location address
        geocoder = new Geocoder(getApplicationContext());
        try {
            // for some reason the latitude and the longitude are saved oppositly
            locationAddressTextView.setText(geocoder.getFromLocation(apartment.getLatitude(), apartment.getLongitude(),1).get(0).getAddressLine(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
        // get the username and profile picture from the database
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(apartment.getUserID());

            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    user = new User();
                    user = snapshot.getValue(User.class);
                    if(mAuth.getInstance().getCurrentUser().getUid().equals(user.getID())){
                        messageButton.setVisibility(View.GONE);
                        username.setText("you");
                    }else{
                        username.setText(user.getName());
                    }

                    if(!user.getImage().isEmpty()){
                        GlideApp.with(getApplication())
                                .load(user.getImage())
                                .transform( new CircleCrop())
                                .into(userImageView);
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            viewPagerAdapterApartmentView = new ViewPagerAdapterApartmentView(getApplicationContext(), apartment.getImage_url());
            viewPager.setAdapter(viewPagerAdapterApartmentView);
            dotsIndicator.setViewPager(viewPager);
            viewPager.getAdapter().registerDataSetObserver(dotsIndicator.getDataSetObserver());

        if(preferences[0]){maleImageView.setVisibility(View.VISIBLE);}
        if(preferences[1]){femaleImageView.setVisibility(View.VISIBLE);}
        if(preferences[2]){petsImageView.setVisibility(View.VISIBLE);}
        if(preferences[3]){smokeFreeImageView.setVisibility(View.VISIBLE);}


    }

    private void chatWithThisUser(User user) {

        Intent intent = new Intent(getApplication(), ChattingActivity.class);
        intent.putExtra("USERID", user.getID());
        startActivity(intent);

    }


    @Override
    protected void onStart() {
        super.onStart();

            mapView.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();

            mapView.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

            mapView.onDestroy();

    }

    @Override
    protected void onPause() {
        super.onPause();

            mapView.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();

            mapView.onResume();

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

            mapView.onLowMemory();

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }
        if(mapView!=null) {
            mapView.onSaveInstanceState(outState);
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        if(apartment!=null) {
            Toast.makeText(getApplicationContext(), Double.toString(apartment.getLatitude()) , Toast.LENGTH_LONG).show();

            LatLng latLng = new LatLng(apartment.getLatitude(), apartment.getLongitude());
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier("black_mushroom", "drawable", getApplicationContext().getPackageName()));
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 90, 100, false);
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));
            if (googleMap != null) {
                googleMap.addMarker(markerOptions);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
            }
        }
    }

    private void initGoogleMap(Bundle savedInstanceState) {
        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);


    }

    }


// create a new view pager adapter
class ViewPagerAdapterApartmentView extends PagerAdapter {
    Context context;
    private List<String> imageUrls;
    ViewPagerAdapterApartmentView(Context context , List<String> imageUrls){
        this.context = context;
        this.imageUrls = imageUrls;

    }

    @Override
    public int getCount() {
        return imageUrls.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view==object;
    }


    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        ImageView imageView = new ImageView(context);



        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(imageUrls.get(position));
        Toast.makeText(context, imageUrls.get(position),Toast.LENGTH_LONG).show();
        // Load the image using Glide

        GlideApp.with(this.context)
                .load(storageReference)
                .transform(new CenterCrop())
                .into(imageView);

        ViewPager vp = (ViewPager) container;
        vp.addView(imageView);
        return imageView;
    }


    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View)object);
    }



}