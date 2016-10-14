# SphinxAPI protocol adapter for JMeter TCP sampler

## How to build

    mvn package

## How to use

1. Copy `target/ApacheJMeter_searchd-1.0.jar` file to `<jmeter folder>/lib/ext/`.
2. Create a test plan with a TCP sampler.
3. Specify `org.apache.jmeter.protocol.tcp.sampler.SearchdClientImpl` in the "TCPClient classname" field.
4. Make sure "Re-use connection" is turned off and "Close connection" is turned on.
5. Specify hex-encoded SphinxAPI request payload in "Text to send" field.

Optionally, if you want the response codes to be parsed, set appropriate values for [JMeter properties](http://jmeter.apache.org/usermanual/get-started.html#configuring_jmeter):

- `tcp.status.prefix` and `tcp.status.suffix` should be some string that cannot occur in hex-encoded binary;
- `tcp.status.properties` should be the name of properties file with status interpretations.

#### For example

`./jmeter.properties` file:

    tcp.status.prefix=S
    tcp.status.suffix=T
    tcp.status.properties=./searchd-status.properties

`./searchd-status.properties` file:

    0000=SEARCHD_OK
    0001=SEARCHD_ERROR
    0002=SEARCHD_RETRY
    0003=SEARCHD_WARNING

In this case, JMeter should be run with `-p ./jmeter.properties` option.
