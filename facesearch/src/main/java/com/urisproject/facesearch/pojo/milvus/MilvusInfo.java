package com.urisproject.facesearch.pojo.milvus;

import lombok.Data;
/**
 * @Author Anthony HE, anthony.zj.he@outlook.com
 * @Date 4/6/2023
 * @Description: It's a POJO (Plain Old Java Object) that
 * contains information needed to interact with Milvus DB.
 */

@Data
public class MilvusInfo {
    public static final String COLLECTION_NAME = "face_home";
    public static final int SHARD_NUM = 8;
    public static final int PARTITION_NUM = 16;
    public static final String PARTITION_PREFIX = "shards_";
    public static final int FEATURE_DIM = 256;

    public static class Field {
        public static final String ARCHIVE_ID = "archive_id";
        public static final String ORG_ID = "org_id";
        public static final String ARCHIVE_FEATURE = "archive_feature";
    }

    public static String getPartitionName(int orgId) {
        return PARTITION_PREFIX + (orgId % PARTITION_NUM);
    }
}