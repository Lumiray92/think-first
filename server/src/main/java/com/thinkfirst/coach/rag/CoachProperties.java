package com.thinkfirst.coach.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** coach 语料与检索配置。 */
@Component
@ConfigurationProperties(prefix = "coach")
public class CoachProperties {

    private boolean enabled = true;

    /** 语料目录，开发期指向 ThinkFirst/app/rag/course07。 */
    private String dataDir = "../app/rag/course07";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }
}
