package com.kangkang.shiyangai

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kangkang.shiyangai.ui.AppRoot
import com.kangkang.shiyangai.ui.theme.KangKangTheme
import com.kangkang.shiyangai.ui.theme.KangKangColors

class MainActivity : ComponentActivity() {
    private var musicPlayer: MediaPlayer? = null
    private var musicMuted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        musicPlayer = MediaPlayer.create(this, R.raw.fuxiao_station)?.apply {
            isLooping = true
            setVolume(0.25f, 0.25f)
            start()
        }
        setContent {
            KangKangTheme {
                Column(Modifier.fillMaxSize().background(KangKangColors.Paper).safeDrawingPadding()) {
                    Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = {
                            musicMuted = !musicMuted
                            val volume = if (musicMuted) 0f else 0.25f
                            musicPlayer?.setVolume(volume, volume)
                        }) {
                            Text(if (musicMuted) "♪ 开启音乐" else "♪ 静音")
                        }
                    }
                    Box(Modifier.weight(1f)) { AppRoot() }
                }
            }
        }
    }

    override fun onPause() {
        musicPlayer?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        musicPlayer?.start()
    }

    override fun onDestroy() {
        musicPlayer?.release()
        musicPlayer = null
        super.onDestroy()
    }
}
