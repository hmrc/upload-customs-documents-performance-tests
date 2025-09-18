# upload-customs-documents-performance-tests
Performance test suite for the [upload-customs-documents-frontend](https://github.com/hmrc/upload-customs-documents-frontend), using [performance-test-runner](https://github.com/hmrc/performance-test-runner) under the hood and the [upload-customs-documents-test-harness-frontend](https://github.com/hmrc/upload-customs-documents-test-harness-frontend).


## Running the tests

Prior to executing the tests ensure you have:

* Docker - to start mongo container
* Installed/configured service manager

Run the following command to start the services locally:
```
docker run --rm -d --name mongo -d -p 27017:27017 mongo:3.6
sm2 -start CDSRC_ALL
sm2 -start UPLOAD_CUSTOMS_DOCUMENTS_TEST_HARNESS_FRONTEND
```

## Logging

The template uses [logback.xml](src/test/resources) to configure log levels. The default log level is *WARN*. This can be updated to use a lower level for example *TRACE* to view the requests sent and responses received during the test.

#### Smoke test

It might be useful to try the journey with one user to check that everything works fine before running the full performance test
```
sbt -Dperftest.runSmokeTest=true -DrunLocal=true  Gatling/test
```

#### Running the performance test
```
sbt -DrunLocal=true -DuseAwesomeStubs=true Gatling/test
```
### Run the example test against staging environment

#### Smoke test
```
sbt -Dperftest.runSmokeTest=true -DrunLocal=false Gatling/test
```

#### Run the performance test

To run a full performance test against staging environment, implement a job builder and run the test **only** from Jenkins.

### Scalafmt
 This repository uses [Scalafmt](https://scalameta.org/scalafmt/), a code formatter for Scala. The formatting rules configured for this repository are defined within [.scalafmt.conf](.scalafmt.conf).

 To apply formatting to this repository using the configured rules in [.scalafmt.conf](.scalafmt.conf) execute:

 ```
 sbt scalafmtAll
 ```

 To check files have been formatted as expected execute:

 ```
 sbt scalafmtCheckAll scalafmtSbtCheck
 ```

performance test job on staging
```
https://performance.tools.staging.tax.service.gov.uk/job/upload-customs-documents-performance-tests/
```
[Visit the official Scalafmt documentation to view a complete list of tasks which can be run.](https://scalameta.org/scalafmt/docs/installation.html#task-keys)