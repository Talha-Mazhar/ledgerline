package com.ledgerline.documents;

import java.util.Optional;

/** The documents module's public API: store a file under a key and read it back. */
public interface DocumentStore {

    void put(String key, byte[] content, String contentType);

    Optional<StoredDocument> get(String key);

    /**
     * A file read back from storage. A class rather than a record: a record's array component would
     * be shared with every caller and compared by identity.
     */
    final class StoredDocument {

        private final byte[] content;
        private final String contentType;

        public StoredDocument(byte[] content, String contentType) {
            this.content = content.clone();
            this.contentType = contentType;
        }

        /** A copy of the file's bytes. */
        public byte[] content() {
            return content.clone();
        }

        public String contentType() {
            return contentType;
        }
    }
}
