package org.icij.datashare.db;

import org.icij.datashare.Repository;
import org.icij.datashare.text.Document;
import org.icij.datashare.text.indexing.LanguageGuesser;
import org.icij.extract.document.TikaDocument;
import org.icij.spewer.FieldNames;
import org.icij.spewer.Spewer;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;

import static java.lang.Long.valueOf;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.HttpHeaders.CONTENT_ENCODING;
import static org.apache.tika.metadata.HttpHeaders.CONTENT_LENGTH;
import static org.apache.tika.metadata.HttpHeaders.CONTENT_TYPE;

public class DatabaseSpewer extends Spewer {
    final Repository repository;
    private final LanguageGuesser languageGuesser;
    public static final String DEFAULT_VALUE_UNKNOWN = "unknown";

    public DatabaseSpewer(Repository repository, LanguageGuesser languageGuesser) {
        super(new FieldNames());
        this.repository = repository;
        this.languageGuesser = languageGuesser;
    }

    @Override
    protected void writeDocument(TikaDocument tikaDocument, Reader reader, TikaDocument parent, TikaDocument root, int level) throws IOException {
        String content = toString(reader).trim();
        Charset charset = Charset.forName(ofNullable(tikaDocument.getMetadata().get(CONTENT_ENCODING)).orElse("utf-8"));
        String contentType = ofNullable(tikaDocument.getMetadata().get(CONTENT_TYPE)).orElse(DEFAULT_VALUE_UNKNOWN).split(";")[0];
        Long contentLength = valueOf(ofNullable(tikaDocument.getMetadata().get(CONTENT_LENGTH)).orElse("-1"));
        String parentId = parent == null ? null: parent.getId();
        String rootId = root == null ? null: root.getId();

        Document document = new Document(tikaDocument.getId(), tikaDocument.getPath(), content, languageGuesser.guess(content),
                charset, contentType, getMetadata(tikaDocument), Document.Status.INDEXED, new HashSet<>(), new Date(), parentId,
                rootId, level, contentLength);
        try {
            repository.create(document);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
