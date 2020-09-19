package com.kapok.model.dto;

import lombok.Data;

@Data
public class BackendDTO {

    private boolean active = true;

    private String routingGroup = "adhoc";

    private String name;

    private int localPort;

    private String proxyTo;

    private String prefix = "/";

    private String trustAll = "true";

    private String preserveHost = "true";

    private boolean ssl;

    private String keystorePath;

    private String keystorePass;

}
