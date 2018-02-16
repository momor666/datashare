package org.icij.datashare.test;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.rules.ExternalResource;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ElasticsearchRule extends ExternalResource {
    public static final String TEST_INDEX = "datashare-test";
    public final Client client;

    public ElasticsearchRule() {
        System.setProperty("es.set.netty.runtime.available.processors", "false");
        Settings settings = Settings.builder().put("cluster.name", "datashare").build();
        try {
            client = new PreBuiltTransportClient(settings).addTransportAddress(
                    new TransportAddress(InetAddress.getByName("elasticsearch"), 9300));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void before() throws Throwable {
        client.admin().indices().create(new CreateIndexRequest(TEST_INDEX));
    }

    @Override
    protected void after() {
        client.admin().indices().delete(new DeleteIndexRequest(TEST_INDEX));
        client.close();
    }
}
