package com.smov.gabriel.orientatree;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Movie;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.module.AppGlideModule;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.common.net.PercentEscaper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private Toolbar toolbar;

    // to show the navigation drawer
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    // user name, e-mail and image that the navigation drawer displays
    private TextView name_textView;
    private TextView email_textView;
    private CircleImageView profile_circleImageView;

    // to show the tabs
    private ViewPager viewPager;
    private TabLayout tabLayout;

    // fragment of each tab
    private CompletedFragment completedFragment;
    private OnGoingFragment onGoingFragment;
    private ProgrammedFragment programmedFragment;

    // useful to reset the last tab when coming back from another activity
    private int tabSelected = 0;

    private FloatingActionButton fab;

    // needed to show snackbar
    private ConstraintLayout home_constraintLayout;

    // user data stored in Auth user
    String userID, userEmail, userName;

    // Firebase services
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    FirebaseStorage storage;
    StorageReference storageReference;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        userID = user.getUid();
        userName = user.getDisplayName();
        userEmail = user.getEmail();

        db = FirebaseFirestore.getInstance();

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        fab = findViewById(R.id.floating_action_button);

        home_constraintLayout = findViewById(R.id.home_constraintLayout);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUIFindActivity();
            }
        });

        toolbar = findViewById(R.id.home_toolbar);

        setSupportActionBar(toolbar);

        // navigation drawer...
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        View hView = navigationView.getHeaderView(0);
        // setting navigation drawer header...
        name_textView = hView.findViewById(R.id.name_textView);
        email_textView = hView.findViewById(R.id.email_textView);
        profile_circleImageView = hView.findViewById(R.id.profile_circleImageView);
        name_textView.setText(userName);
        email_textView.setText(userEmail);

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.openNavDrawer,
                R.string.closeNavDrawer
        );
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // tabs...
        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);
        // fragment initialization
        completedFragment = new CompletedFragment();
        onGoingFragment = new OnGoingFragment();
        programmedFragment = new ProgrammedFragment();
        // binding tabs and fragments
        tabLayout.setupWithViewPager(viewPager);
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), 0);
        viewPagerAdapter.adFragment(completedFragment, "pasadas");
        viewPagerAdapter.adFragment(onGoingFragment, "en curso");
        viewPagerAdapter.adFragment(programmedFragment, "previstas");
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setTabTextColors(R.color.black, R.color.black); // tab text color black, both selected and unselected

        // perform some other actions when clicking specific tab item (like showing or hiding fab)
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        tabSelected = 0;
                        break;
                    case 1:
                        tabSelected = 1;
                        break;
                    case 2:
                        tabSelected = 2;
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                /*switch (tab.getPosition()) {
                    case 1:
                        fab.show();
                        break;
                }*/
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // dowloading the profile pic and show in navigation drawer...
        if (user.getPhotoUrl() != null) {
            StorageReference ref = storageReference.child("profileImages/" + userID);
            Glide.with(this)
                    .load(ref)
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // prevent caching
                    .skipMemoryCache(true) // prevent caching
                    .into(profile_circleImageView);
        }
    }

    @Override
    protected void onResume() {
        viewPager.setCurrentItem(tabSelected);
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.profile_settings_item:
                updateUIEditProfile();
                break;
            case R.id.organize_activity_item:
                updateUIFindTemplate();
                break;
            case R.id.log_out_item:
                logOut();
                break;
            case R.id.delete_profile_item:
                //deleteAccount();
                break;
        }
        //close navigation drawer
        drawerLayout.closeDrawer(GravityCompat.START);
        return false;
    }

    /*private void deleteAccount() {
        new MaterialAlertDialogBuilder(this)
                .setMessage("¿Realmente desea eliminar su perfil y todos sus datos de manera permanente?")
                .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteProfilePicture();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }*/

    /*private void deleteProfilePicture() {
        StorageReference ref = storageReference.child("profileImages/" + userID);
        ref.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                deleteUserData();
            }
        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        deleteUserData();
                    }
                });
    }

    private void deleteUserData() {
        db.collection("users").document(userID)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        deleteAuth();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        deleteAuth();
                    }
                });
    }

    private void deleteAuth() {
        FirebaseUser user = mAuth.getCurrentUser();
        user.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                updateUIIdentification();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showSnackBar("Su cuenta no ha podido eliminarse. Pruebe de nuevo.");
            }
        });
    }*/

    private void logOut() {
        new MaterialAlertDialogBuilder(this)
                .setMessage("¿Desea salir de su sesión?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAuth.signOut();
                        updateUIIdentification();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    // inner created class, needed for tabs
    private class ViewPagerAdapter extends FragmentPagerAdapter {

        private List<Fragment> fragments = new ArrayList<>();
        private List<String> fragmentTitles = new ArrayList<>();

        public ViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        public void adFragment(Fragment fragment, String title) {
            fragments.add(fragment);
            fragmentTitles.add(title);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentTitles.get(position);
        }
    }

    public void updateUIIdentification() {
        Intent intent = new Intent(HomeActivity.this, IdentificationActivity.class);
        startActivity(intent);
        finish();
    }

    private void updateUIEditProfile() {
        Intent intent = new Intent(HomeActivity.this, EditProfileActivity.class);
        startActivity(intent);
    }

    private void updateUIFindTemplate() {
        Intent intent = new Intent(HomeActivity.this, FindTemplate.class);
        startActivity(intent);
    }

    private void updateUIFindActivity() {
        Intent intent = new Intent(HomeActivity.this, FindActivityActivity.class);
        startActivity(intent);
    }

    /*private void showSnackBar(String msg) {
        Snackbar.make(home_constraintLayout, msg, Snackbar.LENGTH_LONG)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                })
                .setDuration(8000)
                .show();
    }*/
}