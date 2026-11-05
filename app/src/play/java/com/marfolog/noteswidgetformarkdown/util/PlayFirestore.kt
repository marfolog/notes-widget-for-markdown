package com.marfolog.noteswidgetformarkdown.util

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings

/**
 * The one Firestore the Play build talks to. Its settings can only be set before the first use, so
 * every sender goes through here.
 *
 * Memory cache only: a send that failed must not sit on disk and go out on a later start, after the
 * user was told it failed and maybe sent it again.
 */
internal object PlayFirestore {
    val instance: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        }
    }
}
