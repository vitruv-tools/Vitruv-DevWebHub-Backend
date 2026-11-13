package tools.vitruv.methodologisttemplate.consistency;

import tools.vitruv.change.interaction.UserInteractionOptions.InputValidator;

/**
 * Shared {@link InputValidator}s for SystemRootVsum text-input dialogs.
 * Used from reactions DSL via {@code .inputValidator(...)}.
 */
public final class SystemRootInputValidators {

    private SystemRootInputValidators() {
    }

    /**
     * Positive integer (≥ 1) for {@code Link.speedMBitsPerSecond}.
     */
    public static final InputValidator POSITIVE_INTEGER_MBITS = new InputValidator() {
        @Override
        public boolean isInputValid(String input) {
            if (input == null || input.isBlank()) {
                return false;
            }
            try {
                return Integer.parseInt(input.trim()) >= 1;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        @Override
        public String getInvalidInputMessage(String input) {
            return "Please enter a positive integer for communication speed in MBits/s (e.g. 1000).";
        }
    };

    /**
     * Non-blank protocol name for Point 2 text prompts.
     */
    public static final InputValidator NON_EMPTY_PROTOCOL_NAME = new InputValidator() {
        @Override
        public boolean isInputValid(String input) {
            return input != null && !input.isBlank();
        }

        @Override
        public String getInvalidInputMessage(String input) {
            return "Protocol name must not be empty.";
        }
    };
}
