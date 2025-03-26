package com.example.muzik.ui.home

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.io.IOException
import kotlin.coroutines.resume


val db = Firebase.firestore

class HomeViewModel(userID: String) : ViewModel() {
    private val _songs: MutableLiveData<List<Song>> = MutableLiveData()
    private val _albums: MutableLiveData<List<Album>> = MutableLiveData()
    private val _favSongs: MutableLiveData<List<Album>> = MutableLiveData()
    private val _favAlbums: MutableLiveData<List<FavAlbum>> = MutableLiveData()
    private val _isLoading: MutableLiveData<Int> = MutableLiveData()

    val songs: LiveData<List<Song>> = _songs
    val albums: LiveData<List<Album>> = _albums
    val favSongs: LiveData<List<Album>> = _favSongs
    val favAlbums: LiveData<List<FavAlbum>> = _favAlbums
    val isLoading: LiveData<Int> = _isLoading

    init {
        viewModelScope.launch {
            val songs = fetchSongsFromFirestore()
            _songs.postValue(songs)
            _isLoading.postValue(25)

            val albums = fetchAlbumsFromFirestore(songs)
            _albums.postValue(albums)
            _isLoading.postValue(50)

            val favSongs = fetchFavoriteSongsFromFirestore(userID, songs)
            _favSongs.postValue(favSongs)
            _isLoading.postValue(75)
            Log.d("onChecking", "favSong: " + favSongs.toString())

            val favAlbums = fetchFavoriteAlbumsFromFirestore(userID, albums)
            _favAlbums.postValue(favAlbums)
            _isLoading.postValue(100)
            Log.d("onChecking", "favAlbum: " + favAlbums.toString())
        }
    }
}

data class Song(
    val songID: String,
    val artist: String,
    val genre: String,
    val lyrics: String,
    val thumbnail: String,
    val title: String,
    val url: String
)

data class Album(
    val albumID: String,
    val songs: List<Song>,
    val title: String,
    val thumbnail: String
)

data class FavAlbum(
    val albumID: String,
    val albums: List<Album>,
    val title: String,
    val thumbnail: String
)

suspend fun fetchAlbumsFromFirestore(songs: List<Song>): List<Album> = coroutineScope {
    val albumsCollection = db.collection("Album")
    val albumsQuerySnapshot = albumsCollection.get().await()
    val albumsList = mutableListOf<Album>()

    val deferredABList = albumsQuerySnapshot.documents.map { document ->
        val albumID = document.id
        val songIDs = document.getString("songIDs") ?: ""
        var songsInAlbum = emptyList<Song>()

        if (songIDs != "") {
            val listIDs: List<String> = songIDs.split(",")

            songsInAlbum = songs.filter { song ->
                listIDs.contains(song.songID)
            }
        }

        val abTitle = document.getString("title") ?: ""
        val abThumbnail = document.getString("thumbnail") ?: ""
        async {
            try {
                val imgUri = Firebase.storage.getReferenceFromUrl(abThumbnail).downloadUrl.await()
                albumsList.add(Album(albumID, songsInAlbum, abTitle, imgUri.toString()))
            } catch (e: Exception) {
                Log.e("onChecking", "Error fetching album: $abTitle")
                songsInAlbum
            }
        }
    }

    deferredABList.awaitAll()
    return@coroutineScope albumsList
}


suspend fun fetchSongsFromFirestore(): List<Song> = coroutineScope {
    val songsCollection = db.collection("Music")
    val querySnapshot = songsCollection.get().await()
    val songsList = mutableListOf<Song>()

    val deferredList = querySnapshot.documents.map { document ->
        val songID = document.id
        val artist = document.getString("artist") ?: ""
        val genre = document.getString("genre") ?: ""
        val lyrics = document.getString("lyrics") ?: ""
        val thumbnail = document.getString("thumbnail") ?: ""
        val title = document.getString("title") ?: ""
        val url = document.getString("url") ?: ""

        async {
            try {
                val musicUri = Firebase.storage.getReferenceFromUrl(url).downloadUrl.await()
                val imgUri = Firebase.storage.getReferenceFromUrl(thumbnail).downloadUrl.await()
                val song = Song(songID, artist, genre, lyrics, imgUri.toString(), title, musicUri.toString())
                songsList.add(song)
            } catch (e: Exception) {
                Log.e("FetchSongs", "Error fetching song: $title")
            }
        }
    }

    deferredList.awaitAll()
    return@coroutineScope songsList
}


suspend fun fetchFavoriteSongsFromFirestore(userID: String, songs: List<Song>): List<Album> = coroutineScope {
    val favoriteCollection = db.collection("User").document(userID)
    val favoriteSongQuerySnapshot = favoriteCollection.collection("FavoriteSongs").get().await()
    val favoriteSongsList =  mutableListOf<Album>()

    val deferredABList = favoriteSongQuerySnapshot.documents.map { document ->
        val favSongsID = document.id
        val favSongsTitle = document.getString("title") ?: ""
        val favSongsThumbnail = document.getString("thumbnail") ?: ""
        val favSongsSongIDs = document.getString("songIDs") ?: ""
        var songsInAlbum = emptyList<Song>()

        if (favSongsSongIDs != "") {
            val listIDs: List<String> = favSongsSongIDs.split(",")

            songsInAlbum = songs.filter { song ->
                listIDs.contains(song.songID)
            }
        }

        async {
            try {
                val imgUri = Firebase.storage.getReferenceFromUrl(favSongsThumbnail).downloadUrl.await()
                favoriteSongsList.add(Album(favSongsID, songsInAlbum, favSongsTitle, imgUri.toString()))
            } catch (e: Exception) {
                Log.e("onChecking", "Error fetching album: $favSongsTitle")
                songsInAlbum
            }
        }
    }
    deferredABList.awaitAll()
    return@coroutineScope favoriteSongsList
}

suspend fun fetchFavoriteAlbumsFromFirestore(userID: String, albums: List<Album>): List<FavAlbum> = coroutineScope {
    val favoriteCollection = db.collection("User").document(userID)
    val favoriteSongQuerySnapshot = favoriteCollection.collection("FavoriteAlbums").get().await()
    val favoriteAlbumsList = mutableListOf<FavAlbum>()

    val deferredABList = favoriteSongQuerySnapshot.documents.map { document ->
        val favSongsID = document.id
        val favSongsTitle = document.getString("title") ?: ""
        val favSongsThumbnail = document.getString("thumbnail") ?: ""
        val favAlbumIDs = document.getString("albumIDs") ?: ""
        var albumsInAlbum = emptyList<Album>()
        Log.d("onLibrary", document.id + ": " + favSongsTitle + ", " + favAlbumIDs + ".")

        if (favAlbumIDs != "") {
            val listIDs: List<String> = favAlbumIDs.split(",")

            albumsInAlbum = albums.filter { album ->
                listIDs.contains(album.albumID)
            }
        }

        async {
            try {
                val imgUri = Firebase.storage.getReferenceFromUrl(favSongsThumbnail).downloadUrl.await()
                favoriteAlbumsList.add(FavAlbum(favSongsID, albumsInAlbum, favSongsTitle, imgUri.toString()))
            } catch (e: Exception) {
                Log.e("onChecking", "Error fetching album: $favSongsTitle")
                favoriteAlbumsList
            }
        }
    }
    deferredABList.awaitAll()
    return@coroutineScope favoriteAlbumsList
}

suspend fun add2FavSongs(context: Context, userID: String, songID: String): Boolean {
    val userRef = db.collection("User").document(userID)
    val favoriteAlbumsRef = userRef.collection("FavoriteSongs").document("FavoriteSongs")

    return try {
        val documentSnapshot = favoriteAlbumsRef.get().await()
        val currentSongIDs = documentSnapshot.getString("songIDs") ?: ""

        val updatedSongIDs = if (currentSongIDs.isNotEmpty()) {
            "$currentSongIDs,$songID"
        } else {
            songID
        }

        favoriteAlbumsRef.update("songIDs", updatedSongIDs).await()
        Toast.makeText(context, "Added to Favorite Songs", Toast.LENGTH_SHORT).show()
        true
    } catch (e: Exception) {
        when (e) {
            is FirebaseFirestoreException, is IOException -> {
                Toast.makeText(context, "Fail to added", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(context, "Fail to get data", Toast.LENGTH_SHORT).show()
            }
        }
        false
    }
}

suspend fun delFavSong(context: Context, userID: String, songID: String): Boolean {
    val userRef = db.collection("User").document(userID)
    val favoriteAlbumsRef = userRef.collection("FavoriteSongs").document("FavoriteSongs")

    return try {
        val documentSnapshot = favoriteAlbumsRef.get().await()
        var currentSongIDs = documentSnapshot.getString("songIDs") ?: ""

        currentSongIDs = ",$currentSongIDs,"
        currentSongIDs = currentSongIDs.replace(",$songID,", ",")

        if (currentSongIDs.first() == ',') {
            currentSongIDs.drop(1)
        }
        if (currentSongIDs.last() == ',') {
            currentSongIDs.dropLast(1)
        }
        if (currentSongIDs.isNotEmpty())
            currentSongIDs
        else
            currentSongIDs = ""

        favoriteAlbumsRef.update("songIDs", currentSongIDs).await()
        Toast.makeText(context, "Deleted from Favorite Songs", Toast.LENGTH_SHORT).show()
        true
    } catch (e: Exception) {
        when (e) {
            is FirebaseFirestoreException, is IOException -> {
                Toast.makeText(context, "Fail to delete", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(context, "Fail to get data", Toast.LENGTH_SHORT).show()
            }
        }
        false
    }
}

suspend fun add2FavAlbums(context: Context, userID: String, albumID: String): Boolean {
    val userRef = db.collection("User").document(userID)
    val favoriteAlbumsRef = userRef.collection("FavoriteAlbums").document("FavoriteAlbums")

    return try {
        val documentSnapshot = favoriteAlbumsRef.get().await()
        var currentAlbumIDs = documentSnapshot.getString("albumIDs") ?: ""

        val updatedAlbumIDs = if (currentAlbumIDs.isNotEmpty()) {
            "$currentAlbumIDs,$albumID"
        } else {
            albumID
        }

        favoriteAlbumsRef.update("albumIDs", updatedAlbumIDs).await()
        Toast.makeText(context, "Added to Favorite Albums", Toast.LENGTH_SHORT).show()
        true
    } catch (e: Exception) {
        when (e) {
            is FirebaseFirestoreException, is IOException -> {
                Toast.makeText(context, "Fail to add", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(context, "Fail to get data", Toast.LENGTH_SHORT).show()
            }
        }
        false
    }
}

// Xoá album yêu thích
suspend fun delFavAlbum(context: Context, userID: String, albumID: String): Boolean {
    val userRef = db.collection("User").document(userID)
    val favoriteAlbumsRef = userRef.collection("FavoriteAlbums").document("FavoriteAlbums")

    return try {
        val documentSnapshot = favoriteAlbumsRef.get().await()
        var currentAlbumIDs = documentSnapshot.getString("albumIDs") ?: ""

        currentAlbumIDs = ",$currentAlbumIDs,"
        currentAlbumIDs = currentAlbumIDs.replace(",$albumID,", ",")

        if (currentAlbumIDs.first() == ',') {
            currentAlbumIDs.drop(1)
        }
        if (currentAlbumIDs.last() == ',') {
            currentAlbumIDs.dropLast(1)
        }
        if (currentAlbumIDs.isNotEmpty())
            currentAlbumIDs
        else
            currentAlbumIDs = ""

        favoriteAlbumsRef.update("albumIDs", currentAlbumIDs).await()
        Toast.makeText(context, "Deleted from Favorite Albums", Toast.LENGTH_SHORT).show()
        true
    } catch (e: Exception) {
        when (e) {
            is FirebaseFirestoreException, is IOException -> {
                Toast.makeText(context, "Fail to delete", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(context, "Fail to get data", Toast.LENGTH_SHORT).show()
            }
        }
        false
    }
}