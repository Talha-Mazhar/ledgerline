{{- define "ledgerline.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "ledgerline.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/* Labels on every object. */}}
{{- define "ledgerline.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" }}
app.kubernetes.io/name: {{ include "ledgerline.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: ledgerline
{{- end }}

{{/* Selector labels for one component: include with (dict "root" $ "component" "backend"). */}}
{{- define "ledgerline.selectorLabels" -}}
app.kubernetes.io/name: {{ include "ledgerline.name" .root }}
app.kubernetes.io/instance: {{ .root.Release.Name }}
app.kubernetes.io/component: {{ .component }}
{{- end }}

{{- define "ledgerline.componentLabels" -}}
{{ include "ledgerline.labels" .root }}
app.kubernetes.io/component: {{ .component }}
{{- end }}

{{/* The name of one component's objects, e.g. "ledgerline-backend". */}}
{{- define "ledgerline.component" -}}
{{- printf "%s-%s" (include "ledgerline.fullname" .root) .component | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "ledgerline.image" -}}
{{- $tag := default .root.Chart.AppVersion .image.tag -}}
{{- if .root.Values.imageRegistry -}}
{{- printf "%s/%s:%s" .root.Values.imageRegistry .image.repository $tag -}}
{{- else -}}
{{- printf "%s:%s" .image.repository $tag -}}
{{- end -}}
{{- end }}

{{- define "ledgerline.secretName" -}}
{{- default (printf "%s-secrets" (include "ledgerline.fullname" .)) .Values.secrets.existingSecret }}
{{- end }}

{{/*
A secret value: the one in values, else the one already in the cluster (so upgrades keep it),
else a new random one. Include with (dict "root" $ "key" "database-password" "value" "...").
*/}}
{{- define "ledgerline.secretValue" -}}
{{- $existing := lookup "v1" "Secret" .root.Release.Namespace (printf "%s-secrets" (include "ledgerline.fullname" .root)) -}}
{{- if .value -}}
{{- .value | b64enc -}}
{{- else if and $existing (hasKey $existing.data .key) -}}
{{- index $existing.data .key -}}
{{- else -}}
{{- randAlphaNum 32 | b64enc -}}
{{- end -}}
{{- end }}

{{/* Pod and container security settings shared by the app's own pods. */}}
{{- define "ledgerline.podSecurityContext" -}}
runAsNonRoot: true
runAsUser: 10001
runAsGroup: 10001
fsGroup: 10001
seccompProfile:
  type: RuntimeDefault
{{- end }}

{{- define "ledgerline.containerSecurityContext" -}}
allowPrivilegeEscalation: false
readOnlyRootFilesystem: true
capabilities:
  drop: [ALL]
{{- end }}

{{/* Addresses of the data stores: in-cluster services, or the external ones from values. */}}
{{- define "ledgerline.databaseUrl" -}}
{{- if .Values.infrastructure.enabled -}}
jdbc:postgresql://{{ include "ledgerline.component" (dict "root" . "component" "postgres") }}:5432/ledgerline
{{- else -}}
{{- required "external.database.url is required when infrastructure.enabled is false" .Values.external.database.url -}}
{{- end -}}
{{- end }}

{{- define "ledgerline.kafkaServers" -}}
{{- if .Values.infrastructure.enabled -}}
{{ include "ledgerline.component" (dict "root" . "component" "kafka") }}:9092
{{- else -}}
{{- required "external.kafka.bootstrapServers is required when infrastructure.enabled is false" .Values.external.kafka.bootstrapServers -}}
{{- end -}}
{{- end }}

{{- define "ledgerline.redisHost" -}}
{{- if .Values.infrastructure.enabled -}}
{{ include "ledgerline.component" (dict "root" . "component" "redis") }}
{{- else -}}
{{- required "external.redis.host is required when infrastructure.enabled is false" .Values.external.redis.host -}}
{{- end -}}
{{- end }}
