package ee.wesl.henkandx

import android.content.Context
import android.view.InputDevice
import android.view.KeyEvent
import ee.wesl.henkandx.core.Key
import ee.wesl.henkandx.core.Profile
import org.json.JSONObject

fun loadProfile(context: Context): Profile {
    val devices = InputDevice.getDeviceIds().map(InputDevice::getDevice).mapNotNull { it?.name }.toSet()
    val profiles = context.assets.list("").orEmpty().filter { it.endsWith(".json") }.map { name ->
        JSONObject(context.assets.open(name).bufferedReader().use { it.readText() })
    }
    return profiles.filter { !it.has("device") || it.getString("device") in devices }
        .sortedBy { it.has("device") }
        .map {
            Profile(
                it.codes("keycodes", KeyEvent::keyCodeFromString, Key::of),
                it.codes("scancodes", String::toInt, Key::of),
                it.codes("nav", KeyEvent::keyCodeFromString, KeyEvent::keyCodeFromString),
                it.optString("register").takeIf(String::isNotEmpty)?.let(KeyEvent::keyCodeFromString),
            )
        }
        .fold(Profile(), Profile::plus)
}

private fun <V> JSONObject.codes(field: String, key: (String) -> Int, value: (String) -> V): Map<Int, V> {
    val map = optJSONObject(field) ?: return emptyMap()
    return map.keys().asSequence().associate { key(it) to value(map.getString(it)) }
}
