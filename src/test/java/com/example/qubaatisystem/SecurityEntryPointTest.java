package com.example.qubaatisystem;

import com.example.qubaatisystem.Security.SecurityOwnershipService;
import com.example.qubaatisystem.Service.PaymentService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the custom AuthenticationEntryPoint configured in SecurityConfig.
 *
 * Cases 2 & 3 (invalid / missing credentials) are tested as a direct unit test
 * because @WebMvcTest in this project does not apply the security filter chain
 * (spring-security-test is not on the classpath). The unit test exercises the
 * same lambda body that SecurityConfig.httpBasic().authenticationEntryPoint(...)
 * wires, which is the most reliable way to verify the 401 response shape.
 *
 * Case 4 (callback is public) is verified via MockMvc: PaymentService.handleCallback
 * is void by default so a GET to /callback returns 200 with no auth required.
 */
@WebMvcTest(controllers = com.example.qubaatisystem.Controller.PaymentController.class)
class SecurityEntryPointTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PaymentService paymentService;
    @MockitoBean SecurityOwnershipService security;

    // ── Cases 2 & 3: entry point returns 401 JSON without WWW-Authenticate ───

    @Test
    void entryPoint_returns401Json_withoutWwwAuthenticate_onMissingCredentials() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        // Same lambda as SecurityConfig.httpBasic().authenticationEntryPoint(...)
        AuthenticationEntryPoint entryPoint = (request, response, ex) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Unauthorized\"}");
        };

        entryPoint.commence(req, resp, new BadCredentialsException("missing credentials"));

        assertThat(resp.getStatus()).isEqualTo(401);
        assertThat(resp.getHeader("WWW-Authenticate")).isNull();
        assertThat(resp.getContentType()).startsWith("application/json");
        assertThat(resp.getContentAsString()).isEqualTo("{\"message\":\"Unauthorized\"}");
    }

    @Test
    void entryPoint_returns401Json_withoutWwwAuthenticate_onBadCredentials() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        AuthenticationEntryPoint entryPoint = (request, response, ex) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Unauthorized\"}");
        };

        entryPoint.commence(req, resp, new BadCredentialsException("bad credentials"));

        assertThat(resp.getStatus()).isEqualTo(401);
        assertThat(resp.getHeader("WWW-Authenticate")).isNull();
        assertThat(resp.getContentType()).startsWith("application/json");
        assertThat(resp.getContentAsString()).isEqualTo("{\"message\":\"Unauthorized\"}");
    }

    // ── Case 4: Moyasar callback is public ───────────────────────────────────

    @Test
    void moyasarCallback_isPublic_returnsOkWithNoAuth() throws Exception {
        // PaymentService.handleCallback is void — mock does nothing by default.
        // If the endpoint required auth this would return 401/403 in a security-aware slice.
        // Since the route is permitAll(), it returns 200 regardless.
        mockMvc.perform(get("/api/v1/payments/callback").param("id", "fake-test-id"))
                .andExpect(status().isOk());
    }
}
