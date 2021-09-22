package io.github.darrensmithwtc.jmeter.backendlistener.newrelic;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.MetricBatchSenderFactory;
import com.newrelic.telemetry.OkHttpPoster;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import com.newrelic.telemetry.metrics.MetricBuffer;

public class NewRelicBackendClient extends AbstractBackendListenerClient {

    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(NewRelicBackendClient.class);

    /**
     * Argument keys.
     */
    private static final String KEY_TEST_NAME = "testName";
    private static final String KEY_CONNECTION_STRING = "connectionString";
    private static final String KEY_LICENCE_KEY = "licenceKey";
    private static final String KEY_SAMPLERS_LIST = "samplersList";
    private static final String KEY_USE_REGEX_FOR_SAMPLER_LIST = "useRegexForSamplerList";
    private static final String KEY_CUSTOM_PROPERTIES_PREFIX = "ai.";
    private static final String KEY_HEADERS_PREFIX = "aih.";
    private static final String KEY_RESPONSE_HEADERS = "responseHeaders";
    private static final String KEY_METRIC_BATCH_SIZE = "metricBatchSize";

    /**
     * Default argument values.
     */
    private static final String DEFAULT_TEST_NAME = "jmeter";
    private static final String DEFAULT_CONNECTION_STRING = "https://metric-api.eu.newrelic.com/metric/v1";
    private static final String DEFAULT_SAMPLERS_LIST = "";
    private static final boolean DEFAULT_USE_REGEX_FOR_SAMPLER_LIST = false;
    private static final String DEFAULT_METRIC_BATCH_SIZE = "10";

    /**
     * Separator for samplers list.
     */
    private static final String SEPARATOR = ";";

    /**
     * Name of the test.
     */
    private String testName;

    /**
     * Licence Key
     */
    private String licenseKey;

    /**
     * Metric BAtch Size
     */
    private Integer metricBatchSize;

    /**
     * Metric Batch Counter
     */
    private Integer metricCount = 0;

    /**
     * Custom properties.
     */
    private Attributes customProperties = new Attributes();

    /**
     * Recording response headers.
     */
    private String[] responseHeaders = {};

    /**
     * List of samplers to record.
     */
    private String samplersList = "";

    /**
     * Regex if samplers are defined through regular expression.
     */
    private Boolean useRegexForSamplerList;

    /**
     * Set of samplers to record.
     */
    private Set<String> samplersToFilter;

    /**
     * New Relic Factory
     */
    private MetricBatchSenderFactory factory;

    /**
     * New Relic Sender
     */
    private MetricBatchSender sender;

    /**
     * New Relic Metric Buffer
     */
    private MetricBuffer metricBuffer;

    public NewRelicBackendClient() {
        super();
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();
        arguments.addArgument(KEY_TEST_NAME, DEFAULT_TEST_NAME);
        arguments.addArgument(KEY_CONNECTION_STRING, DEFAULT_CONNECTION_STRING);
        arguments.addArgument(KEY_LICENCE_KEY, "");
        arguments.addArgument(KEY_SAMPLERS_LIST, DEFAULT_SAMPLERS_LIST);
        arguments.addArgument(KEY_USE_REGEX_FOR_SAMPLER_LIST, Boolean.toString(DEFAULT_USE_REGEX_FOR_SAMPLER_LIST));
        arguments.addArgument(KEY_METRIC_BATCH_SIZE, DEFAULT_METRIC_BATCH_SIZE);

        return arguments;
    }

    @Override
    public void setupTest(BackendListenerContext context) throws Exception {
        testName = context.getParameter(KEY_TEST_NAME, DEFAULT_TEST_NAME);
        metricBatchSize = Integer.parseInt(context.getParameter(KEY_METRIC_BATCH_SIZE, DEFAULT_METRIC_BATCH_SIZE));
        licenseKey = context.getParameter(KEY_LICENCE_KEY);
        samplersList = context.getParameter(KEY_SAMPLERS_LIST, DEFAULT_SAMPLERS_LIST).trim();
        String connectionString = context.getParameter(KEY_CONNECTION_STRING);
        useRegexForSamplerList = context.getBooleanParameter(KEY_USE_REGEX_FOR_SAMPLER_LIST,
                DEFAULT_USE_REGEX_FOR_SAMPLER_LIST);

        Iterator<String> iterator = context.getParameterNamesIterator();
        while (iterator.hasNext()) {
            String paramName = iterator.next();
            if (paramName.startsWith(KEY_CUSTOM_PROPERTIES_PREFIX)) {
                customProperties.put(paramName, context.getParameter(paramName));
            } else if (paramName.equals(KEY_RESPONSE_HEADERS)) {
                responseHeaders = context.getParameter(KEY_RESPONSE_HEADERS).trim().toLowerCase()
                        .split("\\s*".concat(SEPARATOR).concat("\\s*"));
            }
        }

        factory = MetricBatchSenderFactory.fromHttpImplementation(OkHttpPoster::new);

        java.net.URL endpoint = new java.net.URL(connectionString);

        sender = MetricBatchSender
                .create(factory.configureWith(licenseKey).useLicenseKey(true).endpoint(endpoint).build());

        metricBuffer = new MetricBuffer(getCommonAttributes());

        samplersToFilter = new HashSet<String>();
        if (!useRegexForSamplerList) {
            String[] samplers = samplersList.split(SEPARATOR);
            samplersToFilter = new HashSet<String>();
            for (String samplerName : samplers) {
                samplersToFilter.add(samplerName);
            }
        }
    }

    private Attributes getCommonAttributes() {
        Attributes defaultProperties = new Attributes();
        defaultProperties.put("ingestProvider", "JMETER");
        return defaultProperties;
    }

    private void trackRequest(String name, SampleResult sr) {

        metricCount++;

        Attributes properties = new Attributes();
        properties.putAll(customProperties);
        properties.put("Bytes", Long.toString(sr.getBytesAsLong()));
        properties.put("SentBytes", Long.toString(sr.getSentBytes()));
        properties.put("ConnectTime", Long.toString(sr.getConnectTime()));
        properties.put("ErrorCount", Integer.toString(sr.getErrorCount()));
        properties.put("IdleTime", Double.toString(sr.getIdleTime()));
        properties.put("Latency", Double.toString(sr.getLatency()));
        properties.put("BodySize", Long.toString(sr.getBodySizeAsLong()));
        properties.put("TestStartTime", Long.toString(JMeterContextService.getTestStartTime()));
        properties.put("SampleStartTime", Long.toString(sr.getStartTime()));
        properties.put("SampleEndTime", Long.toString(sr.getEndTime()));
        properties.put("SampleLabel", sr.getSampleLabel());
        properties.put("ThreadName", sr.getThreadName());
        properties.put("URL", sr.getUrlAsString());
        properties.put("ResponseCode", sr.getResponseCode());
        properties.put("GrpThreads", Integer.toString(sr.getGroupThreads()));
        properties.put("AllThreads", Integer.toString(sr.getAllThreads()));
        properties.put("SampleCount", Integer.toString(sr.getSampleCount()));
        properties.put("TestName", name);

        for (String header : responseHeaders) {
            Pattern pattern = Pattern.compile("^".concat(header).concat(":(.*)$"),
                    Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(sr.getResponseHeaders());
            if (matcher.find()) {
                properties.put(KEY_HEADERS_PREFIX.concat(header), matcher.group(1).trim());
            }
        }

        Long duration = sr.getTime();

        Gauge metric = new Gauge("wtc.loadtest.result", duration, System.currentTimeMillis(), properties);

        metricBuffer.addMetric(metric);

        if (metricCount.equals(metricBatchSize)) {
            try {
                sender.sendBatch(metricBuffer.createBatch());
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            metricCount = 0;
        }
    }

    @Override
    public void handleSampleResults(List<SampleResult> results, BackendListenerContext context) {

        boolean samplersToFilterMatch;
        for (SampleResult sr : results) {

            samplersToFilterMatch = samplersList.isEmpty()
                    || (useRegexForSamplerList && sr.getSampleLabel().matches(samplersList))
                    || (!useRegexForSamplerList && samplersToFilter.contains(sr.getSampleLabel()));

            if (samplersToFilterMatch) {
                trackRequest(testName, sr);
            }
        }
    }

    @Override
    public void teardownTest(BackendListenerContext context) throws Exception {
        samplersToFilter.clear();
        sender.sendBatch(metricBuffer.createBatch());
        super.teardownTest(context);
    }
}
