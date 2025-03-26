package com.example.muzik.ui.search

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
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
import com.example.muzik.ui.home.updateSliderValue
import com.example.muzik.ui.library.RenderLibrary
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class SearchFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        lateinit var player: ExoPlayer
        val reqAct = requireActivity()

        val view = ComposeView(requireContext()).apply {
            setContent {
                val sharedPref = context.getSharedPreferences("MuzikPrefs", Context.MODE_PRIVATE)
                val userID = sharedPref.getString("userID", "")
                player = ExoPlayerManager.getPlayerInstance(requireContext())
                if (!ExoPlayerManager.isLoaded) {
                    RenderSearch(
                        player,
                        reqAct,
                        HomeViewModel(userID.toString()),
                    )
                }
                else {
                    RenderSearch(
                        player,
                        reqAct
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
@SuppressLint("ResourceAsColor")
@Composable
fun RenderSearch(
    player: ExoPlayer,
    reqAct: FragmentActivity,
    viewModel: HomeViewModel? = null,
) {
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

    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("MuzikPrefs", MODE_PRIVATE)
    val userID = sharedPref.getString("userID", "")

    var searchText by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

    val colorStops = arrayOf(
        0f to Color(0xFF2F2F2F),
        1f to Color(0xFF0F0F0F),
    )

    val imm = reqAct.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val view = reqAct.currentFocus
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

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = Color(0xFF0F0F0F)
    ) {
        Box(
            modifier = Modifier.fillMaxHeight()
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
                    text = "Search",
                    fontFamily = FontFamily(Font(R.font.magistral_bold)),
                    fontSize = 24.sp,
                    color = Color.White,
                )
            }
        }
        Box (
            Modifier.fillMaxHeight()
        ) {
            val scope = rememberCoroutineScope()
            val focusManager = LocalFocusManager.current

            SearchBar(
                modifier = Modifier
                    .padding(
                        top = 64.dp,
                        start = if (!searchActive) 12.dp else 0.dp,
                        end = if (!searchActive) 12.dp else 0.dp
                    )
                    .fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "What do you want to listen to?",
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.magistral_medium)),
                    )
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, "")
                },
                trailingIcon = {
                    Icon(
                        modifier = Modifier.clickable {
                            if (searchText.isNotEmpty()) {
                                searchText = ""
                            } else {
                                searchActive = false
                            }
                        },
                        imageVector = Icons.Default.Close,
                        contentDescription = ""
                    )
                },
                colors = SearchBarDefaults.colors(
                    containerColor = Color.White,
                    dividerColor = Color.DarkGray,
                ),
                shape = RoundedCornerShape(4.dp),
                query = searchText,
                onQueryChange = {
                    searchText = it
                },
                onSearch = {
                    // Ẩn keyboard
                    view?.let {
                        imm.hideSoftInputFromWindow(it.windowToken, 0)
                    }
                },
                active = searchActive,
                onActiveChange = {
                    searchActive = it
                }
            ) {
                Box () {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(Brush.verticalGradient(colorStops = colorStops)),
                        userScrollEnabled = true
                    ) {
                        val filterAlbum: List<Album> = if (searchText.isNotBlank()) {
                            albums.filter {
                                val trimmedTitle = it.title.trimStart().trimEnd()
                                val matchedCharacters = searchText.zip(trimmedTitle)
                                    .takeWhile { (a, b) ->
                                        a.equals(b, ignoreCase = true) }
                                    .count()

                                if (searchText.length < 3 && matchedCharacters > 0)
                                    true
                                else
                                    matchedCharacters > searchText.length / 2
                            }
                        } else {
                            emptyList()
                        }

                        val filterSong: List<Song> = if (searchText.isNotBlank()) {
                            songs.filter {
                                val trimmedTitle = it.title.trimStart().trimEnd()
                                val matchedCharacters = searchText.zip(trimmedTitle)
                                    .takeWhile { (a, b) ->
                                        a.equals(b, ignoreCase = true)
                                    }
                                    .count()

                                if (searchText.length < 3 && matchedCharacters > 0)
                                    true
                                else
                                    matchedCharacters > searchText.length / 2
                            }
                        } else {
                            emptyList()
                        }

                        item {
                            Spacer(Modifier.height(16.dp))
                        }

                        items(filterAlbum.size) { index ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth().height(56.dp)
                                    .padding(start = 16.dp, end = 16.dp)
                                    .clickable {
                                        focusManager.clearFocus()
                                        view?.let {
                                            imm.hideSoftInputFromWindow(it.windowToken, 0)
                                        }
                                        scope.launch {
                                            currentClickedAlbum = albums[index]
                                            isAlbumDetailVisible = true
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .fillMaxHeight()
                                ) {
                                    AsyncImage(
                                        model = filterAlbum[index].thumbnail,
                                        contentDescription = "Album Thumnail",
                                        contentScale = ContentScale.Crop,
                                        placeholder = painterResource(id = R.drawable.ic_album),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(6.dp))
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
                                        text = filterAlbum[index].title,
                                        color = Color.White,
                                        fontFamily = FontFamily(Font(R.font.magistral_medium)),
                                        fontSize = 17.sp,
                                        lineHeight = 20.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        items(filterSong.size) { index ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth().height(56.dp)
                                    .padding(start = 16.dp, end = 16.dp)
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
                                        Box(
                                        ) {
                                            AsyncImage(
                                                model = filterSong[index].thumbnail,
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
                                            ) {
                                                Text(
                                                    text = filterSong[index].title,
                                                    color = Color.White,
                                                    fontFamily = FontFamily(Font(R.font.magistral_medium)),
                                                    fontSize = 16.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_artist),
                                                        contentDescription = "",
                                                        tint = Color.Gray
                                                    )
                                                    Text(
                                                        modifier = Modifier.padding(start = 4.dp),
                                                        text = filterSong[index].artist,
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
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    val scope = rememberCoroutineScope()
                                    IconButton(
                                        onClick = {
                                            view?.let {
                                                imm.hideSoftInputFromWindow(it.windowToken, 0)
                                            }
                                            scope.launch {
                                                onClickedSong(filterSong[index])
                                                onOptionsVisible(true)
                                            }
                                        }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_ver_more),
                                            contentDescription = "",
                                            tint = Color.LightGray
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        item {
                            Spacer(modifier = Modifier.padding(bottom = 52.dp))
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
