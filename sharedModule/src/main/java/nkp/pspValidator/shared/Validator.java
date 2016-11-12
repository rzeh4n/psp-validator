package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.Rule;
import nkp.pspValidator.shared.engine.RulesSection;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationError;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by martin on 2.11.16.
 */
public class Validator {

    private final Engine engine;

    public Validator(Engine engine) {
        this.engine = engine;
    }

    public void run(
            boolean printSectionsWithErrors, boolean printSectionsWithoutErrors,
            boolean printRulesWithErrors, boolean printRulesWithoutErrors) {
        List<RulesSection> rulesSections = engine.getRuleSections();
        for (RulesSection section : rulesSections) {
            Map<Level, Integer> errorsByType = computeTotalErrorsByType(section);
            int totalErrors = computeTotalErrors(errorsByType);
            boolean printSection = totalErrors == 0 && printSectionsWithoutErrors || totalErrors != 0 && printSectionsWithErrors;
            if (printSection) {
                String sectionTitle = String.format("Sekce %s: %s", section.getName(), buildSummary(totalErrors, errorsByType));
                System.out.println();
                System.out.println(sectionTitle);
                System.out.println(buildLine(sectionTitle.length()));
            }
            List<Rule> rules = engine.getRules(section);
            for (Rule rule : rules) {
                ValidationResult result = rule.getResult();
                boolean printRule = printSection && (result.hasErrors() && printRulesWithErrors || !result.hasErrors() && printRulesWithoutErrors);
                if (printRule) {
                    String errorsSummary = buildErrorsSummary(result.getErrors());
                    System.out.println(String.format("Pravidlo %s: %s", rule.getName(), errorsSummary));
                    System.out.println(String.format("\t%s", rule.getDescription()));
                    for (ValidationError error : result.getErrors()) {
                        System.out.println(String.format("\t%s: %s", error.getLevel(), error.getMessage()));
                    }
                }
            }
        }
        Map<Level, Integer> errorsByType = computeTotalErrorsByType(rulesSections);
        int totalErrors = computeTotalErrors(errorsByType);
        System.out.println(String.format("\nCelkem: %s, balík je: %s", buildSummary(totalErrors, errorsByType), buildStatus(errorsByType)));
    }

    private String buildStatus(Map<Level, Integer> errorsByType) {
        Integer errors = errorsByType.get(Level.ERROR);
        if (errors == 0) {
            return "validní";
        } else {
            return "nevalidní";
        }
    }

    private Map<Level, Integer> computeTotalErrorsByType(RulesSection section) {
        Map<Level, Integer> errorsByType = new HashMap<>();
        for (Level level : Level.values()) {
            errorsByType.put(level, 0);
        }
        for (Rule rule : engine.getRules(section)) {
            for (ValidationError error : rule.getResult().getErrors()) {
                Integer count = errorsByType.get(error.getLevel());
                errorsByType.put(error.getLevel(), ++count);
            }
        }
        return errorsByType;
    }

    private String buildLine(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append('-');
        }
        return builder.toString();
    }

    private int computeTotalErrors(Map<Level, Integer> errorByType) {
        int counter = 0;
        for (Integer counterByType : errorByType.values()) {
            counter += counterByType;
        }
        return counter;
    }

    private Map<Level, Integer> computeTotalErrorsByType(List<RulesSection> rulesSections) {
        Map<Level, Integer> errorsByType = new HashMap<>();
        for (Level level : Level.values()) {
            errorsByType.put(level, 0);
        }
        for (RulesSection section : rulesSections) {
            for (Rule rule : engine.getRules(section)) {
                for (ValidationError error : rule.getResult().getErrors()) {
                    Integer count = errorsByType.get(error.getLevel());
                    errorsByType.put(error.getLevel(), ++count);
                }
            }
        }
        return errorsByType;
    }

    private String buildErrorsSummary(List<ValidationError> errors) {
        Map<Level, Integer> errorsByType = new HashMap<>();
        for (Level level : Level.values()) {
            errorsByType.put(level, 0);
        }
        for (ValidationError error : errors) {
            Integer counter = errorsByType.get(error.getLevel());
            errorsByType.put(error.getLevel(), ++counter);
        }
        return buildSummary(errors.size(), errorsByType);
    }

    private String buildSummary(int totalErrors, Map<Level, Integer> errorsByType) {
        StringBuilder builder = new StringBuilder();
        builder.append(StringUtils.declineErrorNumber(totalErrors));
        if (totalErrors != 0) {
            builder.append(" (");
            boolean first = true;
            for (int i = 0; i < Level.values().length; i++) {
                Level level = Level.values()[i];
                int count = errorsByType.get(level);
                if (count != 0) {
                    if (!first) {
                        builder.append(", ");
                    }
                    builder.append(String.format("%dx %s", count, level));
                    first = false;
                }
            }
            builder.append(")");
        }
        return builder.toString();

    }
}
