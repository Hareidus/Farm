package com.hareidus.taboo.farm.modules.l1.social

import com.hareidus.taboo.farm.foundation.database.DatabaseManager
import com.hareidus.taboo.farm.foundation.model.EnemyRecord
import com.hareidus.taboo.farm.foundation.model.FriendRelation
import com.hareidus.taboo.farm.foundation.model.FriendRequest
import com.hareidus.taboo.farm.foundation.model.FriendRequestStatus
import org.bukkit.Bukkit
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 社交管理器 (L1)
 *
 * 职责：
 * 1. 管理好友关系的建立与解除
 * 2. 管理好友请求的发送、接受、拒绝与过期
 * 3. 管理仇人标记的写入与查询
 * 4. 提供关系查询 API（是否好友、是否仇人）
 *
 * 依赖: database_manager
 */
object SocialManager {

    @Config("modules/l1/social.yml", autoReload = true)
    lateinit var config: Configuration
        private set

    /** 好友上限 */
    val maxFriends: Int
        get() = config.getInt("max-friends", 50)

    /** 好友请求过期时间（毫秒） */
    val friendRequestExpiryMs: Long
        get() = TimeUnit.HOURS.toMillis(config.getLong("friend-request-expiry-hours", 72))

    @Awake(LifeCycle.ENABLE)
    fun init() {
        info("[Farm] 社交管理器已加载 (好友上限: $maxFriends, 请求过期: ${config.getLong("friend-request-expiry-hours", 72)}h)")
    }

    // ==================== 好友关系 ====================

    /** 获取玩家的好友列表 */
    fun getFriends(uuid: UUID): List<FriendRelation> {
        return DatabaseManager.database.getFriends(uuid)
    }

    /** 判断两名玩家是否为好友 */
    fun isFriend(a: UUID, b: UUID): Boolean {
        return DatabaseManager.database.isFriend(a, b)
    }

    /** 直接添加好友关系（双向），跳过请求流程 */
    fun addFriend(a: UUID, b: UUID): Boolean {
        if (a == b) return false
        if (isFriend(a, b)) return false
        if (getFriends(a).size >= maxFriends) return false
        if (getFriends(b).size >= maxFriends) return false
        val result = DatabaseManager.database.insertFriend(a, b)
        if (result) {
            Bukkit.getPluginManager().callEvent(FriendAddedEvent(a, b))
        }
        return result
    }

    /** 删除好友关系（双向） */
    fun removeFriend(a: UUID, b: UUID): Boolean {
        val result = DatabaseManager.database.deleteFriend(a, b)
        if (result) {
            Bukkit.getPluginManager().callEvent(FriendRemovedEvent(a, b))
        }
        return result
    }

    // ==================== 好友请求 ====================

    /** 获取玩家的待处理好友请求 */
    fun getPendingRequests(uuid: UUID): List<FriendRequest> {
        return DatabaseManager.database.getPendingFriendRequests(uuid)
            .filter { !isRequestExpired(it) }
    }

    /**
     * 发送好友请求
     * 校验：不能加自己、已是好友、好友上限、重复请求
     */
    fun sendFriendRequest(sender: UUID, receiver: UUID): Boolean {
        if (sender == receiver) return false
        if (isFriend(sender, receiver)) return false
        if (getFriends(sender).size >= maxFriends) return false
        if (getFriends(receiver).size >= maxFriends) return false
        // 检查是否已有待处理的请求
        val pending = DatabaseManager.database.getPendingFriendRequests(receiver)
        if (pending.any { it.senderUUID == sender && !isRequestExpired(it) }) return false
        return DatabaseManager.database.insertFriendRequest(sender, receiver)
    }
    /**
     * 接受好友请求
     * 更新请求状态为 ACCEPTED，并双向写入好友关系
     *
     * @param requestId 请求 ID
     * @param receiverUUID 接收者 UUID（用于从待处理列表中定位请求）
     */
    fun acceptFriendRequest(requestId: Long, receiverUUID: UUID): Boolean {
        val request = findRequestById(requestId, receiverUUID) ?: return false
        if (request.status != FriendRequestStatus.PENDING) return false
        if (isRequestExpired(request)) {
            DatabaseManager.database.updateFriendRequestStatus(requestId, FriendRequestStatus.EXPIRED)
            return false
        }
        // 再次校验好友上限
        if (getFriends(request.senderUUID).size >= maxFriends) return false
        if (getFriends(request.receiverUUID).size >= maxFriends) return false

        val statusUpdated = DatabaseManager.database.updateFriendRequestStatus(requestId, FriendRequestStatus.ACCEPTED)
        if (!statusUpdated) return false

        val friendInserted = DatabaseManager.database.insertFriend(request.senderUUID, request.receiverUUID)
        if (friendInserted) {
            Bukkit.getPluginManager().callEvent(FriendAddedEvent(request.senderUUID, request.receiverUUID))
        }
        return friendInserted
    }

    /**
     * 拒绝好友请求
     *
     * @param requestId 请求 ID
     * @param receiverUUID 接收者 UUID（用于从待处理列表中定位请求）
     */
    fun rejectFriendRequest(requestId: Long, receiverUUID: UUID): Boolean {
        val request = findRequestById(requestId, receiverUUID) ?: return false
        if (request.status != FriendRequestStatus.PENDING) return false
        return DatabaseManager.database.updateFriendRequestStatus(requestId, FriendRequestStatus.REJECTED)
    }

    // ==================== 仇人关系 ====================

    /** 获取玩家的仇人列表 */
    fun getEnemies(uuid: UUID): List<EnemyRecord> {
        return DatabaseManager.database.getEnemies(uuid)
    }

    /** 判断是否为仇人关系 */
    fun isEnemy(victim: UUID, thief: UUID): Boolean {
        return DatabaseManager.database.isEnemy(victim, thief)
    }

    /** 标记仇人（偷菜后自动调用） */
    fun markEnemy(victimUUID: UUID, thiefUUID: UUID): Boolean {
        if (isEnemy(victimUUID, thiefUUID)) return true
        val result = DatabaseManager.database.insertEnemy(victimUUID, thiefUUID)
        if (result) {
            Bukkit.getPluginManager().callEvent(EnemyMarkedEvent(victimUUID, thiefUUID))
        }
        return result
    }

    /** 移除仇人标记 */
    fun removeEnemy(victim: UUID, thief: UUID): Boolean {
        return DatabaseManager.database.deleteEnemy(victim, thief)
    }

    // ==================== 内部工具 ====================

    /** 判断好友请求是否已过期 */
    private fun isRequestExpired(request: FriendRequest): Boolean {
        return System.currentTimeMillis() - request.requestedAt > friendRequestExpiryMs
    }

    /** 通过 ID 查找好友请求（从接收者的待处理列表中匹配） */
    private fun findRequestById(requestId: Long, receiverUUID: UUID): FriendRequest? {
        return DatabaseManager.database.getPendingFriendRequests(receiverUUID)
            .firstOrNull { it.id == requestId }
    }

    /** 重载配置 */
    fun reload() {
        config.reload()
        info("[Farm] 社交管理器配置已重载")
    }
}
