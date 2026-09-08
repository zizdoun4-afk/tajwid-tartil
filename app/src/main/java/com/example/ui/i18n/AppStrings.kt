package com.example.ui.i18n

interface AppStrings {
    // General / App Header
    val appTitle: String
    val appSubtitle: String
    val quranHeaderArabic: String
    val backButton: String
    val cancel: String
    val confirm: String
    val save: String
    val validate: String

    // Bottom Navigation
    val navSurahs: String
    val navLibrary: String
    val navMemorization: String
    val navSettings: String

    // Memorization & Hifz Features
    val memorizationTitle: String
    val memorizationSubtitle: String
    val memorizationHeaderArabic: String
    val addToRevisionTitle: String
    val addToRevisionMessage: String
    val addToRevisionYes: String
    val addToRevisionLater: String
    val statusNew: String
    val statusLearning: String
    val statusReview: String
    val statusMemorized: String
    val hifzTodayReviews: String
    val hifzMySurahs: String
    val hifzReviewList: String
    val hifzWeeklyStats: String
    val hifzStartTraining: String
    val hifzNoDueReviews: String
    val hifzNoSurahsInProgress: String
    val hifzProgressFormat: String
    val hifzVersesReviewed: String
    val hifzVersesMemorized: String
    val hifzPracticeTime: String
    val trainingTitleFormat: String
    val trainingStep1Title: String
    val trainingStep1Desc: String
    val trainingStep2Title: String
    val trainingStep2Desc: String
    val trainingStep3Title: String
    val trainingStep3Desc: String
    val trainingStep4Title: String
    val trainingStep4Desc: String
    val trainingStep5Title: String
    val trainingStep5Desc: String
    val revealTextButton: String
    val hideTextButton: String
    val markMemorizedSuccess: String
    val finishTrainingButton: String
    val nextStepButton: String
    val prevStepButton: String
    val testSelfHelp: String
    val listen3xButton: String
    val listenAccompaniedButton: String
    val recordSoloButton: String
    val stopRecordingDurationFormat: String
    val compareModelLabel: String
    val compareMyVoiceLabel: String
    val blindTestSuccessButton: String
    val blindTestRetryButton: String
    val blindTestPrompt: String
    val trainingExitConfirmTitle: String
    val trainingExitConfirmMessage: String
    val quitButton: String
    val continueTrainingButton: String
    val hifzVersesLearning: String
    val recordRequiredToProceed: String

    // Surah List Screen
    val surahSearchPlaceholder: String
    val surahVersesCount: String
    val revelationMeccan: String
    val revelationMedinan: String
    val noSurahFound: String
    val loadingSurahs: String

    // Ayah Reader Screen
    val loadingAyahs: String
    val ayahHeaderFormat: String // e.g. "%s — Verset %d/%d" or "سورة %s — الآية %d/%d"
    val styleTartilLabel: String
    val styleTajwidLabel: String
    val referenceAudioTitleFormat: String // e.g. "Référence (%s)"
    val referencePlaying: String
    val referenceSubtitle: String
    val listenReferenceContentDescription: String
    val recordingInProgress: String
    val stopRecording: String
    val recordMyVoice: String
    val previous: String
    val next: String

    // Post Recording Dialog
    val postRecordingTitleFormat: String
    val postRecordingMessage: String
    val playMyRecording: String
    val restart: String

    // Metadata Dialog
    val saveRecordingTitle: String
    val reciterNameLabel: String
    val surahLabelFormat: String
    val ayahLabelFormat: String
    val dateLabelFormat: String
    val durationLabelFormat: String

    // Library Screen
    val libraryTitle: String
    val librarySubtitle: String
    val libraryHeaderArabic: String
    val filterPlaceholder: String
    val emptyLibraryMessage: String
    val noFilterResults: String
    val headerTodayFormat: String
    val headerYesterdayFormat: String
    val headerDateFormat: String
    val actionPlay: String
    val actionRename: String
    val actionShare: String
    val actionCopy: String
    val actionDelete: String
    val renameDialogTitle: String
    val customNoteLabel: String
    val deleteConfirmTitle: String
    val deleteConfirmMessageFormat: String

    // Settings Screen
    val settingsTitle: String
    val settingsSubtitle: String
    val settingsHeaderArabic: String
    val languageSectionTitle: String
    val profileSectionTitle: String
    val defaultReciterNameLabel: String
    val defaultStyleSectionTitle: String
    val tartilDesc: String
    val tajwidDesc: String
    val themeSectionTitle: String
    val storageSectionTitle: String
    val cacheSizeLabel: String
    val clearCacheButton: String

    // Messages
    val micPermissionRequired: String
    val cacheClearedSuccess: String
    val recordingSavedSuccess: String
    val recordingDeletedSuccess: String
    val fileCopiedSuccess: String
    val fileCopyError: String
    val recordRenamedSuccess: String

    // New Features: Bookmarks, Translation, Speed/Repeat, Statistics
    val bookmarkAdded: String
    val bookmarkRemoved: String
    val bookmarksTitle: String
    val noBookmarks: String
    val showTranslationLabel: String
    val showTransliterationLabel: String
    val speedLabel: String
    val repeatLabel: String
    val repeatModeOff: String
    val repeatMode1x: String
    val repeatMode3x: String
    val repeatModeLoop: String
    val statsTitle: String
    val totalRecordingsLabel: String
    val totalDurationLabel: String
    val practicedSurahsLabel: String
    val practiceStreakLabel: String

    // Studio Recording Mushaf-Lite Mode
    val studioHeaderTitle: String
    val referenceReciterTitle: String
    val myVoiceStudioTitle: String
    val compareVoicesLabel: String
    val reRecordButton: String

    // Workflow & Take / Session Mode Controls
    val modeAyahLabel: String
    val modeSessionLabel: String
    val myRecordingBadge: String
    val recordingModeLabel: String
    val tapModeOption: String
    val holdModeOption: String
    val recordingVerseFormat: String
    val discardTakeButton: String
    val finishAndNextButton: String
    val listenMyRecitationFormat: String
    val recordThisVerse: String
    val reRecordThisVerse: String
    val tapToStartHelp: String
    val holdToRecord: String
    val holdToReRecord: String
    val holdToRecordHelp: String
    val sessionInProgressFormat: String
    val sessionPaused: String
    val markNextVerseFormat: String
    val pauseButton: String
    val resumeButton: String
    val endSessionButton: String
    val startContinuousSessionFormat: String
    val saveSessionDialogTitle: String
    val saveSessionDialogMessage: String
    val surahLabelTextFormat: String
    val verseRangeLabelFormat: String
    val totalDurationTextFormat: String
    val timeMarkersCountFormat: String
    val recordingActiveTitle: String
    val recordingActiveExitMessage: String
    val discardAndExit: String
    val continueRecording: String

    // Batch delete & Clear field
    val selectAll: String
    val deselectAll: String
    val deleteAllRecordings: String
    val deleteSelectedRecordings: String
    val confirmDeleteAllTitle: String
    val confirmDeleteAllMessage: String
    val clearFieldTooltip: String
}

object ArabicAppStrings : AppStrings {
    override val appTitle = "تجويد وترتيل"
    override val appSubtitle = "تدريب على التلاوة القرآنية"
    override val quranHeaderArabic = "القرآن الكريم"
    override val backButton = "رجوع"
    override val cancel = "إلغاء"
    override val confirm = "تأكيد"
    override val save = "حفظ"
    override val validate = "اعتماد"

    override val navSurahs = "السور"
    override val navLibrary = "المكتبة"
    override val navMemorization = "حفظي"
    override val navSettings = "الإعدادات"

    override val memorizationTitle = "حفظي"
    override val memorizationSubtitle = "برنامج الحفظ والمراجعة التراكمية"
    override val memorizationHeaderArabic = "حفظي"
    override val addToRevisionTitle = "إضافة إلى برنامج المراجعة؟"
    override val addToRevisionMessage = "هل ترغب في إضافة هذا المعلم القرآني إلى برنامج المراجعة والحفظ الدوري؟"
    override val addToRevisionYes = "نعم، إضافة"
    override val addToRevisionLater = "لاحقاً"
    override val statusNew = "جديد"
    override val statusLearning = "قيد الحفظ"
    override val statusReview = "مراجعة"
    override val statusMemorized = "محفوظ"
    override val hifzTodayReviews = "مراجعة اليوم"
    override val hifzMySurahs = "تقدم السور"
    override val hifzReviewList = "قائمة المراجعة"
    override val hifzWeeklyStats = "إحصائيات الأسبوع"
    override val hifzStartTraining = "بدء التدريب"
    override val hifzNoDueReviews = "لا توجد آيات مستحقة للمراجعة اليوم 🎉"
    override val hifzNoSurahsInProgress = "لم تبدأ بعد في حفظ أي سورة. اتلُ وسجل لحفظ آياتك!"
    override val hifzProgressFormat = "%d / %d آية (%d%%)"
    override val hifzVersesReviewed = "آيات تمت مراجعتها"
    override val hifzVersesMemorized = "آيات تم حفظها"
    override val hifzPracticeTime = "وقت التدريب الصوتي"
    override val trainingTitleFormat = "جلسة حفظ: سورة %s (آية %d)"
    override val trainingStep1Title = "١. استماع للنموذج (٣ مرات)"
    override val trainingStep1Desc = "استمع بتركيز لتلاوة القارئ المرجعي وأتقن مخارج الحروف"
    override val trainingStep2Title = "٢. قراءة مصاحبة"
    override val trainingStep2Desc = "اقرأ بصوت مسموع مع الاستماع للتلاوة في نفس الوقت"
    override val trainingStep3Title = "٣. تسجيل التلاوة"
    override val trainingStep3Desc = "سجّل قراءتك بمفردك بدون الاستماع للمرجع"
    override val trainingStep4Title = "٤. الاستماع لتسجيلك"
    override val trainingStep4Desc = "استمع إلى تلاوتك المسجلة وقارنها بأحكام التجويد"
    override val trainingStep5Title = "٥. اختبار الحفظ غيباً"
    override val trainingStep5Desc = "اختبر نفسك دون النظر إلى المصحف الشريف"
    override val revealTextButton = "إظهار النص للتأكد"
    override val hideTextButton = "إخفاء النص"
    override val markMemorizedSuccess = "أحسنت! تم تحديث حالة الحفظ بنجاح ⭐"
    override val finishTrainingButton = "إنهاء الجلسة وحفظ التقدم"
    override val nextStepButton = "الخطوة التالية ▶"
    override val prevStepButton = "◀ الخطوة السابقة"
    override val testSelfHelp = "ردّد الآية من ذاكرتك ثم اضغط على إظهار النص للمقارنة والتثبت"
    override val listen3xButton = "الاستماع ٣ مرات متتالية 🔁"
    override val listenAccompaniedButton = "بدء القراءة المصاحبة 🎧"
    override val recordSoloButton = "تسجيل تلاوتي بمفردي 🎙"
    override val stopRecordingDurationFormat = "إيقاف (%d ثانية) ⏹"
    override val compareModelLabel = "النموذج"
    override val compareMyVoiceLabel = "تلاوتي"
    override val blindTestSuccessButton = "✓ أتقنت الحفظ غيباً"
    override val blindTestRetryButton = "↻ إعادة المحاولة"
    override val blindTestPrompt = "اتلُ الآية من حفظك غيباً، ثم أظهر النص القرآني للمقارنة والتحقق:"
    override val trainingExitConfirmTitle = "مغادرة جلسة التدريب؟"
    override val trainingExitConfirmMessage = "إذا غادرت الآن، فسيتم إلغاء الجلسة الحالية والتسجيل الصوتي المؤقت."
    override val quitButton = "مغادرة"
    override val continueTrainingButton = "متابعة التدريب"
    override val hifzVersesLearning = "قيد الحفظ"
    override val recordRequiredToProceed = "يرجى تسجيل تلاوتك أولاً للمتابعة إلى الخطوة التالية."

    override val surahSearchPlaceholder = "ابحث عن سورة (بالاسم أو الرقم)..."
    override val surahVersesCount = "%d آيات"
    override val revelationMeccan = "مكية"
    override val revelationMedinan = "مدنية"
    override val noSurahFound = "لم يتم العثور على سورة لـ \"%s\""
    override val loadingSurahs = "جاري تحميل السور..."

    override val loadingAyahs = "جاري تحميل الآيات..."
    override val ayahHeaderFormat = "سورة %s — الآية %d/%d"
    override val styleTartilLabel = "ترتيل ▶"
    override val styleTajwidLabel = "تجويد ▶"
    override val referenceAudioTitleFormat = "النموذج الصوتي (%s)"
    override val referencePlaying = "جاري التشغيل..."
    override val referenceSubtitle = "استمع إلى نموذج التلاوة الصحيحة"
    override val listenReferenceContentDescription = "استمع للنموذج"
    override val recordingInProgress = "🎙 جاري التسجيل الصوت..."
    override val stopRecording = "⏹ إيقاف التسجيل"
    override val recordMyVoice = "🎙 تسجيل بصوتي"
    override val previous = "السابق"
    override val next = "التالي"

    override val postRecordingTitleFormat = "تم الانتهاء من التسجيل (%s)"
    override val postRecordingMessage = "استمع إلى تلاوتك قبل حفظها:"
    override val playMyRecording = "▶ الاستماع إلى تسجيلي"
    override val restart = "إعادة"

    override val saveRecordingTitle = "حفظ التسجيل الصوتي"
    override val reciterNameLabel = "اسم القارئ"
    override val surahLabelFormat = "السورة: %s"
    override val ayahLabelFormat = "الآية: %d"
    override val dateLabelFormat = "التاريخ: %s"
    override val durationLabelFormat = "المدة: %s"

    override val libraryTitle = "التسجيلات"
    override val librarySubtitle = "تلاواتي المحفوظة"
    override val libraryHeaderArabic = "المكتبة"
    override val filterPlaceholder = "التصفية حسب القارئ أو السورة..."
    override val emptyLibraryMessage = "لا توجد تسجيلات حتى الآن.\nاتلُ آية وسجّل صوتك!"
    override val noFilterResults = "لم يتم العثور على تسجيل لـ \"%s\""
    override val headerTodayFormat = "📅 اليوم — %s"
    override val headerYesterdayFormat = "📅 الأمس — %s"
    override val headerDateFormat = "📅 %s"
    override val actionPlay = "▶ استماع"
    override val actionRename = "✏️ تعديل الاسم"
    override val actionShare = "📤 مشاركة"
    override val actionCopy = "📋 نسخ إلى التنزيلات"
    override val actionDelete = "🗑 حذف"
    override val renameDialogTitle = "تعديل اسم التسجيل"
    override val customNoteLabel = "ملاحظة مخصصة (اختياري)"
    override val deleteConfirmTitle = "حذف التسجيل؟"
    override val deleteConfirmMessageFormat = "هل أنت تأكد من حذف تلاوة سورة %s (الآية %d)؟ هذه العملية لا يمكن التراجع عنها."

    override val settingsTitle = "الإعدادات"
    override val settingsSubtitle = "التخصيص والتفضيلات"
    override val settingsHeaderArabic = "الإعدادات"
    override val languageSectionTitle = "لغة التطبيق / App Language"
    override val profileSectionTitle = "الملف الشخصي والقارئ الافتراضي"
    override val defaultReciterNameLabel = "اسم القارئ الافتراضي"
    override val defaultStyleSectionTitle = "أسلوب التلاوة الافتراضي"
    override val tartilDesc = "بطيء ومجود ومترسل (مشاري العفاسي)"
    override val tajwidDesc = "تلاوة مجودة ومرتلة (عبد الباسط عبد الصمد)"
    override val themeSectionTitle = "المظهر البصري للتطبيق"
    override val storageSectionTitle = "التخزين والذاكرة المؤقتة"
    override val cacheSizeLabel = "حجم الذاكرة المؤقتة:"
    override val clearCacheButton = "مسح الذاكرة المؤقتة"

    override val micPermissionRequired = "يتطلب الإذن للوصول إلى الميكروفون لتسجيل صوتك"
    override val cacheClearedSuccess = "تم مسح الذاكرة المؤقتة بنجاح"
    override val recordingSavedSuccess = "تم حفظ التسجيل بنجاح!"
    override val recordingDeletedSuccess = "تم حذف التسجيل"
    override val fileCopiedSuccess = "تم نسخ الملف إلى مجلد التنزيلات!"
    override val fileCopyError = "حدث خطأ أثناء نسخ الملف"
    override val recordRenamedSuccess = "تم تعديل التسجيل بنجاح"

    override val bookmarkAdded = "تمت إضافة الآية إلى المرجعية ⭐"
    override val bookmarkRemoved = "تمت إزالة الآية من المرجعية"
    override val bookmarksTitle = "العلامات المرجعية"
    override val noBookmarks = "لا توجد آيات مرجعية محفوظة"
    override val showTranslationLabel = "الترجمة والمعاني"
    override val showTransliterationLabel = "الكتابة الصوتية (Phonétique)"
    override val speedLabel = "السرعة"
    override val repeatLabel = "التكرار"
    override val repeatModeOff = "بدون تكرار"
    override val repeatMode1x = "مرة واحدة"
    override val repeatMode3x = "3 مرات"
    override val repeatModeLoop = "تكرار مستمر 🔁"
    override val statsTitle = "إحصائيات الإنجاز والترتيل"
    override val totalRecordingsLabel = "التسجيلات"
    override val totalDurationLabel = "مدة التسجيل"
    override val practicedSurahsLabel = "السور الممارسة"
    override val practiceStreakLabel = "أيام التدريب"

    override val studioHeaderTitle = "استوديو تسجيل الآيات"
    override val referenceReciterTitle = "القارئ المعلم (استماع)"
    override val myVoiceStudioTitle = "تسجيلي الصوتي (مقارنة)"
    override val compareVoicesLabel = "استماع ومقارنة"
    override val reRecordButton = "إعادة التسجيل"

    override val modeAyahLabel = "نمط آية (مقطع فردي)"
    override val modeSessionLabel = "نمط جلسة (مستمر)"
    override val myRecordingBadge = "تسجيلي ▶"
    override val recordingModeLabel = "طريقة التسجيل:"
    override val tapModeOption = "👆 ضغطة واحدة (Tap)"
    override val holdModeOption = "🖐 ضغط مستمر (Hold)"
    override val recordingVerseFormat = "🎙 تسجيل الآية %d"
    override val discardTakeButton = "إلغاء"
    override val finishAndNextButton = "إنهاء وانتقال للآية التالية"
    override val listenMyRecitationFormat = "استماع لتلاوتي (الآية %d)"
    override val recordThisVerse = "تسجيل هذه الآية"
    override val reRecordThisVerse = "إعادة تسجيل هذه الآية"
    override val tapToStartHelp = "اضغط للبدء • اضغط مرة أخرى للحفظ"
    override val holdToRecord = "اضغط مع الاستمرار للتسجيل"
    override val holdToReRecord = "اضغط مع الاستمرار لإعادة التسجيل"
    override val holdToRecordHelp = "استمر بالضغط أثناء التلاوة • اترك الزر للحفظ"
    override val sessionInProgressFormat = "جلسة قيد التسجيل (الآية %d)"
    override val sessionPaused = "الجلسة موقوفة مؤقتاً"
    override val markNextVerseFormat = "الانتقال للآية %d (علامة زمنية)"
    override val pauseButton = "إيقاف مؤقت"
    override val resumeButton = "استئناف"
    override val endSessionButton = "إنهاء الجلسة"
    override val startContinuousSessionFormat = "بدء جلسة مستمرة (الآية %d)"
    override val saveSessionDialogTitle = "حفظ الجلسة المستمرة؟"
    override val saveSessionDialogMessage = "لقد قمت بتسجيل جلسة تلاوة مستمرة:"
    override val surahLabelTextFormat = "السورة: %s"
    override val verseRangeLabelFormat = "نطاق الآيات: الآية %d ← الآية %d"
    override val totalDurationTextFormat = "المدة الإجمالية: %s"
    override val timeMarkersCountFormat = "العلامات الزمنية: %d آية"
    override val recordingActiveTitle = "التسجيل قيد التشغيل"
    override val recordingActiveExitMessage = "هناك تسجيل نشط حالياً. هل تريد حقاً إلغاءه والخروج؟"
    override val discardAndExit = "إلغاء وخروج"
    override val continueRecording = "متابعة التسجيل"

    override val selectAll = "تحديد الكل"
    override val deselectAll = "إلغاء تحديد الكل"
    override val deleteAllRecordings = "حذف جميع التسجيلات"
    override val deleteSelectedRecordings = "حذف المحدد (%d)"
    override val confirmDeleteAllTitle = "حذف جميع التسجيلات؟"
    override val confirmDeleteAllMessage = "هل أنت تأكد من إمكانية حذف جميع التسجيلات نهائياً من المكتبة؟ هذه العملية لا يمكن التراجع عنها."
    override val clearFieldTooltip = "مسح الحقل"
}

object FrenchAppStrings : AppStrings {
    override val appTitle = "Tajwid & Tartil"
    override val appSubtitle = "Entraînement à la récitation coranique"
    override val quranHeaderArabic = "القرآن الكريم"
    override val backButton = "Retour"
    override val cancel = "Annuler"
    override val confirm = "Confirmer"
    override val save = "Sauvegarder"
    override val validate = "Valider"

    override val navSurahs = "Sourates"
    override val navLibrary = "Bibliothèque"
    override val navMemorization = "Mémorisation"
    override val navSettings = "Réglages"

    override val memorizationTitle = "Mémorisation (Hifz)"
    override val memorizationSubtitle = "Programme de révision et mémorisation continue"
    override val memorizationHeaderArabic = "حفظي"
    override val addToRevisionTitle = "Ajouter à ma révision ?"
    override val addToRevisionMessage = "Voulez-vous intégrer ce verset à votre programme d'apprentissage et de répétition espacée ?"
    override val addToRevisionYes = "Oui, ajouter"
    override val addToRevisionLater = "Plus tard"
    override val statusNew = "Nouveau"
    override val statusLearning = "En apprentissage"
    override val statusReview = "À réviser"
    override val statusMemorized = "Mémorisé"
    override val hifzTodayReviews = "À réviser aujourd'hui"
    override val hifzMySurahs = "Progression par sourate"
    override val hifzReviewList = "Versets en cours"
    override val hifzWeeklyStats = "Statistiques de la semaine"
    override val hifzStartTraining = "S'entraîner"
    override val hifzNoDueReviews = "Aucun verset à réviser aujourd'hui. Félicitations ! 🎉"
    override val hifzNoSurahsInProgress = "Aucune sourate en cours de mémorisation. Enregistrez des versets pour débuter !"
    override val hifzProgressFormat = "%d / %d versets (%d%%)"
    override val hifzVersesReviewed = "Versets révisés"
    override val hifzVersesMemorized = "Versets mémorisés"
    override val hifzPracticeTime = "Temps de pratique"
    override val trainingTitleFormat = "Session Hifz : Sourate %s (v. %d)"
    override val trainingStep1Title = "1. Écoute du modèle (×3)"
    override val trainingStep1Desc = "Écoutez attentivement le récitateur de référence et imprégnez-vous des règles"
    override val trainingStep2Title = "2. Lecture accompagnée"
    override val trainingStep2Desc = "Lisez le texte arabe à voix haute en synchronisation avec l'audio"
    override val trainingStep3Title = "3. Enregistrement seul"
    override val trainingStep3Desc = "Enregistrez votre propre voix sans le modèle de référence"
    override val trainingStep4Title = "4. Réécoute et analyse"
    override val trainingStep4Desc = "Réécoutez votre enregistrement et comparez avec la récitation modèle"
    override val trainingStep5Title = "5. Test sans le texte (Mémoire)"
    override val trainingStep5Desc = "Récitez de mémoire sans regarder le texte coranique"
    override val revealTextButton = "Afficher le texte pour vérifier"
    override val hideTextButton = "Masquer le texte"
    override val markMemorizedSuccess = "Bravo ! Statut de mémorisation mis à jour ⭐"
    override val finishTrainingButton = "Terminer et valider la session"
    override val nextStepButton = "Étape suivante ▶"
    override val prevStepButton = "◀ Étape précédente"
    override val testSelfHelp = "Récitez le verset de tête, puis cliquez sur 'Afficher le texte' pour vérifier votre exactitude."
    override val listen3xButton = "Écouter 3 fois de suite 🔁"
    override val listenAccompaniedButton = "Lancer la lecture accompagnée 🎧"
    override val recordSoloButton = "Enregistrer ma récitation seule 🎙"
    override val stopRecordingDurationFormat = "Arrêter (%ds) ⏹"
    override val compareModelLabel = "Modèle"
    override val compareMyVoiceLabel = "Ma voix"
    override val blindTestSuccessButton = "✓ J'ai réussi (Mémorisé)"
    override val blindTestRetryButton = "↻ Réessayer"
    override val blindTestPrompt = "Récitez le verset de mémoire, puis révélez le texte coranique pour vérifier votre récitation :"
    override val trainingExitConfirmTitle = "Quitter l'entraînement ?"
    override val trainingExitConfirmMessage = "Si vous quittez maintenant, la session en cours et l'enregistrement temporaire seront annulés."
    override val quitButton = "Quitter"
    override val continueTrainingButton = "Continuer"
    override val hifzVersesLearning = "En apprentissage"
    override val recordRequiredToProceed = "Veuillez enregistrer votre récitation avant de passer à l'étape suivante."

    override val surahSearchPlaceholder = "Rechercher une sourate (nom ou numéro)..."
    override val surahVersesCount = "%d versets"
    override val revelationMeccan = "Mecquoise"
    override val revelationMedinan = "Médinoise"
    override val noSurahFound = "Aucune sourate trouvée pour \"%s\""
    override val loadingSurahs = "Chargement des sourates..."

    override val loadingAyahs = "Chargement des versets..."
    override val ayahHeaderFormat = "Sourate %s — Verset %d/%d"
    override val styleTartilLabel = "Tartil ▶"
    override val styleTajwidLabel = "Tajwid ▶"
    override val referenceAudioTitleFormat = "Référence (%s)"
    override val referencePlaying = "Lecture en cours..."
    override val referenceSubtitle = "Écouter le modèle de récitation"
    override val listenReferenceContentDescription = "Écouter réciteur référent"
    override val recordingInProgress = "🎙 Enregistrement en cours..."
    override val stopRecording = "⏹ Arrêter l'enregistrement"
    override val recordMyVoice = "🎙 Enregistrer ma voix"
    override val previous = "Précédent"
    override val next = "Suivant"

    override val postRecordingTitleFormat = "Enregistrement terminé (%s)"
    override val postRecordingMessage = "Réécoutez votre récitation avant de la sauvegarder :"
    override val playMyRecording = "▶ Écouter mon enregistrement"
    override val restart = "Recommencer"

    override val saveRecordingTitle = "Sauvegarder l'enregistrement"
    override val reciterNameLabel = "Nom du réciteur"
    override val surahLabelFormat = "Sourate : %s"
    override val ayahLabelFormat = "Verset : %d"
    override val dateLabelFormat = "Date : %s"
    override val durationLabelFormat = "Durée : %s"

    override val libraryTitle = "Enregistrements"
    override val librarySubtitle = "Mes récitations sauvegardées"
    override val libraryHeaderArabic = "المكتبة"
    override val filterPlaceholder = "Filtrer par réciteur ou sourate..."
    override val emptyLibraryMessage = "Aucun enregistrement pour le moment.\nRécitez un verset et sauvegardez votre voix !"
    override val noFilterResults = "Aucun enregistrement trouvé pour \"%s\""
    override val headerTodayFormat = "📅 Aujourd'hui — %s"
    override val headerYesterdayFormat = "📅 Hier — %s"
    override val headerDateFormat = "📅 %s"
    override val actionPlay = "▶ Écouter"
    override val actionRename = "✏️ Renommer"
    override val actionShare = "📤 Partager"
    override val actionCopy = "📋 Copier vers Téléchargements"
    override val actionDelete = "🗑 Supprimer"
    override val renameDialogTitle = "Renommer l'enregistrement"
    override val customNoteLabel = "Note / Label personnalisé (optionnel)"
    override val deleteConfirmTitle = "Supprimer l'enregistrement ?"
    override val deleteConfirmMessageFormat = "Voulez-vous vraiment supprimer la récitation de %s (Verset %d) ? Cette action est irréversible."

    override val settingsTitle = "Réglages"
    override val settingsSubtitle = "Personnalisation & Préférences"
    override val settingsHeaderArabic = "الإعدادات"
    override val languageSectionTitle = "Langue de l'application / لغة التطبيق"
    override val profileSectionTitle = "Profil & Réciteur par défaut"
    override val defaultReciterNameLabel = "Nom du réciteur par défaut"
    override val defaultStyleSectionTitle = "Style de récitation par défaut"
    override val tartilDesc = "Lent, mesuré & mélodieux (Mishary Alafasy)"
    override val tajwidDesc = "Récitation rythmée & solennelle (Abdul Basit)"
    override val themeSectionTitle = "Thème visuel de l'application"
    override val storageSectionTitle = "Stockage & Cache Audio"
    override val cacheSizeLabel = "Taille du cache audio :"
    override val clearCacheButton = "Vider le cache"

    override val micPermissionRequired = "Permission microphone requise pour vous enregistrer"
    override val cacheClearedSuccess = "Cache audio vidé avec succès"
    override val recordingSavedSuccess = "Enregistrement sauvegardé avec succès !"
    override val recordingDeletedSuccess = "Enregistrement supprimé"
    override val fileCopiedSuccess = "Fichier copié dans le dossier Téléchargements !"
    override val fileCopyError = "Erreur lors de la copie du fichier"
    override val recordRenamedSuccess = "Enregistrement renommé"

    override val bookmarkAdded = "Verset ajouté aux marque-pages ⭐"
    override val bookmarkRemoved = "Verset retiré des marque-pages"
    override val bookmarksTitle = "Marque-pages & Versets favoris"
    override val noBookmarks = "Aucun verset favori sauvegardé"
    override val showTranslationLabel = "Traduction & Signification"
    override val showTransliterationLabel = "Phonétique / Transliteration"
    override val speedLabel = "Vitesse"
    override val repeatLabel = "Répétition"
    override val repeatModeOff = "Sans répétition"
    override val repeatMode1x = "1 fois"
    override val repeatMode3x = "3 fois"
    override val repeatModeLoop = "En boucle 🔁"
    override val statsTitle = "Statistiques & Progression"
    override val totalRecordingsLabel = "Enregistrements"
    override val totalDurationLabel = "Durée totale"
    override val practicedSurahsLabel = "Sourates pratiquées"
    override val practiceStreakLabel = "Jours de pratique"

    override val studioHeaderTitle = "Studio de Récitation Verse-by-Verse"
    override val referenceReciterTitle = "Récitateur de Référence (Écouter)"
    override val myVoiceStudioTitle = "Mon Enregistrement (Comparer)"
    override val compareVoicesLabel = "Écouter & Comparer"
    override val reRecordButton = "Réenregistrer"

    override val modeAyahLabel = "Mode Ayah (Piste unique)"
    override val modeSessionLabel = "Mode Session (Continu)"
    override val myRecordingBadge = "Mon Enregistrement ▶"
    override val recordingModeLabel = "Mode d'enregistrement :"
    override val tapModeOption = "👆 Tap unique"
    override val holdModeOption = "🖐 Maintien (Hold)"
    override val recordingVerseFormat = "🎙 Enregistrement du verset %d"
    override val discardTakeButton = "Abandonner"
    override val finishAndNextButton = "Terminer & Suivant"
    override val listenMyRecitationFormat = "Écouter ma récitation (Verset %d)"
    override val recordThisVerse = "Enregistrer ce verset"
    override val reRecordThisVerse = "Ré-enregistrer ce verset"
    override val tapToStartHelp = "Touchez pour démarrer • Touchez à nouveau pour sauvegarder"
    override val holdToRecord = "Maintenir pour enregistrer"
    override val holdToReRecord = "Maintenir pour ré-enregistrer"
    override val holdToRecordHelp = "Maintenez le bouton pendant la récitation • Relâchez pour sauvegarder"
    override val sessionInProgressFormat = "Session en cours (Verset %d)"
    override val sessionPaused = "Session en pause"
    override val markNextVerseFormat = "Passer au Verset %d (Marquer)"
    override val pauseButton = "Pause"
    override val resumeButton = "Reprendre"
    override val endSessionButton = "Terminer Session"
    override val startContinuousSessionFormat = "Démarrer Session Continue (V. %d)"
    override val saveSessionDialogTitle = "Sauvegarder la session ?"
    override val saveSessionDialogMessage = "Vous avez enregistré une session continue :"
    override val surahLabelTextFormat = "Sourate : %s"
    override val verseRangeLabelFormat = "Plage de versets : Verset %d → Verset %d"
    override val totalDurationTextFormat = "Durée totale : %s"
    override val timeMarkersCountFormat = "Marqueurs temporels : %d verset(s)"
    override val recordingActiveTitle = "Enregistrement en cours"
    override val recordingActiveExitMessage = "Un enregistrement est actuellement actif. Voulez-vous vraiment l'abandonner et quitter ?"
    override val discardAndExit = "Abandonner & Quitter"
    override val continueRecording = "Continuer l'enregistrement"

    override val selectAll = "Tout sélectionner"
    override val deselectAll = "Tout désélectionner"
    override val deleteAllRecordings = "Tout supprimer"
    override val deleteSelectedRecordings = "Supprimer la sélection (%d)"
    override val confirmDeleteAllTitle = "Supprimer tous les enregistrements ?"
    override val confirmDeleteAllMessage = "Êtes-vous sûr de vouloir supprimer définitivement tous les enregistrements de la bibliothèque ? Cette action est irréversible."
    override val clearFieldTooltip = "Effacer le champ"
}

fun getAppStrings(language: AppLanguage): AppStrings {
    return when (language) {
        AppLanguage.ARABIC -> ArabicAppStrings
        AppLanguage.FRENCH -> FrenchAppStrings
    }
}
