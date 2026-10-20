package uk.co.asepstrath.bank.api;

import uk.co.asepstrath.bank.kyc.Question;
import uk.co.asepstrath.bank.kyc.MinMax;

import java.util.List;
import java.util.Arrays;

public class Questionnaire {
    Question[] questions;
    String challengeToken;

    private static String[] employmentStatus = {
        "employed",
        "self_employed",
        "retired",
        "student",
        "unemployed"
    };

    private static String[] incomeBrackets = {
        "0-25k",
        "25k-50k",
        "50k-100k",
        "100k-250k",
        "250k+"
    };

    private static Question[] requiredQuestions = {
        new Question(
            "dob",
            "What is your date of birth?",
            Question.AnswerType.DATE,
            new MinMax(568080000, 1704067200)
        ),
        new Question(
            "employment_status",
            "What is your current employment status?",
            Question.AnswerType.ENUM,
            employmentStatus
        ),
        new Question(
            "annual_income",
            "What is your approximate annual income?",
            Question.AnswerType.ENUM,
            incomeBrackets
        ),
        new Question(
            "years_experience",
            "How many years of investment experience do you have?",
            Question.AnswerType.NUMBER,
            new MinMax(0, 100)
        ),
        new Question(
            "email",
            "What is your email address?",
            Question.AnswerType.STRING,
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        ),
        new Question(
            "comments",
            "Any additional comments?",
            Question.AnswerType.STRING,
            null
        )
    };

    public Questionnaire(String challengeToken) {
        questions = requiredQuestions;
        this.challengeToken = challengeToken;
    }

    public Question[] getQuestions() {
        return questions;
    }

    public String getChallengeToken() {
        return challengeToken;
    }
}
