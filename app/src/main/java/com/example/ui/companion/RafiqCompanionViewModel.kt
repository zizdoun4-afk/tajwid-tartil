package com.example.ui.companion

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayer
import com.example.audio.PacingAssessment
import com.example.audio.QuranRecorderEngine
import com.example.audio.QuranRecorderEngineImpl
import com.example.audio.RecitationAnalysisEngine
import com.example.audio.RecitationDiagnostic
import com.example.audio.TakeResult
import com.example.data.local.db.AppDatabase
import com.example.data.local.db.MemorizationStatusEntity
import com.example.data.local.preferences.UserPreferencesRepository
import com.example.data.repository.MemorizationRepository
import com.example.data.repository.QuranRepository
import com.example.domain.companion.DailyCompanionRoutine
import com.example.domain.companion.DailySmartAgenda
import com.example.domain.companion.SmartCompanionEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

data class RafiqQueryResponse(
    val queryKey: String,
    val titleAr: String,
    val titleFr: String,
    val titleEn: String,
    val detailsAr: String,
    val detailsFr: String,
    val detailsEn: String,
    val actionLabelAr: String?,
    val actionLabelFr: String?,
    val actionLabelEn: String?,
    val actionType: RafiqActionType?,
    val targetSurah: Int = 1,
    val targetAyah: Int = 1
)

enum class RafiqActionType {
    START_TRAINING,
    OPEN_READER,
    LOAD_QUEUE
}

class RafiqCompanionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val memorizationDao = db.memorizationDao()
    private val tajwidProgressDao = db.tajwidProgressDao()
    private val quranCacheDao = db.quranCacheDao()

    private val memorizationRepository = MemorizationRepository(memorizationDao)
    private val quranRepository = QuranRepository(quranCacheDao, application)
    private val prefsRepository = UserPreferencesRepository(application)

    val recorderEngine: QuranRecorderEngine = QuranRecorderEngineImpl(application)
    val audioPlayer = AudioPlayer(application)

    private var userPlayer: MediaPlayer? = null

    // Smart Agenda
    private val _agenda = MutableStateFlow<DailySmartAgenda?>(null)
    val agenda: StateFlow<DailySmartAgenda?> = _agenda.asStateFlow()

    // Smart Daily Companion Routine
    private val _dailyRoutine = MutableStateFlow<DailyCompanionRoutine?>(null)
    val dailyRoutine: StateFlow<DailyCompanionRoutine?> = _dailyRoutine.asStateFlow()

    private val _nextActionSuggestion = MutableStateFlow<String?>(null)
    val nextActionSuggestion: StateFlow<String?> = _nextActionSuggestion.asStateFlow()

    // Interactive Query Response from Rafiq Orchestrator
    private val _activeQueryResponse = MutableStateFlow<RafiqQueryResponse?>(null)
    val activeQueryResponse: StateFlow<RafiqQueryResponse?> = _activeQueryResponse.asStateFlow()

    // Smart Session Queue
    private val _sessionQueue = MutableStateFlow<List<MemorizationStatusEntity>>(emptyList())
    val sessionQueue: StateFlow<List<MemorizationStatusEntity>> = _sessionQueue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    // Active Ayah under study
    private val _currentSurahNumber = MutableStateFlow(1)
    val currentSurahNumber: StateFlow<Int> = _currentSurahNumber.asStateFlow()

    private val _currentAyahNumber = MutableStateFlow(1)
    val currentAyahNumber: StateFlow<Int> = _currentAyahNumber.asStateFlow()

    private val _currentAyahText = MutableStateFlow("")
    val currentAyahText: StateFlow<String> = _currentAyahText.asStateFlow()

    private val _isTextRevealed = MutableStateFlow(false)
    val isTextRevealed: StateFlow<Boolean> = _isTextRevealed.asStateFlow()

    // Audio & Recording State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _lastTakeResult = MutableStateFlow<TakeResult?>(null)
    val lastTakeResult: StateFlow<TakeResult?> = _lastTakeResult.asStateFlow()

    private val recordedDbSamples = mutableListOf<Float>()
    private var sampleCollectorJob: Job? = null

    // Diagnostic
    private val _diagnostic = MutableStateFlow<RecitationDiagnostic?>(null)
    val diagnostic: StateFlow<RecitationDiagnostic?> = _diagnostic.asStateFlow()

    // Audio playback status
    private val _isPlayingModel = MutableStateFlow(false)
    val isPlayingModel: StateFlow<Boolean> = _isPlayingModel.asStateFlow()

    private val _isPlayingUser = MutableStateFlow(false)
    val isPlayingUser: StateFlow<Boolean> = _isPlayingUser.asStateFlow()

    init {
        loadAgenda()
        loadAyahText(1, 1)
    }

    fun loadAgenda() {
        viewModelScope.launch {
            val smartAgenda = SmartCompanionEngine.computeDailyAgenda(
                memorizationDao = memorizationDao,
                tajwidProgressDao = tajwidProgressDao
            )
            _agenda.value = smartAgenda

            val routine = SmartCompanionEngine.computeDailyRoutine(
                memorizationDao = memorizationDao
            )
            _dailyRoutine.value = routine
            _nextActionSuggestion.value = routine.nextActionSuggestionFr

            // If queue is empty, populate from due or weak
            if (_sessionQueue.value.isEmpty()) {
                val queue = when {
                    smartAgenda.dueForReview.isNotEmpty() -> smartAgenda.dueForReview
                    smartAgenda.weakAyahs.isNotEmpty() -> smartAgenda.weakAyahs
                    smartAgenda.inLearning.isNotEmpty() -> smartAgenda.inLearning
                    else -> emptyList()
                }
                _sessionQueue.value = queue
                if (queue.isNotEmpty()) {
                    selectAyah(queue.first().surahNumber, queue.first().ayahNumber)
                }
            }
        }
    }

    fun selectAyah(surahNumber: Int, ayahNumber: Int) {
        _currentSurahNumber.value = surahNumber
        _currentAyahNumber.value = ayahNumber
        _isTextRevealed.value = false
        _diagnostic.value = null
        _lastTakeResult.value = null
        loadAyahText(surahNumber, ayahNumber)
    }

    private fun loadAyahText(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            try {
                val result = quranRepository.getSurahAyahs(surahNumber)
                if (result.isSuccess) {
                    val ayahs = result.getOrNull() ?: emptyList()
                    val target = ayahs.find { it.numberInSurah == ayahNumber }
                    if (target != null) {
                        _currentAyahText.value = target.text
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleRevealText() {
        _isTextRevealed.value = !_isTextRevealed.value
    }

    fun startRecording() {
        viewModelScope.launch {
            stopAudio()
            recordedDbSamples.clear()
            val started = recorderEngine.startTake()
            if (started) {
                _isRecording.value = true
                _diagnostic.value = null
                // Collect amplitude metering
                sampleCollectorJob?.cancel()
                sampleCollectorJob = launch {
                    recorderEngine.uiState.collect { state ->
                        if (state.isTaking) {
                            recordedDbSamples.add(state.meteringDb)
                        }
                    }
                }
            }
        }
    }

    fun stopRecordingAndAnalyze() {
        viewModelScope.launch {
            sampleCollectorJob?.cancel()
            sampleCollectorJob = null
            _isRecording.value = false

            val result = recorderEngine.stopTake()
            _lastTakeResult.value = result

            if (result != null) {
                val file = File(result.filePath)
                val diag = RecitationAnalysisEngine.analyzeRecitation(
                    audioFile = file,
                    surahNumber = _currentSurahNumber.value,
                    ayahNumber = _currentAyahNumber.value,
                    ayahText = _currentAyahText.value,
                    recordedDbSamples = recordedDbSamples.toList()
                )
                _diagnostic.value = diag
                // Auto-reveal text after recitation to allow comparison
                _isTextRevealed.value = true
            }
        }
    }

    fun playModelRecitation() {
        viewModelScope.launch {
            stopAudio()
            try {
                _isPlayingModel.value = true
                val surah = _currentSurahNumber.value
                val ayah = _currentAyahNumber.value
                // Streaming from EveryAyah Alafasy
                val formattedSurah = surah.toString().padStart(3, '0')
                val formattedAyah = ayah.toString().padStart(3, '0')
                val url = "https://everyayah.com/data/Alafasy_128kbps/$formattedSurah$formattedAyah.mp3"

                audioPlayer.togglePlayPause(url) {
                    _isPlayingModel.value = false
                }
            } catch (e: Exception) {
                _isPlayingModel.value = false
            }
        }
    }

    fun playUserRecording() {
        val result = _lastTakeResult.value ?: return
        val file = File(result.filePath)
        if (!file.exists()) return

        stopAudio()
        try {
            _isPlayingUser.value = true
            userPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    _isPlayingUser.value = false
                    releasePlayer()
                }
            }
        } catch (e: Exception) {
            _isPlayingUser.value = false
            releasePlayer()
        }
    }

    fun stopAudio() {
        try {
            audioPlayer.stop()
            _isPlayingModel.value = false
        } catch (e: Exception) {
            // Ignore
        }
        releasePlayer()
    }

    private fun releasePlayer() {
        try {
            userPlayer?.stop()
            userPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            userPlayer = null
            _isPlayingUser.value = false
        }
    }

    /**
     * Completes review of current ayah with self-validation.
     * Updates SRS interval in Room DB and advances to next ayah in queue.
     */
    fun validateCurrentAyah(isMastered: Boolean) {
        viewModelScope.launch {
            val surah = _currentSurahNumber.value
            val ayah = _currentAyahNumber.value

            memorizationRepository.recordTestResult(surah, ayah, isMastered)

            // Reload agenda stats
            loadAgenda()

            // Advance in queue
            val nextIndex = _currentQueueIndex.value + 1
            if (nextIndex < _sessionQueue.value.size) {
                _currentQueueIndex.value = nextIndex
                val nextEntity = _sessionQueue.value[nextIndex]
                selectAyah(nextEntity.surahNumber, nextEntity.ayahNumber)
            }
        }
    }

    fun clearQueryResponse() {
        _activeQueryResponse.value = null
    }

    fun handleQueryReviewToday() {
        viewModelScope.launch {
            val due = memorizationDao.getDueForReview(System.currentTimeMillis()).first()
            val weak = memorizationDao.getWeakVerses()
            if (due.isNotEmpty()) {
                val first = due.first()
                _activeQueryResponse.value = RafiqQueryResponse(
                    queryKey = "REVIEW_TODAY",
                    titleAr = "مراجعة اليوم",
                    titleFr = "Révision d'aujourd'hui",
                    titleEn = "Today's Review",
                    detailsAr = "لديك ${due.size} آية مستحقة للمراجعة في جدول التكرار المتباعد. يُستحسن البدء بسورة ${first.surahNumber} الآية ${first.ayahNumber}.",
                    detailsFr = "Vous avez ${due.size} verset(s) à réviser selon votre cycle SRS. Nous vous conseillons de débuter par la sourate ${first.surahNumber}, verset ${first.ayahNumber}.",
                    detailsEn = "You have ${due.size} verse(s) due for spaced review today. Start with Surah ${first.surahNumber}, Ayah ${first.ayahNumber}.",
                    actionLabelAr = "مراجعة الآية الآن ▶",
                    actionLabelFr = "Réviser ce verset ▶",
                    actionLabelEn = "Review Now ▶",
                    actionType = RafiqActionType.START_TRAINING,
                    targetSurah = first.surahNumber,
                    targetAyah = first.ayahNumber
                )
                selectAyah(first.surahNumber, first.ayahNumber)
            } else if (weak.isNotEmpty()) {
                val first = weak.first()
                _activeQueryResponse.value = RafiqQueryResponse(
                    queryKey = "REVIEW_TODAY",
                    titleAr = "تثبيت الآيات غير المستقرة",
                    titleFr = "Consolidation des fragilités",
                    titleEn = "Reinforce Weak Verses",
                    detailsAr = "لا توجد آيات مستحقة اليوم، لكن لديك ${weak.size} آية بحاجة لتثبيت. نقترح مراجعة سورة ${first.surahNumber} الآية ${first.ayahNumber}.",
                    detailsFr = "Aucun verset dû aujourd'hui, mais vous avez ${weak.size} verset(s) fragile(s) à consolider (Sourate ${first.surahNumber}, Verset ${first.ayahNumber}).",
                    detailsEn = "No reviews overdue, but you have ${weak.size} verse(s) needing consolidation (Surah ${first.surahNumber}, Ayah ${first.ayahNumber}).",
                    actionLabelAr = "تثبيت الآية ▶",
                    actionLabelFr = "Consolider ce verset ▶",
                    actionLabelEn = "Reinforce Verse ▶",
                    actionType = RafiqActionType.START_TRAINING,
                    targetSurah = first.surahNumber,
                    targetAyah = first.ayahNumber
                )
                selectAyah(first.surahNumber, first.ayahNumber)
            } else {
                _activeQueryResponse.value = RafiqQueryResponse(
                    queryKey = "REVIEW_TODAY",
                    titleAr = "الحفظ في مسار مثالي",
                    titleFr = "Mémorisation à jour",
                    titleEn = "All Up to Date",
                    detailsAr = "بارك الله فيك! لا توجد آيات مستحقة أو متعثرة حالياً. يمكنك تعلم حفظ جديد أو تلاوة الورد اليومي.",
                    detailsFr = "Excellent ! Toutes vos révisions sont à jour. Vous pouvez mémoriser de nouveaux versets ou lire votre wird.",
                    detailsEn = "Masha'Allah! All reviews are up to date. You can start memorizing new verses or read Quran.",
                    actionLabelAr = null,
                    actionLabelFr = null,
                    actionLabelEn = null,
                    actionType = null
                )
            }
        }
    }

    fun handleQueryLearnToday() {
        viewModelScope.launch {
            val inLearning = memorizationDao.getInLearningStatus().first()
            val target = if (inLearning.isNotEmpty()) {
                inLearning.first()
            } else {
                // Next after current
                val current = _currentAyahNumber.value
                val surah = _currentSurahNumber.value
                val existing = memorizationDao.getStatusForAyah(surah, current + 1)
                existing ?: MemorizationStatusEntity(surahNumber = surah, ayahNumber = current + 1, status = "NOT_STARTED")
            }

            _activeQueryResponse.value = RafiqQueryResponse(
                queryKey = "LEARN_TODAY",
                titleAr = "حفظ جديد اليوم",
                titleFr = "Nouvel apprentissage",
                titleEn = "New Memorization",
                detailsAr = "الآية الموصى بها لحفظ اليوم: سورة ${target.surahNumber}، الآية ${target.ayahNumber}. التدرب عليها يتم عبر الاستماع 3 مرات ثم الترديد.",
                detailsFr = "Verset recommandé pour aujourd'hui : Sourate ${target.surahNumber}, Verset ${target.ayahNumber}. Commencez par l'écoute 3x puis la répétition guidée.",
                detailsEn = "Recommended verse to memorize today: Surah ${target.surahNumber}, Ayah ${target.ayahNumber}. Listen 3x then repeat.",
                actionLabelAr = "بدء الحفظ ▶",
                actionLabelFr = "Démarrer l'apprentissage ▶",
                actionLabelEn = "Start Memorizing ▶",
                actionType = RafiqActionType.START_TRAINING,
                targetSurah = target.surahNumber,
                targetAyah = target.ayahNumber
            )
            selectAyah(target.surahNumber, target.ayahNumber)
        }
    }

    fun handleQueryWeakAyahs() {
        viewModelScope.launch {
            val weak = memorizationDao.getWeakVerses()
            if (weak.isNotEmpty()) {
                val first = weak.first()
                val listSummary = weak.take(3).joinToString("، ") { "سورة ${it.surahNumber}:${it.ayahNumber}" }
                _activeQueryResponse.value = RafiqQueryResponse(
                    queryKey = "WEAK_AYAHS",
                    titleAr = "الآيات التي تحتاج إلى تثبيت",
                    titleFr = "Versets à consolider",
                    titleEn = "Verses Needing Work",
                    detailsAr = "حددنا ${weak.size} آية بها نسبة أخطاء سابقة في الاختبارات: $listSummary.",
                    detailsFr = "Nous avons identifié ${weak.size} verset(s) avec des erreurs fréquentes : $listSummary.",
                    detailsEn = "Identified ${weak.size} verse(s) with previous test mistakes: $listSummary.",
                    actionLabelAr = "تثبيت الآية الأولى ▶",
                    actionLabelFr = "Consolider la 1ère ▶",
                    actionLabelEn = "Reinforce First ▶",
                    actionType = RafiqActionType.START_TRAINING,
                    targetSurah = first.surahNumber,
                    targetAyah = first.ayahNumber
                )
                selectAyah(first.surahNumber, first.ayahNumber)
            } else {
                _activeQueryResponse.value = RafiqQueryResponse(
                    queryKey = "WEAK_AYAHS",
                    titleAr = "لا توجد مواضع ضعف",
                    titleFr = "Aucune fragilité détectée",
                    titleEn = "No Weak Verses",
                    detailsAr = "حفظك متقن ولم يُسجل أي تعثر في جلسات التسميع الأخيرة.",
                    detailsFr = "Votre mémorisation est solide sans échecs répétés lors des derniers tests.",
                    detailsEn = "Your memorization is solid with no repeated errors in recent tests.",
                    actionLabelAr = null,
                    actionLabelFr = null,
                    actionLabelEn = null,
                    actionType = null
                )
            }
        }
    }

    fun handleQueryReadQuran() {
        viewModelScope.launch {
            val lastSurah = prefsRepository.lastReadSurah.first() ?: 1
            val lastIndex = prefsRepository.lastReadAyahIndex.first()
            val ayahNum = lastIndex + 1
            _activeQueryResponse.value = RafiqQueryResponse(
                queryKey = "READ_QURAN",
                titleAr = "قراءة القرآن الكريم",
                titleFr = "Lecture du Coran",
                titleEn = "Read Quran",
                detailsAr = "موضع قراءتك الأخير: سورة $lastSurah، الآية $ayahNum. جاهز للمتابعة بتلاوة متأنية وخشوع.",
                detailsFr = "Dernière position de lecture : Sourate $lastSurah, Verset $ayahNum. Prêt à reprendre la récitation posée.",
                detailsEn = "Last reading position: Surah $lastSurah, Ayah $ayahNum. Ready to continue your recitation.",
                actionLabelAr = "فتح المصحف والقراءة ▶",
                actionLabelFr = "Ouvrir le Coran ▶",
                actionLabelEn = "Open Reader ▶",
                actionType = RafiqActionType.OPEN_READER,
                targetSurah = lastSurah,
                targetAyah = ayahNum
            )
        }
    }

    fun handleQueryReviewQueue() {
        viewModelScope.launch {
            val due = memorizationDao.getDueForReview(System.currentTimeMillis()).first()
            val weak = memorizationDao.getWeakVerses()
            val queue = (due + weak).distinctBy { "${it.surahNumber}:${it.ayahNumber}" }
            if (queue.isNotEmpty()) {
                _sessionQueue.value = queue
                _currentQueueIndex.value = 0
                val first = queue.first()
                selectAyah(first.surahNumber, first.ayahNumber)
                _activeQueryResponse.value = RafiqQueryResponse(
                    queryKey = "REVIEW_QUEUE",
                    titleAr = "بدء قائمة المراجعة",
                    titleFr = "File de révision lancée",
                    titleEn = "Review Queue Ready",
                    detailsAr = "تم تجهيز قائمة تضم ${queue.size} آية للمراجعة الموجهة بدءاً بسورة ${first.surahNumber} الآية ${first.ayahNumber}.",
                    detailsFr = "File préparée avec ${queue.size} verset(s) à réviser, en commençant par la sourate ${first.surahNumber}, verset ${first.ayahNumber}.",
                    detailsEn = "Prepared queue with ${queue.size} verses to review starting from Surah ${first.surahNumber}, Ayah ${first.ayahNumber}.",
                    actionLabelAr = "ابدأ المراجعة الآن ▶",
                    actionLabelFr = "Démarrer la révision ▶",
                    actionLabelEn = "Start Review ▶",
                    actionType = RafiqActionType.START_TRAINING,
                    targetSurah = first.surahNumber,
                    targetAyah = first.ayahNumber
                )
            } else {
                _activeQueryResponse.value = RafiqQueryResponse(
                    queryKey = "REVIEW_QUEUE",
                    titleAr = "قائمة المراجعة فارغة",
                    titleFr = "File de révision vide",
                    titleEn = "Queue Empty",
                    detailsAr = "لا توجد آيات تتطلب مراجعة فورية اليوم.",
                    detailsFr = "Aucun verset ne nécessite de révision immédiate aujourd'hui.",
                    detailsEn = "No verses require immediate review today.",
                    actionLabelAr = null,
                    actionLabelFr = null,
                    actionLabelEn = null,
                    actionType = null
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
        audioPlayer.release()
        sampleCollectorJob?.cancel()
    }
}
