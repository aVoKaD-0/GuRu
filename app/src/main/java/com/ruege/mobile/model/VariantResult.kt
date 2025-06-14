package com.ruege.mobile.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

/**
 * Класс для хранения результатов выполнения варианта
 */
@Parcelize
data class VariantResult(
    val variantId: String,
    val tasks: List<TaskAnswer>,
    val score: Int,
    val maxScore: Int,
    val completionTime: Long,
    val timestamp: Long
) : Parcelable {

    /**
     * Класс для хранения информации об ответе на задание
     */
    @Parcelize
    data class TaskAnswer(
        val taskId: String,
        val userAnswer: String,
        val correctAnswer: String,
        val isCorrect: Boolean
    ) : Parcelable {
        /**
         * Преобразует ответ в JSON объект
         */
        fun toJson(): JSONObject {
            val json = JSONObject()
            json.put("ответ", userAnswer)
            json.put("верный ответ", correctAnswer)
            json.put("статус", isCorrect)
            json.put("task_id", taskId)
            return json
        }

        companion object {
            /**
             * Создает объект TaskAnswer из JSON объекта
             */
            fun fromJson(json: JSONObject): TaskAnswer {
                return TaskAnswer(
                    taskId = json.optString("task_id", ""),
                    userAnswer = json.optString("ответ", ""),
                    correctAnswer = json.optString("верный ответ", ""),
                    isCorrect = json.optBoolean("статус", false)
                )
            }
        }
    }

    /**
     * Преобразует результаты варианта в JSON строку
     */
    fun toJsonString(): String {
        val tasksArray = JSONArray()
        tasks.forEach { task ->
            tasksArray.put(task.toJson())
        }
        return tasksArray.toString()
    }

    companion object {
        /**
         * Создает объект VariantResult из JSON строки
         */
        fun fromJsonString(jsonString: String, variantId: String, timestamp: Long): VariantResult? {
            return try {
                val tasksArray = JSONArray(jsonString)
                val tasksList = mutableListOf<TaskAnswer>()
                
                for (i in 0 until tasksArray.length()) {
                    val taskJson = tasksArray.getJSONObject(i)
                    tasksList.add(TaskAnswer.fromJson(taskJson))
                }
                
                val score = tasksList.count { it.isCorrect }
                
                VariantResult(
                    variantId = variantId,
                    tasks = tasksList,
                    score = score,
                    maxScore = tasksList.size,
                    completionTime = 0, // Время выполнения неизвестно из JSON
                    timestamp = timestamp
                )
            } catch (e: Exception) {
                null
            }
        }
    }
} 