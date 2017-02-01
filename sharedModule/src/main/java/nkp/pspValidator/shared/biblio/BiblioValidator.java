package nkp.pspValidator.shared.biblio;

import nkp.pspValidator.shared.XmlUtils;
import nkp.pspValidator.shared.biblio.biblioValidator.*;
import nkp.pspValidator.shared.engine.Level;
import nkp.pspValidator.shared.engine.XmlManager;
import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.validationFunctions.ValidationResult;
import org.w3c.dom.*;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Martin Řehánek on 10.1.17.
 */
public class BiblioValidator {

    public static ValidationResult validate(BiblioTemplate biblioTemplate, Document doc, ValidationResult result, String errorLabel) throws InvalidXPathExpressionException, XPathExpressionException {
        XmlManager manager = new XmlManager(false);
        for (String prefix : biblioTemplate.getNamespaces().keySet()) {
            String url = biblioTemplate.getNamespaces().get(prefix);
            manager.setNamespaceUri(prefix, url);
        }
        ExpectedElementDefinition rootElDef = biblioTemplate.getRootElementDefinition();
        String rootElXpath = buildElementPath(null, rootElDef.buildRelativeXpath(), null);
        XPathExpression rootElXpathExpr = manager.buildXpath(rootElXpath);
        NodeList biblioRootEls = (NodeList) rootElXpathExpr.evaluate(doc.getDocumentElement(), XPathConstants.NODESET);
        if (biblioRootEls.getLength() == 0) {
            result.addError(Level.ERROR, buildMessage(errorLabel, "nenalezen kořenový element %s:%s", rootElDef.getElementNameNsPrefix(), rootElDef.getElementName()));
        } else if (biblioRootEls.getLength() > 1) {
            result.addError(Level.ERROR, buildMessage(errorLabel, "nalezeno více kořenových elementů %s:%s", rootElDef.getElementNameNsPrefix(), rootElDef.getElementName()));
        } else { //continue - single root element
            Element biblioRootEl = (Element) biblioRootEls.item(0);
            checkElement(manager, result, biblioRootEl, rootElDef, null, null, errorLabel);
        }
        return result;
    }

    private static String buildMessage(String label, String message, Object... params) {
        return String.format(label + ": " + message, params);
    }

    private static void checkElement(XmlManager manager, ValidationResult result, Element element, ExpectedElementDefinition definition, String parentPath, Integer positionOfElement, String errorLabel) throws InvalidXPathExpressionException, XPathExpressionException {
        String thisElementPath = buildElementPath(parentPath, definition.buildRelativeXpath(), positionOfElement);
        //check element's attributes
        checkAttributes(result, thisElementPath, definition.getExpectedAttributes(), element.getAttributes(), errorLabel);
        //check element's children elements
        checkChildrenElements(manager, result, element, thisElementPath, definition.getExpectedElementDefinitions(), XmlUtils.getChildrenElements(element), errorLabel);
        //check element's text content
        if (definition.getExpectedContentDefinition() != null) {
            checkContent(manager, result, element, thisElementPath, definition.getExpectedContentDefinition(), errorLabel);
        }
        //check element's extra rules
        checkExtraRules(manager, result, element, thisElementPath, definition.getExtraRules(), errorLabel);
    }

    private static void checkExtraRules(XmlManager manager, ValidationResult result, Element currentElement, String currentElementPath, List<ExtraRule> extraRules, String errorLabel) {
        for (ExtraRule rule : extraRules) {
            CheckingResult checkingResult = rule.checkAgainst(manager, currentElement);
            if (!checkingResult.matches()) {
                result.addError(Level.ERROR, buildMessage(errorLabel, "%s: nesplněno extra pravidlo: %s", currentElementPath, checkingResult.getErrorMessage()));
            }
        }
    }

    private static void checkAttributes(ValidationResult result, String parentElementPath, List<ExpectedAttributeDefinition> attrDefs, NamedNodeMap foundAttrs, String errorLabel) {
        Set<Integer> positionsOfUsedAttrs = new HashSet<>();
        for (ExpectedAttributeDefinition attrDef : attrDefs) {
            boolean found = false;
            for (int i = 0; i < foundAttrs.getLength(); i++) {
                Attr attr = (Attr) foundAttrs.item(i);
                if (attr.getName().equals(attrDef.getAttributeName())) {
                    //check whether actual attribute value matches expected value
                    checkAttributeValue(result, parentElementPath, attr.getName(), attr.getValue(), attrDef.getExpectedContentDefinition(), errorLabel);
                    positionsOfUsedAttrs.add(i);
                    found = true;
                    break;
                }
            }
            //attribute for definition not found
            if (!found) {
                if (attrDef.isMandatory()) { //ERROR if mandatory
                    result.addError(Level.ERROR, buildMessage(errorLabel, "%s: nenalezen povinný atribut '%s'", parentElementPath, attrDef.getAttributeName()));
                } else { //ignore if not mandatory
                    //possibly problem INFO
                }
            }
        }
        //WARNING for all not defined attributes
        for (int i = 0; i < foundAttrs.getLength(); i++) {
            if (!positionsOfUsedAttrs.contains(i)) {
                Attr attr = (Attr) foundAttrs.item(i);
                if (!attr.getName().startsWith("xmlns:")) { //ignore namespace definitions
                    result.addError(Level.WARNING, buildMessage(errorLabel, "%s: nalezen neočekávaný atribut '%s'", parentElementPath, attr.getName()));
                }
            }
        }
    }

    private static void checkAttributeValue(ValidationResult result, String parentElementPath, String attrName, String attrValue, ExpectedContentDefinition attrExpetedContent, String errorLabel) {
        if (attrExpetedContent != null) {
            CheckingResult checkingResult = attrExpetedContent.checkAgainst(attrValue);
            if (!checkingResult.matches()) {
                result.addError(Level.ERROR, buildMessage(errorLabel, "%s/@%s: %s", parentElementPath, attrName, checkingResult.getErrorMessage()));
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

    private static void checkContent(XmlManager manager, ValidationResult result, Element element, String parentElementPath, ExpectedContentDefinition definition, String errorLabel) throws InvalidXPathExpressionException, XPathExpressionException {
        String content = XmlUtils.getDirectTextContent(element);
        CheckingResult checkingResult = definition.checkAgainst(content);
        if (!checkingResult.matches()) {
            result.addError(Level.ERROR, buildMessage(errorLabel, "%s: %s", parentElementPath, checkingResult.getErrorMessage()));
        }
    }

    private static void checkChildrenElements(XmlManager manager, ValidationResult result, Element rootElement, String parentElementPath, List<ExpectedElementDefinition> expectedElementDefinitions, List<Element> chilrenElements, String errorLabel) throws InvalidXPathExpressionException, XPathExpressionException {
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
                        result.addError(Level.ERROR, buildMessage(errorLabel, "%s: nalezeno více výskytů neopakovatelného elementu '%s'", parentElementPath, elDef.buildRelativeXpath()));
                        childrenRemaining.remove(foundElementByXpath); //consume
                    } else {
                        found = true;
                        Integer position = foundElementsByXpath.getLength() == 1 ? null : i;
                        checkElement(manager, result, foundElementByXpath, elDef, parentElementPath, position, errorLabel);
                        childrenRemaining.remove(foundElementByXpath); //consume
                    }
                } else { //not found
                    if (elDef.isMandatory() && !found) {
                        result.addError(Level.ERROR, buildMessage(errorLabel, "%s: nenalezen očekávaný povinný element '%s'", parentElementPath, elDef.buildRelativeXpath()));
                    }
                }
            }
            if (foundElementsByXpath.getLength() == 0 && elDef.isMandatory()) { //mandatory element not found
                result.addError(Level.ERROR, String.format("%s: nenalezen očekávaný povinný element '%s'", parentElementPath, elDef.buildRelativeXpath()));
            }
        }
        //unexpected element, warning
        for (Element element : childrenRemaining) {
            result.addError(Level.WARNING, buildMessage(errorLabel, "%s: nalezen neočekávaný element '%s' %s", parentElementPath, element.getTagName(), buildAttributeList(element)));
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

}
