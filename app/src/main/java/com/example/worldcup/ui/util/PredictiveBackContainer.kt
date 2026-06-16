package com.example.worldcup.ui.util

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Wraps [content] with Android's Predictive Back gesture (API 34+).
 *
 * - On Android 14+: the screen shrinks and slides toward the swipe edge as the
 *   user drags, giving a live preview. Releasing past the threshold commits back;
 *   releasing early springs the screen back into place.
 * - On older versions: falls back silently to the standard back behaviour.
 *
 * The [onBack] lambda is called only when the user commits the gesture.
 */
@Composable
fun PredictiveBackContainer(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var scale         by remember { mutableFloatStateOf(1f) }
    var translationX  by remember { mutableFloatStateOf(0f) }
    var pivotFractionX by remember { mutableFloatStateOf(0.5f) }

    PredictiveBackHandler { progress ->
        try {
            progress.collect { event ->
                val p        = event.progress
                val fromLeft = event.swipeEdge == BackEventCompat.EDGE_LEFT
                scale          = lerp(1f, 0.90f, p)
                pivotFractionX = if (fromLeft) 1f else 0f
                translationX   = lerp(0f, if (fromLeft) -72f else 72f, p)
            }
            // User committed — navigate back
            onBack()
        } catch (e: CancellationException) {
            // User cancelled — spring everything back to resting state.
            // The outer coroutine launched by PredictiveBackHandler is still active,
            // so suspend calls here are safe.
            coroutineScope {
                val animScale = Animatable(scale)
                val animTrans = Animatable(translationX)
                launch { animScale.animateTo(1f, spring()) { scale = value } }
                animTrans.animateTo(0f, spring()) { translationX = value }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX  = scale
                scaleY  = scale
                this.translationX = translationX
                transformOrigin   = TransformOrigin(pivotFractionX, 0.5f)
            },
    ) {
        content()
    }
}
