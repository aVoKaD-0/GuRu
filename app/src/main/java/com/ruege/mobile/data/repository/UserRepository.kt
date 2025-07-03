package com.ruege.mobile.data.repository

import com.ruege.mobile.data.local.dao.UserDao
import com.ruege.mobile.data.local.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val userDao: UserDao) {
    
    suspend fun hasUsers(): Boolean = withContext(Dispatchers.IO) {
        return@withContext userDao.getUserCount() > 0
    }
    
    suspend fun insert(
        username: String,
        email: String,
        is2faEnabled: Boolean,
        avatarUrl: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val userEntity = UserEntity(
            username,
            email,
            avatarUrl,
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            is2faEnabled
        )
        return@withContext userDao.insert(userEntity)
    }
    
    suspend fun getFirstUser(): UserEntity? = withContext(Dispatchers.IO) {
        return@withContext userDao.getFirstUser()
    }
} 