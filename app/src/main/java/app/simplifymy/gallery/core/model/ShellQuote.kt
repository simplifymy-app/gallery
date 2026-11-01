package app.simplifymy.gallery.core.model

fun shellQuoted(paths: List<String>): String =
    paths.joinToString(" ") { "'" + it.replace("'", "'\\''") + "'" }
