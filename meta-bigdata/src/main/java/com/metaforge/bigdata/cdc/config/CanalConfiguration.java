package com.metaforge.bigdata.cdc.config;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.metaforge.bigdata.cdc.component.CanalClientService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

/**
 * CDC Canal 配置
 */
@Configuration
@Getter
public class CanalConfiguration implements CommandLineRunner {

    @Autowired
    private CanalClientService canalClientService;

    @Value("${canal.server.host:127.0.0.1}")
    private String canalHost;

    @Value("${canal.server.port:11111}")
    private int canalPort;

    @Value("${canal.destination:example}")
    private String destination;

    @Value("${canal.username:}")
    private String username;

    @Value("${canal.password:}")
    private String password;

    @Override
    public void run(String... args) throws Exception {
        CanalConnector connector;
        // 创建Canal连接器
        if (username != null && !username.isEmpty()) {
            connector = CanalConnectors.newSingleConnector(
                    new InetSocketAddress(canalHost, canalPort),
                    destination, username, password);
        } else {
            connector = CanalConnectors.newSingleConnector(
                    new InetSocketAddress(canalHost, canalPort),
                    destination, "", "");
        }

        // 启动Canal客户端同步CDC守护线程
        if (canalClientService != null) {
            canalClientService.startCanalClientByDemon(connector);
        }
    }
}
