package com.jobmoa.hopefulreturn.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // index.html 로 폴백하면 안 되는(백엔드가 처리하는) 경로 prefix. resourcePath 는 선행 슬래시 없음.
    private static final String[] BACKEND_PREFIXES = {
            "api/", "swagger", "v3/api-docs", "actuator"
    };

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
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
