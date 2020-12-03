# solr-diagnostics
Gathers info from Solr that should help diagnose issues:
* ps output
* dmesg output
* netstat output
* sysctl output
* uname output
* top output
* Solr logs and GC logs
* (optionally) syslog
* process limits
* java version
* solr.xml
* configs
* (Legacy Solr only) cores status
* (SolrCloud only) collections list
* (SolrCloud only) cluster status
* (SolrCloud only) aliases
* (SolrCloud only) overseer status
* metrics snapshot
# Usage
Download the binary from [releases](https://github.com/sematext/solr-diagnostics/releases). Then run it with:

    sudo java -jar solr-diagnostics-x.y.z.jar

You'll need Java (7 or later). It will work without `sudo`, but some information will be missing (e.g. all the `sysctl` info).

The tool will politely say what it collects, then pack everything in a zip archive under `/tmp`. You can change this location by adding `-outputdir /some/other/path`

By default, solr-diagnostics will try to get all Solr logs. You can skip this by adding `-noSolrLogs`. Similarly, you can collect everything from `/var/log` (they are not fetched by default) by adding `-getVarLog`.

# Build from sources
Clone the repository, then run:

    mvn clean package
The self-contained Jar will be `target/com.sematext.solr-diagnostics-x.y.z-SNAPSHOT.jar`
# Roadmap/TODO
See the list of [issues](https://github.com/sematext/solr-diagnostics/issues), and feel free to report new ones or submit pull requests!
