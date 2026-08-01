package io.github.nitsuya.aa.display.ui.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import io.github.duzhaokun123.template.bases.BaseActivity
import io.github.nitsuya.aa.display.BuildConfig
import io.github.nitsuya.aa.display.CoreApi
import io.github.nitsuya.aa.display.R
import io.github.nitsuya.aa.display.databinding.ActivityMainBinding
import io.github.nitsuya.aa.display.util.AADisplayConfig
import io.github.nitsuya.aa.display.util.SharedPreferencesAccess
import io.github.nitsuya.template.bases.getAttr

class MainActivity :
    BaseActivity<ActivityMainBinding>(
        ActivityMainBinding::class.java,
        Config.NO_BACK,
        Config.LAYOUT_MATCH_HORI
    ),
    MenuProvider {
    companion object {
        const val TAG = "AADisplay_MainActivity"
        private const val LEGACY_DISABLE_WAZE_KEY = "DisableWazeOnAa"
        private const val LEGACY_DISABLE_GOOGLE_MAPS_KEY = "DisableGoogleMapsOnAa"
    }

    private val appConfig by lazy {
        SharedPreferencesAccess.openForHooks(this, AADisplayConfig.ConfigName)
    }

    private var savedLauncherPackage = ""
    private var savedDelayDestroyTime = 180
    private var savedCreateVirtualDisplayBefore = ""
    private var savedDestroyVirtualDisplayAfter = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        ActivityMainBinding.inflate(LayoutInflater.from(this))
        super.onCreate(savedInstanceState)
        SharedPreferencesAccess.makeReadableForHooks(this, AADisplayConfig.ConfigName)
        addMenuProvider(this, this)
    }

    override fun initViews() {
        super.initViews()
        baseBinding.etLauncherPackage.inputType = InputType.TYPE_CLASS_TEXT
        baseBinding.etDelayDestroyTime.inputType = InputType.TYPE_CLASS_NUMBER
        listOf(
            baseBinding.etLauncherPackage,
            baseBinding.etDelayDestroyTime,
            baseBinding.etCreateVirtualDisplayBefore,
            baseBinding.etDestroyVirtualDisplayAfter
        ).forEach { input -> input.doAfterTextChanged { updateSaveButtonState() } }
        baseBinding.btnSave.setOnClickListener { persistSettings() }
    }

    override fun initData() {
        val buildTime = CoreApi.buildTime
        Log.d(TAG, "buildtime: $buildTime ${BuildConfig.BUILD_TIME}")
        when (buildTime) {
            0L -> {
                baseBinding.ivIcon.setImageResource(R.drawable.ic_error_outline_24)
                baseBinding.tvActive.setText(R.string.not_activated)
                baseBinding.tvVersion.text = ""
                val colorError = android.R.attr.colorError
                val colorOnError = theme.getAttr(com.google.android.material.R.attr.colorOnError).data
                baseBinding.mcvStatus.setCardBackgroundColor(colorError)
                baseBinding.mcvStatus.outlineAmbientShadowColor = colorError
                baseBinding.mcvStatus.outlineSpotShadowColor = colorError
                baseBinding.tvActive.setTextColor(colorOnError)
                baseBinding.tvVersion.setTextColor(colorOnError)
            }
            BuildConfig.BUILD_TIME -> {
                baseBinding.ivIcon.setImageResource(R.drawable.ic_round_check_circle_24)
                baseBinding.tvActive.setText(R.string.activated)
                baseBinding.tvVersion.text = CoreApi.versionName
            }
            else -> {
                baseBinding.ivIcon.setImageResource(R.drawable.ic_warning_amber_24)
                baseBinding.tvActive.setText(R.string.need_reboot)
                baseBinding.tvVersion.text = "system: ${CoreApi.versionName}\nmodule: ${BuildConfig.VERSION_NAME}"
                baseBinding.mcvStatus.setCardBackgroundColor(
                    MaterialColors.harmonizeWithPrimary(this, getColor(R.color.color_warning))
                )
                baseBinding.mcvStatus.setOnClickListener {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.need_reboot)
                        .setPositiveButton(R.string.reboot) { _, _ ->
                            Shell.getShell().newJob().add("reboot").exec()
                        }
                        .show()
                }
            }
        }
        baseBinding.systemVersion.text = if (Build.VERSION.PREVIEW_SDK_INT != 0) {
            "${Build.VERSION.CODENAME} Preview (API ${Build.VERSION.SDK_INT})"
        } else {
            "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        }
        baseBinding.rootPrivilege.text = if (Shell.getShell().isRoot) "YES" else "NO"
        disableLegacyCustomAaRail()
        restoreLegacyAaAppStateIfNeeded()
        refreshSettingControls()
    }

    override fun onResume() {
        super.onResume()
        refreshSettingControls()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_main, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
        R.id.github -> {
            startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/Stashboy/AADisplay".toUri()))
            true
        }
        else -> false
    }

    private fun refreshSettingControls() {
        savedLauncherPackage = AADisplayConfig.LauncherPackage.get(appConfig).orEmpty()
        savedDelayDestroyTime = AADisplayConfig.DelayDestroyTime.get(appConfig).coerceAtLeast(0)
        savedCreateVirtualDisplayBefore = appConfig.getString(AADisplayConfig.CreateVirtualDisplayBefore.key, "").orEmpty()
        savedDestroyVirtualDisplayAfter = appConfig.getString(AADisplayConfig.DestroyVirtualDisplayAfter.key, "").orEmpty()
        baseBinding.etLauncherPackage.setText(savedLauncherPackage)
        baseBinding.etDelayDestroyTime.setText(savedDelayDestroyTime.toString())
        baseBinding.etCreateVirtualDisplayBefore.setText(savedCreateVirtualDisplayBefore)
        baseBinding.etDestroyVirtualDisplayAfter.setText(savedDestroyVirtualDisplayAfter)
        validateLauncherPackage(showError = false)
        validateDelayDestroyTime(showError = false)
        updateSaveButtonState()
    }

    private fun launcherPackage(): String = baseBinding.etLauncherPackage.text?.toString()?.trim().orEmpty()

    private fun delayDestroyTime(): Int? = baseBinding.etDelayDestroyTime.text?.toString()?.trim()?.toIntOrNull()?.takeIf { it >= 0 }

    private fun isLaunchSpecValid(value: String): Boolean {
        if (value.isBlank()) return false
        val parts = value.split('/', limit = 2)
        val packageName = parts[0]
        val packagePattern = Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*")
        if (!packagePattern.matches(packageName)) return false
        if (parts.size == 1) return true
        val activityName = parts[1]
        if (activityName.isBlank()) return false
        val normalizedActivity = if (activityName.startsWith('.')) {
            packageName + activityName
        } else {
            activityName
        }
        return Regex("[A-Za-z_][A-Za-z0-9_${'$'}]*(\\.[A-Za-z_][A-Za-z0-9_${'$'}]*)*")
            .matches(normalizedActivity)
    }

    private fun validateLauncherPackage(showError: Boolean): Boolean {
        // The settings process is subject to Android package-visibility filtering. Validate
        // only the syntax here; system_server resolves the package/component when AA starts.
        val valid = isLaunchSpecValid(launcherPackage())
        baseBinding.tilLauncherPackage.error = if (!valid && showError) getString(R.string.default_launch_package_invalid) else null
        return valid
    }

    private fun validateDelayDestroyTime(showError: Boolean): Boolean {
        val valid = delayDestroyTime() != null
        baseBinding.tilDelayDestroyTime.error = if (!valid && showError) getString(R.string.delay_destroy_time_invalid) else null
        return valid
    }

    private fun disableLegacyCustomAaRail() {
        if (!AADisplayConfig.CloseLauncherDashboard.get(appConfig)) return
        if (appConfig.edit().putBoolean(AADisplayConfig.CloseLauncherDashboard.key, false).commit()) {
            SharedPreferencesAccess.makeReadableForHooks(this, AADisplayConfig.ConfigName)
            Log.i(TAG, "Disabled legacy custom AA vertical rail; native AA navigation will be used")
        }
    }

    /** Undo system state changed by the removed Waze/Google Maps suppression feature. */
    private fun restoreLegacyAaAppStateIfNeeded() {
        val shell = Shell.getShell()
        if (!shell.isRoot) return
        fun globalSettingEnabled(key: String): Boolean = shell.newJob()
            .add("settings get global $key")
            .exec()
            .out
            .firstOrNull()
            ?.trim() == "1"

        // Check both preferences and global state because the first customized build removed the
        // preferences before it knew that the component changes also needed to be rolled back.
        val restoreWaze = appConfig.getBoolean(LEGACY_DISABLE_WAZE_KEY, false) ||
            globalSettingEnabled("aad_disable_waze_on_aa")
        val restoreGoogleMaps = appConfig.getBoolean(LEGACY_DISABLE_GOOGLE_MAPS_KEY, false) ||
            globalSettingEnabled("aad_disable_google_maps_on_aa")
        if (!restoreWaze && !restoreGoogleMaps) return

        val commands = mutableListOf<String>()
        if (restoreWaze) {
            commands += "pm enable com.waze/com.waze.car_lib.WazeCarAppService"
            commands += "pm enable com.waze/com.waze.android_auto.AndroidAutoPhoneActivity"
            commands += "settings put global aad_disable_waze_on_aa 0"
        }
        if (restoreGoogleMaps) {
            commands += "pm enable com.google.android.apps.maps/com.google.android.apps.gmm.car.GmmCarProjectionService"
            commands += "pm enable com.google.android.apps.maps/com.google.android.apps.gmm.car.LimitedGmmCarProjectionService"
            commands += "pm enable com.google.android.apps.maps/com.google.android.apps.gmm.car.projected.auxiliarymap.GmmCarAuxiliaryProjectionService"
            commands += "pm enable com.google.android.apps.maps/com.google.android.apps.gmm.car.WidescreenWidgetLimitedGmmCarProjectionService"
            commands += "pm enable com.google.android.apps.maps/com.google.android.apps.auto.client.activity.ghost.GhostActivity"
            commands += "settings put global aad_disable_google_maps_on_aa 0"
        }

        val result = shell.newJob().add(*commands.toTypedArray()).exec()
        if (result.isSuccess) {
            appConfig.edit()
                .remove(LEGACY_DISABLE_WAZE_KEY)
                .remove(LEGACY_DISABLE_GOOGLE_MAPS_KEY)
                .commit()
            SharedPreferencesAccess.makeReadableForHooks(this, AADisplayConfig.ConfigName)
            Log.i(TAG, "Restored legacy Waze/Google Maps component state")
        } else {
            Log.w(TAG, "Failed to restore legacy Waze/Google Maps component state: code=${result.code}")
        }
    }

    private fun persistSettings() {
        if (!validateLauncherPackage(showError = true) || !validateDelayDestroyTime(showError = true)) {
            updateSaveButtonState()
            return
        }
        val launcher = launcherPackage()
        val delay = delayDestroyTime() ?: return
        val createBefore = baseBinding.etCreateVirtualDisplayBefore.text?.toString().orEmpty()
        val destroyAfter = baseBinding.etDestroyVirtualDisplayAfter.text?.toString().orEmpty()
        val saved = appConfig.edit()
            .putString(AADisplayConfig.LauncherPackage.key, launcher)
            .putString(AADisplayConfig.DelayDestroyTime.key, delay.toString())
            .putString(AADisplayConfig.CreateVirtualDisplayBefore.key, createBefore)
            .putString(AADisplayConfig.DestroyVirtualDisplayAfter.key, destroyAfter)
            .putBoolean(AADisplayConfig.AutoOpen.key, false)
            .putBoolean(AADisplayConfig.CloseLauncherDashboard.key, false)
            .remove(AADisplayConfig.HomePackage.key)
            .commit()
        val readableForHooks = SharedPreferencesAccess.makeReadableForHooks(this, AADisplayConfig.ConfigName)
        if (!saved || !readableForHooks) {
            Toast.makeText(this, R.string.settings_saved_pref_access_failed, Toast.LENGTH_LONG).show()
            return
        }
        savedLauncherPackage = launcher
        savedDelayDestroyTime = delay
        savedCreateVirtualDisplayBefore = createBefore
        savedDestroyVirtualDisplayAfter = destroyAfter
        Toast.makeText(this, R.string.settings_saved_successfully, Toast.LENGTH_SHORT).show()
        updateSaveButtonState()
    }

    private fun updateSaveButtonState() {
        val changed = launcherPackage() != savedLauncherPackage ||
            delayDestroyTime() != savedDelayDestroyTime ||
            baseBinding.etCreateVirtualDisplayBefore.text?.toString().orEmpty() != savedCreateVirtualDisplayBefore ||
            baseBinding.etDestroyVirtualDisplayAfter.text?.toString().orEmpty() != savedDestroyVirtualDisplayAfter
        baseBinding.btnSave.isEnabled = changed && validateLauncherPackage(showError = false) && validateDelayDestroyTime(showError = false)
        baseBinding.btnSave.alpha = if (baseBinding.btnSave.isEnabled) 1f else 0.6f
    }
}
