package uk.co.asepstrath.bank.kyc;

public class Question {
    String id;
    String questionText;
    AnswerType answerType;
    Object validOptions;

    public enum AnswerType {
        STRING,
        NUMBER,
        DATE,
        ENUM,
    }

    public Question(String id, String questionText, AnswerType answerType, Object validOptions) {
        this.id = id;
        this.questionText = questionText;
        this.answerType = answerType;
        if (validOptions != null) {
            if (answerType == AnswerType.STRING && !(validOptions instanceof String)) {
                throw new IllegalArgumentException("Options for STRING answer type must be a string.");
            }
            if (answerType == AnswerType.NUMBER && !(validOptions instanceof MinMax)) {
                throw new IllegalArgumentException("Options for NUMBER answer type must be a MinMax.");
            }
            if (answerType == AnswerType.DATE && !(validOptions instanceof MinMax)) {
                throw new IllegalArgumentException("Options for DATE answer type must be a MinMax.");
            }
        }
        if (answerType == AnswerType.ENUM && (validOptions == null || !(validOptions instanceof String[]))) {
            throw new IllegalArgumentException("Options for ENUM answer type must be a String[].");
        }
        this.validOptions = validOptions;
    }

    public String getId() {
        return id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public AnswerType getAnswerType() {
        return answerType;
    }

    public Object getValidOptions() {
        return validOptions;
    }
}
