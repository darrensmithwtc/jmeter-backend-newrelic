# jmeter-backend-newrelic

[![CI](https://github.com/darrensmithwtc/jmeter-backend-newrelic/actions/workflows/maven.yml/badge.svg)](https://github.com/darrensmithwtc/jmeter-backend-newrelic/actions/workflows/maven.yml)

A JMeter plug-in that enables you to send test results to New Relic Metrics API.

## Overview

### Description

JMeter Backend New Relic is a JMeter plugin enabling you to send test results to NRDB via the New Relic Metrics API.

The following test results metrics are exposed by the plugin.

- TestStartTime
- SampleStartTime
- SampleEndTime
- ResponseCode
- Duration
- URL
- SampleLabel
- SampleCount
- ErrorCount
- Bytes
- SentBytes
- ConnectTime
- IdleTime
- ThreadName
- GrpThreads
- AllThreads
- (Optional) aih.{response_header}

### Plugin installation

Once you have built or downloaded the plugin JAR file from the [releases](https://github.com/darrensmithwtc/jmeter-backend-newrelic/releases) section,
move the JAR to your `$JMETER_HOME/lib/ext`.

```bash
mv target/jmeter.backendlistener.newrelic-VERSION.jar $JMETER_HOME/lib/ext/
```

Then, restart JMeter and the plugin should be loaded.

### JMeter configuration

To make JMeter send test result metrics to New Relic, in your **Test Pane**, right click on
**Thread Group** > Add > Listener > Backend Listener, and choose `io.github.darrensmithwtc.jmeter.backendlistener.newrelic.NewRelicBackendClient` as `Backend Listener Implementation`.
Then, in the Parameters table, configure the following attributes.

| Attribute                | Description                                                                                                                                                                                                                                         | Required |
| ------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| _connectionString_       | The fully qualified URL [endpoint](https://docs.newrelic.com/docs/using-new-relic/welcome-new-relic/get-started/our-eu-us-region-data-centers/) of the New Relic Metrics Provider.                                                                  | Yes      |
| _testName_               | Name of the test. This value is used to differentiate metrics across test runs or plans in New Relic and allow you to filter them.                                                                                                                  | Yes      |
| _licenceKey_             | String containing your new relic ingestion key                                                                                                                                                                                                      | Yes      |
| _samplersList_           | Optional list of samplers separated by a semi-colon (`;`) that the listener will collect and send metrics to New Relic. If the list is empty, the listener will not filter samplers and send metrics from all of them. Defaults to an empty string. | No       |
| _useRegexForSamplerList_ | If set to `true` the `samplersList` will be evaluated as a regex to filter samplers. Defaults to `false`.                                                                                                                                           | No       |
| _responseHeaders_        | Optional list of response headers spearated by a semi-colon (`;`) that the listener will collect and send values to New Relic.                                                                                                                      | No       |

_Example of configuration:_

![Screenshot of configuration](docs/configuration.jpg 'Screenshot of JMeter configuration')

#### Custom properties

You can add custom data to your metrics by adding properties starting with `ai.`, for example, you might want to provide information related to your environment with the property `ai.environment` and value `staging`.

### Visualization

Test result metrics are available in the **requests** dimension of your New Relic instance.

## Contributing

Feel free to contribute by forking and making pull requests, or simply by suggesting ideas through the
[Issues](https://github.com/darrensmithwtc/jmeter-backend-newrelic/issues) section.

### Build

You can make changes to the plugin and build your own JAR file to test changes. To build the artifact,
execute below Maven command. Make sure `JAVA_HOME` is set properly.

```bash
mvn clean package
```

---

This plugin is inspired by the [Azure](https://github.com/adrianmo/jmeter-backend-azure) plugin, as well as [Elasticsearch](https://github.com/delirius325/jmeter-elasticsearch-backend-listener) and [Kafka](https://github.com/rahulsinghai/jmeter-backend-listener-kafka) backend listener plugins.
