package com.flashsphere.privatednsqs.ui

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

@Suppress("UNCHECKED_CAST")
fun <T> indexedValueSaver(): Saver<IndexedValue<T>?, Any> =
    listSaver(
        save = {
            if (it == null) {
                emptyList()
            } else {
                listOf(it.index, it.value)
            }
        },
        restore = {
            if (it.isNotEmpty()) {
                IndexedValue(
                    index = it[0] as Int,
                    value = it[1] as T
                )
            } else {
                null
            }
        }
    )
