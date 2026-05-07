package org.example.project.domain.chat

import org.example.project.domain.character.Characters
import org.example.project.domain.shared.CharacterId
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class ChatRepository {
    context(_: Transaction)
    fun getCharacterChats(characterId: CharacterId): List<Chat> {
        return Chats.selectAll()
            .where { Chats.character eq characterId.value }
            .orderBy(Chats.updatedAt to SortOrder.DESC)
            .map {
                Chat(
                    characterId = CharacterId(it[Chats.character].value),
                    conversationId = it[Chats.conversationId],
                    createdAt = it[Chats.createdAt],
                    updatedAt = it[Chats.updatedAt],
                )
            }
    }

    context(_: Transaction)
    fun updateChat(update: ChatUpdate): Boolean {
        val existing = Chats.selectAll()
            .where {
                (Chats.character eq update.characterId.value) and
                    (Chats.conversationId eq update.conversationId)
            }
            .singleOrNull()

        return if (existing != null) {
            Chats.update(
                where = {
                    (Chats.character eq update.characterId.value) and
                        (Chats.conversationId eq update.conversationId)
                }
            ) {
                it[updatedAt] = CurrentTimestamp
            } > 0
        } else {
            Chats.insert {
                it[character] = update.characterId.value
                it[conversationId] = update.conversationId
            }

            true
        }
    }}