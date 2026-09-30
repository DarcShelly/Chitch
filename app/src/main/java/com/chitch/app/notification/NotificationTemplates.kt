package com.chitch.app.notification

import android.content.Context
import org.json.JSONObject

data class NotificationContent(val title: String, val body: String)

/** Which template applies to a given task, derived from its routine/habit membership. */
enum class NotificationTemplateKey(val id: String) {
    TASK_START("task_start"),
    TASK_DUE("task_due"),
    HABIT_START("habit_start"),
    HABIT_DUE("habit_due"),
    ROUTINE_TASK_START("routine_task_start"),
    ROUTINE_TASK_DUE("routine_task_due")
}

/**
 * Loads title/body templates from the bundled JSON asset (rather than hardcoding notification
 * copy in Kotlin — see the notification-templates request in chat) and fills in placeholders
 * ({title}, {time}, {type}) with real values at schedule/fire time.
 */
object NotificationTemplates {
    private const val ASSET_PATH = "notification_templates.json"

    @Volatile
    private var cache: Map<String, NotificationContent>? = null

    private fun loadAll(context: Context): Map<String, NotificationContent> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
            val templatesObj = JSONObject(json).getJSONObject("templates")
            val result = mutableMapOf<String, NotificationContent>()
            templatesObj.keys().forEach { key ->
                val entry = templatesObj.getJSONObject(key)
                result[key] = NotificationContent(entry.getString("title"), entry.getString("body"))
            }
            cache = result
            return result
        }
    }

    fun render(
        context: Context,
        key: NotificationTemplateKey,
        title: String,
        time: String,
        type: String
    ): NotificationContent {
        val template = loadAll(context)[key.id]
            ?: NotificationContent(title, "Reminder for \"$title\" at $time")
        val placeholders = mapOf("title" to title, "time" to time, "type" to type)
        return NotificationContent(
            title = fill(template.title, placeholders),
            body = fill(template.body, placeholders)
        )
    }

    private fun fill(template: String, placeholders: Map<String, String>): String {
        var result = template
        placeholders.forEach { (key, value) -> result = result.replace("{$key}", value) }
        return result
    }
}
