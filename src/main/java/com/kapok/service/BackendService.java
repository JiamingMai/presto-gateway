package com.kapok.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.kapok.dao.BackendDao;
import com.kapok.model.QueryHeader;
import com.kapok.model.dto.BackendDTO;
import com.kapok.model.po.BackendPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class BackendService {

    @Autowired
    private BackendDao backendDao;

    private static final Random RANDOM = new Random();

    private final LoadingCache<String, String> queryIdBackendCache;

    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    public BackendService() {
        queryIdBackendCache =
                CacheBuilder.newBuilder()
                        .maximumSize(10000)
                        .expireAfterAccess(30, TimeUnit.MINUTES)
                        .build(
                                new CacheLoader<String, String>() {
                                    @Override
                                    public String load(String queryId) {
                                        return findBackendForUnknownQueryId(queryId);
                                    }
                                });
    }

    public void setBackendForQueryId(String queryId, String backend) {
        queryIdBackendCache.put(queryId, backend);
    }

    /**
     * Performs routing to an adhoc backend.
     *
     * @return
     */
    public String provideAdhocBackend() {
        List<BackendDTO> backends = upcast(this.backendDao.getActiveAdhocBackends());
        if (backends.size() == 0) {
            throw new IllegalStateException("Number of active backends found zero");
        }
        int backendId = Math.abs(RANDOM.nextInt()) % backends.size();
        return backends.get(backendId).getProxyTo();
    }

    /**
     * Performs routing to a given cluster group. This falls back to an adhoc backend, if no scheduled
     * backend is found.
     *
     * @return
     */
    public String provideBackendForRoutingGroup(String routingGroup) {
        List<BackendDTO> backends = upcast(backendDao.getActiveBackends(routingGroup));
        if (backends.isEmpty()) {
            return provideAdhocBackend();
        }
        int backendId = Math.abs(RANDOM.nextInt()) % backends.size();
        return backends.get(backendId).getProxyTo();
    }

    /**
     * Performs routing to a given cluster group. This falls back to an adhoc backend, if no scheduled
     * backend is found.
     *
     * @return
     */
    public String provideBackendForHeader(QueryHeader queryHeader) {
        if (null != queryHeader.getRoutingGroup()) {
            return provideBackendForRoutingGroup(queryHeader.getRoutingGroup());
        } else {
            return provideAdhocBackend();
        }
    }

    /**
     * Performs cache look up, if a backend not found, it checks with all backends and tries to find
     * out which backend has info about given query id.
     *
     * @param queryId
     * @return
     */
    public String findBackendForQueryId(String queryId) {
        String backendAddress = null;
        try {
            backendAddress = queryIdBackendCache.get(queryId);
        } catch (ExecutionException e) {
            log.error("Exception while loading queryId from cache {}", e.getLocalizedMessage());
        }
        return backendAddress;
    }

    /**
     * This tries to find out which backend may have info about given query id. If not found returns
     * the first healthy backend.
     *
     * @param queryId
     * @return
     */
    protected String findBackendForUnknownQueryId(String queryId) {
        List<BackendDTO> backends = upcast(backendDao.getAllBackends());
        Map<String, Future<Integer>> responseCodes = new HashMap<>();
        try {
            for (BackendDTO backend : backends) {
                String target = backend.getProxyTo() + "/v1/query/" + queryId;

                Future<Integer> call =
                        executorService.submit(
                                () -> {
                                    URL url = new URL(target);
                                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                    conn.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
                                    conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(5));
                                    conn.setRequestMethod(HttpMethod.HEAD.name());
                                    return conn.getResponseCode();
                                });
                responseCodes.put(backend.getProxyTo(), call);
            }
            for (Map.Entry<String, Future<Integer>> entry : responseCodes.entrySet()) {
                if (entry.getValue().isDone()) {
                    int responseCode = entry.getValue().get();
                    if (responseCode == 200) {
                        log.info("Found query [{}] on backend [{}]", queryId, entry.getKey());
                        setBackendForQueryId(queryId, entry.getKey());
                        return entry.getKey();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Query id [{}] not found", queryId);
        }
        // Fallback on first active backend if queryId mapping not found.
        return upcast(backendDao.getActiveAdhocBackends()).get(0).getProxyTo();
    }

    private List<BackendDTO> upcast(List<BackendPO> backendPOs) {
        List<BackendDTO> backendDTOs = new ArrayList<>();
        for (BackendPO backendPO : backendPOs) {
            BackendDTO backendDTO = new BackendDTO();
            backendDTO.setActive(backendPO.getActive().equals("1") ? true : false);
            backendDTO.setRoutingGroup(backendPO.getRoutingGroup());
            backendDTO.setProxyTo(backendPO.getBackendUrl());
            backendDTO.setName(backendPO.getName());
            backendDTOs.add(backendDTO);
        }
        return backendDTOs;
    }

}
