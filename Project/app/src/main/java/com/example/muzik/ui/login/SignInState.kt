package com.example.muzik.ui.login

import android.content.ContentValues
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

val db = Firebase.firestore
data class SignInState (
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null
) {
    fun createUserInFirestore(userID: String, username: String, avatarUrl: String) {
        val favSongData = hashMapOf(
            "username" to username,
            "avatarUrl" to avatarUrl
        )

        val userDocRef = db.collection("User").document(userID)
        userDocRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    Log.d("DocumentCheck", "Document with userID exists.")

                } else {
                    Log.d("DocumentCheck", "Document with userID does not exist. Perform document creation.")
                    userDocRef.set(favSongData).addOnSuccessListener {
                        val favSongData = hashMapOf(
                            "title" to "Favorite Songs",
                            "thumbnail" to "gs://onlinemuzikapp.appspot.com/Favorite/FavoriteSong.png",
                            "songIDs" to ""
                        )

                        db.collection("User").document(userID)
                            .collection("FavoriteSongs").document("FavoriteSongs")
                            .set(favSongData)
                            .addOnSuccessListener {
                                // Thực hiện hành động khi tạo collection "FavoriteSongs" thành công
                            }
                            .addOnFailureListener { e ->
                                // Xử lý khi có lỗi xảy ra khi tạo collection "FavoriteSongs"
                            }

                        val favAlbumData = hashMapOf(
                            "title" to "Favorite Albums",
                            "thumbnail" to "gs://onlinemuzikapp.appspot.com/Favorite/FavoriteAlbum.jpg",
                            "albumIDs" to ""
                        )

                        db.collection("User").document(userID)
                            .collection("FavoriteAlbums").document("FavoriteAlbums")
                            .set(favAlbumData)
                            .addOnSuccessListener {
                                // Thực hiện hành động khi tạo collection "FavoriteSongs" thành công
                            }
                            .addOnFailureListener { e ->
                                // Xử lý khi có lỗi xảy ra khi tạo collection "FavoriteSongs"
                            }
                    }
                    .addOnFailureListener { e ->
                        // Xử lý khi có lỗi xảy ra khi tạo document trong collection "User"
                    }
                // Tiếp tục quá trình tạo document ở đây...
                }
            }
            .addOnFailureListener { e ->
                Log.e("DocumentCheck", "Error checking document existence: $e")
            }
    }
}
