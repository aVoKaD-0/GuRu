package com.ruege.mobile.data.local.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.ruege.mobile.data.local.entity.TaskEntity;
import com.ruege.mobile.data.local.entity.UserTaskAttemptEntity;

import java.util.List;

public class TaskWithAttempts {
    @Embedded
    public TaskEntity task;

    @Relation(
        parentColumn = "task_id",
        entityColumn = "task_id"
    )
    public List<UserTaskAttemptEntity> attempts;
} 