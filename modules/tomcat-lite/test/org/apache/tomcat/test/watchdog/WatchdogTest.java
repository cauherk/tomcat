/*
 */
package org.apache.tomcat.test.watchdog;

import java.util.Properties;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.tomcat.util.IntrospectionUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class WatchdogTest extends TestCase {
    String testName;

    Element watchE;

    private Properties props;

    private WatchdogTest delegate;
    private WatchdogClient wc;
    
    public WatchdogTest(String s) throws Throwable {
        String[] comp = s.split(";");
        Class c = Class.forName(comp[1]);
        wc = (WatchdogClient) c.newInstance();
        TestSuite suite = (TestSuite) wc.getSuite();
        // need to encode the base, file, etc in the test name

        System.err.println(s);

        for (int i = 0; i < suite.testCount(); i++) {
            WatchdogTest t = (WatchdogTest) suite.testAt(i);
            if (s.equals(t.getName())) {
                delegate = t;
                return;
            }
        }
    }

    public WatchdogTest(Element watchE, Properties props, String testName) {
        this.testName = testName;
        this.watchE = watchE;
        this.props = props;
    }

    public int countTestCases() {
        return 1;
    }

    public String getName() {
        return testName;
    }

    public void run(TestResult res) {
        if (delegate != null) {
            // Single method run
            wc.beforeSuite();
            delegate.run(res);
            wc.afterSuite(res);
            return;
        }
        GTest test = new GTest();
        NamedNodeMap attrs = watchE.getAttributes();

        for (int i = 0; i < attrs.getLength(); i++) {
            Node n = attrs.item(i);
            String name = n.getNodeName();
            String value = n.getNodeValue();
            value = IntrospectionUtils.replaceProperties(value, props, null);
            try {
                IntrospectionUtils.setProperty(test, name, value);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        try {
            res.startTest(this);
            IntrospectionUtils.execute(test, "execute");
        } catch (Throwable e) {
            res.addError(this, e);
            // res.stop();
        }

        if (test.passCount == 1) {
            res.endTest(this);
            return;
        } else {
            if (test.lastError == null) {
                res.addFailure(this, new AssertionFailedError(test.request
                        + " " + test.description + "\n" + test.resultOut));
            } else {
                res.addError(this, test.lastError);
            }
        }
        res.endTest(this);
    }

}
