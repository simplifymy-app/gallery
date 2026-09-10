package app.simplifymy.gallery.core.settings

/** Language names stay in their own language, so they are never translated. */
enum class Language(val tag: String, val label: String) {
    System("", "System"),
    English("en", "English"),
    Ukrainian("uk", "Українська"),
}
