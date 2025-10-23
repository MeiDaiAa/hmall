package com.hmall.gateway.route;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouterLoader{
    private final RouteDefinitionWriter writer;
    private final NacosConfigManager nacosConfigManager;

    String dataId = "gateway-routes.json";
    String group = "DEFAULT_GROUP";

    @PostConstruct
    public void initRouteConfigListener() throws NacosException {
        String configAndSignListener = nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dataId, group, 5000, new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String s) {
                        updateConfinInfo(s);
                    }
                });
        updateConfinInfo(configAndSignListener);
    }
    private Set<String> routeIds = new HashSet<>();
    private void updateConfinInfo(String s) {
        log.debug("更新路由配置信息:{}", s);
        // 解析路由信息
        List<RouteDefinition> list = JSONUtil.toList(s, RouteDefinition.class);
        // 删除所有路由
        for (String routeId : routeIds) {
            writer.delete(Mono.just(routeId)).subscribe();
        }
        routeIds.clear();
        for(RouteDefinition routeDefinition : list) {
            writer.save(Mono.just(routeDefinition)).subscribe();
            routeIds.add(routeDefinition.getId());
        }
    }
}
