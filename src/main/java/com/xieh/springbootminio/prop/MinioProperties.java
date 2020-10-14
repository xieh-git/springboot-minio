package com.xieh.springbootminio.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 谢辉
 * @Classname MinIoProperties
 * @Description Minio的基本配置
 * @Date 2020/10/13 21:17
 */
@Component
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioProperties {
    private String endpoint;
    private String bucketName;
    private String accessKey;
    private String secretKey;

}
