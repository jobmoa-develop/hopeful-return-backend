package com.jobmoa.hopefulreturn.config;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * SPA(React Router) 정적 리소스 서빙 설정.
 *
 * <p>번들된 프론트엔드(classpath:/static)를 서빙하되, 실제 파일이 없는 경로는
 * index.html 로 폴백해 클라이언트 사이드 라우팅(딥링크 새로고침)이 동작하도록 한다.
 * 단, 백엔드가 처리해야 하는 경로(api/·swagger·v3/api-docs·actuator)는 폴백하지 않고
 * 컨트롤러/에러 처리에 위임한다.
 *
 * <p><b>캐시 전략</b>: 배포 후 브라우저가 이전 JS 를 계속 로드하는 문제를 막기 위해
 * 리소스 종류별로 Cache-Control 을 분리한다.
 * <ul>
 *   <li>{@code /assets/**} — Vite 가 파일명에 콘텐츠 해시를 넣으므로(예: index-BUrVtHd2.js)
 *       내용이 바뀌면 파일명이 바뀐다. 따라서 1년 immutable 로 적극 캐시한다.
 *       (SPA 폴백을 적용하지 않아 존재하지 않는 해시 파일은 404 로 응답 → HTML 오배달 방지)</li>
 *   <li>{@code /**}(index.html 등) — 해시가 없는 고정 진입점이므로 no-cache 로 매 요청 서버
 *       재검증한다. 변경이 없으면 304, 배포로 바뀌면 최신 index.html 을 받아 새 해시 번들을 참조한다.</li>
 * </ul>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // index.html 로 폴백하면 안 되는(백엔드가 처리하는) 경로 prefix. resourcePath 는 선행 슬래시 없음.
    private static final String[] BACKEND_PREFIXES = {
            "api/", "swagger", "v3/api-docs", "actuator"
    };

    // 콘텐츠 해시가 붙은 산출물의 캐시 유효기간(1년). 파일명이 곧 버전이라 안전하게 immutable 로 둔다.
    private static final long ASSET_CACHE_DAYS = 365L;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 해시 파일명(/assets/index-<hash>.js 등): 영구 캐시. SPA 폴백 미적용 → 없는 파일은 404.
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(ASSET_CACHE_DAYS, TimeUnit.DAYS)
                        .cachePublic()
                        .immutable())
                .resourceChain(true);

        // index.html 및 기타 정적 리소스 + SPA 폴백: 매 요청 재검증(no-cache)해 항상 최신 진입점을 서빙.
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    /**
     * 요청 리소스가 존재하면 그대로 서빙, 없으면 SPA 셸(index.html)을 반환한다.
     * 백엔드 경로는 null 을 반환해 정적 서빙 대상에서 제외한다.
     */
    private static class SpaResourceResolver extends PathResourceResolver {

        private final Resource index = new ClassPathResource("static/index.html");

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }
            for (String prefix : BACKEND_PREFIXES) {
                if (resourcePath.startsWith(prefix)) {
                    return null;
                }
            }
            return index.exists() ? index : null;
        }
    }
}
