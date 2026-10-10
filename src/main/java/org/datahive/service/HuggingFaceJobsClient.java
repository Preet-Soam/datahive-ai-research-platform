package org.datahive.service;

/**
 * Reads the deployment-only configuration required for a private Hugging Face
 * dataset and Jobs submission. Secrets intentionally remain in environment
 * variables and are never exposed to a JSP, log entry, or database record.
 */
public final class HuggingFaceJobsClient {
    public static final String BACKEND = "HUGGINGFACE_JOBS";
    private static final int SUBMISSION_TIMEOUT_SECONDS = 300;

    public Status status() {
        String token = setting("HF_TOKEN");
        String namespace = setting("HF_NAMESPACE");
        String callbackUrl = setting("DATAHIVE_PUBLIC_URL");
        if (token == null) return Status.unavailable("Add HF_TOKEN to this server's environment.");
        if (namespace == null) return Status.unavailable("Add HF_NAMESPACE, such as your Hugging Face username or organization.");
        if (callbackUrl == null) return Status.unavailable("Add DATAHIVE_PUBLIC_URL so Jobs can report completion back to DataHive.");
        if (!callbackUrl.startsWith("https://") || callbackUrl.contains("localhost") || callbackUrl.contains("127.0.0.1")) {
            return Status.unavailable("DATAHIVE_PUBLIC_URL must be a public HTTPS address for Hugging Face Jobs callbacks.");
        }
        if (!validNamespace(namespace)) {
            return Status.unavailable("HF_NAMESPACE must be the Hugging Face username or organization name only (for example, my-account), without a URL or repository name.");
        }
        return Status.ready(namespace, callbackUrl);
    }

    /** Uploads a dataset privately and requests a Hugging Face Job; callers only invoke this after an explicit form submit. */
    public Submission submit(java.nio.file.Path csv, long runId, String experimentName,
                             String model, String target, java.util.List<String> features,
                             int epochs, double learningRate, String callbackUrl, String callbackToken)
            throws java.io.IOException, InterruptedException {
        Status status = status();
        if (!status.ready()) throw new java.io.IOException(status.message());
        java.nio.file.Path helper = java.nio.file.Files.createTempFile("datahive-hf-submit-", ".py");
        try {
            try (java.io.InputStream input = HuggingFaceJobsClient.class.getResourceAsStream("/hf/submit_job.py")) {
                if (input == null) throw new java.io.IOException("The Hugging Face job submitter is missing from the application.");
                java.nio.file.Files.copy(input, helper, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            String configuredPython = setting("HF_PYTHON");
            String python = configuredPython == null ? (System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win") ? "python" : "python3") : configuredPython;
            ProcessBuilder builder = new ProcessBuilder(python, helper.toString(), "--csv", csv.toString(),
                    "--run-id", Long.toString(runId), "--experiment-name", experimentName,
                    "--model", model, "--target", target, "--features", toJson(features),
                    "--epochs", Integer.toString(epochs), "--learning-rate", Double.toString(learningRate),
                    "--callback-url", callbackUrl);
            builder.environment().put("HF_TOKEN", setting("HF_TOKEN"));
            builder.environment().put("HF_NAMESPACE", status.namespace());
            builder.environment().put("DATAHIVE_CALLBACK_TOKEN", callbackToken);
            String flavor = setting("HF_JOBS_FLAVOR");
            if (flavor != null) builder.environment().put("HF_JOBS_FLAVOR", flavor);
            Process process = builder.redirectErrorStream(true).start();
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            Thread reader = new Thread(() -> {
                try (java.io.InputStream stream = process.getInputStream()) { stream.transferTo(output); }
                catch (java.io.IOException ignored) { }
            }, "datahive-hf-submit-output");
            reader.setDaemon(true);
            reader.start();
            if (!process.waitFor(SUBMISSION_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new java.io.IOException("Hugging Face dataset upload or Job submission timed out.");
            }
            reader.join(2000);
            String result = output.toString(java.nio.charset.StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                throw new java.io.IOException("Hugging Face submission failed. Check the server's Python huggingface_hub setup and token permissions.");
            }
            String jobId = jsonValue(result, "jobId");
            String jobUrl = jsonValue(result, "jobUrl");
            String datasetRepo = jsonValue(result, "datasetRepo");
            if (jobId == null || jobUrl == null || datasetRepo == null) {
                throw new java.io.IOException("Hugging Face returned an incomplete Job receipt.");
            }
            if (!jobUrl.matches("https://huggingface\\.co/jobs/[A-Za-z0-9._/-]+") ||
                    !datasetRepo.matches(java.util.regex.Pattern.quote(status.namespace()) + "/datahive-run-[0-9]+")) {
                throw new java.io.IOException("Hugging Face returned an invalid Job receipt.");
            }
            return new Submission(jobId, jobUrl, datasetRepo);
        } finally {
            java.nio.file.Files.deleteIfExists(helper);
        }
    }

    private static String toJson(java.util.List<String> values) {
        return values.stream().map(value -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private static String jsonValue(String json, String key) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"").matcher(json);
        if (!matcher.find()) return null;
        return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String setting(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean validNamespace(String namespace) {
        return namespace.matches("[A-Za-z0-9_][A-Za-z0-9._-]{0,95}") &&
                !namespace.endsWith(".") && !namespace.endsWith("-") &&
                !namespace.contains("..") && !namespace.contains("--");
    }

    public record Status(boolean ready, String namespace, String callbackUrl, String message) {
        public boolean isReady() { return ready; }
        public String getNamespace() { return namespace; }
        public String getCallbackUrl() { return callbackUrl; }
        public String getMessage() { return message; }
        static Status unavailable(String message) { return new Status(false, "", "", message); }
        static Status ready(String namespace, String callbackUrl) {
            return new Status(true, namespace, callbackUrl,
                    "Private dataset repository and callback configuration are ready.");
        }
    }
    public record Submission(String jobId, String jobUrl, String datasetRepo) { }
}
