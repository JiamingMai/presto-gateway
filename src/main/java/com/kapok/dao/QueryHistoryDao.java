package com.kapok.dao;

import com.kapok.model.po.QueryHistoryPO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueryHistoryDao {

    List<QueryHistoryPO> getAllQueryHistory();

    QueryHistoryPO getQueryHistoryByQueryId(String queryId);

    List<QueryHistoryPO> getQueryHistoryByUserName(String userName);

    void saveQueryHistory(QueryHistoryPO queryHistoryPO);

}
