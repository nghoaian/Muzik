package com.example.muzik.ui.library

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.muzik.R
import com.example.muzik.ui.ExoPlayerManager
import com.example.muzik.ui.home.AddToPlayList
import com.example.muzik.ui.home.Album
import com.example.muzik.ui.home.AlbumDetailSheet
import com.example.muzik.ui.home.CreatePlaylist
import com.example.muzik.ui.home.FavAlbum
import com.example.muzik.ui.home.HomeViewModel
import com.example.muzik.ui.home.IndeterminateCircularIndicator
import com.example.muzik.ui.home.MusicPlayerControl
import com.example.muzik.ui.home.OptionsSheet
import com.example.muzik.ui.home.Song
import com.example.muzik.ui.home.background_color
import com.example.muzik.ui.home.colorStops
import com.example.muzik.ui.home.updateSliderValue
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class LibraryFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        lateinit var player: ExoPlayer
        val view = ComposeView(requireContext()).apply {
            setContent {
                val sharedPref = context.getSharedPreferences("MuzikPrefs", Context.MODE_PRIVATE)
                val userID = sharedPref.getString("userID", "")
                player = ExoPlayerManager.getPlayerInstance(requireContext())
                if (!ExoPlayerManager.isLoaded) {
                    RenderLibrary(
                        player,
                        HomeViewModel(userID.toString()),
                    )
                }
                else {
                    RenderLibrary(
                        player,
                    )
                }
            }
        }
        return view
    }
    // Bắt sự kiện khi người dùng nhấn nút back
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            requireActivity().moveTaskToBack(false)
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class) @OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderLibrary(player: ExoPlayer, viewModel: HomeViewModel? = null) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("MuzikPrefs", Context.MODE_PRIVATE)
    val userID = sharedPref.getString("userID", "")

    if (viewModel != null) {
        val songs: List<Song> by viewModel.songs.observeAsState(listOf())
        val albums: List<Album> by viewModel.albums.observeAsState(listOf())
        val favSongs: List<Album> by viewModel.favSongs.observeAsState(listOf())
        val favAlbums: List<FavAlbum> by viewModel.favAlbums.observeAsState(listOf())
        val isLoadingProgress: Int by viewModel.isLoading.observeAsState(initial = 0)
        IndeterminateCircularIndicator(isLoadingProgress, 100)

        ExoPlayerManager.saveData(songs, albums, favSongs, favAlbums)
    }

    val songs: List<Song> = ExoPlayerManager.songs!!
    val favSongs: List<Album> = ExoPlayerManager.favSongs!!
    val favAlbums: List<FavAlbum> = ExoPlayerManager.favAlbums!!

    var favSongIDs: String = ""
    favSongs.forEach { album ->
        album.songs.forEach { song ->
            favSongIDs += song.songID + ","
        }
    }
    val favSongIDsList = favSongIDs.dropLast(1).split(",").toMutableList()

    var favAlbumIDs: String = ""
    favAlbums.forEach { favAlbum ->
        favAlbum.albums.forEach { album ->
            favAlbumIDs += album.albumID + ","
        }
    }
    val favAlbumIDsList = favAlbumIDs.dropLast(1).split(",").toMutableList()

    val colorStops = arrayOf(
        0f to Color(0xFF2F2F2F),
        1f to Color(0xFF0F0F0F),
    )

    var currentProgress by remember { mutableFloatStateOf(0f) }
    var currentDuration by remember { mutableFloatStateOf(0f) }
    var currentTitle by remember { mutableStateOf("") }
    var currentArtist by remember { mutableStateOf("") }
    var currentThumbnail by remember { mutableStateOf("") }
    var currentLyric by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(false) }
    var isRepeat by remember { mutableStateOf(false) }
    var isRepeatAll by remember { mutableStateOf(false) }
    var isAlbumDetailVisible by remember { mutableStateOf(false) }
    var isPlayerInit by remember { mutableStateOf(false) }
    var currentClickedSong by remember { mutableStateOf<Song?>(null) }
    var currentClickedAlbum by remember { mutableStateOf<Album?>(null) }
    val albumDetailState = rememberModalBottomSheetState(true)
    val optionsState = rememberModalBottomSheetState()
    var isOptionsVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var currentClickedFavAlbum by remember { mutableStateOf<FavAlbum?>(null) }
    var isFavAlbumsVisible by remember { mutableStateOf(false) }
    val favAlbumsState = rememberModalBottomSheetState(true)
    var isFavAlbum by remember { mutableStateOf(false) }
    var isFavSong by remember { mutableStateOf(false) }

    val onOptionsVisible: (Boolean) -> Unit = { isVisible ->
        isOptionsVisible = isVisible
    }

    val onSliderChange: (Float) -> Unit = {progress ->
        currentProgress = progress
    }

    val onPlayPauseToggle: () -> Unit = {
        isPlaying = !isPlaying
        if (isPlaying) {
            player.play()
        }
        else {
            player.pause()
        }
    }

    val onPlay: () -> Unit = {
        isPlaying = true
    }

    val onClickedSong: (Song) -> Unit = { newSong ->
        currentClickedSong = newSong
    }

    val onMoving: (String, String, String, String) -> Unit = { title, artist, thumbnail, lyric ->
        if (title != "null") {
            if (currentTitle != title) {
                currentTitle = title
                currentArtist = artist
                currentLyric = lyric
                currentThumbnail = thumbnail
                isPlayerInit = true
            }
            onPlay()
        }
        else {
            currentTitle = "Wait for music to play"
            currentArtist = "..."
            currentLyric = ""
            currentThumbnail = ""
        }
    }

    val onNewPosition: (Float) -> Unit = { newPosition ->
        player.seekTo(((newPosition / 100 * currentDuration * 1000f)).toLong())
    }

    val onShuffle: () -> Unit = {
        isShuffle = !isShuffle
        player.shuffleModeEnabled = isShuffle
        if (isShuffle) Toast.makeText(context, "Shuffled", Toast.LENGTH_SHORT).show()
        else Toast.makeText(context, "Ordered", Toast.LENGTH_SHORT).show()
    }

    val onRepeat: () -> Unit = {
        isRepeat = !isRepeat
        if (isRepeat) {
            player.repeatMode = Player.REPEAT_MODE_ONE
        }
        else
            player.repeatMode = Player.REPEAT_MODE_OFF
    }

    val onRepeatAll: () -> Unit = {
        if (!isRepeatAll) {
            player.repeatMode = Player.REPEAT_MODE_ALL
            if (isShuffle) Toast.makeText(context, "Repeat: ON", Toast.LENGTH_SHORT).show()
        }
        else {
            player.repeatMode = Player.REPEAT_MODE_OFF
            if (isShuffle) Toast.makeText(context, "Repeat: OFF", Toast.LENGTH_SHORT).show()
        }
        isRepeatAll = !isRepeatAll
    }

    val onPlayAlbum: (List<Song>) -> Unit = {
        for (i in it.indices) {
            if (i == 0) {
                CreatePlaylist(player, it[i].songID, it[i].title, it[i].artist, it[i].lyrics, it[i].thumbnail, it[i].url, onMoving)
                player.play()
            }
            else {
                AddToPlayList(player, it[i], context, onMoving)
            }
        }
    }

    val onAlbumDetailVisible: (Boolean) -> Unit = {
        isAlbumDetailVisible = it
    }

    val onCurrentClickedSong: (Song) -> Unit = {
        currentClickedSong = it
    }

    val onFavAlbum: (String) -> Unit = {
        isFavAlbum = favAlbumIDsList.contains(it)
    }

    val onFavSong: (String) -> Unit = {
        isFavSong = favSongIDsList.contains(it)
    }

    val onAdd2FavSongs: (String) -> Unit = {
        favSongIDsList.add(it)
        ExoPlayerManager.isLoaded = false
    }

    val onDelFavSong: (String) -> Unit = {
        favSongIDsList.remove(it)
        ExoPlayerManager.isLoaded = false
    }

    val onAdd2FavAlbums: (String) -> Unit = {
        favAlbumIDsList.add(it)
        ExoPlayerManager.isLoaded = false
    }

    val onDelFavAlbum: (String) -> Unit = {
        favAlbumIDsList.remove(it)
        ExoPlayerManager.isLoaded = false
    }

    val onNavigation: () -> Unit = {
        if (player.currentMediaItem?.mediaMetadata?.title == null) {
            currentTitle = "Wait for music to play"
            currentArtist = "..."
            currentLyric = ""
            currentThumbnail = ""
        }
        else {
            val playerSongID = player.currentMediaItem?.mediaMetadata?.trackNumber.toString()
            currentTitle = player.currentMediaItem?.mediaMetadata?.title.toString()
            currentArtist = player.currentMediaItem?.mediaMetadata?.artist.toString()
            currentLyric = player.currentMediaItem?.mediaMetadata?.description.toString()
            currentThumbnail = player.currentMediaItem?.mediaMetadata?.artworkUri.toString()
            currentDuration = (player.duration.toDouble() / 999).roundToInt().toFloat()
            updateSliderValue(player, currentDuration, onSliderChange, onMoving)

            val filterSong = songs.filter {
                it.songID == playerSongID
            }
            if (filterSong.isNotEmpty()) {
                currentClickedSong = filterSong[0]
            }

            onFavSong(currentClickedSong?.songID ?: "")

            isPlaying = if (player.playbackState != 4)
                player.playWhenReady
            else
                false
        }
    }

    player.addListener(object : Player.Listener {
        @Deprecated("Deprecated in Java")
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Log.d("onCheck", (playbackState.toString() + "_" + ExoPlayer.STATE_READY.toString() + "_" + playWhenReady))
            if (playbackState == ExoPlayer.STATE_READY && playWhenReady) {
                val realDurationMillis: Long = player.duration
                currentDuration = (realDurationMillis.toDouble() / 999).roundToInt().toFloat()
                updateSliderValue(player, currentDuration, onSliderChange, onMoving)
            }

            if (playbackState == ExoPlayer.STATE_ENDED) {
                isPlaying = false
            }
        }
    })


    val onCurrentClickedAlbum: (Album) -> Unit = { album ->
        currentClickedAlbum = album
    }

    val onFavAlbumsVisible: (Boolean) -> Unit = { isVisible ->
        isFavAlbumsVisible = isVisible
    }

    player.addListener(object : Player.Listener {
        @Deprecated("Deprecated in Java")
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Log.d("onCheck", (playbackState.toString() + ExoPlayer.STATE_READY.toString() + playWhenReady))
            if (playbackState == ExoPlayer.STATE_READY && playWhenReady) {
                val realDurationMillis: Long = player.duration
                currentDuration = (realDurationMillis.toDouble() / 999).roundToInt().toFloat()
                updateSliderValue(
                    player,
                    currentDuration,
                    onSliderChange,
                    onMoving
                )
            }
            if (playbackState == ExoPlayer.STATE_ENDED) {
                isPlaying = false
            }
        }
    })

    onNavigation()

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = Color(0xFF0F0F0F)
    ) {
        Box(
            modifier = Modifier.fillMaxHeight(0.5f)
        ) {
            // Album page -------------------------------------------------------------------
            currentClickedAlbum?.let {
                AlbumDetailSheet(player,
                    userID!!,
                    isAlbumDetailVisible,
                    onAlbumDetailVisible,
                    albumDetailState,
                    it,
                    isShuffle,
                    onShuffle,
                    onPlayAlbum,
                    onOptionsVisible,
                    onMoving,
                    onCurrentClickedSong,
                    isFavAlbum,
                    onFavAlbum,
                    onAdd2FavAlbums,
                    onDelFavAlbum
                )
            }
            // End Album page -------------------------------------------------------------------

            // Options page -------------------------------------------------------------------
            OptionsSheet (
                player,
                isOptionsVisible,
                onOptionsVisible,
                optionsState,
                currentClickedSong,
                onMoving,
            )
            // End Options page -------------------------------------------------------------------

            currentClickedFavAlbum?.let {
                FavAlbumsSheet(
                    player,
                    isFavAlbumsVisible,
                    onFavAlbumsVisible,
                    favAlbumsState,
                    it,
                    onCurrentClickedAlbum,
                    onAlbumDetailVisible
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F0F))
                    .padding(0.dp, 16.dp, 0.dp, 4.dp),
                verticalAlignment = Alignment.CenterVertically
            )
            {
                Text(
                    modifier = Modifier.padding(12.dp, 0.dp, 0.dp, 0.dp),
                    text = "Library",
                    fontFamily = FontFamily(Font(R.font.magistral_bold)),
                    fontSize = 24.sp,
                    color = Color.White,
                )
            }
        }

        Box (
            Modifier.padding(bottom = 52.dp, top = 64.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                userScrollEnabled = true
            ) {
                items(favSongs.size) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp)
                            .clickable {
                                scope.launch {
                                    currentClickedAlbum = favSongs[index]
                                    isAlbumDetailVisible = true
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .fillMaxHeight()
                        ) {
                            AsyncImage(
                                model = favSongs[index].thumbnail,
                                contentDescription = "Album Thumnail",
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = R.drawable.ic_record),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .fillMaxHeight()
                                .align(Alignment.CenterVertically)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(12.dp, 0.dp),
                                text = favSongs[index].title,
                                color = Color.White,
                                fontFamily = FontFamily(Font(R.font.magistral_medium)),
                                fontSize = 20.sp,
                                lineHeight = 20.sp,
                                letterSpacing = 2.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                items(favAlbums.size) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                            .clickable {
                                scope.launch {
                                    currentClickedFavAlbum = favAlbums[index]
                                    isFavAlbumsVisible = true
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .fillMaxHeight()
                        ) {
                            AsyncImage(
                                model = favAlbums[index].thumbnail,
                                contentDescription = "Album Thumnail",
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = R.drawable.ic_album),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .fillMaxHeight()
                                .align(Alignment.CenterVertically)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(12.dp, 0.dp),
                                text = favAlbums[index].title,
                                color = Color.White,
                                fontFamily = FontFamily(Font(R.font.magistral_medium)),
                                fontSize = 20.sp,
                                lineHeight = 20.sp,
                                letterSpacing = 2.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        Box() {
            MusicPlayerControl(
                isPlayerInit = true,
                userID!!,
                currentClickedSong,
                currentTitle,
                currentArtist,
                currentThumbnail,
                currentProgress,
                currentDuration,
                currentLyric,
                player,
                isPlaying,
                isShuffle,
                isRepeat,
                isRepeatAll,
                onRepeatAll,
                onPlayPauseToggle,
                onMoving,
                onNewPosition,
                onShuffle,
                onRepeat,
                isFavSong,
                onFavSong,
                onAdd2FavSongs,
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavAlbumsSheet(
    player: ExoPlayer,
    isFavAlbumsVisible: Boolean,
    onFavAlbumsVisible: (Boolean) -> Unit,
    favAlbumsState : SheetState,
    currentClickedFavAlbum: FavAlbum,
    onCurrentClickedAlbum: (Album) -> Unit,
    onAlbumDetailVisible: (Boolean) -> Unit
) {
    if (isFavAlbumsVisible) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight(),
            onDismissRequest = {
                onFavAlbumsVisible(false)
            },
            sheetState = favAlbumsState,
            containerColor = Color(0xFF191919),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            dragHandle = {
                Row(
                    modifier = Modifier.padding(0.dp, 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        modifier = Modifier.size(36.dp),
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "",
                        tint = Color.White
                    )
                    Text(
                        text = "Scroll down to close",
                        fontFamily = FontFamily(Font(R.font.magistral_light))
                    )
                }
            }
        ) {
            val colorStops = arrayOf(
                0f to Color(0xFF191919),
                0.5f to Color(0xFF333333),
                1f to Color(0xFF191919),
            )
            Box (Modifier.fillMaxWidth().height(192.dp).background(Brush.verticalGradient(colorStops = colorStops))) {
                Box(
                    modifier = Modifier
                        .size(192.dp)
                        .clip(CircleShape)
                        .align((Alignment.Center)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = currentClickedFavAlbum.thumbnail,
                        contentDescription = "",
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.ic_album),
                        modifier = Modifier
                            .background(Color.DarkGray)
                            .size(192.dp)
                            .clip(CircleShape)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(
                        text = currentClickedFavAlbum.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontFamily = FontFamily(Font(R.font.magistral_bold)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier.padding(16.dp, 0.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    userScrollEnabled = true
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    val albumsInAlbum = currentClickedFavAlbum.albums
                    items(albumsInAlbum.size) { index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCurrentClickedAlbum(albumsInAlbum[index])
                                        onAlbumDetailVisible(true)
                                    }
                            ) {
                                Row(
                                ) {
                                    Box() {
                                        AsyncImage(
                                            model = albumsInAlbum[index].thumbnail,
                                            contentDescription = "",
                                            contentScale = ContentScale.Crop,
                                            placeholder = painterResource(id = R.drawable.ic_record),
                                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                                                .border(BorderStroke(0.5.dp, Color.Black), RoundedCornerShape(8.dp))
                                        )
                                    }
                                    Box() {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 10.dp).fillMaxHeight()
                                        ) {
                                            Text(
                                                text = albumsInAlbum[index].title,
                                                color = Color.White,
                                                fontFamily = FontFamily(
                                                    Font(
                                                        R.font.magistral_medium
                                                    )
                                                ),
                                                fontSize = 16.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}