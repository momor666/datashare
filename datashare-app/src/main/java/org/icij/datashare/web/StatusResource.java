package org.icij.datashare.web;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;
import org.icij.datashare.PropertiesProvider;
import org.icij.datashare.Repository;
import org.icij.datashare.com.DataBus;
import org.icij.datashare.openmetrics.StatusMapper;
import org.icij.datashare.tasks.DocumentCollectionFactory;
import org.icij.datashare.text.indexing.Indexer;
import org.icij.extract.queue.DocumentQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Prefix("/api")
public class StatusResource {
    Logger logger = LoggerFactory.getLogger(getClass());
    private PropertiesProvider propertiesProvider;
    private final Repository repository;
    private final Indexer indexer;
    private final DataBus dataBus;
    private DocumentQueue queue;

    @Inject
    public StatusResource(PropertiesProvider propertiesProvider, Repository repository, Indexer indexer, DataBus dataBus, DocumentCollectionFactory documentCollectionFactory) {
        this.propertiesProvider = propertiesProvider;
        this.repository = repository;
        this.indexer = indexer;
        this.dataBus = dataBus;
        this.queue = documentCollectionFactory.createQueue(propertiesProvider, propertiesProvider.get(PropertiesProvider.QUEUE_NAME_OPTION).orElse("extract:queue"));
    }

    /**
     * Retrieve the status of databus connection, database connection, shared queues and index.
     * Adding "format=openmetrics" parameter to the url will return the status witn openmetrics format.
     *
     * @return the status of datashare elements
     *
     * Example:
     * $(curl localhost:8080/api/status)
     *
     * $(curl localhost:8080/api/status?format=openmetrics)
     */
    @Get("/status")
    public Payload getStatus(Context context) {
        boolean queueStatus = false;
        int queueSize = 0;
        try{
            queueSize = queue.size();
            queueStatus = true;
        } catch (RuntimeException ex){
            logger.error("Queue Health Error : ",ex);
        }
        Status status = new Status(repository.getHealth(), indexer.getHealth(), dataBus.getHealth(), queueStatus, queueSize);
        if ("openmetrics".equals(context.request().query().get("format"))) {
            return new Payload("text/plain;version=0.0.4",
                    new StatusMapper("datashare_" + propertiesProvider.get("platform").orElse("null"), status).toString());
        } else {
            return new Payload(status);
        }
    }

    public static class Status {
        public final boolean database;
        public final boolean index;
        public final boolean databus;
        public final boolean document_queue_status;
        public final int document_queue_size;

        Status(boolean database, boolean index, boolean databus, boolean queue, int queueSize) {
            this.database = database;
            this.index = index;
            this.databus = databus;
            this.document_queue_status = queue;
            this.document_queue_size = queueSize;
        }
    }
}


