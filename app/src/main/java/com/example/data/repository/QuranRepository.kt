package com.example.data.repository

import android.content.Context
import com.example.data.local.db.CachedAyahEntity
import com.example.data.local.db.CachedSurahEntity
import com.example.data.local.db.QuranCacheDao
import com.example.data.remote.AlQuranApi
import com.example.domain.model.Ayah
import com.example.domain.model.Surah
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class QuranRepository(
    private val quranCacheDao: QuranCacheDao,
    private val api: AlQuranApi = Retrofit.Builder()
        .baseUrl(AlQuranApi.BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(AlQuranApi::class.java)
) {

    fun getSurahsFlow(): Flow<List<Surah>> {
        return quranCacheDao.getAllCachedSurahs().map { cachedList ->
            if (cachedList.isNotEmpty()) {
                cachedList.map { it.toDomain() }
            } else {
                DEFAULT_SURAHS
            }
        }
    }

    suspend fun fetchAndCacheSurahList(): Result<List<Surah>> {
        return try {
            val response = api.getAllSurahs()
            val dataList = response.data
            if (response.code == 200 && !dataList.isNullOrEmpty()) {
                val surahs = dataList.map { dto ->
                    Surah(
                        number = dto.number,
                        name = dto.name,
                        englishName = dto.englishName,
                        englishNameTranslation = dto.englishNameTranslation,
                        numberOfAyahs = dto.numberOfAyahs,
                        revelationType = dto.revelationType
                    )
                }
                quranCacheDao.insertSurahs(surahs.map {
                    CachedSurahEntity(
                        number = it.number,
                        name = it.name,
                        englishName = it.englishName,
                        englishNameTranslation = it.englishNameTranslation,
                        numberOfAyahs = it.numberOfAyahs,
                        revelationType = it.revelationType
                    )
                })
                Result.success(surahs)
            } else {
                Result.success(DEFAULT_SURAHS)
            }
        } catch (e: Exception) {
            // Fallback to cache or default surahs
            val cached = quranCacheDao.getAllCachedSurahs().firstOrNull()
            if (!cached.isNullOrEmpty()) {
                Result.success(cached.map { it.toDomain() })
            } else {
                Result.success(DEFAULT_SURAHS)
            }
        }
    }

    suspend fun getSurahAyahs(surahNumber: Int): Result<List<Ayah>> {
        // First check Room cache
        val localAyahs = quranCacheDao.getAyahsForSurah(surahNumber)
        if (localAyahs.isNotEmpty()) {
            return Result.success(localAyahs.map { it.toDomain() })
        }

        // Otherwise fetch from Remote API
        return try {
            val response = api.getSurahUthmani(surahNumber)
            val surahData = response.data
            if (response.code == 200 && surahData != null) {
                val ayahs = surahData.ayahs.map { dto ->
                    Ayah(
                        number = dto.number,
                        text = dto.text,
                        numberInSurah = dto.numberInSurah,
                        juz = dto.juz ?: 1,
                        page = dto.page ?: 1
                    )
                }
                // Cache ayahs locally
                quranCacheDao.insertAyahs(ayahs.map {
                    CachedAyahEntity(
                        surahNumber = surahNumber,
                        number = it.number,
                        text = it.text,
                        numberInSurah = it.numberInSurah,
                        juz = it.juz,
                        page = it.page
                    )
                })
                Result.success(ayahs)
            } else {
                Result.failure(Exception("HTTP Error: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        // Fallback default list of 114 Surahs
        val DEFAULT_SURAHS = listOf(
            Surah(1, "سُورَةُ الفَاتِحَةِ", "Al-Fatihah", "The Opening", 7, "Meccan"),
            Surah(2, "سُورَةُ البَقَرَةِ", "Al-Baqarah", "The Cow", 286, "Medinan"),
            Surah(3, "سُورَةُ آلِ عِمْرَانَ", "Ali 'Imran", "Family of Imran", 200, "Medinan"),
            Surah(4, "سُورَةُ النِّسَاءِ", "An-Nisa", "The Women", 176, "Medinan"),
            Surah(5, "سُورَةُ المَائِدَةِ", "Al-Ma'idah", "The Table Spread", 120, "Medinan"),
            Surah(6, "سُورَةُ الأَنْعَامِ", "Al-An'am", "The Cattle", 165, "Meccan"),
            Surah(7, "سُورَةُ الأَعْرَافِ", "Al-A'raf", "The Heights", 206, "Meccan"),
            Surah(8, "سُورَةُ الأَنْفَالِ", "Al-Anfal", "The Spoils of War", 75, "Medinan"),
            Surah(9, "سُورَةُ التَّوْبَةِ", "At-Tawbah", "The Repentance", 129, "Medinan"),
            Surah(10, "سُورَةُ يُونُسَ", "Yunus", "Jonah", 109, "Meccan"),
            Surah(11, "سُورَةُ هُودٍ", "Hud", "Hud", 123, "Meccan"),
            Surah(12, "سُورَةُ يُوسُفَ", "Yusuf", "Joseph", 111, "Meccan"),
            Surah(13, "سُورَةُ الرَّعْدِ", "Ar-Ra'd", "The Thunder", 43, "Medinan"),
            Surah(14, "سُورَةُ إِبْرَاهِيمَ", "Ibrahim", "Abraham", 52, "Meccan"),
            Surah(15, "سُورَةُ الحِجْرِ", "Al-Hijr", "The Rocky Tract", 99, "Meccan"),
            Surah(16, "سُورَةُ النَّحْلِ", "An-Nahl", "The Bee", 128, "Meccan"),
            Surah(17, "سُورَةُ الإِسْرَاءِ", "Al-Isra", "The Night Journey", 111, "Meccan"),
            Surah(18, "سُورَةُ الكَهْفِ", "Al-Kahf", "The Cave", 110, "Meccan"),
            Surah(19, "سُورَةُ مَرْيَمَ", "Maryam", "Mary", 98, "Meccan"),
            Surah(20, "سُورَةُ طٰهٰ", "Taha", "Ta-Ha", 135, "Meccan"),
            Surah(21, "سُورَةُ الأَنْبِيَاءِ", "Al-Anbiya", "The Prophets", 112, "Meccan"),
            Surah(22, "سُورَةُ الحَجِّ", "Al-Hajj", "The Pilgrimage", 78, "Medinan"),
            Surah(23, "سُورَةُ المُؤْمِنُونَ", "Al-Mu'minun", "The Believers", 118, "Meccan"),
            Surah(24, "سُورَةُ النُّورِ", "An-Nur", "The Light", 64, "Medinan"),
            Surah(25, "سُورَةُ الفُرْقَانِ", "Al-Furqan", "The Criterian", 77, "Meccan"),
            Surah(26, "سُورَةُ الشُّعَرَاءِ", "Ash-Shu'ara", "The Poets", 227, "Meccan"),
            Surah(27, "سُورَةُ النَّمْلِ", "An-Naml", "The Ant", 93, "Meccan"),
            Surah(28, "سُورَةُ القَصَصِ", "Al-Qasas", "The Stories", 88, "Meccan"),
            Surah(29, "سُورَةُ العَنْكَبُوتِ", "Al-'Ankabut", "The Spider", 69, "Meccan"),
            Surah(30, "سُورَةُ الرُّومِ", "Ar-Rum", "The Romans", 60, "Meccan"),
            Surah(31, "سُورَةُ لُقْمَانَ", "Luqman", "Luqman", 34, "Meccan"),
            Surah(32, "سُورَةُ السَّجْدَةِ", "As-Sajdah", "The Prostration", 30, "Meccan"),
            Surah(33, "سُورَةُ الأَحْزَابِ", "Al-Ahzab", "The Combined Forces", 73, "Medinan"),
            Surah(34, "سُورَةُ سَبَإٍ", "Saba", "Sheba", 54, "Meccan"),
            Surah(35, "سُورَةُ فَاطِرٍ", "Fatir", "Originator", 45, "Meccan"),
            Surah(36, "سُورَةُ يٰسۤ", "Ya-Sin", "Ya Sin", 83, "Meccan"),
            Surah(37, "سُورَةُ الصَّافَّاتِ", "As-Saffat", "Those Who Set The Ranks", 182, "Meccan"),
            Surah(38, "سُورَةُ صۤ", "Sad", "Sad", 88, "Meccan"),
            Surah(39, "سُورَةُ الزُّمَرِ", "Az-Zumar", "The Troops", 75, "Meccan"),
            Surah(40, "سُورَةُ غَافِرٍ", "Ghafir", "The Forgiver", 85, "Meccan"),
            Surah(41, "سُورَةُ فُصِّلَتْ", "Fussilat", "Explained In Detail", 54, "Meccan"),
            Surah(42, "سُورَةُ الشُّورَىٰ", "Ash-Shura", "The Consultation", 53, "Meccan"),
            Surah(43, "سُورَةُ الزُّخْرُفِ", "Az-Zukhruf", "The Ornaments Of Gold", 89, "Meccan"),
            Surah(44, "سُورَةُ الدُّخَانِ", "Ad-Dukhan", "The Smoke", 59, "Meccan"),
            Surah(45, "سُورَةُ الجَاثِيَةِ", "Al-Jathiyah", "The Crouching", 37, "Meccan"),
            Surah(46, "سُورَةُ الأَحْقَافِ", "Al-Ahqaf", "The Wind-Curved Sandhills", 35, "Meccan"),
            Surah(47, "سُورَةُ مُحَمَّدٍ", "Muhammad", "Muhammad", 38, "Medinan"),
            Surah(48, "سُورَةُ الفَتْحِ", "Al-Fath", "The Victory", 29, "Medinan"),
            Surah(49, "سُورَةُ الحُجُرَاتِ", "Al-Hujurat", "The Dwellings", 18, "Medinan"),
            Surah(50, "سُورَةُ قۤ", "Qaf", "Qaf", 45, "Meccan"),
            Surah(51, "سُورَةُ الذَّارِيَاتِ", "Adh-Dhariyat", "The Winnowing Winds", 60, "Meccan"),
            Surah(52, "سُورَةُ الطُّورِ", "At-Tur", "The Mount", 49, "Meccan"),
            Surah(53, "سُورَةُ النَّجْمِ", "An-Najm", "The Star", 62, "Meccan"),
            Surah(54, "سُورَةُ القَمَرِ", "Al-Qamar", "The Moon", 55, "Meccan"),
            Surah(55, "سُورَةُ الرَّحْمٰنِ", "Ar-Rahman", "The Beneficent", 78, "Medinan"),
            Surah(56, "سُورَةُ الوَاقِعَةِ", "Al-Waqi'ah", "The Inevitable", 96, "Meccan"),
            Surah(57, "سُورَةُ الحَدِيدِ", "Al-Hadid", "The Iron", 29, "Medinan"),
            Surah(58, "سُورَةُ المُجَادِلَةِ", "Al-Mujadila", "The Pleading Woman", 22, "Medinan"),
            Surah(59, "سُورَةُ الحَشْرِ", "Al-Hashr", "The Exile", 24, "Medinan"),
            Surah(60, "سُورَةُ المُمْتَحَنَةِ", "Al-Mumtahanah", "She That Is To Be Examined", 13, "Medinan"),
            Surah(61, "سُورَةُ الصَّفِّ", "As-Saff", "The Ranks", 14, "Medinan"),
            Surah(62, "سُورَةُ المُجُمُعَةِ", "Al-Jumu'ah", "The Congregation", 11, "Medinan"),
            Surah(63, "سُورَةُ المُنَافِقُونَ", "Al-Munafiqun", "The Hypocrites", 11, "Medinan"),
            Surah(64, "سُورَةُ التَّغَابُنِ", "At-Taghabun", "The Mutual Disillusion", 18, "Medinan"),
            Surah(65, "سُورَةُ الطَّلَاقِ", "At-Talaq", "The Divorce", 12, "Medinan"),
            Surah(66, "سُورَةُ التَّحْرِيمِ", "At-Tahrim", "The Prohibition", 12, "Medinan"),
            Surah(67, "سُورَةُ المُلْكِ", "Al-Mulk", "The Sovereignty", 30, "Meccan"),
            Surah(68, "سُورَةُ القَلَمِ", "Al-Qalam", "The Pen", 52, "Meccan"),
            Surah(69, "سُورَةُ الحَاقَّةِ", "Al-Haqqah", "The Inevitable", 52, "Meccan"),
            Surah(70, "سُورَةُ المَعَارِجِ", "Al-Ma'arij", "The Ascending Stairways", 44, "Meccan"),
            Surah(71, "سُورَةُ نُوحٍ", "Nuh", "Noah", 28, "Meccan"),
            Surah(72, "سُورَةُ الجِنِّ", "Al-Jinn", "The Jinn", 28, "Meccan"),
            Surah(73, "سُورَةُ المُزَّمِّلِ", "Al-Muzzammil", "The Enshrouded One", 20, "Meccan"),
            Surah(74, "سُورَةُ المُدَّثِّرِ", "Al-Muddaththir", "The Cloaked One", 56, "Meccan"),
            Surah(75, "سُورَةُ القِيَامَةِ", "Al-Qiyamah", "The Resurrection", 40, "Meccan"),
            Surah(76, "سُورَةُ الإِنْسَانِ", "Al-Insan", "Man", 31, "Medinan"),
            Surah(77, "سُورَةُ المُرْسَلَاتِ", "Al-Mursalat", "The Emissaries", 50, "Meccan"),
            Surah(78, "سُورَةُ النَّبَإِ", "An-Naba", "The Tidings", 40, "Meccan"),
            Surah(79, "سُورَةُ النَّازِعَاتِ", "An-Nazi'at", "Those Who Drag Forth", 46, "Meccan"),
            Surah(80, "سُورَةُ عَبَسَ", "Abasa", "He Frowned", 42, "Meccan"),
            Surah(81, "سُورَةُ التَّكْوِيرِ", "At-Takwir", "The Overthrowing", 29, "Meccan"),
            Surah(82, "سُورَةُ الإِنْفِطَارِ", "Al-Infitar", "The Cleaving", 19, "Meccan"),
            Surah(83, "سُورَةُ المُطَفِّفِينَ", "Al-Mutaffifin", "Defrauding", 36, "Meccan"),
            Surah(84, "سُورَةُ الإِنْشِقَاقِ", "Al-Inshiqaq", "The Sundering", 25, "Meccan"),
            Surah(85, "سُورَةُ البُرُوجِ", "Al-Buruj", "The Mansions Of The Stars", 22, "Meccan"),
            Surah(86, "سُورَةُ الطَّارِقِ", "At-Tariq", "The Morning Star", 17, "Meccan"),
            Surah(87, "سُورَةُ الأَعْلَىٰ", "Al-A'la", "The Most High", 19, "Meccan"),
            Surah(88, "سُورَةُ الغَاشِيَةِ", "Al-Ghashiyah", "The Overwhelming", 26, "Meccan"),
            Surah(89, "سُورَةُ الفَجْرِ", "Al-Fajr", "The Dawn", 30, "Meccan"),
            Surah(90, "سُورَةُ البَلَدِ", "Al-Balad", "The City", 20, "Meccan"),
            Surah(91, "سُورَةُ الشَّمْسِ", "Ash-Shams", "The Sun", 15, "Meccan"),
            Surah(92, "سُورَةُ اللَّيْلِ", "Al-Layl", "The Night", 21, "Meccan"),
            Surah(93, "سُورَةُ الضُّحَىٰ", "Ad-Duha", "The Morning Hours", 11, "Meccan"),
            Surah(94, "سُورَةُ الشَّرْحِ", "Ash-Sharh", "The Relief", 8, "Meccan"),
            Surah(95, "سُورَةُ التِّينِ", "At-Tin", "The Fig", 8, "Meccan"),
            Surah(96, "سُورَةُ العَلَقِ", "Al-'Alaq", "The Clot", 19, "Meccan"),
            Surah(97, "سُورَةُ القَدْرِ", "Al-Qadr", "The Power", 5, "Meccan"),
            Surah(98, "سُورَةُ البَيِّنَةِ", "Al-Bayyinah", "The Clear Proof", 8, "Medinan"),
            Surah(99, "سُورَةُ الزَّلْزَلَةِ", "Az-Zalzalah", "The Earthquake", 8, "Medinan"),
            Surah(100, "سُورَةُ العَادِيَاتِ", "Al-'Adiyat", "The Courser", 11, "Meccan"),
            Surah(101, "سُورَةُ القَارِعَةِ", "Al-Qari'ah", "The Calamity", 11, "Meccan"),
            Surah(102, "سُورَةُ التَّكَاثُرِ", "At-Takathur", "Rivalry In World Increase", 8, "Meccan"),
            Surah(103, "سُورَةُ العَصْرِ", "Al-'Asr", "The Declining Day", 3, "Meccan"),
            Surah(104, "سُورَةُ الهُمَزَةِ", "Al-Humazah", "The Traducer", 9, "Meccan"),
            Surah(105, "سُورَةُ الفِيلِ", "Al-Fil", "The Elephant", 5, "Meccan"),
            Surah(106, "سُورَةُ قُرَيْشٍ", "Quraysh", "Quraysh", 4, "Meccan"),
            Surah(107, "سُورَةُ المَاعُونِ", "Al-Ma'un", "Small Kindnesses", 7, "Meccan"),
            Surah(108, "سُورَةُ الكَوْثَرِ", "Al-Kawthar", "Abundance", 3, "Meccan"),
            Surah(109, "سُورَةُ الكَافِرُونَ", "Al-Kafirun", "The Disbelievers", 6, "Meccan"),
            Surah(110, "سُورَةُ النَّصْرِ", "An-Nasr", "The Divine Support", 3, "Medinan"),
            Surah(111, "سُورَةُ المَسَدِ", "Al-Masad", "The Palm Fibre", 5, "Meccan"),
            Surah(112, "سُورَةُ الإِخْلَاصِ", "Al-Ikhlas", "The Sincerity", 4, "Meccan"),
            Surah(113, "سُورَةُ الفَلَقِ", "Al-Falaq", "The Daybreak", 5, "Meccan"),
            Surah(114, "سُورَةُ النَّاسِ", "An-Nas", "Mankind", 6, "Meccan")
        )
    }
}
