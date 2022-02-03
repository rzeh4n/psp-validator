package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.externalUtils.ExternalUtilManager;

import java.io.File;
import java.util.*;

/**
 * Created by Martin Řehánek on 2.11.16.
 */
public class FdmfRegistry {

    private final Map<String, FdmfConfiguration> monographFdmfByVersion = new HashMap<>();
    private final Map<String, FdmfConfiguration> periodicalFdmfByVersion = new HashMap<>();
    private final Map<String, FdmfConfiguration> audioDocGramFdmfByVersion = new HashMap<>();

    public FdmfRegistry(ValidatorConfigurationManager validatorConfigManager) throws ValidatorConfigurationException {
        init(validatorConfigManager);
    }

    public void initBinaryFileProfiles(ExternalUtilManager externalUtilManager) throws ValidatorConfigurationException {
        for (FdmfConfiguration fdmfConfig : monographFdmfByVersion.values()) {
            fdmfConfig.initBinaryFileProfiles(externalUtilManager);
        }
        for (FdmfConfiguration fdmfConfig : periodicalFdmfByVersion.values()) {
            fdmfConfig.initBinaryFileProfiles(externalUtilManager);
        }
        for (FdmfConfiguration fdmfConfig : audioDocGramFdmfByVersion.values()) {
            fdmfConfig.initBinaryFileProfiles(externalUtilManager);
        }
    }

    private void init(ValidatorConfigurationManager validatorConfigManager) throws ValidatorConfigurationException {
        loadFdmfConfigs(validatorConfigManager, "monograph", monographFdmfByVersion);
        loadFdmfConfigs(validatorConfigManager, "periodical", periodicalFdmfByVersion);
        loadFdmfConfigs(validatorConfigManager, "audio_doc_gram", audioDocGramFdmfByVersion);
    }

    private void loadFdmfConfigs(ValidatorConfigurationManager validatorConfigManager, String fdmfDirPefix, Map<String, FdmfConfiguration> mapToStoreResults) throws ValidatorConfigurationException {
        File[] fdmfDirs = validatorConfigManager.getFdmfDir().listFiles((dir, name) -> name.matches(fdmfDirPefix + "_[0-9]+(\\.([0-9])+)*"));
        for (File fdmfDir : fdmfDirs) {
            String versionNumber = fdmfDir.getName().substring(fdmfDirPefix.length() + 1);
            mapToStoreResults.put(versionNumber, new FdmfConfiguration(
                    fdmfDir,
                    validatorConfigManager.getFdmfConfigXsd(),
                    validatorConfigManager.getBinaryFileProfileXsd(),
                    validatorConfigManager.getMetadataProfileXsd()));
        }
    }

    public Set<String> getMonographFdmfVersions() {
        return monographFdmfByVersion.keySet();
    }

    public Set<String> getPeriodicalFdmfVersions() {
        return periodicalFdmfByVersion.keySet();
    }

    public Set<String> getAudioDocGramFdmfVersions() {
        return audioDocGramFdmfByVersion.keySet();
    }

    public FdmfConfiguration getMonographFdmfConfig(String dmfVersion) {
        return monographFdmfByVersion.get(dmfVersion);
    }

    public FdmfConfiguration getPeriodicalFdmfConfig(String dmfVersion) {
        return periodicalFdmfByVersion.get(dmfVersion);
    }

    public FdmfConfiguration getAudioDocGramFdmfConfig(String dmfVersion) {
        return audioDocGramFdmfByVersion.get(dmfVersion);
    }

    public FdmfConfiguration getFdmfConfig(Dmf dmf) throws UnknownFdmfException {
        switch (dmf.getType()) {
            case MONOGRAPH: {
                FdmfConfiguration file = monographFdmfByVersion.get(dmf.getVersion());
                if (file == null) {
                    throw new UnknownFdmfException(dmf);
                } else {
                    return file;
                }
            }
            case PERIODICAL: {
                FdmfConfiguration file = periodicalFdmfByVersion.get(dmf.getVersion());
                if (file == null) {
                    throw new UnknownFdmfException(dmf);
                } else {
                    return file;
                }
            }
            case AUDIO_DOC_GRAM: {
                FdmfConfiguration file = audioDocGramFdmfByVersion.get(dmf.getVersion());
                if (file == null) {
                    throw new UnknownFdmfException(dmf);
                } else {
                    return file;
                }
            }
            default:
                throw new IllegalStateException();
        }
    }


    public static class UnknownFdmfException extends Exception {

        public UnknownFdmfException(Dmf dmf) {
            super(String.format("Není definována fDMF (formalizovaná DMF) pro: %s", dmf));
        }

    }

}
