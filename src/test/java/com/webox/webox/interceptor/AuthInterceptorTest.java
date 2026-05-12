package com.webox.webox.interceptor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthInterceptorTest {

    @Autowired
    MockMvc mvc;

    @Test
    void unauthenticated_menu_redirects_to_login() throws Exception {
        mvc.perform(get("/menu"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrl("/login"));
    }

    @Test
    void static_resources_not_blocked() throws Exception {
        mvc.perform(get("/css/app.css")).andExpect(status().isNotFound());
    }
}
