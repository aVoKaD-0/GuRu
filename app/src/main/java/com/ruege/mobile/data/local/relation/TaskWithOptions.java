package com.ruege.mobile.data.local.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.ruege.mobile.data.local.entity.TaskEntity;
import com.ruege.mobile.data.local.entity.TaskOptionEntity;

import java.util.List;

public class TaskWithOptions {
    @Embedded
    public TaskEntity task;

    @Relation(
        parentColumn = "id",
        entityColumn = "task_id"
    )
    public List<TaskOptionEntity> options;
} 