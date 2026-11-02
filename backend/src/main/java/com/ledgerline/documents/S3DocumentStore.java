package com.ledgerline.documents;

import java.net.URI;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * {@link DocumentStore} on the S3 API. Path-style addressing ({@code host/bucket/key}) is what
 * MinIO expects; it also works with Amazon S3.
 */
@Service
class S3DocumentStore implements DocumentStore {

    private static final Logger log = LoggerFactory.getLogger(S3DocumentStore.class);

    private final S3Client s3;
    private final String bucket;

    S3DocumentStore(DocumentStorageProperties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .region(Region.of(properties.region()))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())));
        if (!properties.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.endpoint()));
        }
        this.s3 = builder.build();
        this.bucket = properties.bucket();
    }

    /** Creates the bucket on first start, so a fresh environment needs no manual setup. */
    @EventListener(ApplicationReadyEvent.class)
    void createBucketIfMissing() {
        try {
            s3.headBucket(request -> request.bucket(bucket));
        } catch (S3Exception missing) {
            if (missing.statusCode() != 404) {
                log.warn("Document storage refused the bucket check: {}", missing.getMessage());
                return;
            }
            s3.createBucket(request -> request.bucket(bucket));
            log.info("Created document bucket {}", bucket);
        } catch (SdkException unavailable) {
            // Storage being down at start-up must not stop the app; uploads fail until it's back.
            log.warn("Document storage isn't reachable yet: {}", unavailable.getMessage());
        }
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        s3.putObject(
                request -> request.bucket(bucket).key(key).contentType(contentType),
                RequestBody.fromBytes(content));
    }

    @Override
    public Optional<StoredDocument> get(String key) {
        try {
            ResponseBytes<GetObjectResponse> object =
                    s3.getObjectAsBytes(request -> request.bucket(bucket).key(key));
            String contentType = object.response().contentType();
            return Optional.of(new StoredDocument(
                    object.asByteArray(), contentType == null ? "application/octet-stream" : contentType));
        } catch (NoSuchKeyException missing) {
            return Optional.empty();
        }
    }
}
