package ee.wesl.henkandx.engine.mozc

import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Input
import org.mozc.android.inputmethod.japanese.protobuf.ProtoUserDictionaryStorage.UserDictionary
import org.mozc.android.inputmethod.japanese.protobuf.ProtoUserDictionaryStorage.UserDictionary.PosType
import org.mozc.android.inputmethod.japanese.protobuf.ProtoUserDictionaryStorage.UserDictionaryStorage
import java.io.File

class MozcDictionary(private val mozc: Mozc) {
    data class Word(val reading: String, val word: String, val pos: String = "名詞")

    private val file = File(mozc.profile, "user_dictionary.db")

    var words: List<Word>
        get() = runCatching { file.inputStream().use(UserDictionaryStorage::parseFrom) }.getOrNull()
            ?.dictionariesList.orEmpty()
            .flatMap { it.entriesList }
            .map { entry -> Word(entry.key, entry.value, names.entries.firstOrNull { it.value == entry.pos }?.key ?: "名詞") }
        set(value) {
            val dictionary = UserDictionary.newBuilder().setId(1).setName("Henkan DX")
            value.distinct().forEach {
                dictionary.addEntries(UserDictionary.Entry.newBuilder().setKey(it.reading).setValue(it.word).setPos(names[it.pos] ?: PosType.NOUN))
            }
            val tmp = File(file.path + ".tmp")
            tmp.outputStream().use { UserDictionaryStorage.newBuilder().addDictionaries(dictionary).build().writeTo(it) }
            tmp.renameTo(file)
            mozc.eval(Input.newBuilder().setType(Input.CommandType.RELOAD))
        }

    fun export(): String = words.joinToString("") { "${it.reading}\t${it.word}\t${it.pos}\t\n" }

    fun import(tsv: String) {
        words = words + tsv.lineSequence()
            .filterNot { it.isBlank() || it.startsWith("#") }
            .map { it.split('\t') }
            .filter { it.size >= 2 && it[0].isNotEmpty() && it[1].isNotEmpty() }
            .map { Word(it[0], it[1], it.getOrNull(2)?.takeIf(names::containsKey) ?: "名詞") }
    }

    private companion object {
        val names = mapOf(
            "名詞" to PosType.NOUN,
            "短縮よみ" to PosType.ABBREVIATION,
            "サジェストのみ" to PosType.SUGGESTION_ONLY,
            "固有名詞" to PosType.PROPER_NOUN,
            "人名" to PosType.PERSONAL_NAME,
            "姓" to PosType.FAMILY_NAME,
            "名" to PosType.FIRST_NAME,
            "組織" to PosType.ORGANIZATION_NAME,
            "地名" to PosType.PLACE_NAME,
            "名詞サ変" to PosType.SA_IRREGULAR_CONJUGATION_NOUN,
            "名詞形動" to PosType.ADJECTIVE_VERBAL_NOUN,
            "数" to PosType.NUMBER,
            "アルファベット" to PosType.ALPHABET,
            "記号" to PosType.SYMBOL,
            "顔文字" to PosType.EMOTICON,
            "副詞" to PosType.ADVERB,
            "連体詞" to PosType.PRENOUN_ADJECTIVAL,
            "接続詞" to PosType.CONJUNCTION,
            "感動詞" to PosType.INTERJECTION,
            "接頭語" to PosType.PREFIX,
            "助数詞" to PosType.COUNTER_SUFFIX,
            "接尾一般" to PosType.GENERIC_SUFFIX,
            "接尾人名" to PosType.PERSON_NAME_SUFFIX,
            "接尾地名" to PosType.PLACE_NAME_SUFFIX,
            "形容詞" to PosType.ADJECTIVE,
            "終助詞" to PosType.SENTENCE_ENDING_PARTICLE,
            "句読点" to PosType.PUNCTUATION,
            "独立語" to PosType.FREE_STANDING_WORD,
            "抑制単語" to PosType.SUPPRESSION_WORD,
        )
    }
}
