package com.example.muzik.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.media3.exoplayer.ExoPlayer
import com.example.muzik.ui.home.Album
import com.example.muzik.ui.home.FavAlbum
import com.example.muzik.ui.home.Song

// Singleton
object ExoPlayerManager {
    private var exoPlayer: ExoPlayer? = null

    var songs: List<Song>? = null
    var albums: List<Album>? = null
    var favSongs: List<Album>? = null
    var favAlbums: List<FavAlbum>? = null
    var isLoaded: Boolean = false

    fun getPlayerInstance(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
        }
        return exoPlayer!!
    }

    fun saveData(
        newSongs: List<Song>,
        newAlbums: List<Album>,
        newFavSongs: List<Album>,
        newFavAlbums: List<FavAlbum>
    ) {
        songs = newSongs
        albums = newAlbums
        favSongs = newFavSongs
        favAlbums = newFavAlbums
        isLoaded = true
    }
}