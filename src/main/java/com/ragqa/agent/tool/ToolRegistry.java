package com.ragqa.agent.tool;

import com.ragqa.agent.model.IntentType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工具註冊中心
 * 管理所有可用的工具
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolRegistry {

    private final List<Tool> toolBeans;
    private final Map<String, Tool> toolsByName = new HashMap<>();
    private final Map<IntentType, Tool> toolsByIntent = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("初始化工具註冊中心...");
        for (Tool tool : toolBeans) {
            registerTool(tool);
            log.info("註冊工具: {} - {}", tool.name(), tool.description());
        }
        log.info("工具註冊完成，共 {} 個工具", toolsByName.size());
    }

    public void registerTool(Tool tool) {
        toolsByName.put(tool.name(), tool);
        toolsByIntent.put(tool.intentType(), tool);
    }

    public Tool getTool(String name) {
        return toolsByName.get(name);
    }

    public Tool getToolForIntent(IntentType intentType) {
        return toolsByIntent.get(intentType);
    }

    public List<Tool> getAllTools() {
        return new ArrayList<>(toolsByName.values());
    }

    public List<String> getAllToolNames() {
        return toolsByName.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
    }

    public boolean hasTool(String name) {
        return toolsByName.containsKey(name);
    }
}
