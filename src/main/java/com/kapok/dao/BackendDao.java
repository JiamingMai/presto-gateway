package com.kapok.dao;

import com.kapok.model.po.BackendPO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackendDao {

    List<BackendPO> getAllBackends();

    List<BackendPO> getAllActiveBackends();

    List<BackendPO> getActiveAdhocBackends();

    List<BackendPO> getActiveBackends(String routingGroup);

    BackendPO addBackend(BackendPO backend);

    BackendPO updateBackend(BackendPO backend);

    void deactivateBackend(String backendName);

    void activateBackend(String backendName);



}
