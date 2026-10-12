package com.avemuzik.player.data.repository

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.avemuzik.player.R
import com.avemuzik.player.data.media.LibraryBuilder
import com.avemuzik.player.data.media.MediaStoreScanner
import com.avemuzik.player.model.Library
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface LibraryState {
    /** Henüz tarama yapılmadı (ör. izin bekleniyor). */
    data object Idle : LibraryState
    data object Loading : LibraryState
    data class Ready(val library: Library) : LibraryState
    data class Error(val reason: Reason) : LibraryState {
        enum class Reason { PERMISSION, UNKNOWN }
    }
}

/**
 * Müzik kütüphanesinin tek kaynağı. Kütüphane uygulama ömrü boyunca bellekte tutulur;
 * ekran değişimlerinde yeniden tarama yapılmaz. MediaStore değiştiğinde (yeni dosya,
 * silinen dosya) arka planda sessizce yenilenir.
 */
class MusicRepository(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val scanner = MediaStoreScanner(context)
    private val scanMutex = Mutex()

    private val _state = MutableStateFlow<LibraryState>(LibraryState.Idle)
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    val library: Flow<Library> = state
        .mapNotNull { (it as? LibraryState.Ready)?.library }
        .distinctUntilChanged()

    private var observerRegistered = false
    private var pendingRefresh: Job? = null

    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            // Birden çok değişikliği tek taramada topla.
            pendingRefresh?.cancel()
            pendingRefresh = scope.launch {
                delay(REFRESH_DEBOUNCE_MS)
                scan()
            }
        }
    }

    /** İlk taramayı başlatır; kütüphane zaten yüklüyse hiçbir şey yapmaz. */
    fun ensureLoaded() {
        val current = _state.value
        // compareAndSet: art arda iki çağrı iki ayrı tarama başlatmasın.
        if ((current is LibraryState.Idle || current is LibraryState.Error) &&
            _state.compareAndSet(current, LibraryState.Loading)
        ) {
            refresh()
        }
    }

    fun refresh() {
        scope.launch { scan() }
    }

    private suspend fun scan() = scanMutex.withLock {
        if (_state.value !is LibraryState.Ready) _state.value = LibraryState.Loading
        _state.value = try {
            val library = withContext(Dispatchers.IO) {
                LibraryBuilder.build(scanner.scan(), context.getString(R.string.various_artists))
            }
            registerObserverOnce()
            LibraryState.Ready(library)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            LibraryState.Error(LibraryState.Error.Reason.PERMISSION)
        } catch (e: Exception) {
            LibraryState.Error(LibraryState.Error.Reason.UNKNOWN)
        }
    }

    private fun registerObserverOnce() {
        if (observerRegistered) return
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaObserver,
        )
        observerRegistered = true
    }

    private companion object {
        const val REFRESH_DEBOUNCE_MS = 2_000L
    }
}
