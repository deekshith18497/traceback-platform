
package com.lostfound.service;

import com.lostfound.dto.inbox.*;
import com.lostfound.entity.*;
import com.lostfound.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InboxService {

    private final InboxConversationRepository conversationRepository;
    private final InboxMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final LostItemRepository lostItemRepository;
    private final FoundItemRepository foundItemRepository;

    public InboxService(
            InboxConversationRepository conversationRepository,
            InboxMessageRepository messageRepository,
            UserRepository userRepository,
            LostItemRepository lostItemRepository,
            FoundItemRepository foundItemRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.lostItemRepository = lostItemRepository;
        this.foundItemRepository = foundItemRepository;
    }

    @Transactional
    public ConversationResponse startConversation(
            String email, StartConversationRequest request) {

        User currentUser = getUser(email);
        String itemType = request.getItemType().trim().toUpperCase();

        User owner;
        String itemTitle;

        if ("LOST".equals(itemType)) {
            LostItem item = lostItemRepository.findById(request.getItemId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Lost item not found"));

            if (!"ACTIVE".equalsIgnoreCase(item.getStatus())) {
                throw new IllegalArgumentException(
                        "This lost item is no longer active");
            }

            owner = item.getUser();
            itemTitle = item.getTitle();

        } else if ("FOUND".equals(itemType)) {
            FoundItem item = foundItemRepository.findById(request.getItemId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Found item not found"));

            if (!"ACTIVE".equalsIgnoreCase(item.getStatus())
                    && !"FOUND".equalsIgnoreCase(item.getStatus())) {
                throw new IllegalArgumentException(
                        "This found item is no longer active");
            }

            owner = item.getUser();
            itemTitle = item.getTitle();

        } else {
            throw new IllegalArgumentException(
                    "itemType must be LOST or FOUND");
        }

        if (currentUser.getId().equals(owner.getId())) {
            throw new IllegalArgumentException(
                    "You cannot start a conversation with yourself");
        }

        InboxConversation conversation =
                conversationRepository.findExistingConversation(
                        itemType,
                        request.getItemId(),
                        currentUser.getId(),
                        owner.getId())
                .orElseGet(() -> {
                    InboxConversation created = new InboxConversation();
                    created.setParticipantOne(currentUser);
                    created.setParticipantTwo(owner);
                    created.setItemType(itemType);
                    created.setItemId(request.getItemId());
                    created.setItemTitle(itemTitle);
                    return conversationRepository.save(created);
                });

        saveMessage(conversation, currentUser, request.getMessage());

        return toConversationResponse(conversation, currentUser);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getInbox(String email) {
        User currentUser = getUser(email);

        return conversationRepository
                .findInboxForUser(currentUser.getId())
                .stream()
                .map(c -> toConversationResponse(c, currentUser))
                .toList();
    }

    @Transactional
    public List<MessageResponse> getMessages(
            String email, Long conversationId) {

        User currentUser = getUser(email);
        InboxConversation conversation =
                getAuthorizedConversation(conversationId, currentUser);

        List<InboxMessage> messages =
                messageRepository
                        .findByConversationIdOrderByCreatedAtAsc(
                                conversation.getId());

        LocalDateTime now = LocalDateTime.now();

        for (InboxMessage message : messages) {
            if (!message.getSender().getId().equals(currentUser.getId())
                    && message.getReadAt() == null) {
                message.setReadAt(now);
            }
        }

        return messages.stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(
            String email,
            Long conversationId,
            SendMessageRequest request) {

        User currentUser = getUser(email);

        InboxConversation conversation =
                getAuthorizedConversation(conversationId, currentUser);

        InboxMessage message =
                saveMessage(conversation, currentUser, request.getContent());

        return toMessageResponse(message);
    }

    private InboxMessage saveMessage(
            InboxConversation conversation,
            User sender,
            String content) {

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "Message cannot be empty");
        }

        if (content.trim().length() > 2000) {
            throw new IllegalArgumentException(
                    "Message cannot exceed 2000 characters");
        }

        InboxMessage message = new InboxMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content.trim());

        InboxMessage saved = messageRepository.save(message);

        // Update conversation ordering for the inbox list.
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return saved;
    }

    private InboxConversation getAuthorizedConversation(
            Long conversationId, User currentUser) {

        InboxConversation conversation =
                conversationRepository.findById(conversationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Conversation not found"));

        Long userId = currentUser.getId();

        boolean isParticipant =
                conversation.getParticipantOne().getId().equals(userId)
                || conversation.getParticipantTwo().getId().equals(userId);

        if (!isParticipant) {
            throw new IllegalArgumentException(
                    "You are not allowed to access this conversation");
        }

        return conversation;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(
                        email.trim().toLowerCase())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }

    private ConversationResponse toConversationResponse(
            InboxConversation conversation, User currentUser) {

        ConversationResponse response = new ConversationResponse();

        User otherUser =
                conversation.getParticipantOne().getId()
                        .equals(currentUser.getId())
                ? conversation.getParticipantTwo()
                : conversation.getParticipantOne();

        response.setId(conversation.getId());
        response.setOtherUserId(otherUser.getId());
        response.setOtherUserName(otherUser.getName());
        response.setItemType(conversation.getItemType());
        response.setItemId(conversation.getItemId());
        response.setItemTitle(conversation.getItemTitle());

        List<InboxMessage> messages =
                messageRepository
                        .findByConversationIdOrderByCreatedAtAsc(
                                conversation.getId());

        if (!messages.isEmpty()) {
            InboxMessage latest = messages.get(messages.size() - 1);
            response.setLastMessage(latest.getContent());
            response.setLastMessageAt(latest.getCreatedAt());
        }

        response.setUnreadCount(
                messageRepository
                        .countByConversationIdAndSenderIdNotAndReadAtIsNull(
                                conversation.getId(), currentUser.getId()));

        return response;
    }

    private MessageResponse toMessageResponse(InboxMessage message) {
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setSenderId(message.getSender().getId());
        response.setSenderName(message.getSender().getName());
        response.setContent(message.getContent());
        response.setCreatedAt(message.getCreatedAt());
        response.setReadAt(message.getReadAt());
        return response;
    }
}
