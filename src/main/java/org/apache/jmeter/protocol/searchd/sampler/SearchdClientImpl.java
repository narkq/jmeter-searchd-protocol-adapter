/*
 * SphinxAPI protocol implementation for TCP Sampler Client.
 *
 * Input/Output strings are passed as hex-encoded binary strings.
 *
 */

package org.apache.jmeter.protocol.tcp.sampler;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JOrphanUtils;
import org.apache.log.Logger;

/**
 * SphinxAPI Client implementation.
 * Reads data acctording to Sphinx binary protocol.
 * The EOM byte is NOT USED.
 *
 * Input data is assumed to be in hex, and is converted to binary
 */
public class SearchdClientImpl extends TCPClientDecorator {

    private static final Logger log = LoggingManager.getLoggerForClass();

    private static final String STATUS_PREFIX = JMeterUtils.getPropDefault("tcp.status.prefix", "");

    private static final String STATUS_SUFFIX = JMeterUtils.getPropDefault("tcp.status.suffix", "");

    private InputStream currentInputStream = null;

    private OutputStream currentOutputStream = null;

    private String currentQuery = null;

    public SearchdClientImpl() {
        super(new BinaryTCPClientImpl());
        tcpClient.setEolByte(Byte.MAX_VALUE+1);
    }

    /**
     * Pretends to be writing the data.
     *
     * Should be writing to output stream, but saves its arguments for later instead.
     * Will work only if TCPSampler calls read() right after write() and never reuses connections.
     * Actual writing logic is implemented in doQuery().
     *
     * God help us.
     */
    public void write(OutputStream os, String s) {
        currentOutputStream = os;
        currentQuery = s;
    }

    /**
     * {@inheritDoc}
     */
    public void write(OutputStream os, InputStream is) throws IOException {
        this.tcpClient.write(os, is);
    }

    /**
     * Pretends to be reading the data.
     *
     * Will work only if TCPSampler calls read() right after write() and never reuses connections.
     * Actual reading logic is implemented in doQuery().
     *
     * @return hex-encoded binary string
     * @throws ReadException 
     */
    public String read(InputStream is) throws ReadException {
        currentInputStream = is;
        try {
            return doQuery();
        }
        finally {
            currentInputStream = null;
            currentOutputStream = null;
            currentQuery = null;
        }
    }

    /**
     * Actual implementation of reading searchd response.
     * @return hex-encoded binary string
     * @throws ReadException 
     */
    private String doQuery() throws ReadException {
        int greetingLen = 4;
        int msgLen = 0;
        int msgLenOffset = 4;
        int headerLen = 8;
        int statusLen = 2;
        int bytesRead = -1;
        String requestHeader = "00000001";
        String status = null;
        String msg = null;
        byte[] msgBuffer = new byte[0];
        byte[] greetingBuffer = new byte[greetingLen];
        byte[] headerBuffer = new byte[headerLen];
        try {
            this.tcpClient.write(currentOutputStream, requestHeader);
            bytesRead = currentInputStream.read(greetingBuffer, 0, greetingLen);
            String greeting = JOrphanUtils.baToHexString(greetingBuffer);
            if (log.isDebugEnabled()) {
                log.debug("Read greeting: " + greeting);
            }
            if (bytesRead != greetingLen) {
                throw new ReadException("", new Error("Could not read greeting"), greeting);
            }
            this.tcpClient.write(currentOutputStream, currentQuery);
            bytesRead = currentInputStream.read(headerBuffer, 0, headerLen);
            String header = JOrphanUtils.baToHexString(headerBuffer);
            if (log.isDebugEnabled()) {
                log.debug("Read header: " + header);
            }
            if (bytesRead != headerLen) {
                throw new ReadException("", new Error("Could not read reply header"), header);
            } else {
                status = JOrphanUtils.baToHexString(Arrays.copyOfRange(headerBuffer, 0, statusLen));
                msgLen = byteArrayToInt(Arrays.copyOfRange(headerBuffer, msgLenOffset, headerLen));
                msgBuffer = new byte[msgLen];
                bytesRead = JOrphanUtils.read(currentInputStream, msgBuffer, 0, msgLen);
                msg = JOrphanUtils.baToHexString(msgBuffer);
                if (bytesRead < msgLen) {
                    throw new ReadException("", new Error("Incomplete message read, expected " + msgLen + " bytes, got " + bytesRead + " bytes"), msg);
                }
            }
    
            if (log.isDebugEnabled()) {
                log.debug("Read reply message: status " + status + ", length " + msgLen);
            }
            return STATUS_PREFIX + status + STATUS_SUFFIX + msg;
        } 
        catch(IOException e) {
            throw new ReadException("", e, JOrphanUtils.baToHexString(msgBuffer));
        }
    }

    /**
     * Not useful, as the byte is never used.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public byte getEolByte() {
        return tcpClient.getEolByte();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEolByte(int eolInt) {
        throw new UnsupportedOperationException("Cannot set eomByte for prefixed messages");
    }
}
