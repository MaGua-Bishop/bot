package com.li.bot.entity.vo;

/**
 * @Author: li
 * @CreateTime: 2024-10-24
 */
public class RandomPrizePoolVO {

    private String prizePoolId;

    private Long lotteryCreateTgId;

    public RandomPrizePoolVO(String prizePoolId, Long lotteryCreateTgId) {
        this.prizePoolId = prizePoolId;
        this.lotteryCreateTgId = lotteryCreateTgId;
    }

    public String getPrizePoolId() {
        return prizePoolId;
    }

    public void setPrizePoolId(String prizePoolId) {
        this.prizePoolId = prizePoolId;
    }

    public Long getLotteryCreateTgId() {
        return lotteryCreateTgId;
    }

    public void setLotteryCreateTgId(Long lotteryCreateTgId) {
        this.lotteryCreateTgId = lotteryCreateTgId;
    }
}
