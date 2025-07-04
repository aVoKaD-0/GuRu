package com.ruege.mobile.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.function.Consumer 

/**
 * Вспомогательный объект для упрощения сбора Kotlin Flow из Java.
 */
object FlowJavaHelper {

    /**
     * Собирает значения из Flow в указанном CoroutineScope.
     *
     * @param T Тип элементов во Flow.
     * @param flow Поток для сбора.
     * @param scope CoroutineScope, в котором будет запущен сбор.
     * @param onEach Java Consumer, вызываемый для каждого элемента.
     */
    @JvmStatic
    fun <T> collectInScope(
        flow: Flow<T>,
        scope: CoroutineScope,
        onEach: Consumer<T> 
    ) {
        scope.launch {
            flow.collect {
                onEach.accept(it) 
            }
        }
    }
} 