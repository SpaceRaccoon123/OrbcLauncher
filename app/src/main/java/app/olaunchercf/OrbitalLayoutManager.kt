package app.olaunchercf

import android.content.Context
import android.os.Vibrator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.cos

class OrbitalLayoutManager(
    private val context: Context,
    var radiusFactor: Float = 0.35f,
    var isRightHanded: Boolean = true,
    var isOrbitalEnabled: Boolean = true,
    var isHapticsEnabled: Boolean = true
) : LinearLayoutManager(context, VERTICAL, false) {

    private var lastCenteredPosition = -1
    private val vibrator: Vibrator? by lazy {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        val scrolled = super.scrollVerticallyBy(dy, recycler, state)
        applyTransformation()
        return scrolled
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        super.onLayoutChildren(recycler, state)
        applyTransformation()
    }

    private fun applyTransformation() {
        val parentHeight = height
        if (parentHeight == 0) return

        val parentCenterY = parentHeight / 2f
        val maxDistance = parentHeight / 2f

        var closestChildPos = -1
        var minDistanceToCenter = Float.MAX_VALUE

        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            val childCenterY = (getDecoratedTop(child) + getDecoratedBottom(child)) / 2f
            val distance = (childCenterY - parentCenterY) / maxDistance
            val clampedDistance = distance.coerceIn(-1f, 1f)
            val absDistance = abs(clampedDistance)

            if (absDistance < minDistanceToCenter) {
                minDistanceToCenter = absDistance
                closestChildPos = getPosition(child)
            }

            if (isOrbitalEnabled) {
                val angle = clampedDistance * (Math.PI / 3).toFloat()
                val xOffset = (1f - cos(angle)) * (width * radiusFactor)
                child.translationX = if (isRightHanded) -xOffset else xOffset
            } else {
                child.translationX = 0f
            }

            val centerProximity = 1f - absDistance
            val scale = 0.85f + (0.30f * centerProximity)
            val alpha = 0.35f + (0.65f * centerProximity)

            child.scaleX = scale
            child.scaleY = scale
            child.alpha = alpha
        }

        if (isHapticsEnabled && minDistanceToCenter < 0.12f && closestChildPos != lastCenteredPosition) {
            lastCenteredPosition = closestChildPos
            triggerCenterHaptic()
        }
    }

    private fun triggerCenterHaptic() {
        vibrator?.let { v ->
            if (v.hasVibrator()) {
                @Suppress("DEPRECATION")
                v.vibrate(12)
            }
        }
    }
}
