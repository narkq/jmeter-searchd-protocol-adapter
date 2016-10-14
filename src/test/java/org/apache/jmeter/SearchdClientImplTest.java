package org.apache.jmeter;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.util.Properties;
import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.util.JOrphanUtils;
import org.apache.jmeter.protocol.tcp.sampler.BinaryTCPClientImpl;
import org.apache.jmeter.protocol.tcp.sampler.ReadException;
import org.apache.jmeter.protocol.tcp.sampler.SearchdClientImpl;

/**
 * Unit test for simple App.
 */
public class SearchdClientImplTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public SearchdClientImplTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( SearchdClientImplTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testIfItWorksAtAll() throws IOException, ReadException
    {
        String request = loadFile("request.txt");
        String validReply = loadFile("validReply.txt");
        InputStream is = new ByteArrayInputStream(BinaryTCPClientImpl.hexStringToByteArray(validReply));
        OutputStream os = new ByteArrayOutputStream();
        SearchdClientImpl impl = new SearchdClientImpl();
        impl.write(os, request);
        assertEquals("First 2 bytes of the reply should be 0", "0000", impl.read(is).substring(0, 4));
        assertEquals("Input stream has remaining bytes", 0, is.available());
    }

    private String loadFile(String path) throws IOException
    {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(path).getFile());
        return new Scanner(file).useDelimiter("\\Z").next();
    }
}
