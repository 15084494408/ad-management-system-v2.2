package com.enterprise.ad.common.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 微信小程序登录工具类
 * 通过 code 换取 openid 和 session_key
 */
@Component
@Slf4j
public class WxMaUtil {

    private static final String JS_CODE2SESSION_URL =
        "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    @Value("${wechat.miniapp.appid:}")
    private String appid;

    @Value("${wechat.miniapp.secret:}")
    private String secret;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    /**
     * 微信 code2session 响应
     */
    @Data
    public static class WxSessionResult {
        @JsonProperty("openid")
        private String openid;
        @JsonProperty("session_key")
        private String sessionKey;
        @JsonProperty("unionid")
        private String unionid;
        @JsonProperty("errcode")
        private Integer errcode;
        @JsonProperty("errmsg")
        private String errmsg;

        public boolean isSuccess() {
            return openid != null && !openid.isEmpty();
        }
    }

    /**
     * 通过微信 code 获取 openid
     *
     * @param code 小程序 wx.login() 返回的 code
     * @return WxSessionResult 包含 openid 等信息
     */
    public WxSessionResult code2Session(String code) {
        if (appid == null || appid.isBlank() || "your-appid-placeholder".equals(appid)) {
            log.warn("微信小程序 appid 未配置，请设置环境变量 WX_APPID");
            throw new RuntimeException("微信小程序未配置，请联系管理员");
        }

        String url = String.format(JS_CODE2SESSION_URL, appid, secret, code);
        log.info("调用微信 jscode2session 接口");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            log.debug("微信接口响应: {}", body);

            WxSessionResult result = objectMapper.readValue(body, WxSessionResult.class);
            if (!result.isSuccess()) {
                log.error("微信 code2session 失败: errcode={}, errmsg={}", result.getErrcode(), result.getErrmsg());
                throw new RuntimeException("微信登录失败: " + (result.getErrmsg() != null ? result.getErrmsg() : "未知错误"));
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信 jscode2session 接口异常", e);
            throw new RuntimeException("微信登录服务异常，请稍后重试");
        }
    }
}
