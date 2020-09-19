package com.kapok.model.po;

import lombok.Data;

@Data
public class QueryHistoryPO {

    private String queryId;

    private String queryText;

    private Long createTime;

    private String backendUrl;

    private String userName;

    private String source;

}
