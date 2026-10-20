package uk.co.asepstrath.bank.kyc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

public class QuestionTests {
    @Test
    public void testQuestion() {
        Question q = new Question("test", "Testing question.", Question.AnswerType.STRING, null);
        Assertions.assertEquals("test", q.getId());
        Assertions.assertEquals("Testing question.", q.getQuestionText());
        Assertions.assertEquals(Question.AnswerType.STRING, q.getAnswerType());
        Assertions.assertEquals(null, q.getValidOptions());
    }

    @Test
    public void testOptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Question q = new Question(
                "string",
                "What happens with a string question with a non-string options?",
                Question.AnswerType.STRING,
                50
            );
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Question q = new Question(
                "number",
                "What happens with a number question with a non-number options?",
                Question.AnswerType.NUMBER,
                "not a range"
            );
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Question q = new Question(
                "date",
                "What happens with a date question with a non-date-range options?",
                Question.AnswerType.DATE,
                "not a range"
            );
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Question q = new Question(
                "enum",
                "What happens with a enum question with a non string-array options?",
                Question.AnswerType.ENUM,
                "not a list of strings"
            );
        });
    }
}
