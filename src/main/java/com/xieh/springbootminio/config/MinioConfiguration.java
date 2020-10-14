package com.xieh.springbootminio.config;

import com.xieh.springbootminio.prop.MinioProperties;
import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 谢辉
 * @Classname MinioConfiguration
 * @Description Minio的配置类
 * @Date 2020/10/13 21:35
 */

@Configuration
public class MinioConfiguration {
    @Autowired
    private MinioProperties minioProp;

    @Bean
    public MinioClient minioClient() throws InvalidPortException, InvalidEndpointException {
        MinioClient client = new MinioClient(minioProp.getEndpoint(), minioProp.getAccessKey(), minioProp.getSecretKey());
        return client;
    }
}
