package com.li.bot.handle.message;

import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class HelpMessage implements IMessage {

    @Autowired
    private UserMapper userMapper;


    @Override
    public String getMessageName() {
        return "/help";
    }


    @Override
    public synchronized void execute(BotServiceImpl bot, Message message) {
        try {
            String text = "\uD83E\uDD16欢迎使用茶社抽奖Bot，命令说明\n" +
                    "/start 抽奖id -开始抽奖\n" +
                    "/view 抽奖id -创建抽奖者查看中奖用户\n" +
                    "/exchange 中奖id -创建抽奖者核销中奖者奖励\n\n" +
                    "功能:\n" +
                    "<b>创建抽奖者</b>\n"+
                    "1. 发送<b>gift 积分 个数</b> 创建积分红包抽奖\n" +
                    "2. 创建红包抽奖后可选择抽奖条件(必须加入群聊|订阅频道)才能抽奖\n" +
                    "<b>抽奖条件注意:</b>\n" +
                    "<b>⚠\uFE0F选择加入群聊 需把机器人拉到群聊并设置管理员,群聊必须设置公开群聊.否则抽奖无效</b>\n" +
                    "<b>⚠\uFE0F选择订阅频道 需把机器人拉到频道并设置管理员.否则抽奖无效</b>\n" +
                    "3. 将抽奖信息转发到<b>频道|群聊</b>成员进行抽奖\n"+
                    "4. 抽奖结束后，抽奖者可查看中奖用户，并核销奖励\n\n" +
                    "<b>中奖用户</b>\n"+
                    "1. 保存中奖id并及时找<b>创建抽奖者</b>核销,核销成功后转成积分\n" +
                    "2. 可在内置键盘中的个人中心<b>提现</b>和查看中奖记录\n";
            bot.execute(SendMessage.builder().chatId(message.getChatId()).text(text).parseMode("html").build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        return;
    }
}
