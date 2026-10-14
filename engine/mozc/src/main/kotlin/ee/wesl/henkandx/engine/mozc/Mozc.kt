package ee.wesl.henkandx.engine.mozc

import android.content.Context
import com.google.android.apps.inputmethod.libs.mozc.session.MozcJni
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Command
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Input
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Output
import java.io.File

class Mozc private constructor(val profile: File) {
    val dataVersion: String get() = MozcJni.getDataVersion()

    @Synchronized
    fun eval(command: Command): Command = Command.parseFrom(MozcJni.evalCommand(command.toByteArray()))

    fun eval(input: Input.Builder): Output = eval(Command.newBuilder().setInput(input).build()).output

    var incognito: Boolean
        get() = eval(Input.newBuilder().setType(Input.CommandType.GET_CONFIG)).config.incognitoMode
        set(value) {
            val config = eval(Input.newBuilder().setType(Input.CommandType.GET_CONFIG)).config.toBuilder().setIncognitoMode(value)
            eval(Input.newBuilder().setType(Input.CommandType.SET_CONFIG).setConfig(config))
        }

    fun forget() {
        eval(Input.newBuilder().setType(Input.CommandType.CLEAR_USER_HISTORY))
        eval(Input.newBuilder().setType(Input.CommandType.CLEAR_USER_PREDICTION))
    }

    companion object {
        @Volatile
        private var instance: Mozc? = null

        fun get(context: Context): Mozc = instance ?: synchronized(this) { instance ?: load(context.applicationContext).also { instance = it } }

        private fun load(context: Context): Mozc {
            val dir = context.noBackupFilesDir
            val data = File(dir, "mozc.data")
            val installed = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
            if (data.lastModified() < installed) {
                val tmp = File(dir, "mozc.data.tmp")
                context.assets.open("mozc.data").use { src -> tmp.outputStream().use(src::copyTo) }
                tmp.renameTo(data)
            }
            val profile = File(dir, "mozc").apply { mkdirs() }
            check(MozcJni.onPostLoad(profile.path, data.path))
            return Mozc(profile)
        }
    }
}
