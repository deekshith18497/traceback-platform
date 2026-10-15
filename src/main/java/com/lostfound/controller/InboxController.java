
package com.lostfound.controller;

import com.lostfound.dto.inbox.*;
import com.lostfound.service.InboxService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inbox")
public class InboxController {

    private final InboxService inboxService;

    public InboxController(InboxService inboxService) {
        this.inboxService = inboxService;
    }

    @PostMapping("/conversations")
    public ResponseEntity<ConversationResponse> startConversation(
            Authentication authentication,
            @Valid @RequestBody StartConversationRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inboxService.startConversation(
                        authentication.getName(), request));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getInbox(
            Authentication authentication) {

        return ResponseEntity.ok(
                inboxService.getInbox(authentication.getName()));
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            Authentication authentication,
            @PathVariable Long id) {

        return ResponseEntity.ok(
                inboxService.getMessages(authentication.getName(), id));
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inboxService.sendMessage(
                        authentication.getName(), id, request));
    }
}
