package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.XsdImportsResourceResolver;
import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;
import nkp.pspValidator.shared.engine.exceptions.EmptyParamEvaluationException;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import nkp.pspValidator.shared.engine.params.ValueParam;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Martin Řehánek on 27.10.16.
 */
public class VfCheckPremisIsValidByXsd extends ValidationFunction {

    public static final String PARAM_XSD_FILE = "xsd_file";
    public static final String PARAM_METS_FILES = "mets_files";
    public static final String PARAM_LEVEL = "level";

    public VfCheckPremisIsValidByXsd(String name, Engine engine) {
        super(name, engine, new Contract()
                .withValueParam(PARAM_XSD_FILE, ValueType.FILE, 1, 1)
                .withValueParam(PARAM_LEVEL, ValueType.LEVEL, 0, 1)
                .withValueParam(PARAM_METS_FILES, ValueType.FILE_LIST, 0, null)
        );
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramXsdFile = valueParams.getParams(PARAM_XSD_FILE).get(0).getEvaluation();
            File xsdFile = (File) paramXsdFile.getData();
            if (xsdFile == null) {
            } else if (!xsdFile.exists()) {
                return singlErrorResult(invalidFileDoesNotExist(xsdFile));
            } else if (xsdFile.isDirectory()) {
                return singlErrorResult(invalidFileIsDir(xsdFile));
            } else if (!xsdFile.canRead()) {
                return singlErrorResult(invalidCannotReadFile(xsdFile));
            }

            List<File> metsFiles = new ArrayList<>();
            List<ValueParam> metsFilesParams = valueParams.getParams(PARAM_METS_FILES);
            for (ValueParam param : metsFilesParams) {
                ValueEvaluation evaluation = param.getEvaluation();
                List<File> files = (List<File>) evaluation.getData();
                if (files == null) {
                    throw new EmptyParamEvaluationException(PARAM_METS_FILES, evaluation);
                }
                for (File file : files) {
                    metsFiles.add(file.getAbsoluteFile());
                }
            }

            Level level = Level.ERROR;
            List<ValueParam> paramsLevel = valueParams.getParams(PARAM_LEVEL);
            if (!paramsLevel.isEmpty()) {
                ValueParam paramLevel = paramsLevel.get(0);
                ValueEvaluation evaluation = paramLevel.getEvaluation();
                if (evaluation.getData() == null) {
                    return invalidValueParamNull(PARAM_LEVEL, evaluation);
                } else {
                    level = (Level) evaluation.getData();
                }
            }

            return validate(metsFiles, xsdFile, level);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            e.printStackTrace();
            return invalidUnexpectedError(e);

        }
    }

    private ValidationResult validate(List<File> metsFiles, File xsdFile, Level level) {
        ValidationResult result = new ValidationResult();
        for (File metsFile : metsFiles) {
            validate(metsFile, "mets:techMD", "OBJ_", xsdFile, level, result);
            validate(metsFile, "mets:digiprovMD", "EVT_", xsdFile, level, result);
            validate(metsFile, "mets:digiprovMD", "AGENT_", xsdFile, level, result);
        }
        return result;
    }

    private void validate(File metsFile, String amdSecElement, String idPrefix, File xsdFile, Level level, ValidationResult result) {
        try {
            Document metsDoc = engine.getXmlDocument(metsFile, true);
            String xpathStr = String.format("/mets:mets/mets:amdSec/%s[starts-with(@ID,'%s')]", amdSecElement, idPrefix);
            NodeList techMdEls = (NodeList) engine.buildXpath(xpathStr).evaluate(metsDoc, XPathConstants.NODESET);
            for (int i = 0; i < techMdEls.getLength(); i++) {
                Element techMdEl = (Element) techMdEls.item(i);
                validate(techMdEl, metsFile, xsdFile, level, result, idPrefix.equals("OBJ_"));
            }
        } catch (XmlFileParsingException e) {
            result.addError(invalid(level, metsFile, "%s", e.getMessage()));
        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(level, metsFile, "%s", e.getMessage()));
        } catch (XPathExpressionException e) {
            result.addError(invalid(level, metsFile, "%s", e.getMessage()));
        }
    }

    private void validate(Element techMdEl, File metsFile, File xsdFile, Level level, ValidationResult result, boolean print) {
        String id = techMdEl.getAttribute("ID");
        try {
            /*if(!metsFile.getName().endsWith("1.xml")){
                return;
            }*/
            String xpathStr = "mets:mdWrap/mets:xmlData/*[1]";
            XPathExpression xPath = engine.buildXpath(xpathStr);
            Element mixEl = (Element) xPath.evaluate(techMdEl, XPathConstants.NODE);
            if (mixEl == null) {
                result.addError(invalid(level, metsFile, "%s: nenalezen element %s", id, xpathStr));
            } else {
                Document mixDoc = XmlUtils.elementToNewDocument(mixEl, true);
                /*if(print){
                    try {
                        System.out.println(XmlUtils.elementToXml(mixDoc.getDocumentElement()));
                    } catch (TransformerException e) {
                        e.printStackTrace();
                    }
                }*/
                DOMSource source = new DOMSource(mixDoc);
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                schemaFactory.setResourceResolver(new XsdImportsResourceResolver(xsdFile.getParentFile()));
                Schema schema = schemaFactory.newSchema(xsdFile);
                Validator validator = schema.newValidator();
                validator.validate(source);
            }

        } catch (InvalidXPathExpressionException e) {
            result.addError(invalid(level, metsFile, "%s: %s", id, e.getMessage()));
        } catch (XPathExpressionException e) {
            result.addError(invalid(level, metsFile, "%s: %s", id, e.getMessage()));
        } catch (ParserConfigurationException e) {
            result.addError(invalid(level, metsFile, "%s: %s", id, e.getMessage()));
        } catch (SAXException e) {
            result.addError(invalid(level, metsFile, "%s: %s", id, e.getMessage()));
        } catch (IOException e) {
            result.addError(invalid(level, metsFile, "%s: %s", id, e.getMessage()));
        }
    }

}
