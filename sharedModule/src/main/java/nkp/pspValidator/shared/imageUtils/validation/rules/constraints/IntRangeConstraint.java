package nkp.pspValidator.shared.imageUtils.validation.rules.constraints;

import nkp.pspValidator.shared.imageUtils.validation.Constraint;

/**
 * Created by Martin Řehánek on 17.11.16.
 */
public class IntRangeConstraint implements Constraint {
    private final Integer min;
    private final Integer max;

    public IntRangeConstraint(Integer min, Integer max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean matches(Object data) {
        if (data == null) {
            return false;
        } else {
            Integer value = (Integer) data;
            if (min != null && value < min) {
                return false;
            } else if (max != null && value > max) {
                return false;
            } else {
                return true;
            }
        }
    }

    public String toString() {
        if (min != null && max != null) {
            return String.format("musí být v intervalu <%d, %d>", min, max);
        } else if (min != null) {
            return String.format("musí být alespoň %d", min);
        } else if (max != null) {
            return String.format("musí být nejvýše %d", max);
        } else {
            return "musí být číslo (integer)";
        }
    }
}
