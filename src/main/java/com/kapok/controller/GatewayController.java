package com.kapok.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.kapok.dao.BackendDao;
import com.kapok.dao.QueryHistoryDao;
import com.kapok.model.MultiReadHttpServletRequest;
import com.kapok.model.QueryHeader;
import com.kapok.model.po.QueryHistoryPO;
import com.kapok.service.BackendService;
import com.kapok.util.CodecUtil;
import com.kapok.util.FileUtils;
import com.kapok.util.HttpServletRequestReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.kapok.util.GatewayConstant.*;

@Slf4j
@Controller
public class GatewayController {

    private final int QUERY_TEXT_LENGTH_FOR_HISTORY = 200;

    private static final Pattern EXTRACT_BETWEEN_SINGLE_QUOTES = Pattern.compile("'([^\\s']+)'");

    private final Map<String, String> queryIdBackendCache = new HashMap<>();

    @Autowired
    private BackendService backendService;

    @Autowired
    private QueryHistoryDao queryHistoryDao;

    @Autowired
    private RestTemplate restTemplate;

    private int serverApplicationPort = 8080;

    @RequestMapping(value = "/**", method = {RequestMethod.PUT, RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public Object route(HttpServletRequest request) throws IOException {
        MultiReadHttpServletRequest multiReadHttpServletRequest = null;
        try {
            multiReadHttpServletRequest = new MultiReadHttpServletRequest(request);
        } catch (IOException e) {
            log.error("cannot wrap HttpServletRequest to MultiReadHttpServletRequest", e);
        }
        String targetLocation = rewriteTarget(multiReadHttpServletRequest);
        String responseJsonString = forward(targetLocation, multiReadHttpServletRequest);
        JSONObject responseJsonObj = JSON.parseObject(responseJsonString);
        // do with the case that there is no error
        postHandle(responseJsonObj, multiReadHttpServletRequest);
        return responseJsonObj;
    }

    private void postHandle(JSONObject responseJsonObj, MultiReadHttpServletRequest request) throws IOException {
        if (request.getRequestURI().startsWith(V1_STATEMENT_PATH)
                && request.getMethod().equals(HttpMethod.POST)) {
            String queryId = responseJsonObj.getString("id");
            if (null != queryId && queryId.equals("")) {
                queryIdBackendCache.put(queryId, request.getHeader(PROXY_TARGET_HEADER));
            }
            // save query information to history table in MySQL
            queryHistoryDao.saveQueryHistory(getQueryDetailsFromRequest(request));
        } else {
            log.debug("SKIPPING For {}", request.getRequestURI());
        }

        // replace nextUri
        if (responseJsonObj.containsKey(NEXT_URI)) {
            String nextUri = responseJsonObj.getString(NEXT_URI);
            int index = nextUri.indexOf("/", 7);
            String proxyNextUri =
                    request.getScheme()
                            + "://"
                            + request.getRemoteHost()
                            + ":"
                            + request.getServerPort()
                            + nextUri.substring(index);
            responseJsonObj.put(NEXT_URI, proxyNextUri);
        }

        // replace infoUri
        if (responseJsonObj.containsKey(INFO_URI)) {
            String nextUri = responseJsonObj.getString(INFO_URI);
            int index = nextUri.indexOf("/", 7);
            String proxyNextUri =
                    request.getScheme()
                            + "://"
                            + request.getRemoteHost()
                            + ":"
                            + request.getServerPort()
                            + nextUri.substring(index);
            responseJsonObj.put(INFO_URI, proxyNextUri);
        }
    }

    private QueryHistoryPO getQueryDetailsFromRequest(MultiReadHttpServletRequest request)
            throws IOException {
        QueryHistoryPO queryDetail = new QueryHistoryPO();
        queryDetail.setBackendUrl(request.getHeader(PROXY_TARGET_HEADER));
        queryDetail.setCreateTime(System.currentTimeMillis());
        queryDetail.setUserName(request.getHeader(USER_HEADER));
        queryDetail.setSource(request.getHeader(SOURCE_HEADER));
        String queryText = CharStreams.toString(request.getReader());
        queryDetail.setQueryText(
                queryText.length() > QUERY_TEXT_LENGTH_FOR_HISTORY
                        ? queryText.substring(0, QUERY_TEXT_LENGTH_FOR_HISTORY) + "..."
                        : queryText);
        return queryDetail;
    }

    private String forward(String targetLocation, HttpServletRequest request) {
        HttpHeaders httpHeaders = new HttpHeaders();
        Enumeration<String> headerEnum = request.getHeaderNames();
        while(headerEnum.hasMoreElements()) {
            String headerName = headerEnum.nextElement();
            String headerValue = request.getHeader(headerName);
            httpHeaders.add(headerName, headerValue);
        }
        String requestBody = HttpServletRequestReader.readAsChars(request);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, httpHeaders);
        HttpMethod httpMethod = parseHttpMethod(request);
        ResponseEntity<byte[]> result = restTemplate.exchange(targetLocation, httpMethod, entity, byte[].class);
        String responseJsonData = CodecUtil.decodeResponseBody(result.getBody(), result);
        return responseJsonData;
    }

    private HttpMethod parseHttpMethod(HttpServletRequest request) {
        switch (request.getMethod()) {
            case "GET":
                return HttpMethod.GET;
            case "POST":
                return HttpMethod.POST;
            case "PUT":
                return HttpMethod.PUT;
            case "DELETE":
                return HttpMethod.DELETE;
            case "TRACE":
                return HttpMethod.TRACE;
            case "OPTIONS":
                return HttpMethod.OPTIONS;
            case "HEAD":
                return HttpMethod.HEAD;
            case "PATCH":
                return HttpMethod.PATCH;
            case "CONNECT":
            default:
                log.info("Unsupported method");
        }
        return HttpMethod.GET;
    }

    private boolean isPathWhiteListed(String path) {
        return path.startsWith(V1_STATEMENT_PATH)
                || path.startsWith(V1_QUERY_PATH)
                || path.startsWith(PRESTO_UI_PATH)
                || path.startsWith(V1_INFO_PATH);
    }

    private String rewriteTarget(HttpServletRequest request) {
        /* Here comes the load balancer / gateway */
        String backendAddress = "http://localhost:" + serverApplicationPort;

        // Only load balance presto query APIs.
        if (isPathWhiteListed(request.getRequestURI())) {
            String queryId = extractQueryIdIfPresent(request);

            // Find query id and get url from cache
            if (!Strings.isNullOrEmpty(queryId)) {
                backendAddress = backendService.findBackendForQueryId(queryId);
            } else {
                String routingGroup = request.getHeader(ROUTING_GROUP_HEADER);
                QueryHeader queryHeader = new QueryHeader();
                queryHeader.setRoutingGroup(routingGroup);
                queryHeader.setSource(request.getHeader(SOURCE_HEADER));
                queryHeader.setUser(request.getHeader(USER_HEADER));
                Enumeration<String> enumeration = request.getHeaders(SESSION_HEADER);
                queryHeader.setSessionProperties(enumeration);
                // This falls back on adhoc backend if there are no cluster found for the routing group.
                backendAddress = backendService.provideBackendForHeader(queryHeader);
            }
            // set target backend so that we could save queryId to backend mapping later.
            ((MultiReadHttpServletRequest) request).addHeader(PROXY_TARGET_HEADER, backendAddress);
        }
        String targetLocation =
                backendAddress
                        + request.getRequestURI()
                        + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

        String originalLocation =
                request.getScheme()
                        + "://"
                        + request.getRemoteHost()
                        + ":"
                        + request.getServerPort()
                        + request.getRequestURI()
                        + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

        log.debug("Rerouting [{}]--> [{}]", originalLocation, targetLocation);
        return targetLocation;
    }

    protected String extractQueryIdIfPresent(HttpServletRequest request) {
        String path = request.getRequestURI();
        String queryParams = request.getQueryString();
        try {
            String queryText = CharStreams.toString(request.getReader());
            if (!Strings.isNullOrEmpty(queryText)
                    && queryText.toLowerCase().contains("system.runtime.kill_query")) {
                // extract and return the queryId
                String[] parts = queryText.split(",");
                for (String part : parts) {
                    if (part.contains("query_id")) {
                        Matcher m = EXTRACT_BETWEEN_SINGLE_QUOTES.matcher(part);
                        if (m.find()) {
                            String queryQuoted = m.group();
                            if (!Strings.isNullOrEmpty(queryQuoted) && queryQuoted.length() > 0) {
                                return queryQuoted.substring(1, queryQuoted.length() - 1);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting query payload from request", e);
        }
        if (path == null) {
            return null;
        }
        String queryId = null;

        log.debug("trying to extract query id from path [{}] or queryString [{}]", path, queryParams);
        if (path.startsWith(V1_STATEMENT_PATH) || path.startsWith(V1_QUERY_PATH)) {
            String[] tokens = path.split("/");
            if (tokens.length >= 4) {
                if (path.contains("queued")
                        || path.contains("scheduled")
                        || path.contains("executing")
                        || path.contains("partialCancel")) {
                    queryId = tokens[4];
                } else {
                    queryId = tokens[3];
                }
            }
        } else if (path.startsWith(PRESTO_UI_PATH)) {
            queryId = queryParams;
        }
        log.debug("query id in url [{}]", queryId);
        return queryId;
    }

}
