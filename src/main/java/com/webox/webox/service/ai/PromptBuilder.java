package com.webox.webox.service.ai;

import com.webox.webox.entity.MenuItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
            你是 WeBox 企业员工餐食订购平台的 AI 美食推荐助手。
            严格遵守以下要求：
            1. 只能从给定的"当日菜单"中挑选推荐菜品，严禁发明不存在的菜品或编造 code。
            2. 考虑用户的自然语言诉求（如口味、辣度、健康偏好、饱腹感等）。
            3. 必须避开用户标注的所有过敏原。
            4. 推荐 3-5 个菜品，按匹配度由高到低排序。
            5. 输出必须是严格的 JSON，schema 如下（不要包含任何其他文字）：
               {"recommendations":[{"code":"item_xxx","reason":"一句中文推荐理由"}]}
            """;

    public List<Map<String, String>> build(String query, List<String> userAllergens, List<MenuItem> menu) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(msg("system", SYSTEM_PROMPT));

        StringBuilder user = new StringBuilder();
        user.append("用户诉求：").append(query == null ? "" : query.trim()).append('\n');
        if (userAllergens != null && !userAllergens.isEmpty()) {
            user.append("用户过敏原（必须避开）：").append(String.join(", ", userAllergens)).append('\n');
        }
        user.append("当日菜单（JSON 数组）：\n[");
        for (int i = 0; i < menu.size(); i++) {
            MenuItem m = menu.get(i);
            if (i > 0) {
                user.append(',');
            }
            user.append('\n').append("  {")
                    .append("\"code\":\"").append(escape(m.getCode())).append("\",")
                    .append("\"name\":\"").append(escape(m.getName())).append("\",")
                    .append("\"description\":\"").append(escape(m.getDescription())).append("\",")
                    .append("\"category\":\"").append(escape(m.getCategory())).append("\",")
                    .append("\"price\":").append(m.getPrice() == null ? "0" : m.getPrice().toPlainString()).append(",")
                    .append("\"allergens\":\"").append(escape(m.getAllergens() == null ? "" : m.getAllergens())).append("\"")
                    .append("}");
        }
        user.append("\n]\n");
        user.append("请输出 JSON。");
        messages.add(msg("user", user.toString()));
        return messages;
    }

    private static Map<String, String> msg(String role, String content) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}
