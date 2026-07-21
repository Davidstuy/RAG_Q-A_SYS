# Agent 工具接口设计

## 🎯 工具接口标准

### 1. 基础工具接口
```java
public interface Tool {
    /**
     * 工具名称
     */
    String name();
    
    /**
     * 工具描述
     */
    String description();
    
    /**
     * 执行工具
     */
    ToolExecution execute(ToolInput input);
    
    /**
     * 工具参数定义
     */
    List<ToolParameter> parameters();
}
```

### 2. 工具输入
```java
@Data
public class ToolInput {
    private Map<String, Object> parameters;
    private String sessionId;
    private List<String> context; // 对话上下文
}
```

### 3. 工具输出
```java
@Data
public class ToolExecution {
    private boolean success;
    private Object result;
    private String errorMessage;
    private Map<String, Object> metadata;
}
```

---

## 🛠️ 具体工具实现

### 1. 天气查询工具
```java
public class WeatherTool implements Tool {
    
    @Override
    public String name() {
        return "weather_query";
    }
    
    @Override
    public String description() {
        return "查询指定城市的天气信息";
    }
    
    @Override
    public List<ToolParameter> parameters() {
        return List.of(
            new ToolParameter("city", "城市名称", true),
            new ToolParameter("date", "日期（可选）", false)
        );
    }
    
    @Override
    public ToolExecution execute(ToolInput input) {
        try {
            String city = (String) input.getParameters().get("city");
            String date = (String) input.getParameters().get("date");
            
            // 调用天气API
            WeatherResponse weather = weatherService.getWeather(city, date);
            
            return ToolExecution.success(weather);
        } catch (Exception e) {
            return ToolExecution.error("天气查询失败: " + e.getMessage());
        }
    }
}
```

### 2. 汇率转换工具
```java
public class CurrencyConverterTool implements Tool {
    
    @Override
    public String name() {
        return "currency_converter";
    }
    
    @Override
    public String description() {
        return "货币汇率转换";
    }
    
    @Override
    public List<ToolParameter> parameters() {
        return List.of(
            new ToolParameter("from", "源货币", true),
            new ToolParameter("to", "目标货币", true),
            new ToolParameter("amount", "金额", true)
        );
    }
    
    @Override
    public ToolExecution execute(ToolInput input) {
        try {
            String from = (String) input.getParameters().get("from");
            String to = (String) input.getParameters().get("to");
            double amount = Double.parseDouble((String) input.getParameters().get("amount"));
            
            // 调用汇率API
            double rate = exchangeRateService.getRate(from, to);
            double result = amount * rate;
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("original_amount", amount);
            resultData.put("from_currency", from);
            resultData.put("to_currency", to);
            resultData.put("rate", rate);
            resultData.put("converted_amount", result);
            
            return ToolExecution.success(resultData);
        } catch (Exception e) {
            return ToolExecution.error("汇率转换失败: " + e.getMessage());
        }
    }
}
```

### 3. 文档检索工具（基于现有RAG）
```java
public class DocumentSearchTool implements Tool {
    
    @Override
    public String name() {
        return "document_search";
    }
    
    @Override
    public String description() {
        return "在文档中搜索信息";
    }
    
    @Override
    public List<ToolParameter> parameters() {
        return List.of(
            new ToolParameter("query", "搜索查询", true),
            new ToolParameter("top_k", "返回结果数量", false)
        );
    }
    
    @Override
    public ToolExecution execute(ToolInput input) {
        try {
            String query = (String) input.getParameters().get("query");
            int topK = Integer.parseInt((String) input.getParameters().getOrDefault("top_k", "5"));
            
            // 调用现有的RAG服务
            ChatResponse response = chatService.chat(new ChatRequest(query, topK), input.getSessionId());
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("answer", response.getAnswer());
            resultData.put("sources", response.getSources());
            resultData.put("relevance_score", response.getRelevanceScore());
            
            return ToolExecution.success(resultData);
        } catch (Exception e) {
            return ToolExecution.error("文档搜索失败: " + e.getMessage());
        }
    }
}
```

---

## 🎯 工具注册与管理

### 工具注册中心
```java
@Component
public class ToolRegistry {
    
    private final Map<String, Tool> tools = new HashMap<>();
    
    @PostConstruct
    public void init() {
        // 注册所有工具
        registerTool(new WeatherTool());
        registerTool(new CurrencyConverterTool());
        registerTool(new DocumentSearchTool());
        // 可以添加更多工具...
    }
    
    public void registerTool(Tool tool) {
        tools.put(tool.name(), tool);
    }
    
    public Tool getTool(String name) {
        return tools.get(name);
    }
    
    public List<Tool> getAllTools() {
        return new ArrayList<>(tools.values());
    }
}
```

---

## 🔧 工具调用示例

### 单工具调用
```java
// 用户: "帮我查一下北京明天天气"
// 意图识别: 天气查询
// 工具选择: WeatherTool
// 参数: city="北京", date="明天"

ToolInput input = new ToolInput();
input.setParameters(Map.of(
    "city", "北京",
    "date", "明天"
));
input.setsessionId("session123");

ToolExecution result = weatherTool.execute(input);
```

### 多工具串联
```java
// 用户: "帮我查一下北京今天的天气，然后如果下雨的话，提醒我带伞"
// 步骤1: 查天气
ToolExecution weatherResult = weatherTool.execute(input);

// 步骤2: 判断是否下雨
boolean isRaining = ((WeatherResponse) weatherResult.getResult()).isRaining();

// 步骤3: 生成提醒
if (isRaining) {
    return "北京今天会下雨，记得带伞！";
} else {
    return "北京今天不会下雨，可以放心出门。";
}
```