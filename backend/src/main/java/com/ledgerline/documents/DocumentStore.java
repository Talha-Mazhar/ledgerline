package com.ledgerline.documents;

import java.util.Optional;

/** The documents module's public API: store a file under a key and read it back. */
public interface DocumentStore {

    void put(String key, byte[] content, String contentType);

    Optional<StoredDocument> get(String key);

    /** A file read back from storage. */
    record StoredDocument(byte[] content, String contentType) {}
}
