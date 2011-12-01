package org.mule.devkit.it.studio;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.examples.RecursiveElementNameAndTextQualifier;
import org.junit.Test;
import org.mule.util.IOUtils;

import static org.junit.Assert.assertTrue;

public class ParameterAnnotationsModuleStudioXmlTest {

    private static final String EXPECTED_STUDIO_XML = "expected-studio-xml.xml";
    private static final String ACTUAL_STUDIO_XML = "META-INF/parameter-annotations-studio.xml";

    @Test
    public void connectorModulesStudioXmlGeneration() throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        String expectedXml = IOUtils.toString(ParameterAnnotationsModuleStudioXmlTest.class.getClassLoader().getResourceAsStream(EXPECTED_STUDIO_XML));
        String actualXml = IOUtils.toString(ParameterAnnotationsModuleStudioXmlTest.class.getClassLoader().getResourceAsStream(ACTUAL_STUDIO_XML));
        Diff diff = new Diff(expectedXml, actualXml);
        diff.overrideElementQualifier(new RecursiveElementNameAndTextQualifier());
        DetailedDiff detailedDiff = new DetailedDiff(diff);
        assertTrue(detailedDiff.toString(), detailedDiff.similar());
    }
}