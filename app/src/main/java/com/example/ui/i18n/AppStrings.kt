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
    val navSettings: String

    // Surah List Screen
    val surahSearchPlaceholder: String
    val surahVersesCount: String
    val revelationMeccan: String
    val revelationMedinan: String
    val noSurahFound: String
    val loadingSurahs: String
    val bookmarkSearchPlaceholder: String
    val fiSabilAllahTitle: String
    val fiSabilAllahSubtitle: String
    val tabSurahsFormat: String
    val tabBookmarksFormat: String
    val bookmarkAyahHeaderFormat: String
    val removeBookmark: String
    val noBookmarksFound: String

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
}

object ArabicAppStrings : AppStrings {
    override val appTitle = "التجويد والترتيل"
    override val appSubtitle = "تدريب على التلاوة القرآنية"
    override val quranHeaderArabic = "القرآن الكريم"
    override val backButton = "رجوع"
    override val cancel = "إلغاء"
    override val confirm = "تأكيد"
    override val save = "حفظ"
    override val validate = "اعتماد"

    override val navSurahs = "السور"
    override val navLibrary = "المكتبة"
    override val navSettings = "الإعدادات"

    override val surahSearchPlaceholder = "ابحث عن سورة (بالاسم أو الرقم)..."
    override val surahVersesCount = "%d آيات"
    override val revelationMeccan = "مكية"
    override val revelationMedinan = "مدنية"
    override val noSurahFound = "لم يتم العثور على سورة لـ \"%s\""
    override val loadingSurahs = "جاري تحميل السور..."
    override val bookmarkSearchPlaceholder = "ابحث في المفضلة (باسم السورة أو رقمها)..."
    override val fiSabilAllahTitle = "في سبيل الله"
    override val fiSabilAllahSubtitle = "🤲 هذا العمل لوجه الله تعالى — في سبيل الله"
    override val tabSurahsFormat = "السور (%d)"
    override val tabBookmarksFormat = "المفضلة (%d)"
    override val bookmarkAyahHeaderFormat = "📌 سورة %s — الآية %d"
    override val removeBookmark = "حذف من المفضلة"
    override val noBookmarksFound = "لا توجد آيات مضافة للمفضلة حالياً"

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
    override val navSettings = "Réglages"

    override val surahSearchPlaceholder = "Rechercher une sourate (nom ou numéro)..."
    override val surahVersesCount = "%d versets"
    override val revelationMeccan = "Mecquoise"
    override val revelationMedinan = "Médinoise"
    override val noSurahFound = "Aucune sourate trouvée pour \"%s\""
    override val loadingSurahs = "Chargement des sourates..."
    override val bookmarkSearchPlaceholder = "Rechercher dans les favoris (nom ou numéro)..."
    override val fiSabilAllahTitle = "Pour l'amour d'Allah (Fi Sabil Allah)"
    override val fiSabilAllahSubtitle = "🤲 Œuvre dédiée en vue d'Allah — Fi Sabil Allah"
    override val tabSurahsFormat = "Sourates (%d)"
    override val tabBookmarksFormat = "Favoris (%d)"
    override val bookmarkAyahHeaderFormat = "📌 %s — Verset %d"
    override val removeBookmark = "Supprimer des favoris"
    override val noBookmarksFound = "Aucun verset mis en favori"

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
}

fun getAppStrings(language: AppLanguage): AppStrings {
    return when (language) {
        AppLanguage.ARABIC -> ArabicAppStrings
        AppLanguage.FRENCH -> FrenchAppStrings
    }
}
