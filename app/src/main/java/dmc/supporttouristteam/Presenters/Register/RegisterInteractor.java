package dmc.supporttouristteam.Presenters.Register;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import dmc.supporttouristteam.Models.User;
import dmc.supporttouristteam.Utils.Config;

public class RegisterInteractor implements RegisterContract.Interactor {
    private RegisterContract.OnOperationListener listener;

    public RegisterInteractor(RegisterContract.OnOperationListener listener) {
        this.listener = listener;
    }

    @Override
    public void openGallery(Activity activity) {
        // start picker to get image for cropping and then use the image in cropping activity
        CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(activity);
    }

    @Override
    public void register(String name, String email, String password, Uri pickedImgUri) {
        // this method create user account with specific email and password
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            updateUserInfo(name, pickedImgUri, currentUser);
                        } else {
                            // If sign in fails, display a message to the user.
                            listener.onFail("Đăng ký thất bại");
                        }
                    }
                });
    }

    private void updateUserInfo(final String name, final Uri pickedImgUri, final FirebaseUser currentUser) {
        // need to upload user photo to firebase storage and get url
        StorageReference mStorage = FirebaseStorage.getInstance().getReference().child(Config.RF_USER_PHOTOS);
        final StorageReference imageFilePath = mStorage.child(pickedImgUri.getLastPathSegment());
        imageFilePath.putFile(pickedImgUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // image uploaded successfully
                // now we can get our image url
                imageFilePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        // uri contain user image url
                        UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .setPhotoUri(uri)
                                .build();
                        currentUser.updateProfile(profileUpdate).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    // user info updated successfully
                                    addUserIntoDatabase(currentUser);
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void addUserIntoDatabase(FirebaseUser currentUser) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(Config.RF_USERS).child(currentUser.getUid());
        reference.setValue(new User(currentUser.getUid(),
                currentUser.getEmail(),
                currentUser.getDisplayName(),
                currentUser.getPhotoUrl().toString()))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            FirebaseAuth.getInstance().signOut();
                            listener.onSuccess("Đăng ký thành công");
                        } else {
                            listener.onFail("Đăng ký thất bại");
                        }
                    }
                });
    }
}
