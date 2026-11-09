# The settings are baked into the image, so the deployment needs no files on the host
# (Jenkins runs inside a container, so bind-mounted files would not be found).
FROM prom/prometheus:v3.5.0
COPY prometheus.yml /etc/prometheus/prometheus.yml
