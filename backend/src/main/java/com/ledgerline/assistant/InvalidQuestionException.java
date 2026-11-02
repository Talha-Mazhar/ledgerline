package com.ledgerline.assistant;

import com.ledgerline.DomainException;

class InvalidQuestionException extends DomainException {

    InvalidQuestionException() {
        super(Kind.INVALID_REQUEST, "Invalid question", "Ask a question of up to 500 characters.", null);
    }
}
