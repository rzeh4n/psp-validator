package nkp.pspValidator.shared.metadataProfile;

import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.XmlManager;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;
import org.w3c.dom.*;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Martin Řehánek on 10.1.17.
 */
public class MetadataProfileValidator {

    public static ValidationResult validate(MetadataProfile metadataProfile, File metadataFile, Document doc, ValidationResult result, String errorLabel) throws InvalidXPathExpressionException, XPathExpressionException {
        XmlManager manager = new XmlManager(false);
        for (String prefix : metadataProfile.getNamespaces().keySet()) {
            String url = metadataProfile.getNamespaces().get(prefix);
            manager.setNamespaceUri(prefix, url);
        }
        ExpectedElementDefinition rootElDef = metadataProfile.getRootElementDefinition();
        String rootElXpath = buildElementPath(null, rootElDef.buildRelativeXpath(), null);
        XPathExpression rootElXpathExpr = manager.buildXpath(rootElXpath);
        NodeList biblioRootEls = (NodeList) rootElXpathExpr.evaluate(doc.getDocumentElement(), XPathConstants.NODESET);
        if (biblioRootEls.getLength() == 0) {
            result.addError(Level.ERROR, metadataFile, ErrorMessage.from(errorLabel, "nenalezen kořenový element %s:%s", rootElDef.getElementNameNsPrefix(), rootElDef.getElementName()).build());
        } else if (biblioRootEls.getLength() > 1) {
            result.addError(Level.ERROR, metadataFile, ErrorMessage.from(errorLabel, "nalezeno více kořenových elementů %s:%s", rootElDef.getElementNameNsPrefix(), rootElDef.getElementName()).build());
        } else { //continue - single root element
            Element biblioRootEl = (Element) biblioRootEls.item(0);
            checkElement(manager, metadataFile, result, biblioRootEl, rootElDef, null, null, errorLabel);
        }
        return result;
    }

    private static void checkElement(XmlManager manager, File metadataFile, ValidationResult result, Element element, ExpectedElementDefinition definition, String parentPath, Integer positionOfElement, String errorLabel) throws InvalidXPathExpressionException, XPathExpressionException {
        String thisElementPath = buildElementPath(parentPath, definition.buildRelativeXpath(), positionOfElement);
        //check element's attributes
        checkAttributes(metadataFile, result, thisElementPath, definition.getExpectedAttributes(), definition.isIgnoreUnexpectedAttributes(), element.getAttributes(), errorLabel);
        //check element's children elements
        checkChildrenElements(manager, metadataFile, result, element, thisElementPath, definition.getExpectedChildElementDefinitions(), definition.isIgnoreUnexpectedChildElements(), XmlUtils.getChildrenElements(element), errorLabel);
        //check element's text content
        if (definition.getExpectedContentDefinition() != null) {//expected content
            String content = XmlUtils.getDirectTextContent(element);
            CheckingResult checkingResult = definition.getExpectedContentDefinition().checkAgainst(content);
            if (!checkingResult.matches()) {
                result.addError(Level.ERROR, metadataFile, ErrorMessage.from(errorLabel, "%s: %s", thisElementPath, checkingResult.getErrorMessage()).build());
            }
        } else if (definition.getRecommendedContentDefinition() != null) {//recommended content
            String content = XmlUtils.getDirectTextContent(element);
            CheckingResult checkingResult = definition.getRecommendedContentDefinition().checkAgainst(content);
            if (!checkingResult.matches()) {
                result.addError(Level.WARNING, metadataFile, ErrorMessage.from(errorLabel, "%s: %s", thisElementPath, checkingResult.getErrorMessage()).build());
            }
        }
        //check element's extra rules
        checkExtraRules(manager, metadataFile, result, element, thisElementPath, definition.getExtraRules(), errorLabel);
    }

    private static void checkExtraRules(XmlManager manager, File metadataFile, ValidationResult result, Element currentElement, String currentElementPath, List<ExtraRule> extraRules, String errorLabel) {
        for (ExtraRule rule : extraRules) {
            CheckingResult checkingResult = rule.checkAgainst(manager, currentElement);
            if (!checkingResult.matches()) {
                result.addError(Level.ERROR, metadataFile, ErrorMessage.from(errorLabel, "%s: %s", currentElementPath, checkingResult.getErrorMessage()).build());
            }
        }
    }

    private static void checkAttributes(File metadataFile, ValidationResult result, String parentElementPath, List<ExpectedAttributeDefinition> attrDefs, boolean ignoreUnexpectedAttributes, NamedNodeMap foundAttrs, String errorLabel) {
        Set<Integer> positionsOfUsedAttrs = new HashSet<>();
        for (ExpectedAttributeDefinition attrDef : attrDefs) {
            boolean found = false;
            for (int i = 0; i < foundAttrs.getLength(); i++) {
                Attr attr = (Attr) foundAttrs.item(i);
                if (attr.getName().equals(attrDef.getAttributeName())) {
                    //check whether actual attribute value matches expected value
                    checkAttributeValue(metadataFile, result, parentElementPath, attr.getName(), attr.getValue(),
                            attrDef.getExpectedContentDefinition(), attrDef.getRecommendedContentDefinition(),
                            errorLabel);
                    positionsOfUsedAttrs.add(i);
                    found = true;
                    break;
                }
            }
            //defined attribute not found
            if (!found) {
                if (attrDef.isMandatory()) { //ERROR if mandatory
                    result.addError(Level.ERROR, metadataFile, ErrorMessage.from(errorLabel, "%s: nenalezen povinný atribut '%s'", parentElementPath, attrDef.getAttributeName()).build());
                } else { //INFO if not mandatory
                    result.addError(Level.INFO, metadataFile, ErrorMessage.from(errorLabel, "%s: nenalezen doporučený atribut '%s'", parentElementPath, attrDef.getAttributeName()).build());
                }
            }
        }
        //WARNING for all undefined attributes
        for (int i = 0; i < foundAttrs.getLength(); i++) {
            if (!positionsOfUsedAttrs.contains(i)) {
                Attr attr = (Attr) foundAttrs.item(i);
                if (!attr.getName().startsWith("xmlns:") && !ignoreUnexpectedAttributes) { //ignore namespace definitions
                    result.addError(Level.WARNING, metadataFile, ErrorMessage.from(errorLabel, "%s: nalezen neočekávaný atribut '%s'", parentElementPath, attr.getName()).build());
                }
            }
        }
    }

    private static void checkAttributeValue(File metadataFile, ValidationResult result, String parentElementPath, String attrName, String attrValue,
                                            ContentDefinition attrExpectedContent, ContentDefinition attrRecommendedContent,
                                            String errorLabel) {
        if (attrExpectedContent != null) {
            CheckingResult checkingResult = attrExpectedContent.checkAgainst(attrValue);
            if (!checkingResult.matches()) {
                result.addError(Level.ERROR, metadataFile, ErrorMessage.from(errorLabel, "%s/@%s: %s", parentElementPath, attrName, checkingResult.getErrorMessage()).build());
            }
        } else if (attrRecommendedContent != null) {
            CheckingResult checkingResult = attrRecommendedContent.checkAgainst(attrValue);
            if (!checkingResult.matches()) {
                result.addError(Level.WARNING, metadataFile, ErrorMessage.from(errorLabel, "%s/@%s: %s", parentElementPath, attrName, checkingResult.getErrorMessage()).build());
            }
        }
    }

    private static String buildElementPath(String parentPath, String relativePath, Integer positionInRoot) {
        StringBuilder builder = new StringBuilder();
        if (parentPath != null) {
            builder.append(parentPath);
        }
        builder.append('/').append(relativePath);
        if (positionInRoot != null) {
            builder.append('[').append(positionInRoot).append(']');
        }
        return builder.toString();
    }

    private static void checkChildrenElements(XmlManager manager, File metadataFile, ValidationResult result, Element rootElement, String parentElementPath, List<ExpectedElementDefinition> expectedElementDefinitions, boolean ignoreUnexpectedElements, List<Element> chilrenElements, String errorLabel) throws InvalidXPathExpressionException, XPathExpressionException {
        Set<Element> childrenRemaining = new HashSet<>();
        childrenRemaining.addAll(chilrenElements);
        for (ExpectedElementDefinition elDef : expectedElementDefinitions) {
            String xpath = elDef.buildRelativeXpath();
            XPathExpression xPathExpression = manager.buildXpath(xpath);
            NodeList foundElementsByXpath = (NodeList) xPathExpression.evaluate(rootElement, XPathConstants.NODESET);
            boolean found = false;
            for (int i = 0; i < foundElementsByXpath.getLength(); i++) {
                Element foundElementByXpath = (Element) foundElementsByXpath.item(i);
                if (childrenRemaining.contains(foundElementByXpath)) { //i.e. actual element has not yet been consumed by another xpath
                    if (!elDef.isRepeatable() && found) {
                        result.addError(Level.ERROR, metadataFile, ErrorMessage.from(errorLabel, "%s: nalezeno více výskytů neopakovatelného elementu '%s'", parentElementPath, elDef.buildRelativeXpath()).build());
                        childrenRemaining.remove(foundElementByXpath); //consume
                    } else {
                        found = true;
                        Integer position = foundElementsByXpath.getLength() == 1 ? null : i;
                        checkElement(manager, metadataFile, result, foundElementByXpath, elDef, parentElementPath, position, errorLabel);
                        childrenRemaining.remove(foundElementByXpath); //consume
                    }
                } else {
                    if (!found) { //element not found
                        if (elDef.isMandatory()) { //ERROR if mandatory
                            result.addError(Level.ERROR, metadataFile, ErrorMessage.from(errorLabel, "%s: nenalezen očekávaný povinný element '%s'", parentElementPath, elDef.buildRelativeXpath())
                                    .withCustomMessage(elDef.getErrorMessage())
                                    .build());
                        } else { //INFO if not mandatory
                            result.addError(Level.INFO, metadataFile, ErrorMessage.from(errorLabel, "%s: nenalezen očekávaný doporučený element '%s'", parentElementPath, elDef.buildRelativeXpath())
                                    .withCustomMessage(elDef.getErrorMessage())
                                    .build());
                        }
                    }
                }
            }
            if (foundElementsByXpath.getLength() == 0) { //element not found
                if (elDef.isMandatory()) { //ERROR if mandatory
                    result.addError(Level.ERROR, metadataFile, ErrorMessage.from(errorLabel, "%s: nenalezen očekávaný povinný element '%s'", parentElementPath, elDef.buildRelativeXpath())
                            .withCustomMessage(elDef.getErrorMessage())
                            .build());
                } else { //INFO if not mandatory
                    result.addError(Level.INFO, metadataFile, ErrorMessage.from(errorLabel, "%s: nenalezen očekávaný doporučený element '%s'", parentElementPath, elDef.buildRelativeXpath())
                            .withCustomMessage(elDef.getErrorMessage())
                            .build());
                }
            }
        }
        //unexpected element, warning
        for (Element element : childrenRemaining) {
            if (!ignoreUnexpectedElements) {
                result.addError(Level.WARNING, metadataFile, ErrorMessage.from(errorLabel, "%s: nalezen neočekávaný element '%s' %s", parentElementPath, element.getTagName(), buildAttributeList(element))
                        .build());
            }
        }
    }

    private static String buildAttributeList(Element element) {
        NamedNodeMap attributes = element.getAttributes();
        if (attributes.getLength() > 0) {
            StringBuilder builder = new StringBuilder();
            builder.append('(');
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attr = (Attr) attributes.item(i);
                if (i != 0 && i == attributes.getLength() - 1) {
                    builder.append(", ");
                }
                builder.append(attr.getName()).append("=\"").append(attr.getValue()).append('\"');
            }
            builder.append(')');
            return builder.toString();
        } else {
            return "";
        }
    }

    static class ErrorMessage {
        private final String errorLabel;
        private final String messagePattern;
        private final Object[] messageParams;
        private String customMessage;

        private ErrorMessage(String errorLabel, String messagePattern, Object... messageParams) {
            this.errorLabel = errorLabel;
            this.messagePattern = messagePattern;
            this.messageParams = messageParams;
        }

        public static ErrorMessage from(String errorLabel, String messagePattern, Object... messageParams) {
            return new ErrorMessage(errorLabel, messagePattern, messageParams);
        }

        public ErrorMessage withCustomMessage(String customMessage) {
            this.customMessage = customMessage;
            return this;
        }

        public String build() {
            String prefix = errorLabel != null ? errorLabel + ": " : "";
            if (customMessage != null && !customMessage.trim().isEmpty()) {
                return String.format(prefix + messagePattern, messageParams) + ": " + customMessage;
            } else {
                return String.format(prefix + messagePattern, messageParams);
            }
        }
    }

}
