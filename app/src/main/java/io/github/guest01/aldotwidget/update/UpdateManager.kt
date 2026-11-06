package io.github.guest01.aldotwidget.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import io.github.guest01.aldotwidget.BuildConfig
import io.github.guest01.aldotwidget.data.Notifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** 설정 앱 카드의 확인 상태 */
enum class CheckState { IDLE, CHECKING, FAILED }

/** 업데이트 창의 설치 단계 */
sealed interface InstallState {
    /** 아직 시작 전 (새 버전 안내와 '바로 설치') */
    data object None : InstallState
    /** 이 앱에 설치 허용이 꺼져 있다. 설정에서 켜고 돌아오면 이어서 받는다 */
    data object NeedPermission : InstallState
    data class Downloading(val read: Long, val total: Long) : InstallState
    /** 시스템에 설치를 맡겼다. 곧 앱이 닫힌다 */
    data object Installing : InstallState
    data object DownloadFailed : InstallState
    /** 시스템이 설치를 막았다 (갤럭시 '보안 위험 자동 차단'일 가능성이 크다) */
    data object Blocked : InstallState
    /**
     * 설치가 취소됐다. Google Play 프로텍트 창의 '앱 설치 안함'이나 설치 확인 창의 '취소'.
     * (Fold8 실측: Play 프로텍트에서 취소하면 STATUS_FAILURE_ABORTED, INSTALL_FAILED_VERIFICATION_FAILURE)
     */
    data object Cancelled : InstallState
    /** 그 밖의 이유로 설치하지 못했다 */
    data object InstallFailed : InstallState
}

data class UpdateUiState(
    val check: CheckState = CheckState.IDLE,
    /** 마지막으로 확인에 성공한 시각. 0이면 아직 확인 전 */
    val lastCheckedAt: Long = 0L,
    /** 방금 직접 확인했는지 ("방금 확인했어요") */
    val checkedByUser: Boolean = false,
    /** 찾아 둔 새 버전. 최신이면 null */
    val available: UpdateInfo? = null,
    val install: InstallState = InstallState.None,
)

/**
 * 업데이트 확인과 앱 안 설치를 맡는다. 앱 전체에서 하나 (AppContainer).
 *
 * - 확인: 하루 한 번(자동 갱신 작업, 앱을 열 때) 또는 설정 앱 카드의 '업데이트 확인'. 새 버전을 찾으면 버전마다 한 번 알린다.
 * - 설치: APK를 앱 캐시에 받고 크기·sha256을 확인한 뒤 PackageInstaller로 설치한다.
 *   Android 12+에서 자기 자신을 업데이트할 때는 확인 창 없이 설치된다 (USER_ACTION_NOT_REQUIRED,
 *   설치 허용과 매니페스트의 UPDATE_PACKAGES_WITHOUT_USER_ACTION이 필요하다). 설치되는 동안 앱 프로세스는 끝난다.
 * - 받기는 화면과 상관없는 범위에서 돌아서, 업데이트 창을 닫아도 계속 받는다.
 */
class UpdateManager(
    private val context: Context,
    private val store: UpdateStore,
    private val notifier: Notifier,
    private val checker: UpdateChecker,
    private val client: OkHttpClient,
    private val currentVersionCode: Int = BuildConfig.VERSION_CODE,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val checkMutex = Mutex()
    private var downloadJob: Job? = null

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    init {
        // 지난번에 찾아 둔 새 버전을 바로 보여 준다 (설정의 '업데이트' 칩). 이미 설치된 버전이면 버린다
        scope.launch {
            val available = store.available()?.takeIf { it.isNewerThan(currentVersionCode) }
            val lastCheckedAt = store.lastCheckedAt()
            _state.update { it.copy(lastCheckedAt = lastCheckedAt, available = it.available ?: available) }
        }
    }

    /** 자동 확인: 하루가 지났을 때만. 실패해도 아무것도 띄우지 않는다 (다음 갱신 때 다시) */
    suspend fun checkIfDue(now: Long = System.currentTimeMillis()) {
        if (UpdatePolicy.isDue(store.lastCheckedAt(), now)) check(byUser = false)
    }

    fun checkIfDueInBackground() {
        scope.launch { checkIfDue() }
    }

    /** 설정 앱 카드의 '업데이트 확인' */
    fun checkNow() {
        if (_state.value.check == CheckState.CHECKING) return
        scope.launch { check(byUser = true) }
    }

    private suspend fun check(byUser: Boolean) = checkMutex.withLock {
        _state.update { it.copy(check = CheckState.CHECKING) }
        try {
            val info = checker.fetch()
            val newer = info.takeIf { it.isNewerThan(currentVersionCode) }
            val now = System.currentTimeMillis()
            store.saveCheck(now, newer)
            _state.update {
                it.copy(
                    check = CheckState.IDLE, lastCheckedAt = now, checkedByUser = byUser, available = newer,
                    // 더 새 버전이 나왔으면 이전 버전의 실패 안내는 지운다
                    install = if (newer?.versionCode != it.available?.versionCode) InstallState.None else it.install,
                )
            }
            Log.i(TAG, "check: latest=${info.version}(${info.versionCode}) current=$currentVersionCode byUser=$byUser")
            if (newer != null && newer.versionCode > store.notifiedVersionCode()) {
                // 앱 안에서 직접 찾았으면 이미 보고 있으니 알림은 보내지 않고, 보낸 것으로만 기록한다
                if (!byUser) notifier.notifyUpdateAvailable(newer)
                store.setNotifiedVersionCode(newer.versionCode)
            }
        } catch (e: CancellationException) {
            _state.update { it.copy(check = CheckState.IDLE) }
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "check failed (byUser=$byUser)", e)
            _state.update { it.copy(check = if (byUser) CheckState.FAILED else CheckState.IDLE) }
        }
    }

    // ── 설치 ──────────────────────────────────────────────

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** '바로 설치'와 '다시 시도'. 설치 허용이 꺼져 있으면 안내부터 */
    fun startInstall() {
        val cached = _state.value.available ?: return
        if (downloadJob?.isActive == true) return
        if (!canInstall()) {
            _state.update { it.copy(install = InstallState.NeedPermission) }
            return
        }
        downloadJob = scope.launch {
            _state.update { it.copy(install = InstallState.Downloading(0, cached.size)) }
            try {
                // 받기 직전에 update.json을 다시 읽는다. 찾아 둔 뒤 새 릴리스가 나왔으면 APK 이름이 바뀌어
                // 예전 주소로는 받을 수 없다. 다시 읽지 못하면 찾아 둔 정보로 받는다
                val fresh = try {
                    checker.fetch().takeIf { it.isNewerThan(currentVersionCode) }
                } catch (e: IOException) {
                    null
                }
                val info = fresh ?: cached
                if (fresh != null && fresh != cached) {
                    store.saveCheck(System.currentTimeMillis(), fresh)
                    _state.update { it.copy(available = fresh, install = InstallState.Downloading(0, fresh.size)) }
                }
                val apk = download(info)
                _state.update { it.copy(install = InstallState.Installing) }
                store.setInstallingVersionCode(info.versionCode)
                installApk(apk)
            } catch (e: CancellationException) {
                _state.update { it.copy(install = InstallState.None) }
                throw e
            } catch (e: IOException) {
                Log.w(TAG, "download/install failed", e)
                _state.update { it.copy(install = InstallState.DownloadFailed) }
            }
        }
    }

    /** 설정에서 설치 허용을 켜고 돌아왔으면 이어서 받는다 (화면이 다시 보일 때마다 부른다) */
    fun continueIfPermitted() {
        if (_state.value.install == InstallState.NeedPermission && canInstall()) startInstall()
    }

    fun cancelDownload() {
        downloadJob?.cancel()
    }

    /** 업데이트 창을 닫을 때: 받는 중·설치 중이 아니면 안내 단계를 처음으로 되돌린다 */
    fun resetInstallState() {
        _state.update {
            when (it.install) {
                is InstallState.Downloading, InstallState.Installing -> it
                else -> it.copy(install = InstallState.None)
            }
        }
    }

    /** 브라우저로 받기: 릴리스의 APK 주소 */
    fun apkUrl(info: UpdateInfo): String = checker.apkUrl(info).toString()

    /** InstallResultReceiver가 시스템의 설치 결과를 알려 준다 (성공하면 그 전에 앱이 끝나서 오지 않는다) */
    fun onInstallFailed(status: Int, message: String?) {
        Log.w(TAG, "install failed: status=$status message=$message")
        scope.launch { store.setInstallingVersionCode(0) }
        val next = when (status) {
            PackageInstaller.STATUS_FAILURE_BLOCKED -> InstallState.Blocked
            PackageInstaller.STATUS_FAILURE_ABORTED -> InstallState.Cancelled
            else -> InstallState.InstallFailed
        }
        _state.update { it.copy(install = next) }
    }

    /** 확인 창이 필요하다며 시스템이 넘긴 창을 띄운 뒤에는 다시 '설치 중'으로 */
    fun onUserActionRequested() {
        _state.update { it.copy(install = InstallState.Installing) }
    }

    private fun updateDir() = File(context.cacheDir, UPDATE_DIR).apply { mkdirs() }

    private suspend fun download(info: UpdateInfo): File = withContext(Dispatchers.IO) {
        val dir = updateDir()
        // 지난번에 받다 만 파일이나 이전 버전 파일은 지운다
        dir.listFiles()?.forEach { it.delete() }
        val part = File(dir, "${info.apk}.part")
        val request = Request.Builder().url(checker.apkUrl(info)).build()
        client.newCall(request).executeAsync().use { response ->
            if (!response.isSuccessful) throw IOException("APK HTTP ${response.code}")
            val digest = MessageDigest.getInstance("SHA-256")
            var read = 0L
            response.body.byteStream().use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var lastReported = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        read += n
                        if (read - lastReported >= 64 * 1024 || read == info.size) {
                            lastReported = read
                            _state.update { it.copy(install = InstallState.Downloading(read, info.size)) }
                        }
                        if (read > info.size) throw IOException("APK가 update.json보다 커요")
                    }
                }
            }
            val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
            if (read != info.size || !sha256.equals(info.sha256, ignoreCase = true)) {
                part.delete()
                throw IOException("APK가 update.json과 달라요 (size=$read sha256=$sha256)")
            }
        }
        File(dir, info.apk).also { part.renameTo(it) }
    }

    private fun installApk(apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                session.openWrite("base.apk", 0, apk.length()).use { out ->
                    apk.inputStream().use { it.copyTo(out) }
                    session.fsync(out)
                }
                // 시스템이 결과를 채워 넣어야 해서 MUTABLE (받는 쪽은 앱 안 수신기로 고정)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                val result = PendingIntent.getBroadcast(
                    context, sessionId, Intent(context, InstallResultReceiver::class.java), flags,
                )
                session.commit(result.intentSender)
            }
        } catch (e: Exception) {
            installer.abandonSession(sessionId)
            throw IOException("설치를 시작하지 못했어요", e)
        }
    }

    /** 새 버전이 처음 깨어났을 때 (UpdatedReceiver): 받아 둔 파일을 지우고, 앱 안에서 설치했으면 완료 알림 */
    suspend fun onPackageReplaced(versionName: String) {
        File(context.cacheDir, UPDATE_DIR).deleteRecursively()
        val installing = store.installingVersionCode()
        if (installing in 1..currentVersionCode) {
            notifier.notifyUpdated(versionName)
        }
        store.setInstallingVersionCode(0)
        // 지금 버전보다 새 버전이 아니면 찾아 둔 정보도 버린다
        if (store.available()?.isNewerThan(currentVersionCode) != true) store.saveCheck(store.lastCheckedAt(), null)
    }

    private companion object {
        const val TAG = "UpdateManager"
        const val UPDATE_DIR = "update"
    }
}
