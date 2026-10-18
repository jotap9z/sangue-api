package com.sangueplus.api.config

import com.sangueplus.api.services.AcessoEquipeService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Component
class EquipeInterceptor(
    private val acesso: AcessoEquipeService
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val caminho = request.requestURI
            .removePrefix(request.contextPath)

        val base = "/api/hemocentros/me"

        if (caminho != base && !caminho.startsWith("$base/")) {
            return true
        }

        if (request.method == "OPTIONS") {
            return true
        }

        if (
            request.method == "POST" &&
            caminho == "$base/equipe/inicializar"
        ) {
            // O próprio serviço verifica se a inicialização é permitida.
            return true
        }

        acesso.autorizar(
            request.getHeader("Authorization"),
            request.method,
            caminho
        )

        return true
    }
}

@Configuration
class EquipeWebConfig(
    private val interceptor: EquipeInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(
        registry: InterceptorRegistry
    ) {
        registry.addInterceptor(interceptor)
            .addPathPatterns(
                "/api/hemocentros/me",
                "/api/hemocentros/me/**"
            )
    }
}