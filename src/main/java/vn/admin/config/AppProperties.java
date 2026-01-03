package vn.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private long customersFirstPageCacheTtlMs = 0L;

    public long getCustomersFirstPageCacheTtlMs() { return customersFirstPageCacheTtlMs; }
    public void setCustomersFirstPageCacheTtlMs(long customersFirstPageCacheTtlMs) { this.customersFirstPageCacheTtlMs = customersFirstPageCacheTtlMs; }
}
