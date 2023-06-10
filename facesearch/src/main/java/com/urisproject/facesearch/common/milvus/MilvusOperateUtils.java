package com.urisproject.facesearch.common.milvus;


import com.arcsoft.face.FaceEngine;


import com.urisproject.facesearch.factory.MilvusPoolFactory;
import com.urisproject.facesearch.pojo.face.FaceMilvus;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.IDs;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.stereotype.Component;
import com.tangcheng.face_search.pojo.face.faceMilvus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Milvus utility class
@Data
@Component
@Log4j2
public class MilvusOperateUtils {
    private GenericObjectPool<MilvusServiceClient> milvusServiceClientGenericObjectPool;  // Pool to manage connection objects
    private final int MAX_POOL_SIZE = 5;

    private MilvusOperateUtils() {
        // Private constructor to create a pool
        // Object pool factory
        MilvusPoolFactory milvusPoolFactory = new MilvusPoolFactory();
        // Object pool configuration
        GenericObjectPoolConfig<FaceEngine> objectPoolConfig = new GenericObjectPoolConfig<>();
        objectPoolConfig.setMaxTotal(8);
        AbandonedConfig abandonedConfig = new AbandonedConfig();

        abandonedConfig.setRemoveAbandonedOnMaintenance(true); // Check for leaks during Maintenance

        abandonedConfig.setRemoveAbandonedOnBorrow(true); // Check for leaks when borrowing

        abandonedConfig.setRemoveAbandonedTimeout(MAX_POOL_SIZE); // An object that has not been returned to the pool 10 seconds after borrowing is considered a leak

        // Object pool
        milvusServiceClientGenericObjectPool = new GenericObjectPool(milvusPoolFactory, objectPoolConfig);
        milvusServiceClientGenericObjectPool.setAbandonedConfig(abandonedConfig);
        milvusServiceClientGenericObjectPool.setTimeBetweenEvictionRunsMillis(5000); // Run a maintenance task every 5 seconds
        log.info("milvus object pool creation succeeded, maintaining " + MAX_POOL_SIZE + " objects");
    }

    // Create a Collection, similar to creating a table in a relational database
    private void createCollection(String collection) {
        MilvusServiceClient milvusServiceClient = null;
        try {
            // Manage objects through the object pool
            milvusServiceClient = milvusServiceClientGenericObjectPool.borrowObject();
            FieldType fieldType1 = FieldType.newBuilder()
                    .withName(FaceMilvus.Field.USER_NAME)
                    .withDescription("Username")
                    .withDataType(DataType.Int64)
                    .build();
            FieldType fieldType2 = FieldType.newBuilder()
                    .withName(FaceMilvus.Field.USER_CODE)
                    .withDescription("ID")
                    .withDataType(DataType.Int64)
                    .withPrimaryKey(true)
                    .withAutoID(false)
                    .build();
            FieldType fieldType3 = FieldType.newBuilder()
                    .withName(FaceMilvus.Field.FEATURE)
                    .withDescription("Feature vector")
                    .withDataType(DataType.FloatVector)
                    .withDimension(FaceMilvus.FEATURE_DIM)
                    .build();
            CreateCollectionParam createCollectionReq = CreateCollectionParam.newBuilder()
                    .withCollectionName(collection)
                    .withDescription("Face feature vector library")
                    .withShardsNum(2)
                    .addFieldType(fieldType2)
                    .addFieldType(fieldType1)
                    .addFieldType(fieldType3)
                    .build();
            R<RpcStatus> result = milvusServiceClient.createCollection(createCollectionReq);
            log.info("Creation result" + result.getStatus() + "0 for success");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Recycle objects to the object pool
            if (milvusServiceClient != null) {
                milvusServiceClientGenericObjectPool.returnObject(milvusServiceClient);
            }
        }
    }

    // Load data into the given collection
    public void loadCollection(String collectionName) {
        MilvusServiceClient milvusServiceClient = null;
        try {
            // Manage objects through the object pool
            milvusServiceClient = milvusServiceClientGenericObjectPool.borrowObject();
            LoadCollectionParam loadCollectionParam = new LoadCollectionParam(collectionName);
            R<RpcStatus> result = milvusServiceClient.loadCollection(loadCollectionParam);
            log.info("Load collection result: " + result.getStatus() + ", 0 for success");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Recycle objects to the object pool
            if (milvusServiceClient != null) {
                milvusServiceClientGenericObjectPool.returnObject(milvusServiceClient);
            }
        }
    }

    // Insert data into the collection
    public void insert(String collectionName, List<FaceMilvus> faceList) {
        MilvusServiceClient milvusServiceClient = null;
        try {
            // Manage objects through the object pool
            milvusServiceClient = milvusServiceClientGenericObjectPool.borrowObject();
            List<Long> userCode = new ArrayList<>();
            List<String> userName = new ArrayList<>();
            List<Float> userFeature = new ArrayList<>();

            for (FaceMilvus face : faceList) {
                userCode.add(face.getUserCode());
                userName.add(face.getUserName());
                userFeature.addAll(face.getFeature());
            }

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withInt64FieldData(FaceMilvus.Field.USER_CODE, userCode)
                    .withStringFieldData(FaceMilvus.Field.USER_NAME, userName)
                    .withFloatVectorFieldData(FaceMilvus.Field.FEATURE, faceMilvus.FEATURE_DIM, userFeature)
                    .build();

            R<MutationResult> result = milvusServiceClient.insert(insertParam);
            log.info("Insert result: " + result.getStatus() + ", 0 for success");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Recycle objects to the object pool
            if (milvusServiceClient != null) {
                milvusServiceClientGenericObjectPool.returnObject(milvusServiceClient);
            }
        }
    }

    // Create an index on the collection
    public void createIndex(String collectionName) {
        MilvusServiceClient milvusServiceClient = null;
        try {
            // Manage objects through the object pool
            milvusServiceClient = milvusServiceClientGenericObjectPool.borrowObject();
            CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldName(FaceMilvus.Field.FEATURE)
                    .withIndexType(MetricType.IP)
                    .withNList(16384)
                    .build();

            R<RpcStatus> result = milvusServiceClient.createIndex(createIndexParam);
            log.info("Create index result: " + result.getStatus() + ", 0 for success");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Recycle objects to the object pool
            if (milvusServiceClient != null) {
                milvusServiceClientGenericObjectPool.returnObject(milvusServiceClient);
            }
        }
    }

    // Search for similar face feature vectors in the collection
    public List<SearchResults> search(String collectionName, List<Float> queryFeature, int topK) {
        MilvusServiceClient milvusServiceClient = null;
        List<SearchResults> searchResultsList = new ArrayList<>();
        try {
            // Manage objects through the object pool
            milvusServiceClient = milvusServiceClientGenericObjectPool.borrowObject();
            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withTopK(topK)
                    .withFloatVectorQueryParam(FaceMilvus.Field.FEATURE, faceMilvus.FEATURE_DIM, queryFeature)
                    .withParamsInJson("{\"metric_type\": \"IP\"}")
                    .build();

            R<SearchResultsWrapper> result = milvusServiceClient.search(searchParam);

            if (result.getStatus().equals(RpcStatus.OK)) {
                searchResultsList = result.getValue().getSearchResultsList();
            } else {
                log.error("Search failed, status: " + result.getStatus());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Recycle objects to the object pool
            if (milvusServiceClient != null) {
                milvusServiceClientGenericObjectPool.returnObject(milvusServiceClient);
            }
        }

        return searchResultsList;
    }
}ServiceClient;