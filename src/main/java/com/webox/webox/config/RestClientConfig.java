package com.webox.webox.config;
import com.webox.webox.service.ai.QwenProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(QwenProperties.class)
public class RestClientConfig {
    @Bean
    public RestClient aiRestClient(QwenProperties props) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5000);
        rf.setReadTimeout(props.getTimeoutMs() > 0 ? props.getTimeoutMs() : 20000);
        return RestClient.builder().requestFactory(rf).build();
    }
}
