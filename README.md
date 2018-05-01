# solr-diagnostics
Gathers info from Solr: logs, configs, etc. Useful for remote debugging
# Usage
Download the binary from [releases](https://github.com/sematext/solr-diagnostics/releases). Then run it with:

    java -jar solr-diagnostics-x.y.z.jar
You'll need Java (7) to run it.
# Build from sources
Clone the repository, then run:

    mvn clean package
The self-contained Jar will be `target/com.sematext.solr-diagnostics-x.y.z-SNAPSHOT.jar`

