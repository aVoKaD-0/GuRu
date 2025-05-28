package com.ruege.mobile.data.local.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.ruege.mobile.data.local.entity.UserEntity;
import com.ruege.mobile.data.local.entity.UserTaskAttemptEntity;

import java.util.List;

public class UserWithTaskAttempts {
    @Embedded
    public UserEntity user;

    @Relation(
        parentColumn = "user_id",
        entityColumn = "user_id"
    )
    public List<UserTaskAttemptEntity> attempts;
} 