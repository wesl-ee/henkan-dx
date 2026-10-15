package ee.wesl.henkandx

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView

class PacksActivity : Activity() {
    private lateinit var column: LinearLayout

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        setContentView(
            ScrollView(this).apply {
                fitsSystemWindows = true
                addView(column)
            },
        )
    }

    override fun onResume() {
        super.onResume()
        column.removeAllViews()
        packs.forEach { (lang, labels) ->
            val pack = "$packageName.$lang"
            val installed = runCatching { packageManager.getPackageInfo(pack, 0) }.isSuccess
            column.addView(
                LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, pad / 2, 0, pad / 2)
                    addView(
                        CheckBox(context).apply {
                            isChecked = installed
                            isClickable = false
                            isFocusable = false
                        },
                    )
                    addView(
                        Button(context).apply {
                            setText(if (installed) labels.second else labels.first)
                            setPadding(pad, pad, pad, pad)
                            setOnClickListener { if (installed) uninstall(pack) else install() }
                        },
                        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
                    )
                },
            )
        }
    }

    private val pad get() = (16 * resources.displayMetrics.density).toInt()

    // Dev builds have no release so we link to the list of releases.
    private fun install() = startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES + BuildConfig.VERSION_NAME.takeUnless { '-' in it }?.let { "/tag/v$it" }.orEmpty())),
    )

    private fun uninstall(pack: String) = startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pack, null)))

    private companion object {
        const val RELEASES = "https://github.com/wesl-ee/henkan-dx/releases"
        val packs = mapOf("ja" to (R.string.install_ja to R.string.uninstall_ja))
    }
}
