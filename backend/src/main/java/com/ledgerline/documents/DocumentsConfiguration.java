package com.ledgerline.documents;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DocumentStorageProperties.class)
class DocumentsConfiguration {}
