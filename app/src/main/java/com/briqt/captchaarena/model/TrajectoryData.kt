package com.briqt.captchaarena.model

/**
 * Represents a single touch point in a gesture trajectory.
 */
data class TrajectoryPoint(
    val x: Float,
    val y: Float,
    val timestamp: Long,
    val pressure: Float = 0f,
    val size: Float = 0f
)

/**
 * Complete trajectory data for analysis.
 */
data class TrajectoryData(
    val points: List<TrajectoryPoint>,
    val startTime: Long,
    val endTime: Long,
    val success: Boolean
) {
    val duration: Long get() = endTime - startTime
    val totalDistance: Float get() {
        if (points.size < 2) return 0f
        return points.zipWithNext().sumOf { (a, b) ->
            kotlin.math.hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble())
        }.toFloat()
    }
    val averageSpeed: Float get() = if (duration > 0) totalDistance / duration else 0f
    val maxYDeviation: Float get() {
        if (points.isEmpty()) return 0f
        val startY = points.first().y
        return points.maxOf { kotlin.math.abs(it.y - startY) }
    }
}
