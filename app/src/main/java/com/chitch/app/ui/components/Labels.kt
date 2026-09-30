package com.chitch.app.ui.components

import com.chitch.app.data.entity.RepeatType

fun repeatTypeLabel(repeatType: RepeatType): String = when (repeatType) {
    RepeatType.ONE_TIME -> "One time"
    RepeatType.DAILY -> "Daily"
    RepeatType.ALTERNATE -> "Alternate days"
    RepeatType.WEEKLY -> "Weekly"
    RepeatType.MONTHLY -> "Monthly"
}
