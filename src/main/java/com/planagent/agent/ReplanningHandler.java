package com.planagent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReplanningHandler {
    private static final Logger log = LoggerFactory.getLogger(ReplanningHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public enum FailureType {
        SUCCESS,
        FULLY_BOOKED,
        NOT_AVAILABLE,
        UNKNOWN
    }

    public FailureType analyze(String toolResultJson) {
        try {
            var node = mapper.readTree(toolResultJson);

            // Search/info tools (weather, route, activities, restaurants) have no status field
            // They always succeed — they just return information
            if (!node.has("status")) {
                return FailureType.SUCCESS;
            }

            String status = node.get("status").asText("");

            // Success: booking/order/payment/notification confirmed
            if (status.contains("已预订") || status.contains("已下单") || status.contains("支付成功")
                || status.contains("已支付") || status.contains("已发送")) {
                return FailureType.SUCCESS;
            }

            // Failure classifications
            if (status.contains("已满") || status.contains("已约满") || status.contains("满座")) {
                return FailureType.FULLY_BOOKED;
            }
            if (status.contains("不可用") || status.contains("已下架") || status.contains("不存在")
                || status.contains("失败")) {
                return FailureType.NOT_AVAILABLE;
            }

            return FailureType.UNKNOWN;
        } catch (Exception e) {
            log.warn("Failed to parse tool result: {}", e.getMessage());
            return FailureType.UNKNOWN;
        }
    }

    public String generateReplanPromptWithExpandedSearch(String failedTool, String failedItem,
                                                          String failureReason, String currentPlan,
                                                          int attemptNumber) {
        return """
            %s (%s) %s。这是第%d次尝试。

            当前计划:
            %s

            请进行局部调整: 仅替换失败项，将搜索距离放宽到%dkm。
            仅输出替换方案的搜索和预订步骤，不要重新规划整个行程。
            """.formatted(failedItem, failedTool, failureReason, attemptNumber,
                currentPlan, 5 + attemptNumber * 3);
    }

    public boolean isFailure(FailureType type) {
        return type == FailureType.FULLY_BOOKED
            || type == FailureType.NOT_AVAILABLE
            || type == FailureType.UNKNOWN;
    }
}
