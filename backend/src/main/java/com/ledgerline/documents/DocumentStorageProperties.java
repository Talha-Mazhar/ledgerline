package com.ledgerline.documents;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where documents are stored: {@code ledgerline.documents.*} in application.yaml.
 *
 * @param endpoint the S3 endpoint; MinIO's URL locally, empty for Amazon S3
 * @param region the S3 region (MinIO accepts any)
 * @param bucket the bucket documents go into; created at start-up if missing
 * @param accessKey the access key id
 * @param secretKey the secret access key
 */
@ConfigurationProperties("ledgerline.documents")
record DocumentStorageProperties(String endpoint, String region, String bucket, String accessKey, String secretKey) {}
