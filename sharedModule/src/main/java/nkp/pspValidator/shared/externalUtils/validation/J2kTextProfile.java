package nkp.pspValidator.shared.externalUtils.validation;

import nkp.pspValidator.shared.engine.exceptions.ExternalUtilOutputParsingException;
import nkp.pspValidator.shared.externalUtils.ExternalUtil;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;

/**
 * Created by Martin Řehánek on 18.11.16.
 */
public class J2kTextProfile extends J2kProfile {

    public J2kTextProfile(ExternalUtilManager externalUtilManager, ExternalUtil externalUtil) {
        super(externalUtilManager, externalUtil);
    }

    @Override
    Object processExternalUtilOutput(String toolRawOutput, ExternalUtil util) throws ExternalUtilOutputParsingException {
        return toolRawOutput;
    }
}