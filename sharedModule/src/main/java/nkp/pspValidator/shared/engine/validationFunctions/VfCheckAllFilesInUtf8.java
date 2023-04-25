package nkp.pspValidator.shared.engine.validationFunctions;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;

import java.io.*;
import java.util.List;

public class VfCheckAllFilesInUtf8 extends ValidationFunction {

    public static final String PARAM_FILES = "files";

    public VfCheckAllFilesInUtf8(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 1, 1)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramFiles = valueParams.getParams(PARAM_FILES).get(0).getEvaluation();
            List<File> files = (List<File>) paramFiles.getData();
            if (files == null) {
                //ignore
                //return invalidValueParamNull(PARAM_FILES, paramFiles);
                return ValidationResult.ok();
            }
            return validate(files);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }

    }

    private ValidationResult validate(List<File> files) {
        ValidationResult result = new ValidationResult();
        for (File file : files) {
            try {
                String encoding = detectEncoding(file);
                if (!"UTF-8".equals(encoding)) {
                    result.addError(invalid(Level.ERROR, "soubor %s není v kódování UTF-8, ale v %s", file.getName(), encoding));
                }
            } catch (IOException e) {
                result.addError(invalid(Level.ERROR, "chyba v detekci kódování souboru %s", file.getName()));
            }
        }
        return result;
    }

    private String detectEncoding(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            CharsetDetector detector = new CharsetDetector();
            detector.setText(new BufferedInputStream(is));
            CharsetMatch match = detector.detect();
            return match.getName();
        }
    }

}
