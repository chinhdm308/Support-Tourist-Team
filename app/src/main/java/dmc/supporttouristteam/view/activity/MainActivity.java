package dmc.supporttouristteam.view.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;
import com.theartofdev.edmodo.cropper.CropImage;

import dmc.supporttouristteam.R;
import dmc.supporttouristteam.service.TrackerService;
import dmc.supporttouristteam.util.Common;
import dmc.supporttouristteam.view.fragment.AccountFragment;
import dmc.supporttouristteam.view.fragment.ChatsFragment;
import dmc.supporttouristteam.view.fragment.FriendsFragment;
import dmc.supporttouristteam.view.fragment.LovePlacesFragment;

public class MainActivity extends AppCompatActivity implements ChipNavigationBar.OnItemSelectedListener {

    private ChipNavigationBar navigationBar;

    private static final int PERMISSIONS_REQUEST = 1;
    private static final int REQUEST_CHECK_SETTINGS = 1;

    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        // Update token
        updateToken();

        // Update location
        checkGPS();

        navigationBar.setOnItemSelectedListener(this);

        if (savedInstanceState == null) {
            navigationBar.setItemSelected(R.id.menu_chats, true);
        }

//        FirebaseDatabase.getInstance().getReference(Config.RF_GROUPS).removeValue();
//        FirebaseDatabase.getInstance().getReference(Config.RF_CHATS).removeValue();
//        FirebaseDatabase.getInstance().getReference(Config.RF_LOVE_PLACES).removeValue();
    }

    private void updateToken() {
        DatabaseReference tokensRef= FirebaseDatabase.getInstance().getReference(Common.RF_TOKENS);

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String newToken = instanceIdResult.getToken();
                Log.e("newToken",newToken);
                tokensRef.child(currentUser.getUid())
                        .setValue(newToken);
            }
        });
    }

    private void checkGPS() {
        // Check GPS is enabled
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show();
//            finish();
            getAllPermissionForLocationRequest();
        } else {
            startTrackerService();
        }
    }

    private void startTrackerService() {
        startService(new Intent(this, TrackerService.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Start the service when the permission is granted
            startTrackerService();
        } else {
            finish();
        }
    }

    private void init() {
        navigationBar = findViewById(R.id.navigation_bar);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public void onItemSelected(int i) {
        if (i == R.id.menu_chats) {
            getSupportActionBar().setTitle("Tr?? chuy???n");
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ChatsFragment()).commit();
        }
        if (i == R.id.menu_friends) {
            getSupportActionBar().setTitle("B???n b??");
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new FriendsFragment()).commit();
        }
        if (i == R.id.menu_account) {
            getSupportActionBar().setTitle("Th??ng tin c?? nh??n");
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new AccountFragment()).commit();
        }
        if (i == R.id.menu_love_places) {
            getSupportActionBar().setTitle("?????a ??i???m y??u th??ch");
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new LovePlacesFragment()).commit();
        }
    }

    private void getAllPermissionForLocationRequest() {
        LocationRequest request = LocationRequest.create();
        request.setInterval(10000);
        request.setFastestInterval(5000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(request);

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);

        Task<LocationSettingsResponse> responseTask = settingsClient.
                checkLocationSettings(builder.build());

        responseTask.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                startTrackerService();
            }
        });
        responseTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(Common.TAG, "Location enabled by user!");
                startTrackerService();
            }
            if (resultCode == RESULT_CANCELED) {
                Log.d(Common.TAG, "Location not enabled, user cancelled.");
                finish();
            }
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                AccountFragment accountFragment = (AccountFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                accountFragment.onSetAvatarTemp(result.getUri());
                MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("???nh ?????i di???n")
                        .setMessage("B???n c?? mu???n thay ?????i ???nh ?????i di???n kh??ng ?")
                        .setNegativeButton("H???y", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                accountFragment.onSelectedImage(null);
                                dialog.dismiss();
                            }
                        })
                        .setPositiveButton("?????ng ??", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                accountFragment.onSelectedImage(result.getUri());
                            }
                        })
                        .setCancelable(false);
                dialogBuilder.show();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}