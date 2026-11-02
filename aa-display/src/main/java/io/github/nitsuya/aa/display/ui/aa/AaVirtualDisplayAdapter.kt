package io.github.nitsuya.aa.display.ui.aa

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.ITaskStackListener
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.*
import android.view.*
import android.window.TaskSnapshot
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.github.kyuubiran.ezxhelper.utils.*
import de.robv.android.xposed.XSharedPreferences
import io.github.nitsuya.aa.display.BuildConfig
import io.github.nitsuya.aa.display.model.RecentTask
import io.github.nitsuya.aa.display.model.RecentTaskInfo
import io.github.nitsuya.aa.display.service.ShellManagerService
import io.github.nitsuya.aa.display.util.AADisplayConfig
import io.github.nitsuya.aa.display.xposed.CoreManagerService
import io.github.nitsuya.aa.display.xposed.IShellManager
import io.github.nitsuya.aa.display.xposed.TipUtil
import io.github.nitsuya.aa.display.xposed.log
import io.github.nitsuya.aa.display.xposed.util.Instances
import io.github.nitsuya.template.bases.runMain


class AaVirtualDisplayAdapter(
      private val context: Context
    , private val config: XSharedPreferences?
    , private val onReady: (suspend AaVirtualDisplayAdapter.(it:AaVirtualDisplayAdapter) -> Unit)
) {
    companion object {
        const val TAG = "AADisplay_AaVirtualDisplayAdapter"
        private const val DISPLAY_IME_POLICY_LOCAL = 0

        /** Package names to ignore in recent task list */
        private val IGNORE_RECENT_PACKAGE = setOf(
            BuildConfig.APPLICATION_ID,
            "com.android.launcher3"
        )
    }

    /** User-entered package or flattened component used to resolve the default app. */
    private var mLauncherSpec: String? = null

    /** Resolved launch activity. Resolution happens in system_server, outside app visibility limits. */
    private var mLauncherComponent: ComponentName? = null

    /** Actual package name used for task tracking and teardown protection. */
    private var mLauncherPackageName: String? = null

    /** Task ID of the default launch package */
    private var mLauncherPackageTaskId: Int? = null
    private var mIsDestroying = false
    private val mTrackedPackageUsers = linkedMapOf<String, MutableSet<Int>>()
    private val mTaskStackListener = TaskStackListener()
    var mDisplayId = Display.INVALID_DISPLAY
    var mDensityDpi: Int = 0

    private val mTransaction = SurfaceControl.Transaction()
    private var mSurfaceControls = mutableMapOf<SurfaceControl, SurfaceControl>()
    public lateinit var mVirtualDisplay: VirtualDisplay
    private lateinit var mDisplayWindowManager: WindowManager
    private val mForceView = View(context)

    private var mDoInit = false
    private var mShellManager: IShellManager? = null
    private var mServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            log(TAG, "ShellManagerService connected: $name")
            mShellManager = IShellManager.Stub.asInterface(service)
            if(mDoInit) return
            mDoInit = !mDoInit
            mShellManager?.createVirtualDisplayBefore()
            runMain {
               onReady(this@AaVirtualDisplayAdapter)
            }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            log(TAG, "ShellManagerService disconnected: $name")
            mShellManager = null
        }
    }

    init {
        refreshLauncherPackage("init")
        val bound = CoreManagerService.systemContext.bindService(
            Intent(ShellManagerService::class.java.name).apply {
                setPackage(BuildConfig.APPLICATION_ID)
            }
            , mServiceConnection
            , AppCompatActivity.BIND_AUTO_CREATE
        )
        log(TAG, "bind ShellManagerService requested=$bound")
    }

    fun setSurface(surface: Surface?){
        if (!::mVirtualDisplay.isInitialized) {
            log(TAG, "setSurface ignored before virtual display init: surface=${surface != null}")
            return
        }
        log(TAG, "setSurface: surface=${surface != null}, display=$mDisplayId")
        mVirtualDisplay.surface = surface
    }

    @SuppressLint("WrongConstant")
    fun onConnected(width: Int, height: Int, densityDpi: Int, surface: Surface?, onVirtualDisplayCreated: ((Int) -> Unit)) {
        mIsDestroying = false
        mTrackedPackageUsers.clear()
        refreshLauncherPackage("connect")
        trackPackage(mLauncherPackageName, 0)
        val indent = Binder.clearCallingIdentity()
        try {
            mVirtualDisplay = Instances.displayManager.createVirtualDisplay(
                "AADisplay-${System.currentTimeMillis()}",
                width,
                height,
                densityDpi,
                surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
                    or DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE
                    // Keep this app display out of the presentation-display category.
                    or DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
                    //or (1 shl 8) //DisplayManager.VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL
                    or (1 shl 10) //DisplayManager.VIRTUAL_DISPLAY_FLAG_TRUSTED
                    or (1 shl 11) //DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP
                    or (1 shl 12) //DisplayManager.VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED
                    or (1 shl 13) //DisplayManager.VIRTUAL_DISPLAY_FLAG_TOUCH_FEEDBACK_DISABLED
            )
        } finally {
            Binder.restoreCallingIdentity(indent)
        }
        mDisplayId = mVirtualDisplay.display.displayId
        mDensityDpi = densityDpi
        log(TAG, "virtual display created: id=$mDisplayId, ${width}x$height,$densityDpi, surface=${surface != null}, launcher=${mLauncherSpec.orEmpty()}")

        try {
            Instances.iWindowManager.apply {
                val configuredImePolicy = AADisplayConfig.DisplayImePolicy.get(config)
                if (configuredImePolicy != DISPLAY_IME_POLICY_LOCAL) {
                    log(TAG, "override display IME policy: $configuredImePolicy -> $DISPLAY_IME_POLICY_LOCAL")
                }
                setDisplayImePolicy(mDisplayId, DISPLAY_IME_POLICY_LOCAL)
                setShouldShowWithInsecureKeyguard(mDisplayId, false)
                setShouldShowSystemDecors(mDisplayId, false)
            }
        } catch (e : Throwable){
            log(TAG, "设置虚拟屏幕参数失败: ", e)
        }
        //mDisplayWindowManager = context.createDisplayContext(mVirtualDisplay.display).getSystemService(WindowManager::class.java).apply {
        mDisplayWindowManager = context.createDisplayContext(mVirtualDisplay.display).createWindowContext(mVirtualDisplay.display, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null).getSystemService(WindowManager::class.java).apply {
            addView(
                mForceView,
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSPARENT
                ).also {
                    it.gravity = Gravity.START or Gravity.TOP
                    it.screenOrientation = if (width > height) {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                    it.alpha = 0f
                    it.width = 0
                    it.height = 0
                }
            )
        }
        Instances.iActivityTaskManager.registerTaskStackListener(mTaskStackListener)
        // Only launch the explicitly configured app; never fall back to a phone launcher.
        startDefaultPackage()
        onVirtualDisplayCreated(mDisplayId)
    }

    fun onReconnected(width: Int, height: Int, densityDpi: Int){
        mVirtualDisplay.resize(width, height, densityDpi)
        mDensityDpi = densityDpi
        refreshLauncherPackage("reconnect")
        trackPackage(mLauncherPackageName, 0)
    }

    fun onDestroy() {
        mIsDestroying = true
        trackPackage(mLauncherPackageName, 0)
        val protectedPackages = linkedSetOf<String>().apply {
            add(BuildConfig.APPLICATION_ID)
            mLauncherPackageName?.let { add(it) }
            addAll(getForegroundPackagesOnDisplay(Display.DEFAULT_DISPLAY))
        }
        tryOrNull { Instances.iActivityTaskManager.unregisterTaskStackListener(mTaskStackListener) }
        tryOrNull {
            val taskInfos = Instances.iActivityTaskManager.getAllRootTaskInfosOnDisplay(mDisplayId)
            taskInfos.forEach { task ->
                trackPackageFromTask(task)
                removeTask(task.taskId)
            }
            mTrackedPackageUsers
                .filterKeys { pkg -> pkg.isNotBlank() && !protectedPackages.contains(pkg) }
                .forEach { (pkg, userIds) ->
                    userIds.ifEmpty { mutableSetOf(0) }.forEach { userId ->
                        try {
                            Instances.activityManagerHidden.forceStopPackageAsUser(pkg, userId)
                            log(TAG, "onDestroy forceStop: $pkg (user=$userId)")
                        } catch (e: Throwable) {
                            log(TAG, "onDestroy forceStop failed: $pkg (user=$userId)", e)
                        }
                    }
                }
            mTrackedPackageUsers.clear()
        }
        tryOrNull {
            if(mDisplayId != Display.INVALID_DISPLAY) {
                // Second pass after initial removals to catch tasks recreated during teardown races.
                val remains = Instances.iActivityTaskManager.getAllRootTaskInfosOnDisplay(mDisplayId)
                remains.forEach { task ->
                    removeTask(task.taskId)
                }
            }
        }
        mSurfaceControls.values.forEach { it.release() }
        mSurfaceControls.clear()
        tryOrNull { mDisplayWindowManager.removeView(mForceView) }
        tryOrNull {
            if (::mVirtualDisplay.isInitialized) {
                mVirtualDisplay.release()
            }
        }
        mDisplayId = Display.INVALID_DISPLAY
        mDensityDpi = 0

        // Keep the service bound until the display has actually been released: this command is
        // explicitly named "after destroying virtual display" and may rely on that state.
        try {
            mShellManager?.destroyVirtualDisplayAfter()
        } catch (e: Throwable) {
            log(TAG, "onDestroy destroyVirtualDisplayAfter ignored:", e)
        } finally {
            tryOrNull { CoreManagerService.systemContext.unbindService(mServiceConnection) }
            mShellManager = null
        }
    }

    private fun getForegroundPackagesOnDisplay(displayId: Int): Set<String> {
        val tasks = try {
            Instances.iActivityTaskManager.getAllRootTaskInfosOnDisplay(displayId)
        } catch (_: Throwable) {
            emptyList()
        }
        if (tasks.isEmpty()) return emptySet()
        val topMost = tasks.firstOrNull { it.topActivity != null }
        val packages = linkedSetOf<String>()
        topMost?.topActivity?.packageName?.let { pkg ->
            if (pkg.isNotBlank()) packages.add(pkg)
        }
        runCatching {
            topMost?.getObjectAs("baseActivity", ComponentName::class.java) as? ComponentName
        }.getOrNull()?.packageName?.let { pkg ->
            if (pkg.isNotBlank()) packages.add(pkg)
        }
        return packages
    }

    fun onTouch(event: MotionEvent) = injectInputEvent(event)

    fun onPressKey(action: Int) {
        // Keep Nitsuya's event shape. Some head-unit/phone combinations ignore BACK when it is
        // synthesized as a virtual hard key with FLAG_FROM_SYSTEM.
        val downTime = SystemClock.uptimeMillis()
        injectInputEvent(KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, action, 0).apply {
            source = InputDevice.SOURCE_KEYBOARD
        })
        injectInputEvent(KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, action, 0).apply {
            source = InputDevice.SOURCE_KEYBOARD
        })
    }

    fun addMirror(surfaceControl: SurfaceControl){
        val sc = SurfaceControl::class.java.newInstance(args(), argTypes()) as SurfaceControl
        try{
            if(!Instances.iWindowManager.mirrorDisplay(mDisplayId, sc)){
                sc.release()
                return
            }
        } catch (e: Throwable){
            TipUtil.showToast("addMirror error: ${e.message}")
            log(TAG, "addMirror error:", e)
            sc.release()
            return
        }
        if (!sc.isValid) {
            sc.release()
            TipUtil.showToast("addMirror not Valid")
            return
        }
        try {
            mTransaction
                .apply {
                    invokeMethod("show", args(sc), argTypes(SurfaceControl::class.java))
                }
                .reparent(sc, surfaceControl)
                .apply()
        } catch (e: Throwable){
            log(TAG, "addMirror show error:", e)
            return
        }
        mSurfaceControls.put(surfaceControl, sc)?.release()
    }

    fun removeMirror(surfaceControl: SurfaceControl){
        mSurfaceControls.remove(surfaceControl)?.also {sc ->
            mTransaction.apply {
                invokeMethod("remove", args(sc), argTypes(SurfaceControl::class.java))
            }.apply()
            sc.release()
        }
    }

    fun getRecentTask(): RecentTask {
        return try{
            RecentTask(
                recentTaskInfo(0),
                if(mDisplayId == Display.INVALID_DISPLAY) emptyList() else recentTaskInfo(mDisplayId)
            )
        } catch (e: Throwable){
            log(TAG, "RecentTask Exception", e)
            RecentTask(emptyList(), emptyList())
        }
    }

    /** Reopen the explicitly configured default app from the AA rail Home button. */
    fun startLauncher(){
        startDefaultPackage()
    }

    /**
     * Launch the app corresponding to default launch package name
     * Called when virtual display is created. If the app is already running, bring it to front; otherwise start a new instance
     */
    private fun startDefaultPackage(){
        val componentName = mLauncherComponent
        if(componentName == null) {
            log(TAG, "startDefaultPackage skipped: no configured package")
            return
        }
        if(mLauncherPackageTaskId != null){
            moveTaskToFront(mLauncherPackageTaskId!!)
        } else {
            startActivity(componentName, 0)
        }
    }

    fun startActivity(packageName: String, userId: Int): Boolean{
        val componentName = resolveLaunchComponent(packageName) ?: return false
        return startActivity(componentName, userId)
    }

    private fun startActivity(componentName: ComponentName, userId: Int): Boolean{
        try {
            if(mDisplayId == Display.INVALID_DISPLAY) return false
            log(TAG, "startActivity: $componentName on display=$mDisplayId user=$userId")
            context.invokeMethod(
                "startActivityAsUser",
                args(
                    Intent().apply {
                        //addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        component = componentName
                        `package` = component?.packageName ?: return false
                        action = Intent.ACTION_VIEW
                        putExtra("displayId", mDisplayId)
                        //putExtra("isUcarMode", true)
                        setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    ActivityOptions.makeBasic().apply {
                        launchDisplayId = mDisplayId
                        this.invokeMethod("setCallerDisplayId", args(mDisplayId), argTypes(Integer.TYPE))
                    }.toBundle(),
                    UserHandle::class.java.newInstance(
                        args(userId),
                        argTypes(Integer.TYPE)
                    )
                ), argTypes(Intent::class.java, Bundle::class.java, UserHandle::class.java)
            )
            return true
      } catch (e: Throwable) {
          log(TAG, "startActivity error:", e)
          return false
      }
    }

    private fun refreshLauncherPackage(reason: String) {
        config?.reload()
        val configured = AADisplayConfig.LauncherPackage.get(config)?.trim()?.takeIf { it.isNotEmpty() }
        val component = configured?.let(::resolveLaunchComponent)
        if (mLauncherSpec != configured || mLauncherComponent != component) {
            log(TAG, "launcher[$reason]: configured=${configured.orEmpty()}, resolved=${component?.flattenToShortString().orEmpty()}")
            mLauncherPackageTaskId = null
        }
        mLauncherSpec = configured
        mLauncherComponent = component
        mLauncherPackageName = component?.packageName
    }

    private fun resolveLaunchComponent(launchSpec: String): ComponentName? {
        val requested = launchSpec.trim()
        if (requested.isEmpty()) return null
        return if (requested.contains("/")) {
            val component = ComponentName.unflattenFromString(requested) ?: return null
            runCatching { Instances.packageManager.getActivityInfo(component, 0) }
                .map { component }
                .getOrElse {
                    log(TAG, "resolveLaunchComponent activity not found: $requested", it)
                    null
                }
        } else {
            Instances.packageManager.getLaunchIntentForPackage(requested)?.component
        }
    }


    fun startTaskId(taskId: Int?, packageName: String, userId: Int): Boolean {
        if(mDisplayId == Display.INVALID_DISPLAY) return false
        if(taskId == null){
            return startActivity(packageName, userId)
        }
        return try {
            moveTaskId(taskId, true)
        } catch (e: Throwable){
            log(TAG,"startTaskId error:", e)
            startActivity(packageName, userId)
        }
    }

    fun moveTaskId(taskId: Int, isVirtualDisplay: Boolean): Boolean {
        if(mDisplayId == Display.INVALID_DISPLAY) return false
        try {
            Instances.iActivityTaskManager.moveRootTaskToDisplay(taskId, if(isVirtualDisplay) mDisplayId else 0)
        } catch (e: Throwable){
            log(TAG,"moveTaskId error:", e)
        }
        return try {
            moveTaskToFront(taskId)
        } catch (e: Throwable){
            log(TAG,"moveTaskId error:", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun moveTaskToFront(taskId: Int): Boolean {
        if(mDisplayId == Display.INVALID_DISPLAY) return false
        return try {
            Instances.activityManager.moveTaskToFront(taskId, 0)
            true
        } catch (e: Throwable){
            log(TAG,"moveTaskToFront error:", e)
            false
        }
    }

    fun removeTask(taskId: Int): Boolean {
        if(mDisplayId == Display.INVALID_DISPLAY) return false
        return try {
            Instances.iActivityTaskManager.removeTask(taskId)
            true
        } catch (e: Throwable){
            log(TAG,"removeTask error:", e)
            false
        }
    }

    /** Move the second task on the virtual display to front. */
    fun moveSecondTaskToFront(){
        if(mDisplayId == Display.INVALID_DISPLAY)
            return
        val allRootTaskInfosOnDisplay = Instances.iActivityTaskManager.getAllRootTaskInfosOnDisplay(mDisplayId).filter { i -> i.topActivity != null }
        if(allRootTaskInfosOnDisplay.size < 2){
            return
        }
        moveTaskToFront(allRootTaskInfosOnDisplay[1].taskId)
    }

    private fun injectInputEvent(event: InputEvent): Boolean {
        if (mDisplayId == Display.INVALID_DISPLAY) return false
        return try {
            event.invokeMethod("setDisplayId", args(mDisplayId), argTypes(Integer.TYPE))
            val result = Instances.iInputManager.injectInputEvent(event, 0)
            if (!result) {
                log(TAG, "injectInputEvent failed: ${event.javaClass.simpleName}")
            }
            result
        } catch (e: Throwable) {
            log(TAG, "injectInputEvent exception:", e)
            false
        }
    }

    private fun trackPackage(packageName: String?, userId: Int = 0) {
        val pkg = packageName?.trim()?.takeIf { it.isNotEmpty() } ?: return
        mTrackedPackageUsers.getOrPut(pkg) { linkedSetOf() }.add(userId)
    }

    private fun trackPackageFromTask(taskInfo: Any) {
        val userId = runCatching {
            taskInfo.getObjectAs("userId", Int::class.javaPrimitiveType) as? Int
        }.getOrNull() ?: 0
        runCatching {
            taskInfo.getObjectAs("topActivity", ComponentName::class.java) as? ComponentName
        }.getOrNull()?.packageName?.let { trackPackage(it, userId) }
        runCatching {
            taskInfo.getObjectAs("baseActivity", ComponentName::class.java) as? ComponentName
        }.getOrNull()?.packageName?.let { trackPackage(it, userId) }
        runCatching {
            taskInfo.getObjectAs("baseIntent", Intent::class.java) as? Intent
        }.getOrNull()?.component?.packageName?.let { trackPackage(it, userId) }
    }

    /**
     * Get recent task list for the specified display
     * Filter out ignored package names.
     */
    private fun recentTaskInfo(displayId: Int): List<RecentTaskInfo> {
        val allRootTaskInfosOnDisplay = Instances.iActivityTaskManager.getAllRootTaskInfosOnDisplay(displayId)
        log(TAG, "RecentTask $displayId, ${allRootTaskInfosOnDisplay.size}")
        return allRootTaskInfosOnDisplay
            .map { taskInfo ->
                val topActivity = taskInfo.topActivity ?: return@map null
                if(IGNORE_RECENT_PACKAGE.contains(topActivity.packageName)) {
                    return@map null
                }

                var taskDescription = taskInfo.taskDescription
                if(taskDescription == null){
                    taskDescription = Instances.iActivityTaskManager.getTaskDescription(taskInfo.taskId) ?: return@map null
                }

                var icon = runCatching { taskDescription.icon }.getOrNull()
                if (icon == null) {
                    icon = Instances.packageManager.getActivityIcon(topActivity).toBitmap()
                }
                var label = taskDescription.label
                if(label == null){
                    label = Instances.packageManager.getActivityInfo(topActivity, 0).loadLabel(Instances.packageManager).toString()
                }

                val packageName = topActivity.packageName

                var snapshot: Bitmap? = runCatching {
                    try {
                        if (Build.VERSION.SDK_INT >= 34) {//14+
                            Instances.iActivityTaskManager.getTaskSnapshot(taskInfo.taskId, true, true)
                        } else {
                            Instances.iActivityTaskManager.getTaskSnapshot(taskInfo.taskId, true)
                        }
                    } catch (e: Throwable){
                        Instances.iActivityTaskManager.getTaskSnapshot(taskInfo.taskId, true)
                    }?.let { taskSnapshot ->
                        taskSnapshot.hardwareBuffer?.let { buffer ->
                            Bitmap.wrapHardwareBuffer(buffer, taskSnapshot.colorSpace)
                        }
                    }
                }.let { result ->
                    if(result.isFailure){
                        log(TAG,"load snapshot exception", result.exceptionOrNull())
                        null
                    } else {
                        result.getOrNull()
                    }
                }

                log(TAG, "RecentTask: $packageName, ${taskInfo.taskId}, snapshot:${snapshot != null}")

                RecentTaskInfo(
                    icon,
                    taskInfo.taskId,
                    label,
                    snapshot
                )
            }
            .filterNotNull()
    }


    inner class TaskStackListener : ITaskStackListener.Stub() {
        override fun onTaskStackChanged() {}
        override fun onActivityPinned(packageName: String?, userId: Int, taskId: Int, stackId: Int) {}
        override fun onActivityUnpinned() {}
        override fun onActivityRestartAttempt(task: ActivityManager.RunningTaskInfo?, homeTaskVisible: Boolean, clearedTask: Boolean, wasVisible: Boolean) {}
        override fun onActivityForcedResizable(packageName: String?, taskId: Int, reason: Int) {}
        override fun onActivityDismissingDockedTask() {}
        override fun onActivityLaunchOnSecondaryDisplayFailed(taskInfo: ActivityManager.RunningTaskInfo?, requestedDisplayId: Int) {}
        override fun onActivityLaunchOnSecondaryDisplayRerouted(taskInfo: ActivityManager.RunningTaskInfo?, requestedDisplayId: Int) {}
        /**
         * Called when a new task is created
         * Record task IDs for the explicitly configured launch package.
         */
        override fun onTaskCreated(taskId: Int, componentName: ComponentName?) {
            val packageName = componentName?.packageName ?: return
            trackPackage(packageName, 0)
            if(packageName == mLauncherPackageName) {
                mLauncherPackageTaskId = taskId
            }
        }
        
        /**
         * Called when a task is removed
         * Keep the task id current; explicit AA Home is responsible for relaunching it.
         */
        override fun onTaskRemoved(taskId: Int) {
            if(mIsDestroying) {
                if(mLauncherPackageTaskId == taskId) {
                    mLauncherPackageTaskId = null
                }
                return
            }
            if(mLauncherPackageTaskId == taskId) {
                mLauncherPackageTaskId = null
            }
        }
        override fun onTaskMovedToFront(taskInfo: ActivityManager.RunningTaskInfo) {
            val packageName = taskInfo.topActivity?.packageName ?: return
            val userId = runCatching {
                taskInfo.getObjectAs("userId", Int::class.javaPrimitiveType) as? Int
            }.getOrNull() ?: 0
            val displayId = runCatching {
                taskInfo.getObjectAs("displayId", Int::class.javaPrimitiveType) as? Int
            }.getOrNull() ?: Display.INVALID_DISPLAY
            trackPackage(packageName, userId)
            if (packageName == mLauncherPackageName && displayId == mDisplayId) {
                mLauncherPackageTaskId = taskInfo.taskId
            }
        }
        override fun onTaskDescriptionChanged(taskInfo: ActivityManager.RunningTaskInfo) {}
        override fun onActivityRequestedOrientationChanged(taskId: Int, requestedOrientation: Int) {}
        override fun onTaskRemovalStarted(taskInfo: ActivityManager.RunningTaskInfo?) {}
        override fun onTaskProfileLocked(taskInfo: ActivityManager.RunningTaskInfo?) {}
        override fun onTaskProfileLocked(taskInfo: ActivityManager.RunningTaskInfo?, userId: Int) {}
        override fun onTaskSnapshotChanged(taskId: Int, snapshot: TaskSnapshot?) {}
        override fun onBackPressedOnTaskRoot(taskInfo: ActivityManager.RunningTaskInfo?) {}
        override fun onTaskDisplayChanged(taskId: Int, newDisplayId: Int) {}
        override fun onRecentTaskListUpdated() {}
        override fun onRecentTaskRemovedForAddTask(taskId: Int) {}
        override fun onRecentTaskListFrozenChanged(frozen: Boolean) {}
        override fun onTaskFocusChanged(taskId: Int, focused: Boolean) {}
        override fun onTaskRequestedOrientationChanged(taskId: Int, requestedOrientation: Int) {}
        override fun onActivityRotation(displayId: Int) {}
        override fun onTaskMovedToBack(taskInfo: ActivityManager.RunningTaskInfo?) {}
        override fun onLockTaskModeChanged(mode: Int) {}
        override fun onTaskSnapshotInvalidated(taskId: Int) {}

        //Samsung OneUi
        override fun onActivityDismissingSplitTask(str: String?) {}
        override fun onOccludeChangeNotice(componentName: ComponentName?, z: Boolean) {}
        override fun onTaskbarIconVisibleChangeRequest(componentName: ComponentName?, z: Boolean) {}
        //Samsung OneUi 7
        override fun onTaskWindowingModeChanged(i: Int) {}
    }
}
