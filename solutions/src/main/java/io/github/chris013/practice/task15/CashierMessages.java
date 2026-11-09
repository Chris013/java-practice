package io.github.chris013.practice.task15;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The text the cashier's screen shows for each outcome. Task 15: read {@code tasks/15-gift-card-results.md} first.
 *
 * <p>The switch is exhaustive and has no {@code default}. Adding a result type or a {@link DeclineReason} is a
 * compile error here, so nobody can ship a till that shows "Unknown error" for a new case.
 */
public final class CashierMessages {

    private CashierMessages() {}

    /** One exhaustive switch over the result type. There is deliberately no {@code default} branch. */
    public static String forResult(RedemptionResult result) {
        return switch (result) {
            case Redeemed(BigDecimal charged, BigDecimal remainingBalance) ->
                    "Paid %s EUR with gift card. Remaining balance: %s EUR.".formatted(euros(charged), euros(remainingBalance));
            case PartiallyRedeemed(BigDecimal charged, BigDecimal stillToPay) ->
                    "Gift card covered %s EUR. Please collect %s EUR.".formatted(euros(charged), euros(stillToPay));
            case Declined(DeclineReason reason) -> forDecline(reason);
        };
    }

    /** A nested switch over the enum. It is exhaustive too, so a new reason is a compile error here as well. */
    private static String forDecline(DeclineReason reason) {
        return switch (reason) {
            case UNKNOWN_CARD -> "Gift card not found. Please check the code.";
            case WRONG_PIN -> "Wrong PIN. Please try again.";
            case CARD_BLOCKED -> "Gift card blocked after too many wrong PINs. Refer the customer to the service desk.";
            case EXPIRED -> "Gift card expired.";
            case EMPTY -> "Gift card has no balance left.";
        };
    }

    private static String euros(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
