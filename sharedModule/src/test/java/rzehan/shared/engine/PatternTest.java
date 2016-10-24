package rzehan.shared.engine;

import org.junit.Test;
import rzehan.shared.engine.exceptions.InvalidVariableTypeException;
import rzehan.shared.engine.exceptions.VariableNotDefinedException;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by martin on 20.10.16.
 */
public class PatternTest {


    @Test
    public void noVariableRegexpConstant() {
        Engine mgr = new Engine(new ProvidedVarsManagerImpl());

        assertTrue(mgr.newPattern(mgr.newExpression(true, "amdsec")).matches("amdsec"));
        assertTrue(mgr.newPattern(mgr.newExpression(true, "AMDSEC")).matches("AMDSEC"));

        //case sensitivity
        assertFalse(mgr.newPattern(mgr.newExpression(true, "amdsec")).matches("AMDSEC"));
        assertFalse(mgr.newPattern(mgr.newExpression(true, "AMDSEC")).matches("amdsec"));
        assertTrue(mgr.newPattern(mgr.newExpression(false, "amdsec")).matches("AMDSEC"));
        assertTrue(mgr.newPattern(mgr.newExpression(false, "AMDSEC")).matches("amdsec"));

        //more expressions per pattern
        assertTrue(mgr.newPattern(mgr.newExpression(true, "amdsec"), mgr.newExpression(true, "ocr")).matches("amdsec"));
        assertTrue(mgr.newPattern(mgr.newExpression(true, "amdsec"), mgr.newExpression(true, "ocr")).matches("ocr"));
        assertFalse(mgr.newPattern(mgr.newExpression(true, "amdsec"), mgr.newExpression(true, "ocr")).matches("alto"));
    }

    @Test
    public void noVariableRegexp() {
        Engine mgr = new Engine(new ProvidedVarsManagerImpl());
        assertTrue(mgr.newPattern(mgr.newExpression(true, "info_.+\\.xml")).matches("info_SOMETHING.xml"));
        assertFalse(mgr.newPattern(mgr.newExpression(true, "info_.+\\.xml")).matches("info_SOMETHINGxml"));
        assertFalse(mgr.newPattern(mgr.newExpression(true, "info_.+\\.xml")).matches("info_.xml"));

        assertTrue(mgr.newPattern(mgr.newExpression(true, "info\\.xml")).matches("info.xml"));
        assertFalse(mgr.newPattern(mgr.newExpression(true, "info\\.xml")).matches("info,xml"));

        assertTrue(mgr.newPattern(mgr.newExpression(true, "info_.+\\.xml"), mgr.newExpression(true, "info.xml")).matches("info.xml"));
        assertTrue(mgr.newPattern(mgr.newExpression(true, "info_.+\\.xml"), mgr.newExpression(true, "info.xml")).matches("info_SOMETHING.xml"));

        assertTrue(mgr.newPattern(mgr.newExpression(true, "[a-z_\\-\\.]+")).matches("x.y_z-a"));
        assertFalse(mgr.newPattern(mgr.newExpression(true, "[a-z_\\-\\.]+")).matches(""));
        assertTrue(mgr.newPattern(mgr.newExpression(false, "[a-z_\\-\\.]+")).matches("X.y_Z-a"));
    }


    @Test
    public void variableRegexp() {
        Engine mgr = new Engine(new ProvidedVarsManagerImpl());

        mgr.defineVariable("VAR1", new Variable("neco"));
        mgr.defineVariable("VAR2", new Variable("nic"));
        mgr.defineVariable("NUM", new Variable(Integer.valueOf(6)));
        assertTrue(mgr.newPattern(mgr.newExpression(true, "prefix_${VAR1}_suffix")).matches("prefix_neco_suffix"));
        assertTrue(mgr.newPattern(mgr.newExpression(true, "prefix_${VAR1}\\+${VAR2}_suffix")).matches("prefix_neco+nic_suffix"));

        try {
            mgr.newPattern(mgr.newExpression(true, "prefix_${VAR3}_suffix")).matches("prefix_vsechno_suffix");
            fail();
        } catch (VariableNotDefinedException e) {

        }

        try {
            mgr.newPattern(mgr.newExpression(true, "prefix_${NUM}_suffix")).matches("prefix_3_suffix");
            fail();
        } catch (InvalidVariableTypeException e) {

        }


        mgr.newPattern(mgr.newExpression(true, "prefix_${VAR1}_suffix")).matches("prefix_neco_suffix");

        mgr.defineVariable("PSP_ID", new Variable("b50eb6b0-f0a4-11e3-b72e-005056827e52"));
        assertTrue(mgr.newPattern(mgr.newExpression(true, "info_${PSP_ID}\\.xml")).matches("info_b50eb6b0-f0a4-11e3-b72e-005056827e52.xml"));
        assertTrue(mgr.newPattern(mgr.newExpression(true, "amd_mets_${PSP_ID}_[0-9]+\\.xml")).matches("amd_mets_b50eb6b0-f0a4-11e3-b72e-005056827e52_0.xml"));
        assertTrue(mgr.newPattern(mgr.newExpression(true, "amd_mets_${PSP_ID}_[0-9]+\\.xml")).matches("amd_mets_b50eb6b0-f0a4-11e3-b72e-005056827e52_123.xml"));
        assertFalse(mgr.newPattern(mgr.newExpression(true, "amd_mets_${PSP_ID}_[0-9]+\\.xml")).matches("amd_mets_b50eb6b0-f0a4-11e3-b72e-005056827e52_.xml"));

        //escaping special charachters in variables - \\.[]{}()*+-?^$|
        mgr.defineVariable("DOT", new Variable("."));
        assertTrue(mgr.newPattern(mgr.newExpression(true, "${DOT}co${DOT}uk")).matches(".co.uk"));
        assertFalse(mgr.newPattern(mgr.newExpression(true, "${DOT}co${DOT}uk")).matches("0coXuk"));
    }


}
