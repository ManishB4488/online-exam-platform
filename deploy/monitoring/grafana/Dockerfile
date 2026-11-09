FROM grafana/grafana:12.1.0
# The dashboards must NOT live under /var/lib/grafana: that folder is the data volume,
# and an existing volume hides whatever the new image puts there.
COPY --chown=472:0 provisioning /etc/grafana/provisioning
COPY --chown=472:0 dashboards /etc/grafana/dashboards
