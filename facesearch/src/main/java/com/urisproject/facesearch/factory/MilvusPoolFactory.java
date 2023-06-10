package com.urisproject.facesearch.factory;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.beans.factory.annotation.Value;

/**
 * @Author Anthony HE, anthony.zj.he@outlook.com
 * @Date 10/6/2023
 * @Description:
 */
public class MilvusPoolFactory extends BasePooledObjectFactory<MilvusServiceClient> {
    @Value("${milvus.host}")
    private String host; // The server address where Milvus runs
    @Value("${milvus.port}")
    private Integer port; // The port number on which Milvus listens

    @Override
    public MilvusServiceClient create() throws Exception {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .build();
        return new MilvusServiceClient(connectParam);
    }

    @Override
    public PooledObject<MilvusServiceClient> wrap(MilvusServiceClient milvusServiceClient) {
        return new DefaultPooledObject<>(milvusServiceClient);
    }
}