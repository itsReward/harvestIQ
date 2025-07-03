package com.maizeyield.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.core.Ordered

@Configuration
class WebConfig(
    private val apiLoggingInterceptor: ApiLoggingInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(apiLoggingInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/health",
                "/api/actuator/**",
                "/swagger-ui/**",
                "/api-docs/**"
            )
    }

    @Bean
    fun contentCachingFilter(): FilterRegistrationBean<ContentCachingFilter> {
        val registrationBean = FilterRegistrationBean<ContentCachingFilter>()
        registrationBean.filter = ContentCachingFilter()
        registrationBean.addUrlPatterns("/api/*")
        registrationBean.order = Ordered.HIGHEST_PRECEDENCE
        return registrationBean
    }
}

class ContentCachingFilter : Filter {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        val wrappedRequest = ContentCachingRequestWrapper(httpRequest)
        val wrappedResponse = ContentCachingResponseWrapper(httpResponse)

        try {
            chain.doFilter(wrappedRequest, wrappedResponse)
        } finally {
            // Important: Copy cached content back to the original response
            wrappedResponse.copyBodyToResponse()
        }
    }
}