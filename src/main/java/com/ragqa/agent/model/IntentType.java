package com.ragqa.agent.model;

/**
 * 意圖類型枚舉
 * 定義系統支持的所有用戶意圖
 */
public enum IntentType {
    WEATHER_QUERY("天氣查詢", "查詢天氣相關信息"),
    CURRENCY_CONVERT("匯率轉換", "貨幣匯率轉換"),
    DOCUMENT_SEARCH("文檔檢索", "在文檔中搜索信息"),
    CALCULATION("計算", "數學計算"),
    REMINDER("提醒", "設置提醒"),
    GENERAL_CHAT("一般對話", "普通對話交流"),
    UNKNOWN("未知", "無法識別的意圖");

    private final String name;
    private final String description;

    IntentType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
