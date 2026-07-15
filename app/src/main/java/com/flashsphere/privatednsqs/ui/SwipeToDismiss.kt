package com.flashsphere.privatednsqs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxState.Companion.Saver
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.flashsphere.privatednsqs.R

@Composable
fun rememberNoFlingSwipeToDismissBoxState(
    initialValue: SwipeToDismissBoxValue = SwipeToDismissBoxValue.Settled,
    confirmValueChange: (SwipeToDismissBoxValue) -> Boolean = { true },
    positionalThreshold: (totalDistance: Float) -> Float = SwipeToDismissBoxDefaults.positionalThreshold,
): SwipeToDismissBoxState {
    val density = Density(Float.POSITIVE_INFINITY)
    @Suppress("Deprecation")
    return rememberSaveable(
        saver =
            Saver(
                confirmValueChange = confirmValueChange,
                density = density,
                positionalThreshold = positionalThreshold
            )
    ) {
        SwipeToDismissBoxState(initialValue, density, confirmValueChange, positionalThreshold)
    }
}

@Composable
fun SwipeToDismissBackground(state: SwipeToDismissBoxState) {
    val horizontalArrangement = when (state.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> Arrangement.Start
        SwipeToDismissBoxValue.EndToStart -> Arrangement.End
        else -> null
    }

    if (horizontalArrangement == null) return

    Row(modifier = Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.errorContainer)
        .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement
    ) {
        Icon(
            painterResource(R.drawable.ic_delete),
            contentDescription = stringResource(R.string.delete)
        )
    }
}
