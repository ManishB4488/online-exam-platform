FROM jenkins/jenkins:lts-jdk21

USER root

# The Docker command line, so the pipeline can build images
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl gnupg \
 && install -m 0755 -d /etc/apt/keyrings \
 && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
 && chmod a+r /etc/apt/keyrings/docker.asc \
 && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" > /etc/apt/sources.list.d/docker.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends docker-ce-cli docker-buildx-plugin docker-compose-plugin \
 && rm -rf /var/lib/apt/lists/*

# Node.js 22, to build the React app
RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
 && apt-get install -y --no-install-recommends nodejs \
 && rm -rf /var/lib/apt/lists/*

# Ansible, to deploy the app (in its own virtual environment, so it cannot clash with Debian's Python)
RUN apt-get update \
 && apt-get install -y --no-install-recommends python3 python3-venv \
 && python3 -m venv /opt/ansible \
 && /opt/ansible/bin/pip install --no-cache-dir ansible-core \
 && ln -s /opt/ansible/bin/ansible-playbook /usr/local/bin/ansible-playbook \
 && ln -s /opt/ansible/bin/ansible /usr/local/bin/ansible \
 && rm -rf /var/lib/apt/lists/*

USER jenkins
