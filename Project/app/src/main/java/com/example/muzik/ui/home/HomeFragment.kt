package com.example.muzik.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import coil.compose.AsyncImage
import com.example.muzik.R
import com.example.muzik.ui.ExoPlayerManager
import com.example.muzik.ui.login.GoogleAuthUIClient
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import kotlin.math.roundToInt

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)

        lateinit var player: ExoPlayer

        val view = ComposeView(requireContext()).apply {
            setContent {
                val sharedPref = context.getSharedPreferences("MuzikPrefs", MODE_PRIVATE)
                val userID = sharedPref.getString("userID", "")
                player = ExoPlayerManager.getPlayerInstance(requireContext())

                if (!ExoPlayerManager.isLoaded) {
                    RenderHome(
                        player,
                        HomeViewModel(userID.toString()),
                        navController = findNavController(),
                        bottomNavigationView
                    )
                }
                else {
                    RenderHome(
                        player,
                        navController = findNavController(),
                        bottomNavigationView = bottomNavigationView
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

val background_color = Color(0xFF0F0F0F)
var sliderUpdateJob: Job? = null

@Composable
fun IndeterminateCircularIndicator(
    isLoadingProgress: Int,
    removeAt: Int,
    bottomNavigationView : BottomNavigationView? = null
) {
    if (isLoadingProgress+1 < removeAt) {
        Surface(
            modifier = Modifier.zIndex(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background_color),
                contentAlignment = Alignment.Center
            ) {
                Column (
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    CircularProgressIndicator(
                        strokeWidth = 6.dp,
                        modifier = Modifier
                            .background(background_color)
                            .width(48.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Text(
                        modifier = Modifier.padding(0.dp, 12.dp, 0.dp, 0.dp),
                        text = "$isLoadingProgress %",
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily(Font(R.font.magistral_light)),
                        fontSize = 20.sp,
                        color = Color.White,
                    )
                }
            }
        }
    }
    else {
        if (bottomNavigationView != null) {
            bottomNavigationView.visibility = View.VISIBLE
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@androidx.annotation.OptIn(UnstableApi::class) @OptIn(ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun RenderHome(player : ExoPlayer, viewModel: HomeViewModel? = null, navController: NavController, bottomNavigationView : BottomNavigationView) {
    if (viewModel != null) {
        val songs: List<Song> by viewModel.songs.observeAsState(listOf())
        val albums: List<Album> by viewModel.albums.observeAsState(listOf())
        val favSongs: List<Album> by viewModel.favSongs.observeAsState(listOf())
        val favAlbums: List<FavAlbum> by viewModel.favAlbums.observeAsState(listOf())
        val isLoadingProgress: Int by viewModel.isLoading.observeAsState(initial = 0)
        IndeterminateCircularIndicator(isLoadingProgress, 50,  bottomNavigationView)

        ExoPlayerManager.saveData(songs, albums, favSongs, favAlbums)
    }

    val songs: List<Song> = ExoPlayerManager.songs!!
    val albums: List<Album> = ExoPlayerManager.albums!!
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

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val googleAuthUiClient by lazy {
        GoogleAuthUIClient(
            context = context,
            oneTapClient = Identity.getSignInClient(context)
        )
    }

//    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
//        // Show controls on lock screen even when user hides sensitive content.
//        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
//        .setSmallIcon(R.drawable.ic_stat_player)
//        // Add media control buttons that invoke intents in your media service
//        .addAction(R.drawable.ic_prev, "Previous", prevPendingIntent) // #0
//        .addAction(R.drawable.ic_pause, "Pause", pausePendingIntent) // #1
//        .addAction(R.drawable.ic_next, "Next", nextPendingIntent) // #2
//        // Apply the media style template.
//        .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
//            .setShowActionsInCompactView(1 /* #1: pause button \*/)
////            .setMediaSession(mediaSession.getSessionToken()))
////        .setContentTitle("Wonderful music")
////        .setContentText("My Awesome Band")
////        .setLargeIcon(albumArtBitmap)
////        .build()

    val sharedPref = context.getSharedPreferences("MuzikPrefs", MODE_PRIVATE)
    val userID = sharedPref.getString("userID", "")
    val username = sharedPref.getString("username", "")
    val avatarURL = sharedPref.getString("avatarURL", "")

    var isPlayerInit by remember { mutableStateOf(false) }
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var currentClickedSong by remember { mutableStateOf<Song?>(null) }
    var isAlbumDetailVisible by remember { mutableStateOf(false) }
    val albumDetailState = rememberModalBottomSheetState(true)
    var currentClickedAlbum by remember { mutableStateOf<Album?>(null) }
    var isOptionsVisible by remember { mutableStateOf(false) }
    var optionsState = rememberModalBottomSheetState()
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

    onNavigation()

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet (
                drawerContainerColor = Color(0xFF202020).copy(0.99f),
                drawerContentColor = Color.Black,
                drawerShape = RoundedCornerShape(8.dp)
            ) {
                Row (
                    modifier = Modifier
                        .padding(8.dp, 12.dp)
                        .clickable {
                            scope.launch {
                                drawerState.apply {
                                    if (isClosed) open() else close()
                                }
                            }
                        },
                ) {
                    AsyncImage(
                        model = if (avatarURL != "") avatarURL else R.drawable.ic_boyuser,
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.ic_boyuser),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(
                                CircleShape
                            )
                    )

                    Column {
                        Text(
                            modifier = Modifier.padding(10.dp, 0.dp, 0.dp, 0.dp),
                            text = username.toString(),
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily(Font(R.font.magistral_bold)),
                            fontSize = 20.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            modifier = Modifier.padding(10.dp, 2.dp, 0.dp, 0.dp),
                            text = "See profile",
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily(Font(R.font.magistral_book)),
                            fontSize = 12.sp,
                            color = Color.White,
                        )
                    }
                }

                Divider(thickness = 1.dp, color = Color.Gray)

                IconButton (
                    modifier = Modifier
                        .padding(0.dp, 4.dp)
                        .fillMaxWidth(),
                    onClick = {
                        //
                        scope.launch {
                            drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    }
                ) {
                    Row (
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier = Modifier,
                            painter = painterResource(R.drawable.ic_setting),
                            contentDescription = "Navigate to setting",
                            tint = Color.White
                        )
                        Text(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .padding(start = 16.dp),
                            text = "See profile",
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily(Font(R.font.magistral_medium)),
                            fontSize = 16.sp,
                            color = Color.White,
                        )
                    }
                }

                IconButton (
                    modifier = Modifier
                        .padding(0.dp, 4.dp)
                        .fillMaxWidth(),
                    onClick = {
                        googleAuthUiClient.signOut()
                        navController.navigate(R.id.navigation_login)
                        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                        bottomNavigationView.visibility = View.INVISIBLE
                        scope.launch {
                            drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    }
                ) {
                    Row (
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier = Modifier,
                            painter = painterResource(R.drawable.ic_logout),
                            contentDescription = "Log out",
                            tint = Color.White
                        )
                        Text(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .padding(start = 16.dp),
                            text = "Log out",
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily(Font(R.font.magistral_medium)),
                            fontSize = 16.sp,
                            color = Color.White,
                        )
                    }
                }
            }
        },
        drawerState = drawerState
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = background_color
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
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

                //
                LazyColumn(
                    modifier = Modifier
                        .padding(8.dp, 0.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = true
                )
                {
                    // Sticker Header for User Control -------------------------------------------------------------------
                    stickyHeader {
                        Row(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .wrapContentHeight()
                                .background(background_color)
                                .padding(4.dp, 16.dp, 0.dp, 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        )
                        {
                            AsyncImage(
                                model = if (avatarURL != "") avatarURL else R.drawable.ic_boyuser,
                                contentDescription = "User Avatar",
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = R.drawable.ic_boyuser),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(
                                        CircleShape
                                    )
                                    .clickable {
                                        scope.launch {
                                            drawerState.apply {
                                                if (isClosed) open() else close()
                                            }
                                        }
                                    }
                            )

                            Text(
                                modifier = Modifier.padding(10.dp, 0.dp, 0.dp, 0.dp),
                                text = username.toString(),
                                fontFamily = FontFamily(Font(R.font.magistral_bold)),
                                fontSize = 20.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Albums -------------------------------------------------------------------
                    item() {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .height(212.dp)
                                .padding(top = 8.dp)
                        ) {
                            items(if (albums.size < 7) albums.size else 6) { index ->
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF272727))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    currentClickedAlbum = albums[index]
                                                    isAlbumDetailVisible = true
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(60.dp)
                                                    .fillMaxHeight()
                                            ) {
                                                AsyncImage(
                                                    model = albums[index].thumbnail,
                                                    contentDescription = "Album Thumnail",
                                                    contentScale = ContentScale.Crop,
                                                    placeholder = painterResource(id = R.drawable.ic_album),
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(topStart = 4.dp))
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
                                                        .padding(6.dp, 0.dp),
                                                    text = albums[index].title,
                                                    color = Color.White,
                                                    fontFamily = FontFamily(Font(R.font.magistral_medium)),
                                                    fontSize = 15.sp,
                                                    lineHeight = 20.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (albums.size < 6) {
                                items(6 - albums.size) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF272727))
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(60.dp)
                                                        .fillMaxHeight()
                                                ) {
                                                    AsyncImage(
                                                        model = R.drawable.ic_album,
                                                        contentDescription = "Album Thumnail",
                                                        contentScale = ContentScale.Crop,
                                                        placeholder = painterResource(id = R.drawable.ic_album),
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(RoundedCornerShape(topStart = 4.dp))
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .weight(2f)
                                                        .fillMaxHeight()
                                                ) {
                                                    Text(
                                                        modifier = Modifier
                                                            .align(Alignment.CenterStart)
                                                            .padding(6.dp, 0.dp),
                                                        text = "Coming Soon",
                                                        fontStyle = FontStyle.Italic,
                                                        color = Color.White,
                                                        fontFamily = FontFamily(Font(R.font.magistral_bold)),
                                                        fontSize = 15.sp,
                                                        lineHeight = 20.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Musics
                    item() {
                        MusicList(
                            songs,
                            player,
                            currentClickedSong,
                            onCurrentClickedSong,
                            onMoving,
                            onClickedSong,
                            isOptionsVisible,
                            onOptionsVisible,
                            optionsState
                        )
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
                        onDelFavSong
                    )
                }
            }
        }
    }
}

fun preparePrevious(player: ExoPlayer, context: Context, onMoving: (String, String, String, String) -> Unit) {
    if (player.hasPreviousMediaItem()) {
        Log.d(
            "OnMoving",
            "Current: " + player.currentMediaItem?.mediaMetadata?.title.toString() + "__" + player.currentMediaItem?.mediaMetadata?.artist.toString() + "__" + player.currentMediaItem?.mediaMetadata?.artworkUri.toString()
        )
        player.seekToPrevious()
        Log.d(
            "OnMoving",
            "Previous To: " + player.currentMediaItem?.mediaMetadata?.title.toString() + "__" + player.currentMediaItem?.mediaMetadata?.artist.toString() + "__" + player.currentMediaItem?.mediaMetadata?.artworkUri.toString()
        )
        onMoving(
            player.currentMediaItem?.mediaMetadata?.title.toString(),
            player.currentMediaItem?.mediaMetadata?.artist.toString(),
            player.currentMediaItem?.mediaMetadata?.artworkUri.toString(),
            player.currentMediaItem?.mediaMetadata?.description.toString()
        )
    } else {
        Toast.makeText(context, "No previous song", Toast.LENGTH_SHORT)
            .show()
    }
}
fun prepareNext(player: ExoPlayer, context: Context, onMoving: (String, String, String, String) -> Unit) {
    if (player.hasNextMediaItem()) {
        Log.d(
            "OnMoving",
            "Current: " + player.currentMediaItem?.mediaMetadata?.title.toString() + "__" + player.currentMediaItem?.mediaMetadata?.artist.toString() + "__" + player.currentMediaItem?.mediaMetadata?.artworkUri.toString()
        )
        player.seekToNext()
        Log.d(
            "OnMoving",
            "Next To: " + player.currentMediaItem?.mediaMetadata?.title.toString() + "__" + player.currentMediaItem?.mediaMetadata?.artist.toString() + "__" + player.currentMediaItem?.mediaMetadata?.artworkUri.toString()
        )
        onMoving(
            player.currentMediaItem?.mediaMetadata?.title.toString(),
            player.currentMediaItem?.mediaMetadata?.artist.toString(),
            player.currentMediaItem?.mediaMetadata?.artworkUri.toString(),
            player.currentMediaItem?.mediaMetadata?.description.toString()
        )
    } else {
        Toast.makeText(context, "This is last song", Toast.LENGTH_SHORT)
            .show()
    }
}
fun prepareNext(player: ExoPlayer, onMoving: (String, String, String, String) -> Unit) {
    if (player.hasNextMediaItem()) {
        Log.d(
            "OnMoving",
            "Current: " + player.currentMediaItem?.mediaMetadata?.title.toString() + "__" + player.currentMediaItem?.mediaMetadata?.artist.toString() + "__" + player.currentMediaItem?.mediaMetadata?.artworkUri.toString()
        )
        player.seekToNext()
        Log.d(
            "OnMoving",
            "Next To: " + player.currentMediaItem?.mediaMetadata?.title.toString() + "__" + player.currentMediaItem?.mediaMetadata?.artist.toString() + "__" + player.currentMediaItem?.mediaMetadata?.artworkUri.toString()
        )
        onMoving(
            player.currentMediaItem?.mediaMetadata?.title.toString(),
            player.currentMediaItem?.mediaMetadata?.artist.toString(),
            player.currentMediaItem?.mediaMetadata?.artworkUri.toString(),
            player.currentMediaItem?.mediaMetadata?.description.toString()
        )
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun updateSliderValue(
    player: ExoPlayer,
    duration: Float,
    onSliderValueChange: (Float) -> Unit,
    onMoving: (String, String, String, String) -> Unit)
{
    sliderUpdateJob?.cancel() // Hủy bỏ coroutine trước khi khởi tạo lại

    sliderUpdateJob = GlobalScope.launch(Dispatchers.Main) {
        var progress: Float = 0f
        while (isActive && player.isPlaying && progress < 101f) {
            delay(100)

            if (progress > 99) {
                prepareNext(player, onMoving)
            }
            if (duration > 0) {
                val currentPositionSeconds = player.currentPosition / 999f
                progress = (currentPositionSeconds / duration) * 100
                onSliderValueChange(progress)
            }
        }
    }
}

fun CreatePlaylist(player: ExoPlayer, id: String, title: String, artist: String, lyric: String, thumbnail: String, url: String, onMoving: (String, String, String, String) -> Unit) {
    val mediaMetadata = MediaMetadata
        .Builder()
        .setTrackNumber(id.toInt())
        .setTitle(title)
        .setArtist(artist)
//        .setSubtitle(url)
        .setDescription(lyric)
        .setArtworkUri(
            Uri.parse(thumbnail)
        )
        .build()

    val mediaItem = MediaItem
        .Builder()
        .setUri(url)
        .setMediaMetadata(mediaMetadata)
        .build()

    onMoving(title, artist, thumbnail, lyric)

    player.setMediaItem(mediaItem)
    player.prepare()
}

fun AddNextSongToPlayList(player: ExoPlayer, song: Song?, context: Context, onMoving: (String, String, String, String) -> Unit) {
    if (player.mediaItemCount > 0 && song != null) {
        val mediaMetadata = MediaMetadata
            .Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setDescription(song.lyrics)
            .setArtworkUri(
                Uri.parse(song.thumbnail)
            )
            .build()

        val mediaItem = MediaItem
            .Builder()
            .setUri(song.url)
            .setMediaMetadata(mediaMetadata)
            .build()

        player.addMediaItem(player.currentMediaItemIndex + 1, mediaItem)
        player.prepare()

        if (player.playbackState == 4) {
            player.seekToNext()
            player.play()
            onMoving(song.title, song.artist, song.thumbnail, song.lyrics)
        }

        Toast.makeText(context, "Added next song", Toast.LENGTH_SHORT).show()
    }
    else {
        if (song != null) {
            CreatePlaylist(player, song.songID, song.title, song.artist, song.lyrics, song.thumbnail, song.url, onMoving)
            player.play()
        }
    }
}

fun AddToPlayList(player: ExoPlayer, song: Song?, context: Context, onMoving: (String, String, String, String) -> Unit) {
    if (player.mediaItemCount > 0 && song != null) {
        val mediaMetadata = MediaMetadata
            .Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setDescription(song.lyrics)
            .setArtworkUri(
                Uri.parse(song.thumbnail)
            )
            .build()

        val mediaItem = MediaItem
            .Builder()
            .setUri(song.url)
            .setMediaMetadata(mediaMetadata)
            .build()

        player.addMediaItem(mediaItem)
        player.prepare()

        if (player.playbackState == 4) {
            player.seekToNext()
            player.play()
            onMoving(song.title, song.artist, song.thumbnail, song.lyrics)
        }

        Toast.makeText(context, "Added next song", Toast.LENGTH_SHORT).show()
    }
    else {
        if (song != null) {
            CreatePlaylist(player, song.songID, song.title, song.artist, song.lyrics, song.thumbnail, song.url, onMoving)
            player.play()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailSheet(
    player: ExoPlayer,
    userID: String,
    isAlbumDetailVisible: Boolean,
    onAlbumDetailVisible: (Boolean) -> Unit,
    albumDetailState : SheetState,
    currentClickedAlbum: Album,
    isShuffle: Boolean,
    onShuffle: () -> Unit,
    onPlayAlbum: (List<Song>) -> Unit,
    onOptionsVisible: (Boolean) -> Unit,
    onMoving: (String, String, String, String) -> Unit,
    onCurrentClickedSong: (Song) -> Unit,
    isFavAlbum: Boolean,
    onFavAlbum: (String) -> Unit,
    onAdd2FavAlbums: (String) -> Unit,
    onDelFavAlbum: (String) -> Unit,
    isFromFav: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (isAlbumDetailVisible) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight(),
            onDismissRequest = {
                onAlbumDetailVisible(false)
            },
            sheetState = albumDetailState,
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
            var imgSize = 248.dp
            if (isFromFav) imgSize = 192.dp

            Box(
                modifier = Modifier
                    .size(imgSize)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            ) {
                AsyncImage(
                    model = if (currentClickedAlbum.thumbnail != "") currentClickedAlbum.thumbnail else R.drawable.ic_album,
                    contentDescription = "",
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(id = R.drawable.ic_album),
                    modifier = Modifier
                        .shadow(
                            36.dp,
                            ambientColor = Color.Black,
                            spotColor = Color.White
                        )
                        .background(Color.DarkGray)
                        .size(imgSize)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.25f)
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(
                        text = currentClickedAlbum.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontFamily = FontFamily(Font(R.font.magistral_bold)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (!isFromFav) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.65f)
                                    .fillMaxHeight(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(
                                    modifier = Modifier.size(24.dp),
                                    onClick = {
                                        if (!isFavAlbum) {
                                            scope.launch {
                                                if (add2FavAlbums(
                                                        context,
                                                        userID,
                                                        currentClickedAlbum.albumID
                                                    )
                                                ) {
                                                    onAdd2FavAlbums(currentClickedAlbum.albumID)
                                                    onFavAlbum(currentClickedAlbum.albumID)
                                                }
                                            }
                                        }
                                        else {
                                            scope.launch {
                                                if (delFavAlbum(
                                                        context,
                                                        userID,
                                                        currentClickedAlbum.albumID
                                                    )
                                                ) {
                                                    onDelFavAlbum(currentClickedAlbum.albumID)
                                                    onFavAlbum(currentClickedAlbum.albumID)
                                                }
                                            }
                                        }
                                    }) {
                                    onFavAlbum(currentClickedAlbum.albumID)
                                    Icon(
                                        painter = painterResource(if (isFavAlbum) R.drawable.ic_fav else R.drawable.ic_unfav),
                                        modifier = Modifier.size(24.dp),
                                        contentDescription = "",
                                        tint = if (isFavAlbum) Color(0xFF57B65F) else Color.White,
                                    )
                                }
                            }
                        } else {
                            Row (
                                Modifier
                                    .fillMaxWidth(0.65f)
                                    .fillMaxHeight()) {}
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val _isShuffle = if (isShuffle) {
                                Color(0xFF57B65F)
                            } else {
                                Color.White
                            }
                            IconButton(onClick = { onShuffle() }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_shuffle),
                                    contentDescription = "",
                                    tint = _isShuffle,
                                    modifier = Modifier
                                        .size(24.dp)
                                )
                            }
                            val ctx = LocalContext.current
                            var isAlbumPlaying by remember { mutableStateOf(false) }
                            IconButton(
                                modifier = Modifier.size(64.dp),
                                onClick = {
                                    if (!isAlbumPlaying) {
                                        onPlayAlbum(currentClickedAlbum.songs)
                                        Toast.makeText(ctx, "Playing", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                    isAlbumPlaying = !isAlbumPlaying
                                }) {
                                val _isPlaying = if (isAlbumPlaying) {
                                    R.drawable.ic_circle_pause
                                } else {
                                    R.drawable.ic_circle_play
                                }
                                Icon(
                                    painter = painterResource(_isPlaying),
                                    contentDescription = "",
                                    tint = Color(0xFF57B65F),
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }
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
                    val songsInAB = currentClickedAlbum.songs
                    items(songsInAB.size) { index ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .clickable {
                                        onCurrentClickedSong(songsInAB[index])
                                        CreatePlaylist(
                                            player,
                                            songsInAB[index].songID,
                                            songsInAB[index].title,
                                            songsInAB[index].artist,
                                            songsInAB[index].lyrics,
                                            songsInAB[index].thumbnail,
                                            songsInAB[index].url,
                                            onMoving
                                        )
                                        player.play()
                                    }
                            ) {
                                Row(
                                ) {
                                    Box(
                                    ) {
                                        AsyncImage(
                                            model = songsInAB[index].thumbnail,
                                            contentDescription = "",
                                            contentScale = ContentScale.Crop,
                                            placeholder = painterResource(id = R.drawable.ic_record),
                                            modifier = Modifier.size(56.dp)
                                        )
                                    }
                                    Box() {
                                        Column(
                                            modifier = Modifier.padding(
                                                start = 10.dp
                                            ),
                                        ) {
                                            Text(
                                                text = songsInAB[index].title,
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
                                            Spacer(
                                                modifier = Modifier.height(
                                                    2.dp
                                                )
                                            )
                                            Row {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_artist),
                                                    contentDescription = "",
                                                    tint = Color.Gray
                                                )
                                                Text(
                                                    modifier = Modifier.padding(start = 4.dp),
                                                    text = songsInAB[index].artist,
                                                    color = Color.Gray,
                                                    fontFamily = FontFamily(
                                                        Font(
                                                            R.font.magistral_book
                                                        )
                                                    ),
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        onCurrentClickedSong(songsInAB[index])
                                        onOptionsVisible(true)
                                    }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_ver_more),
                                        contentDescription = "",
                                        tint = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsSheet(
    player: ExoPlayer,
    isOptionsVisible: Boolean,
    onOptionsVisible: (Boolean) -> Unit,
    optionsState: SheetState,
    currentClickedSong: Song?,
    onMoving: (String, String, String, String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    if (isOptionsVisible) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight(0.36f),
            onDismissRequest = {
                onOptionsVisible(false)
            },
            sheetState = optionsState,
            containerColor = Color(0xFF191919),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column () {
                Row(modifier = Modifier.padding(start = 16.dp)) {
                    Box(
                    ) {
                        AsyncImage(
                            model = currentClickedSong?.thumbnail,
                            contentDescription = "",
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(id = R.drawable.ic_record),
                            modifier = Modifier
                                .size(48.dp)
                        )
                    }
                    Box() {
                        Column(
                            modifier = Modifier.padding(start = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            currentClickedSong?.let {
                                Text(
                                    text = it.title,
                                    color = Color.White,
                                    fontFamily = FontFamily(Font(R.font.magistral_medium)),
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = it.artist,
                                    color = Color.Gray,
                                    fontFamily = FontFamily(Font(R.font.magistral_book)),
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Divider(
                    color = Color.DarkGray,
                    thickness = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )

                Row(modifier = Modifier.padding(start = 16.dp))
                {
                    IconButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = {
                            onOptionsVisible(false)
                            scope.launch {
                                AddNextSongToPlayList(
                                    player,
                                    currentClickedSong,
                                    context,
                                    onMoving
                                )
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = "Add next song",
                            )
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp),
                                text = "Next song to playlist",
                                fontSize = 16.sp,
                                fontFamily = FontFamily(Font(R.font.magistral_medium)),
                            )
                        }
                    }
                }
                Row (modifier = Modifier.padding(start = 16.dp)) {
                    IconButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = {
                            scope.launch {
                                onOptionsVisible(false)
                                AddToPlayList(player, currentClickedSong, context, onMoving)
                            }
                        }
                    ) {
                        Row (
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_addtoplaylist),
                                contentDescription = "Add song to playlist",
                            )
                            Text(modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                                fontSize = 16.sp,
                                text = "Add song to playlist",
                                fontFamily = FontFamily(Font(R.font.magistral_medium)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(UnstableApi::class) @Composable
fun MusicList(
    songs: List<Song>? = null,
    player: ExoPlayer,
    currentClickedSong: Song?,
    onCurrentClickedSong: (Song) -> Unit,
    onMoving: (String, String, String, String) -> Unit,
    onClickedSong: (Song) -> Unit,
    isOptionsVisible: Boolean,
    onOptionsVisible: (Boolean) -> Unit,
    optionsState: SheetState,
) {
    val scope = rememberCoroutineScope()

    OptionsSheet (
        player,
        isOptionsVisible,
        onOptionsVisible,
        optionsState,
        currentClickedSong,
        onMoving,
    )

    LazyColumn(
        modifier = Modifier
            .padding(start = 2.dp, end = 2.dp, top = 12.dp, bottom = 28.dp)
            .height(700.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        if (songs != null) {
            items(if (songs.size < 10) songs.size else 10) { index ->
                Row (modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clickable {
                                onCurrentClickedSong(songs[index])
                                CreatePlaylist(
                                    player,
                                    songs[index].songID,
                                    songs[index].title,
                                    songs[index].artist,
                                    songs[index].lyrics,
                                    songs[index].thumbnail,
                                    songs[index].url,
                                    onMoving
                                )
                                player.play()
                            }
                    ) {
                        Row(
                        ) {
                            Box() {
                                AsyncImage(
                                    model = songs[index].thumbnail,
                                    contentDescription = "",
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(id = R.drawable.ic_record),
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                            Box() {
                                Column (
                                    modifier = Modifier.padding(start = 10.dp),
                                ) {
                                    Text(
                                        text = songs[index].title,
                                        color = Color.White,
                                        fontFamily = FontFamily(Font(R.font.magistral_medium)),
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row (
                                        verticalAlignment = Alignment.CenterVertically
                                    ){
                                        Icon(
                                            painter = painterResource(R.drawable.ic_artist),
                                            contentDescription = "",
                                            tint = Color.Gray
                                        )
                                        Text(
                                            modifier = Modifier.padding(start = 4.dp),
                                            text = songs[index].artist,
                                            color = Color.Gray,
                                            fontFamily = FontFamily(Font(R.font.magistral_book)),
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    onClickedSong(songs[index])
                                    onOptionsVisible(true)
                                }
                            }) {
                            Icon (
                                painter = painterResource(R.drawable.ic_ver_more),
                                contentDescription = "",
                                tint = Color.LightGray
                            )
                        }
                    }
                }
            }

            if (songs.size < 10) {
                items(10 - songs.size) {
                    Row (modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Row(
                            ) {
                                Box(
                                ) {
                                    AsyncImage(
                                        model = R.drawable.ic_record,
                                        contentDescription = "",
                                        contentScale = ContentScale.Crop,
                                        placeholder = painterResource(id = R.drawable.ic_record),
                                        modifier = Modifier
                                            .size(56.dp)
                                    )
                                }
                                Box() {
                                    Column(
                                        modifier = Modifier.padding(start = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(0.dp)
                                    ) {
                                        Text(
                                            text = "Coming Soon",
                                            fontStyle = FontStyle.Italic,
                                            color = Color.White,
                                            fontFamily = FontFamily(Font(R.font.magistral_medium)),
                                            fontSize = 16.sp,
                                        )
                                        Row (
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_artist),
                                                contentDescription = "",
                                                tint = Color.Gray
                                            )
                                            Text(
                                                modifier = Modifier.padding(start = 4.dp),
                                                fontStyle = FontStyle.Italic,
                                                text = "Talented singers",
                                                color = Color.Gray,
                                                fontFamily = FontFamily(Font(R.font.magistral_book)),
                                                fontSize = 14.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

val colorStops = arrayOf(
    0f to Color.DarkGray,
    1f to background_color,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayingListSheet(
    player: ExoPlayer,
    isShuffle: Boolean,
    onShuffle: () -> Unit,
    isRepeat: Boolean,
    onRepeat: () -> Unit,
    isRepeatAll: Boolean,
    onRepeatAll: () -> Unit,
    showPlayingList: Boolean,
    playingListState: SheetState,
    onShowPlayingList: (Boolean) -> Unit,
    onMoving: (String, String, String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    if (showPlayingList) {
        ModalBottomSheet(
            onDismissRequest = {
                onShowPlayingList(false)
            },
            sheetState = playingListState,
            containerColor = Color.DarkGray,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            dragHandle = {
                Row (
                    modifier = Modifier.padding(bottom = 12.dp, top = 24.dp),
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
                        fontFamily = FontFamily(Font(R.font.magistral_light)),
                    )
                }
            }
        ) {
            Box (
                modifier = Modifier.background(Brush.verticalGradient(colorStops = colorStops))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp, 0.dp)
                ) {
                    stickyHeader {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Playing List",
                                fontFamily = FontFamily(Font(R.font.magistral_bold)),
                                fontSize = 24.sp
                            )

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        onRepeatAll()
                                    }
                                }
                            ) {
                                val iconColor = if (isRepeatAll) {
                                    Color(0xFF57B65F)
                                } else {
                                    Color.White
                                }
                                Icon(
                                    painter = painterResource(R.drawable.ic_repeat),
                                    contentDescription = "",
                                    modifier = Modifier.size(24.dp),
                                    tint = iconColor
                                )
                            }

//                            IconButton(
//                                onClick = {
//                                    scope.launch {
//                                        onRepeatAll()
//                                    }
//                                }
//                            ) {
//                                val iconColor = if (isRepeatAll) {
//                                    Color(0xFF57B65F)
//                                } else {
//                                    Color.White
//                                }
//                                Icon(
//                                    painter = painterResource(R.drawable.ic_replay),
//                                    contentDescription = "",
//                                    modifier = Modifier.size(24.dp),
//                                    tint = iconColor
//                                )
//                            }

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        onShuffle()
                                    }
                                }
                            ) {
                                val iconColor = if (isShuffle) {
                                    Color(0xFF57B65F)
                                } else {
                                    Color.White
                                }
                                Icon(
                                    painter = painterResource(R.drawable.ic_shuffle),
                                    contentDescription = "",
                                    modifier = Modifier.size(24.dp),
                                    tint = iconColor
                                )
                            }

                        }
                    }

                    item { 
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    if (player.mediaItemCount == 0) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "Choose a song to enjoy ...",
                                    color = Color.White,
                                    fontFamily = FontFamily(Font(R.font.magistral_book)),
                                    fontSize = 16.sp,
                                )
                            }
                        }
                    }

                    items(player.mediaItemCount) { index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Row(
                                ) {
                                    Box(
                                    ) {
                                        AsyncImage(
                                            model = player.getMediaItemAt(index).mediaMetadata.artworkUri.toString(),
                                            contentDescription = "",
                                            contentScale = ContentScale.Crop,
                                            placeholder = painterResource(id = R.drawable.ic_record),
                                            modifier = Modifier
                                                .size(48.dp)
                                        )
                                    }
                                    Box() {
                                        Column(
                                            modifier = Modifier.padding(start = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(0.dp)
                                        ) {
                                            Text(
                                                text = player.getMediaItemAt(index).mediaMetadata.title.toString(),
                                                color = Color.White,
                                                fontFamily = FontFamily(Font(R.font.magistral_medium)),
                                                fontSize = 16.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = player.getMediaItemAt(index).mediaMetadata.artist.toString(),
                                                color = Color.Gray,
                                                fontFamily = FontFamily(Font(R.font.magistral_book)),
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Row () {
                                    IconButton(
                                        enabled = index != 0,
                                        onClick = {
                                            if (player.currentMediaItemIndex != index) {
                                                player.moveMediaItem(index, index - 1)
                                            }
                                            else {
                                                prepareNext(player, onMoving)
                                                player.moveMediaItem(index, index - 1)
                                            }
                                        }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_move_up),
                                            contentDescription = "",
                                            tint = Color.LightGray
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (player.currentMediaItemIndex != index) {
                                                player.moveMediaItem(index, index + 1)
                                            }
                                            else {
                                                prepareNext(player, onMoving)
                                                player.moveMediaItem(index, index + 1)
                                            }
                                        }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_move_down),
                                            contentDescription = "",
                                            tint = Color.LightGray
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            player.removeMediaItem(index)
                                            onMoving(
                                                player.currentMediaItem?.mediaMetadata?.title.toString(),
                                                player.currentMediaItem?.mediaMetadata?.artist.toString(),
                                                player.currentMediaItem?.mediaMetadata?.artworkUri.toString(),
                                                player.currentMediaItem?.mediaMetadata?.description.toString()
                                                )
                                        }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_delete),
                                            contentDescription = "",
                                            tint = Color.LightGray
                                        )
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


@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MusicPlayerControl(
    isPlayerInit: Boolean,
    userID: String,
    currentClickedSong: Song?,
    currentTitle: String,
    currentArtist: String,
    currentThumbnail: String,
    currentProgress: Float,
    currentDuration: Float,
    currentLyric: String,
    player: ExoPlayer,
    isPlaying: Boolean,
    isShuffle: Boolean,
    isRepeat: Boolean,
    isRepeatAll: Boolean,
    onRepeatAll: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onMoving: (String, String, String, String) -> Unit,
    onNewPosition: (Float) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    isFavSong: Boolean,
    onFavSong: (String) -> Unit,
    onAdd2FavSongs: (String) -> Unit,
    onDelFavSong: ((String) -> Unit)? = null
) {
    val mLocalContext = LocalContext.current
    val bottomSheetState = rememberModalBottomSheetState(true)
    var showSongDetailSheet by remember { mutableStateOf(false) }
    var showPlayingList by remember { mutableStateOf(false) }
    val playingListState = rememberModalBottomSheetState(true)
    val scope = rememberCoroutineScope()

    val onShowPlayingList: (Boolean) -> Unit = { isShow ->
        showPlayingList = isShow
    }

    val colorStops = arrayOf(
        0f to Color.DarkGray,
        0.15f to background_color,
    )

    if (showSongDetailSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSongDetailSheet = false
            },
            sheetState = bottomSheetState,
            containerColor = Color.DarkGray,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            dragHandle = {
                Row (
                    modifier = Modifier.padding(bottom = 0.dp, top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "",
                        tint = Color.White
                    )
                    Text(
                        text = "Scroll down to close",
                        fontFamily = FontFamily(Font(R.font.magistral_light)),
                    )
                }
            }
        ) {
            Column (
                modifier = Modifier.background(Brush.verticalGradient(colorStops = colorStops))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.35f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = if (currentThumbnail != "") currentThumbnail else R.drawable.ic_record,
                        contentDescription = "",
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.ic_record),
                        modifier = Modifier
                            .shadow(
                                24.dp,
                                ambientColor = Color.Black,
                                spotColor = Color.White
                            )
                            .size(256.dp)
                            .clip(RoundedCornerShape(12.dp)).background(Color.DarkGray)
                    )
                }
                Box(
                    modifier = Modifier.padding(start = 36.dp, end = 36.dp, top = 16.dp)
                ) {
                    Column {
                        Row() {
                            Column(
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                Text(
                                    text = currentTitle,
                                    fontFamily = FontFamily(Font(R.font.magistral_medium)),
                                    fontSize = 20.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentArtist,
                                    fontFamily = FontFamily(Font(R.font.magistral_book)),
                                    fontSize = 16.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (!isFavSong) {
                                        scope.launch {
                                            if (currentClickedSong != null) {
                                                if (add2FavSongs(
                                                        mLocalContext,
                                                        userID,
                                                        currentClickedSong.songID
                                                    )
                                                ) {
                                                    onAdd2FavSongs(currentClickedSong.songID)
                                                    onFavSong(currentClickedSong.songID)
                                                }
                                            }
                                        }
                                    }
                                    else {
                                        scope.launch {
                                            if (currentClickedSong != null) {
                                                if (delFavSong(
                                                        mLocalContext,
                                                        userID,
                                                        currentClickedSong.songID
                                                    )
                                                ) {
                                                    if (onDelFavSong != null) {
                                                        onDelFavSong(currentClickedSong.songID)
                                                    }
                                                    onFavSong(currentClickedSong.songID)
                                                }
                                            }
                                        }
                                    }
                                }) {
                                if (currentClickedSong != null) {
                                    onFavSong(currentClickedSong.songID)
                                }
                                Icon(
                                    painter = painterResource(if (isFavSong) R.drawable.ic_fav else R.drawable.ic_unfav),
                                    modifier = Modifier.size(24.dp),
                                    contentDescription = "",
                                    tint = if (isFavSong) Color(0xFF57B65F) else Color.White,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Slider(
                            modifier = Modifier.height(16.dp),
                            value = currentProgress,
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.DarkGray,
                            ),
                            onValueChange = { newPosition ->
                                onNewPosition(newPosition)
                            },
                            thumb = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_circle),
                                    contentDescription = "",
                                    tint = Color.White
                                )
                            },
                            enabled = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "0:00",
                                fontFamily = FontFamily(Font(R.font.magistral_light)),
                                fontSize = 12.sp,
                                color = Color.Gray,
                            )
                            Text(
                                text = "0:00",
                                fontFamily = FontFamily(Font(R.font.magistral_light)),
                                fontSize = 12.sp,
                                color = Color.Gray,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    onShuffle()
                                }
                            ) {
                                val iconColor = if (isShuffle) {
                                    Color(0xFF57B65F)
                                } else {
                                    Color.White
                                }
                                Icon(
                                    painter = painterResource(R.drawable.ic_shuffle),
                                    contentDescription = "",
                                    modifier = Modifier.size(24.dp),
                                    tint = iconColor
                                )
                            }

                            IconButton(
                                onClick = {
                                    preparePrevious(player, mLocalContext, onMoving)
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_previous_song),
                                    contentDescription = "",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            IconButton(
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(64.dp),
                                onClick = {
                                    onPlayPauseToggle()
                                }
                            ) {
                                val iconRes = if (isPlaying) {
                                    R.drawable.ic_circle_pause
                                } else {
                                    R.drawable.ic_circle_play
                                }
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = "",
                                    tint = Color.White,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            IconButton(
                                onClick = {
                                    prepareNext(player, mLocalContext, onMoving)
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_next_song),
                                    contentDescription = "",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    onRepeatAll()
                                }
                            ) {
                                val iconColor = if (isRepeatAll) {
                                    Color(0xFF57B65F)
                                } else {
                                    Color.White
                                }
                                Icon(
                                    painter = painterResource(R.drawable.ic_repeat),
                                    contentDescription = "",
                                    modifier = Modifier.size(24.dp),
                                    tint = iconColor
                                )
                            }
                        }
                    }
                }
                Box (
                    modifier = Modifier
                        .padding(36.dp, 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(Color(0xFF232323))
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        stickyHeader () {
                            Box (
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF303030))
                            ) {
                                Text(
                                    modifier = Modifier.padding(12.dp),
                                    text = "Lyrics",
                                    color = Color.LightGray,
                                    fontFamily = FontFamily(Font(R.font.magistral_bold)),
                                    fontSize = 16.sp,
                                )
                            }
                        }
                        val temp = currentLyric.split(Regex("(?=\\p{Lu})"))
                        Log.d("onLyric", temp.toString())
                        items(temp.size) { index ->
                            Text(
                                modifier = Modifier.padding(12.dp, 0.dp),
                                text = temp[index],
                                fontFamily = FontFamily(Font(R.font.magistral_book)),
                                fontSize = 20.sp,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    PlayingListSheet(
        player,
        isShuffle,
        onShuffle,
        isRepeat,
        onRepeat,
        isRepeatAll,
        onRepeatAll,
        showPlayingList,
        playingListState,
        onShowPlayingList,
        onMoving
    )

    if (isPlayerInit) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, bottom = 12.dp)
        ) {
            val (image) = createRefs()
            val movePrevious = SwipeAction(
                icon = painterResource(R.drawable.ic_back),
                background = Color.Gray.copy(0.9f),
                isUndo = true,
                onSwipe = {
                    preparePrevious(player, mLocalContext, onMoving)
                }
            )
            val moveNext = SwipeAction(
                icon = painterResource(R.drawable.ic_next),
                background = Color.Gray.copy(0.9f),
                isUndo = true,
                onSwipe = {
                    prepareNext(player, mLocalContext, onMoving)
                }
            )

            SwipeableActionsBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .constrainAs(image) {
                        start.linkTo(parent.start)
                        bottom.linkTo(parent.bottom, margin = 46.dp)
                        end.linkTo(parent.end)
                    }
                    .clickable {
                        showSongDetailSheet = true
                    },

                swipeThreshold = 128.dp,
                backgroundUntilSwipeThreshold = Color.DarkGray,
                startActions = listOf(movePrevious),
                endActions = listOf(moveNext)
            ) {
                Box() {
                    Column(
                        modifier = Modifier
                            .background(Color.DarkGray)
                            .fillMaxWidth(),
//                        verticalArrangement = Arrangement.spacedBy((-1).dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                AsyncImage(
                                    model = if (currentThumbnail != "") currentThumbnail else R.drawable.ic_record,
                                    contentDescription = "",
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(id = R.drawable.ic_record),
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.65f)
                                    .padding(start = 8.dp)
//                                    .align(Alignment.CenterVertically)
                            ) {
                                Column (
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ){
                                    Text(
                                        text = currentTitle,
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        fontFamily = FontFamily(Font(R.font.magistral_medium)),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentArtist,
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        fontFamily = FontFamily(Font(R.font.magistral_book)),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                showPlayingList = true
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_playinglist),
                                            contentDescription = "",
                                            tint = Color.White
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            onPlayPauseToggle()
                                        }
                                    ) {
                                        val iconRes = if (isPlaying) {
                                            R.drawable.ic_pause
                                        } else {
                                            R.drawable.ic_play
                                        }
                                        Icon(
                                            painter = painterResource(iconRes),
                                            contentDescription = "",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Row() {
                            Slider(
                                modifier = Modifier.height(4.dp),
                                value = currentProgress,
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(thumbColor = Color.White),
                                onValueChange = {},
                                thumb = {}
                            )
                        }
                    }
                }
            }
        }
    }
}