package app.simplifymy.gallery.core.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import app.simplifymy.gallery.MainActivity
import app.simplifymy.gallery.ui.theme.AppIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The aliases are declared relative to the manifest package, which the debug build's
 * `applicationIdSuffix` does not touch - so derive it from a class instead of the package name.
 */
private val ALIAS_PREFIX = MainActivity::class.java.name.substringBeforeLast('.') + ".icon."

/** Points the launcher entry at one of the manifest's icon aliases, enabling exactly one. */
@Singleton
class AppIconManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pending = AtomicReference<AppIcon?>(null)

    /** Records the choice. The swap itself waits for [flush]. */
    fun request(icon: AppIcon) = pending.set(icon)

    /**
     * Only safe once the app is off screen. The running task is rooted at the alias being disabled,
     * so swapping while the app is visible finishes the task out from under the user.
     */
    fun flush() {
        val icon = pending.getAndSet(null) ?: return
        scope.launch { swap(icon) }
    }

    private fun swap(icon: AppIcon) {
        val packages = context.packageManager
        if (AppIcon.entries.all { packages.isEnabled(it) == (it == icon) }) return
        // Enable first: disabling every alias before the new one exists drops the app from the launcher.
        packages.setEnabled(icon, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        AppIcon.entries
            .filter { it != icon }
            .forEach { packages.setEnabled(it, PackageManager.COMPONENT_ENABLED_STATE_DISABLED) }
    }

    private fun PackageManager.setEnabled(icon: AppIcon, state: Int) =
        setComponentEnabledSetting(component(icon), state, PackageManager.DONT_KILL_APP)

    private fun PackageManager.isEnabled(icon: AppIcon) =
        when (getComponentEnabledSetting(component(icon))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            else -> icon.enabledByDefault
        }

    private fun component(icon: AppIcon) =
        ComponentName(context.packageName, ALIAS_PREFIX + icon.name)
}
