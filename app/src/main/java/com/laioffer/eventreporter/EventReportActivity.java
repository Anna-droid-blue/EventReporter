package com.laioffer.eventreporter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

public class EventReportActivity extends AppCompatActivity {
    private static final String TAG = EventReportActivity.class.getSimpleName();
    private EditText mEditTextLocation;
    private EditText mEditTextTitle;
    private EditText mEditTextContent;
    private ImageView mImageViewSend;
    private ImageView mImageViewCamera;
    private DatabaseReference database;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private LocationTracker mLocationTracker;
    //private Activity mActivity;

    private static int RESULT_LOAD_IMAGE = 1;
    private ImageView img_event_picture;
    private Uri mImgUri;

    private FirebaseStorage storage;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_report);

        //auth
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

        mAuth.signInAnonymously().addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                Log.d(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());
                if (!task.isSuccessful()) {
                    Log.w(TAG, "signInAnonymously", task.getException());
                }
            }
        });

        mEditTextLocation = (EditText) findViewById(R.id.edit_text_event_location);
        mEditTextTitle = (EditText) findViewById(R.id.edit_text_event_title);
        mEditTextContent = (EditText) findViewById(R.id.edit_text_event_content);
        mImageViewCamera = (ImageView) findViewById(R.id.img_event_camera);
        mImageViewSend = (ImageView) findViewById(R.id.img_event_report);
        database = FirebaseDatabase.getInstance().getReference();

        img_event_picture = (ImageView) findViewById(R.id.img_event_picture_capture);

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        mImageViewSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String key = uploadEvent();
            }
        });

        //mActivity = this;
        mLocationTracker = new LocationTracker(this);
        mLocationTracker.getLocation();
        final double latitude = mLocationTracker.getLatitude();
        final double longitude = mLocationTracker.getLongitude();
        new AsyncTask<Void, Void, Void>() {
            private List<String> mAddressList = new ArrayList<String>();

            @Override
            protected Void doInBackground(Void... urls) {
                mAddressList = mLocationTracker.getCurrentLocationViaJSON(latitude,longitude);
                return null;
            }

            @Override
            protected void onPostExecute(Void input) {
                if (mAddressList.size() >= 3) {
                    mEditTextLocation.setText(mAddressList.get(0) + ", " + mAddressList.get(1) +
                            ", " + mAddressList.get(2) + ", " + mAddressList.get(3));
                }
            }
        }.execute();

        //Add click listener for the image to pick up images from gallery through implicit intent
        mImageViewCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, RESULT_LOAD_IMAGE);
            }
        });

        mImageViewSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String key = uploadEvent();
                if (mImgUri != null) {
                    uploadImage(key);
                    mImgUri = null;
                }
            }
        });
    }

    private void uploadImage(final String eventId) {
        if (mImgUri == null) {
            return;
        }
        StorageReference imgRef = storageRef.child("images/" + mImgUri.getLastPathSegment()
                + "_" + System.currentTimeMillis());
        UploadTask uploadTask = imgRef.putFile(mImgUri);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                @SuppressWarnings("VisibleForTests")
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                Log.i(TAG, "upload successfully" + eventId);
                database.child("events").child(eventId).child("imgUri")
                        .setValue(downloadUrl.toString());
                img_event_picture.setImageDrawable(null);
                img_event_picture.setVisibility(View.GONE);
            }
        });
    }

    private String uploadEvent() {
        String title = mEditTextTitle.getText().toString();
        String location = mEditTextLocation.getText().toString();
        String description = mEditTextContent.getText().toString();
        if (location.equals("") || description.equals("") || title.equals("") || Utils.username == null) {
            return null;
        }
        //create event instance
        Event event = new Event();
        event.setTitle(title);
        event.setAddress(location);
        event.setDescription(description);
        event.setLatitude(mLocationTracker.getLatitude());
        event.setLongitude(mLocationTracker.getLongitude());
        event.setTime(System.currentTimeMillis());
        event.setUsername(Utils.username);
        String key = database.child("events").push().getKey();
        event.setId(key);
        database.child("events").child(key).setValue(event, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError != null) {
                    Toast toast = Toast.makeText(getBaseContext(),
                                    "The event is failed, please check your network status.",
                                    Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    Toast toast = Toast.makeText(getBaseContext(), "The event is reported",
                                    Toast.LENGTH_SHORT);
                    toast.show();
                    mEditTextTitle.setText("");
                    mEditTextLocation.setText("");
                    mEditTextContent.setText("");
                }
            }
        });
        return key;
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }
    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
                Uri selectedImage = data.getData();
                img_event_picture.setVisibility(View.VISIBLE);
                img_event_picture.setImageURI(selectedImage);
                mImgUri = selectedImage;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}