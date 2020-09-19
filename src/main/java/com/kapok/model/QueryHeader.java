package com.kapok.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Enumeration;
import java.util.Properties;

@Data
@Slf4j
public class QueryHeader {

    private String user;

    private String source;

    private String routingGroup;

    private Properties sessionProperties;

    public void setSessionProperties(Enumeration<String> sessionPropertiesEnum) {
        if (sessionPropertiesEnum == null) {
            return;
        }
        while (sessionPropertiesEnum.hasMoreElements()) {
            String property = sessionPropertiesEnum.nextElement();
            log.debug("session:" + property);
            if (property != null) {
                String[] mProperties = property.split(",");
                for (String p : mProperties) {
                    String[] keyValue = p.split("=");
                    if (keyValue.length == 2) {
                        if (sessionProperties == null) {
                            sessionProperties = new Properties();
                        }
                        sessionProperties.setProperty(keyValue[0], keyValue[1]);
                    }
                }
            }
        }
    }
}
