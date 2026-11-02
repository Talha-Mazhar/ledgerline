package com.ledgerline.assistant;

import com.ledgerline.identity.CurrentUser;
import java.util.Objects;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** {@code POST /assistant/questions}: ask about your own money. */
@RestController
class AssistantController {

    record QuestionRequest(String question) {
        QuestionRequest {
            Objects.requireNonNull(question, "question is required");
        }
    }

    record AnswerResponse(String answer) {}

    private final Assistant assistant;
    private final CurrentUser currentUser;

    AssistantController(Assistant assistant, CurrentUser currentUser) {
        this.assistant = assistant;
        this.currentUser = currentUser;
    }

    @PostMapping("/api/v1/assistant/questions")
    AnswerResponse askAssistant(@RequestBody QuestionRequest request) {
        return new AnswerResponse(assistant.ask(currentUser.id(), request.question()));
    }
}
