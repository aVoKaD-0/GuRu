package com.ruege.mobile.data.network.api

import com.ruege.mobile.data.network.dto.request.AnswerRequest
import com.ruege.mobile.data.network.dto.response.AnswerResult
import com.ruege.mobile.data.network.dto.response.TaskDetailDto
import com.ruege.mobile.data.network.dto.response.TaskDto
import com.ruege.mobile.data.network.dto.response.TaskTextResponseDto
import com.ruege.mobile.model.AnswerCheckResult
import com.ruege.mobile.data.network.dto.response.TextDataDto
import com.ruege.mobile.data.network.dto.response.SolutionDto
import com.ruege.mobile.data.network.dto.response.TasksWithTextsResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API сервис для получения заданий из таблицы ruege_tasks.
 */
interface TaskApiService {
    /**
     * Получает список всех групп заданий.
     */
    @GET("tasks")
    suspend fun getAllTasks(): Response<List<Map<String, Any>>>
    
    /**
     * Получает задания для конкретной EGE категории.
     */
    @GET("tasks/by-ege/{ege_number}")
    suspend fun getTasksByEgeNumber(
        @Path("ege_number") egeNumber: String,
        @Query("limit") limit: Int = 20,
        @Query("skip") skip: Int = 0
    ): Response<List<TaskDto>>
    
    /**
     * Получает задания для конкретной EGE категории с поддержкой пагинации.
     */
    @GET("tasks/by-ege/{ege_number}")
    suspend fun getTasksByEgeNumberPaginated(
        @Path("ege_number") egeNumber: String,
        @Query("limit") limit: Int = 20,
        @Query("page_number") pageNumber: Int = 1,
        @Query("include_text") includeText: Boolean = false
    ): Response<TasksWithTextsResponseDto>
    
    /**
     * Получает группы заданий ЕГЭ.
     */
    @GET("tasks/groups")
    suspend fun getTaskGroups(): Response<List<Map<String, Any>>>
    
    /**
     * Получает детальную информацию о задании.
     */
    @GET("tasks/detail/{task_id}")
    suspend fun getTaskDetail(
        @Path("task_id") taskId: String,
        @Query("include_text") includeText: Boolean = false
    ): Response<TaskDto>
    
    /**
     * Получает текст задания по его ID.
     */
    @GET("tasks/text/{id}")
    suspend fun getTaskTextById(@Path("id") id: String): Response<TaskTextResponseDto>
    
    /**
     * Отправляет ответ пользователя на проверку.
     */
    /*
    @POST("tasks/{task_id}/check_answer")
    suspend fun checkAnswer(
        @Path("task_id") taskId: Int,
        @Body answerRequest: AnswerRequest
    ): AnswerCheckResult
    */
} 