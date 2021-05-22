package com.smov.gabriel.orientatree;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.smov.gabriel.orientatree.model.User;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    /*private Button camera_button, galery_button;
    private ImageView profile_imageView;*/
    private Toolbar toolbar;
    private Button updateButton;
    private MaterialButton editPicture_button;
    private TextInputLayout name_textInputLayout, surname_textInputLayout;
    private TextInputEditText editName_editText, editSurname_editText;
    private CircleImageView profileCircleImageView;

    // to show the navigation drawer
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    // user name, e-mail and image that the navigation drawer displays
    private TextView name_textView;
    private TextView email_textView;
    private CircleImageView profile_circleImageView;
    // user data stored in Auth user, and that is shown in the navigation drawer

    private String userID, userEmail, userName;

    private User currentUser;

    private Uri galleryImageUri;

    private FirebaseAuth mAuth;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private FirebaseUser user;
    private FirebaseFirestore db;

    private final int CAMERA_IMAGE_CODE = 1001;
    private final int GALLERY_IMAGE_CODE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        userID = mAuth.getCurrentUser().getUid();
        userEmail = mAuth.getCurrentUser().getEmail();
        userName = mAuth.getCurrentUser().getDisplayName();

        toolbar = findViewById(R.id.editProfile_toolbar);
        setSupportActionBar(toolbar);

        updateButton = findViewById(R.id.updeteProfile_button);
        editPicture_button = findViewById(R.id.editPicture_button);
        name_textInputLayout = findViewById(R.id.editName_TextField);
        surname_textInputLayout = findViewById(R.id.editSurname_TextField);
        profileCircleImageView = findViewById(R.id.editProfile_circleImageView);
        editName_editText = findViewById(R.id.editName_editText);
        editSurname_editText = findViewById(R.id.editSurname_editText);

        // setting the navigation drawer...
        drawerLayout = findViewById(R.id.drawer_layout_profile);
        navigationView = findViewById(R.id.nav_view_profile);
        // setting the navigation drawer's heading
        View hView =  navigationView.getHeaderView(0);
        name_textView = hView.findViewById(R.id.name_textView);
        email_textView = hView.findViewById(R.id.email_textView);
        profile_circleImageView = hView.findViewById(R.id.profile_circleImageView);
        name_textView.setText(userName);
        email_textView.setText(userEmail);
        if(user.getPhotoUrl() != null) {
            StorageReference ref = storageReference.child("profileImages/" + userID);
            Glide.with(this)
                    .load(ref)
                    .diskCacheStrategy(DiskCacheStrategy.NONE ) // prevent caching
                    .skipMemoryCache(true) // prevent caching
                    .into(profileCircleImageView); // set in the activity picture
            Glide.with(this)
                    .load(ref)
                    .diskCacheStrategy(DiskCacheStrategy.NONE ) // prevent caching
                    .skipMemoryCache(true) // prevent caching
                    .into(profile_circleImageView); // set into drawer picture
        }
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

        /*camera_button = findViewById(R.id.camera_button);
        galery_button = findViewById(R.id.gallery_button);
        profile_imageView = findViewById(R.id.profile_imageView);*/

        editName_editText.setText(userName);

        db.collection("users").document(userID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        currentUser = documentSnapshot.toObject(User.class);
                        if(currentUser.getSurname() != null) {
                            editSurname_editText.setText(currentUser.getSurname());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        Toast.makeText(EditProfileActivity.this, "No se pudieron recuperar los datos, vuelva a intentarlo en unos instantes", Toast.LENGTH_SHORT).show();
                    }
                });

        /*camera_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, CAMERA_IMAGE_CODE);
                }
            }
        });

        galery_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePicture();
            }
        });*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        /*if(requestCode == CAMERA_IMAGE_CODE) {
            switch (resultCode) {
                case RESULT_OK:
                    Bitmap bitmap = (Bitmap)data.getExtras().get("data");
                    profile_imageView.setImageBitmap(bitmap);
                    handleUpload(bitmap);
            }
        } else if (requestCode == GALLERY_IMAGE_CODE) {
            if(data != null && data.getData() != null) {
                switch (resultCode) {
                    case RESULT_OK:
                        galleryImageUri = data.getData();
                        profile_imageView.setImageURI(galleryImageUri);
                        uploadPicture();
                }
            }
        }*/
        super.onActivityResult(requestCode, resultCode, data);
    }

    // picture from camera
    private void handleUpload(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        StorageReference ref = storageReference.child("profileImages/" + userID);
        ref.putBytes(baos.toByteArray())
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        getDownloadUrl(ref);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EditProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getDownloadUrl(StorageReference ref) {
        ref.getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        //Toast.makeText(EditProfileActivity.this, uri.toString(), Toast.LENGTH_SHORT).show();
                        setUserProfileUrl(uri);
                    }
                });
    }

    private void setUserProfileUrl(Uri uri) {
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setPhotoUri(uri)
                .build();
        user.updateProfile(request)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Toast.makeText(EditProfileActivity.this, "Uri set successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EditProfileActivity.this, "Fail at setting uri", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // picture from galery
    private void choosePicture() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, GALLERY_IMAGE_CODE);
    }


    private void uploadPicture() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Uploading image...");
        pd.show();
        StorageReference ref = storageReference.child("profileImages/" + userID);
        ref.putFile(galleryImageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        pd.dismiss();
                        Snackbar.make(findViewById(android.R.id.content), "Image uploaded", Snackbar.LENGTH_LONG).show();
                        setUserProfileUrl(galleryImageUri);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pd.dismiss();
                        Toast.makeText(EditProfileActivity.this, "Failed to upload", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        double progressPercent = (100.00 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                        pd.setMessage("Percentage: " + (int)progressPercent + "%");
                    }
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.my_activities_item:
                //updateUIHome();
                break;
            case R.id.organize_activity_item:
                break;
            case R.id.profile_settings_item:
                //updateUIEditProfile();
                break;
            case R.id.log_out_item:
                //logOut();
                break;
            case R.id.delete_profile_item:
                //deleteAccount();
                break;
        }
        //close navigation drawer
        drawerLayout.closeDrawer(GravityCompat.START);
        return false;
    }
}