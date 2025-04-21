package com.example.googleauth;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GoogleSignIn";
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private FirebaseAuth auth;
    private ShapeableImageView imageView;
    private TextView name, mail;

    private final ActivityResultLauncher<IntentSenderRequest> signInLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            try {
                SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(result.getData());
                String idToken = credential.getGoogleIdToken();
                if (idToken != null) {
                    AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                    auth.signInWithCredential(firebaseCredential).addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            updateUI();
                            printUserInfo();
                            Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Firebase sign in failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (ApiException e) {
                Log.e(TAG, "Google sign-in failed", e);
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();

        imageView = findViewById(R.id.profileImage);
        name = findViewById(R.id.nameTV);
        mail = findViewById(R.id.mailTV);

        oneTapClient = Identity.getSignInClient(this);
        signInRequest = BeginSignInRequest.builder().setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder().setSupported(true).setServerClientId(getString(R.string.client_id)).setFilterByAuthorizedAccounts(false).build()).setAutoSelectEnabled(true).build();

        findViewById(R.id.signIn).setOnClickListener(view -> {
            oneTapClient.beginSignIn(signInRequest).addOnSuccessListener(this, result -> {
                try {
                    IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder(result.getPendingIntent().getIntentSender()).build();
                    signInLauncher.launch(intentSenderRequest);
                } catch (Exception e) {
                    Log.e(TAG, "Couldn't launch One Tap sign-in", e);
                }
            }).addOnFailureListener(this, e -> {
                Toast.makeText(this, "One Tap sign-in failed", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "One Tap failed", e);
            });
        });

        findViewById(R.id.signout).setOnClickListener(view -> {
            auth.signOut();
            oneTapClient.signOut().addOnSuccessListener(unused -> {
                Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
                name.setText("");
                mail.setText("");
            });
        });

        if (auth.getCurrentUser() != null) {
            updateUI();
            printUserInfo();
        }
    }

    private void printUserInfo() {
        FirebaseUser user = auth.getCurrentUser();

        try {
            JSONObject userJson = new JSONObject();
            assert user != null;
            userJson.put("getDisplayName", user.getDisplayName());
            userJson.put("getEmail", user.getEmail());
            userJson.put("isAnonymous", user.isAnonymous());
            userJson.put("isEmailVerified", user.isEmailVerified());
            userJson.put("getMetadata", user.getMetadata());
            userJson.put("getPhoneNumber", user.getPhoneNumber());
            userJson.put("getPhotoUrl", user.getPhotoUrl());
            userJson.put("getProviderData", user.getProviderData());
            userJson.put("getProviderId", user.getProviderId());
            userJson.put("getUid", user.getUid());

            Log.d("GoogleUserData", userJson.toString(4));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void updateUI() {
        Glide.with(this).load(Objects.requireNonNull(auth.getCurrentUser()).getPhotoUrl()).into(imageView);
        name.setText(auth.getCurrentUser().getDisplayName());
        mail.setText(auth.getCurrentUser().getEmail());
    }
}
