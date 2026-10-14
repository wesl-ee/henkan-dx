package ee.wesl.henkandx.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PolicyTest {
    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "text,                  0x1,        0x0,       false, true",
        "multi-line,            0x20001,    0x0,       false, true",
        "short message,         0x41,       0x0,       false, true",
        "person name,           0x61,       0x0,       false, true",
        "password,              0x81,       0x0,       true,  false",
        "visible password,      0x91,       0x0,       true,  false",
        "web password,          0xe1,       0x0,       true,  false",
        "number password,       0x12,       0x0,       true,  false",
        "no suggestions,        0x80001,    0x0,       false, true",
        "keep note,             0xac001,    0x54000001, false, true",
        "incognito,             0x1,        0x1000000, true,  false",
        "uri,                   0x11,       0x0,       false, false",
        "email,                 0x21,       0x0,       false, false",
        "web email,             0xd1,       0x0,       false, false",
        "number,                0x2,        0x0,       false, false",
        "signed decimal number, 0x3002,     0x0,       false, false",
        "phone,                 0x3,        0x0,       false, false",
        "datetime,              0x4,        0x0,       false, false",
        "null,                  0x0,        0x0,       false, false",
    )
    fun `field policy`(name: String, inputType: String, imeOptions: String, isSensitive: Boolean, isComposable: Boolean) {
        val type = inputType.removePrefix("0x").toInt(16)
        val options = imeOptions.removePrefix("0x").toInt(16)
        assertEquals(isSensitive, sensitive(type, options), "sensitive")
        assertEquals(isComposable, composable(type, options), "composable")
    }
}
