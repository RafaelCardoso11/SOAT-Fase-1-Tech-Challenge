package com.fiap.soat.oficina.config

import com.fiap.soat.oficina.autenticacao.adapter.inbound.web.JwtAuthenticationFilter
import com.fiap.soat.oficina.autenticacao.domain.port.outbound.TokenProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val tokenProvider: TokenProvider,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/api/auth/**",
                    "/api/public/**",
                    "/api/ping",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/health",
                ).permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(
                JwtAuthenticationFilter(tokenProvider),
                UsernamePasswordAuthenticationFilter::class.java,
            )
        return http.build()
    }
}
