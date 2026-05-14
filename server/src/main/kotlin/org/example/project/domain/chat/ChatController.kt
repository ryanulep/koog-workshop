package org.example.project.domain.chat

import ai.koog.prompt.message.Message
import org.example.project.domain.shared.CharacterId
import org.springframework.web.bind.annotation.*
import kotlin.uuid.Uuid

@RestController
@RequestMapping("/chats")
class ChatController(
    private val chatService: ChatService,
    private val askQuestionRepository: AskQuestionRepository
) {

    @PostMapping("/answer")
    suspend fun answerQuestion(
        @RequestParam characterId: String,
        @RequestParam sessionId: String,
        @RequestParam answer: String
    ) {
        askQuestionRepository.answerQuestion(CharacterId(Uuid.parse(characterId)), sessionId, answer)
    }

    @GetMapping("/character/{characterId}")
    suspend fun getCharacterChatDetails(@PathVariable characterId: String): List<ChatDetails> {
        return chatService.getCharacterChatDetails(CharacterId(Uuid.parse(characterId)))
    }

    @PostMapping("/update")
    fun updateChat(@RequestBody update: ChatUpdate) {
        chatService.updateChat(update)
    }

    @GetMapping("/history/{conversationId}")
    suspend fun getChatHistory(@PathVariable conversationId: String): List<Message> =
        chatService.getChatHistory(conversationId)
}
