# hmpps-person-record-gatling
Core person record: Gatling performance tests

## Getting started.

### Adding a new test
- Add the new endpoint URI and sql in `applicaion.conf`
- Add the chain builder for the new endpoint in `ApiHelper` file
- Feed the data and add the scenario in `CorePersonRecordSimulation` file
- Inject the scenario inside **init** block in `CorePersonRecordSimulation` file


### Setting up load
- Users are injected during runtime from workflow
- Calculate number of users per second based on the required requests during a certain time
- Add that number for the new endpoint

### Assertions / SLA thresholds
Simulation-wide assertions are configured in `CorePersonRecordSimulation` and read from `AppConfig`. The build fails if any of these are breached:
- `minSuccessPercentage` (default `95.0`) - minimum percentage of successful requests, override with `-DminSuccessPercentage=`
- `p95ThresholdMillis` (default `1000`) - maximum 95th percentile response time in ms, override with `-Dp95ThresholdMillis=`
- `p99ThresholdMillis` (default `2000`) - maximum 99th percentile response time in ms, override with `-Dp99ThresholdMillis=`

### Running unit tests
Unit tests (e.g. for `TokenManager` and `CsvGenerator`) live under `src/test/kotlin` and run with:
```
./gradlew test
```
`./gradlew check` runs both the unit tests and `ktlintCheck`, and is run automatically on every pull request via the `pr_check.yml` workflow.


### Running tests in local
Port forward to [Access the DEV RDS Database](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/other-topics/rds-external-access.html#accessing-your-rds-database)

Make sure you are authenticated to Cloud Platform and have `cloud-platform` CLI and `jq` installed, as [run_local.sh](run_local.sh) fetches the database details and Gatling client credentials from Kubernetes secrets at runtime.

Run [run_local.sh](run_local.sh)

### Viewing Test Results

After running the tests, the results will be available in the `build/reports/gatling` directory. Open the `index.html` file in a web browser to view the results.

In CI, each `gatling_run.yml` run uploads its HTML report as a workflow artifact. Assertions (success rate and p95/p99 response-time thresholds, see `AppConfig`) will fail the build if breached, but there is currently **no historical trend tracking** between runs - artifacts are only useful for inspecting a single run in isolation, and older ones are pruned by GitHub's artifact retention policy.

**Recommendation for future work:** integrate results with a dashboard (e.g. push Gatling's `js/stats.json` per run to Grafana/InfluxDB, or use Gatling Enterprise) so that response-time and error-rate trends can be tracked and regressions spotted over time, rather than only comparing against the fixed thresholds in a single run.

For more information about the Gatling tests, see the [Gatling README](README.md).