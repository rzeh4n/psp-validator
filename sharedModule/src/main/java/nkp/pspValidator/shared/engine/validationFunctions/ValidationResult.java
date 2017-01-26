package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class ValidationResult {

    private final List<ValidationProblem> errors = new ArrayList<>();

    public boolean hasProblems() {
        return !errors.isEmpty();
    }

    public void addError(ValidationProblem error) {
        errors.add(error);
    }

    public void addError(Level level, String messae) {
        errors.add(new ValidationProblem(level, messae));
    }

    public List<ValidationProblem> getProblems() {
        return errors;
    }


}
