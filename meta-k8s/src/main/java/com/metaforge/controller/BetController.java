package com.metaforge.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

@Controller
@RequestMapping("/")
public class BetController {
    // 使用AtomicReference保证可见性，配合同步块实现原子操作
    private static final AtomicReference<Double> BANKER_BALANCE = new AtomicReference<>(10000.0);
    private static final double HOUSE_EDGE = 0.97;
    private static final String SESSION_BALANCE = "balance";
    private static final String BOSS_BALANCE = "bankerBalance";
    private static final double INIT_PLAYER_BALANCE = 1000.0;

    @GetMapping
    public String index(HttpSession session, Model model) {
        initBalance(session);
        model.addAttribute("balance", session.getAttribute(SESSION_BALANCE));
        model.addAttribute("bankerBalance", BANKER_BALANCE.get());
        return "index";
    }

    @PostMapping("/bet")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> placeBet(
            @RequestParam BigDecimal amount,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        double playerBalance = getCurrentBalance(session);

        try {
            validateBet(amount, playerBalance);

            boolean win = new Random().nextBoolean();
            double payout = calculatePayout(amount);

            synchronized (BANKER_BALANCE) {
                double currentBanker = BANKER_BALANCE.get();

                if (win) {
                    // 验证庄家资金
                    if (currentBanker < payout) {
                        throw new IllegalStateException("庄家资金不足，无法支付奖金");
                    }
                    // 资金转移：庄家 -> 玩家
                    BANKER_BALANCE.set(currentBanker - payout);
                    playerBalance += payout;
                } else {
                    // 资金转移：玩家 -> 庄家
                    BANKER_BALANCE.set(currentBanker + amount.doubleValue());
                    playerBalance -= amount.doubleValue();
                }
            }

            session.setAttribute(SESSION_BALANCE, playerBalance);

            response.put("success", true);
            response.put("balance", playerBalance);
            response.put("bankerBalance", BANKER_BALANCE.get());
            response.put("message", buildMessage(win, payout));

        } catch (IllegalArgumentException | IllegalStateException e) {
            response.put("success", false);
            response.put("bankerBalance", BANKER_BALANCE.get());
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    // 其他辅助方法保持不变...

    private String buildMessage(boolean win, double payout) {
        double banker = BANKER_BALANCE.get();
        if (win) {
            return String.format("获胜！赢得 %.2f 元（庄家余额：%.2f）", payout, banker);
        }
        return String.format("未中奖（庄家余额：%.2f）", banker);
    }

    private void initBalance(HttpSession session) {
        session.setAttribute(SESSION_BALANCE, INIT_PLAYER_BALANCE);
        session.setAttribute(BOSS_BALANCE, BANKER_BALANCE.get());
    }

    private double getCurrentBalance(HttpSession session) {
        return (double) session.getAttribute(SESSION_BALANCE);
    }

    private void validateBet(BigDecimal amount, double balance) {
        double val = amount.doubleValue();
        if (val <= 0) throw new IllegalArgumentException("投注金额必须大于0");
        if (val > balance) throw new IllegalArgumentException("余额不足");
        if (val > 10000) throw new IllegalArgumentException("单次投注上限为10000元");
    }

    private double calculatePayout(BigDecimal amount) {
        return amount.doubleValue() * (1 - HOUSE_EDGE);
    }
}
