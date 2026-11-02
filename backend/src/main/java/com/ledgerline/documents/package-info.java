/**
 * Documents module: files kept in object storage, such as identity documents and monthly
 * statements. Uses the S3 API, so the same code talks to MinIO locally and to Amazon S3 (or any
 * S3-compatible store) in production. Depends on no other module.
 */
@NullMarked
@ApplicationModule(displayName = "Documents", allowedDependencies = {})
package com.ledgerline.documents;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
