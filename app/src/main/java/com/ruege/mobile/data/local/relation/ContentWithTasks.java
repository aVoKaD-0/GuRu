package com.ruege.mobile.data.local.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.ruege.mobile.data.local.entity.ContentEntity;
import com.ruege.mobile.data.local.entity.TaskEntity;

import java.util.List;

public class ContentWithTasks {
    @Embedded
    public ContentEntity content;

    /* // ЗАКОММЕНТИРОВАНО: Связь по content_id больше не работает, т.к. столбец удален из TaskEntity
    @Relation(
        parentColumn = "content_id",
        entityColumn = "content_id"
    )
    public List<TaskEntity> tasks;
    */
} 