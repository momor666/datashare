package org.icij.datashare.db;

import org.icij.datashare.Repository;
import org.icij.datashare.text.Document;
import org.icij.datashare.text.Language;
import org.icij.datashare.text.NamedEntity;
import org.icij.datashare.text.nlp.Pipeline;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import static java.nio.charset.Charset.forName;
import static java.util.stream.Collectors.toSet;
import static org.icij.datashare.json.JsonObjectMapper.MAPPER;
import static org.icij.datashare.text.Document.Status.fromCode;
import static org.icij.datashare.text.Language.parse;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public class JooqRepository implements Repository {
    private static final String DOCUMENT = "document";
    private static final String DOCUMENT_NER = "document_ner_pipeline_type";
    private static final String NAMED_ENTITY = "named_entity";

    private final ConnectionProvider connectionProvider;
    private SQLDialect dialect;

    public JooqRepository(final ConnectionProvider connectionProvider, final SQLDialect dialect) {
        this.connectionProvider = connectionProvider;
        this.dialect = dialect;
    }

    @Override
    public NamedEntity getNamedEntity(String id) throws SQLException {
        try(Connection conn = connectionProvider.acquire()) {
            DSLContext create = DSL.using(conn, dialect);
            return createFrom(create.select().from(table(NAMED_ENTITY)).where(field("id").eq(id)).fetch().get(0));
        }
    }

    @Override
    public void create(List<NamedEntity> neList) throws SQLException {
        try(Connection conn = connectionProvider.acquire()) {
            DSLContext create = DSL.using(conn, dialect);
            InsertValuesStep9<Record, Object, Object, Object, Object, Object, Object, Object, Object, Object>
                    insertQuery = create.insertInto(table(NAMED_ENTITY),
                    field("id"), field("mention"), field("ne_offset"), field("extractor"),
                    field("category"), field("doc_id"), field("root_id"),
                    field("extractor_language"), field("hidden"));
            neList.forEach(ne -> insertQuery.values(
                    ne.getId(), ne.getMention(), ne.getOffset(), ne.getExtractor().code,
                    ne.getCategory().getAbbreviation(), ne.getDocumentId(), ne.getRootDocument(),
                    ne.getExtractorLanguage().iso6391Code(), ne.isHidden()));
            insertQuery.execute();
        }
    }

    @Override
    public Document getDocument(final String id) throws SQLException, IOException {
        try(Connection conn = connectionProvider.acquire()) {
            DSLContext create = DSL.using(conn, dialect);
            Record docResult = create.select().from(table(DOCUMENT)).where(field("id").eq(id)).fetch().get(0);
            Result<Record> nerResults = create.select().from(table(DOCUMENT_NER)).where(field("doc_id").eq(id)).fetch();
            return createFrom(docResult, nerResults);
        }
    }

    @Override
    public void create(Document doc) throws SQLException {
        try(Connection conn = connectionProvider.acquire()) {
            DSLContext ctx = DSL.using(conn, dialect);
            ctx.transaction(cfg -> {
                DSLContext context = DSL.using(cfg);
                context.insertInto(table(DOCUMENT),
                                    field("id"), field("path"), field("content"), field("status"),
                                    field("charset"), field("language"), field("content_type"),
                                    field("extraction_date"), field("parent_id"), field("root_id"),
                                    field("extraction_level"), field("content_length"), field("metadata")).
                                    values(doc.getId(), doc.getPath().toString(), doc.getContent(), doc.getStatus().code,
                                            doc.getContentEncoding().toString(), doc.getLanguage().iso6391Code(), doc.getContentType(),
                                            new Timestamp(doc.getExtractionDate().getTime()), doc.getParentDocument(), doc.getRootDocument(),
                                            doc.getExtractionLevel(), doc.getContentLength(),
                                            MAPPER.writeValueAsString(doc.getMetadata())).execute();

                if (!doc.getNerTags().isEmpty()) {
                    InsertValuesStep2<Record, Object, Object> insertNerPipelines = context.insertInto(table(DOCUMENT_NER), field("doc_id"), field("type_id"));
                    doc.getNerTags().forEach(type -> insertNerPipelines.values(doc.getId(), type.code));
                    insertNerPipelines.execute();
                }
            });
        }
    }

    @Override
    public NamedEntity deleteNamedEntity(String id) { return null;}

    @Override
    public Document deleteDocument(String id) { return null;}

    private NamedEntity createFrom(Record record) {
        return NamedEntity.create(NamedEntity.Category.parse(record.get("category", String.class)),
                record.get("mention", String.class), record.get("ne_offset", Integer.class),
                record.get("doc_id", String.class), Pipeline.Type.fromCode(record.get("extractor", Integer.class)),
                Language.parse(record.get("extractor_language", String.class)));
    }

    private Document createFrom(Record result, Result<Record> nerResults) throws IOException {
        Map<String, Object> metadata = MAPPER.readValue(result.get("metadata", String.class), HashMap.class);
        Set<Pipeline.Type> nerTags = nerResults.intoSet("type_id", Integer.class).stream().map(Pipeline.Type::fromCode).collect(toSet());
        return new Document(result.get("id", String.class), Paths.get(result.get("path", String.class)),
                result.get("content", String.class), parse(result.get("language", String.class)), forName(result.get("charset", String.class)),
                result.get("content_type", String.class), metadata, fromCode(result.get("status", Integer.class)), nerTags,
                new Date(result.get("extraction_date", Timestamp.class).getTime()), result.get("parent_id", String.class), result.get("root_id", String.class),
                result.get("extraction_level", Integer.class), result.get("content_length", Long.class)
        );
    }
}
