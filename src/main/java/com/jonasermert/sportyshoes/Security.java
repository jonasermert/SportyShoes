package com.jonasermert.sportyshoes;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
class Security {
  @Bean
  PasswordEncoder passwords() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService users(Accounts accounts) {
    return email ->
        accounts
            .findByEmail(email)
            .map(a -> User.withUsername(a.email).password(a.password).roles(a.role).build())
            .orElseThrow(() -> new UsernameNotFoundException("Unknown account"));
  }

  @Bean
  SecurityFilterChain filter(HttpSecurity http) throws Exception {
    http.csrf()
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .and()
        .authorizeRequests()
        .antMatchers("/api/admin/**")
        .hasRole("ADMIN")
        .antMatchers("/api/me", "/api/password", "/api/purchases/**")
        .authenticated()
        .antMatchers("/api/**")
        .permitAll()
        .anyRequest()
        .permitAll()
        .and()
        .formLogin()
        .loginProcessingUrl("/api/login")
        .successHandler((q, r, a) -> r.setStatus(204))
        .failureHandler((q, r, e) -> r.sendError(401, "Invalid credentials"))
        .and()
        .logout()
        .logoutUrl("/api/logout")
        .logoutSuccessHandler((q, r, a) -> r.setStatus(204))
        .and()
        .exceptionHandling()
        .authenticationEntryPoint((q, r, e) -> r.sendError(401, "Sign in required"));
    return http.build();
  }
}
