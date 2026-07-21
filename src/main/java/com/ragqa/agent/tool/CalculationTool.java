package com.ragqa.agent.tool;

import com.ragqa.agent.model.IntentType;
import com.ragqa.agent.model.ToolExecution;
import com.ragqa.agent.model.ToolInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 計算工具
 * 支持基本數學計算
 */
@Slf4j
@Component
public class CalculationTool implements Tool {

    @Override
    public String name() {
        return "calculation";
    }

    @Override
    public String description() {
        return "執行數學計算，支持加減乘除等基本運算";
    }

    @Override
    public IntentType intentType() {
        return IntentType.CALCULATION;
    }

    @Override
    public List<ToolParameter> parameters() {
        return List.of(
            new ToolParameter("expression", "數學表達式（如：2+3*4）", true)
        );
    }

    @Override
    public ToolExecution execute(ToolInput input) {
        String expression = input.getStringParameter("expression");

        if (expression == null || expression.isBlank()) {
            return ToolExecution.error("請提供數學表達式");
        }

        log.info("計算: expression={}", expression);

        try {
            // 簡單計算實現（支持 +, -, *, /）
            double result = evaluateExpression(expression);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("expression", expression);
            metadata.put("result", result);

            return ToolExecution.success(
                String.format("%s = %.2f", expression, result),
                metadata
            );
        } catch (Exception e) {
            log.error("計算失敗", e);
            return ToolExecution.error("計算失敗: " + e.getMessage());
        }
    }

    /**
     * 簡單表達式計算
     * 注意：這是一個簡化實現，生產環境應使用更安全的表達式解析器
     */
    private double evaluateExpression(String expression) {
        // 移除空格
        expression = expression.replaceAll("\\s+", "");

        // 處理基本的四則運算
        // 使用簡單的狀態機解析
        double result = 0;
        double currentNumber = 0;
        char operator = '+';

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (Character.isDigit(c) || c == '.') {
                // 構建數字
                StringBuilder numberStr = new StringBuilder();
                while (i < expression.length() &&
                       (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    numberStr.append(expression.charAt(i));
                    i++;
                }
                i--; // 回退一位
                currentNumber = Double.parseDouble(numberStr.toString());
            }

            if (!Character.isDigit(c) && c != '.' || i == expression.length() - 1) {
                // 遇到運算符或到達末尾，執行上一個運算
                switch (operator) {
                    case '+':
                        result += currentNumber;
                        break;
                    case '-':
                        result -= currentNumber;
                        break;
                    case '*':
                        result *= currentNumber;
                        break;
                    case '/':
                        if (currentNumber == 0) {
                            throw new ArithmeticException("除數不能為零");
                        }
                        result /= currentNumber;
                        break;
                }

                if (c == '+' || c == '-' || c == '*' || c == '/') {
                    operator = c;
                }
                currentNumber = 0;
            }
        }

        return result;
    }
}
