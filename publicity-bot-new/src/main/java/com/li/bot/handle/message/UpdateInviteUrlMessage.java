package com.li.bot.handle.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.li.bot.entity.database.Convoys;
import com.li.bot.entity.database.Invite;
import com.li.bot.mapper.InviteMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FleetService;
import com.li.bot.sessions.UpdateConvoysSession;
import com.li.bot.sessions.UpdateConvoysSessionList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class UpdateInviteUrlMessage implements IMessage{

    @Override
    public String getMessageName() {
        return "updateInviteUrl";
    }

    @Autowired
    private UpdateConvoysSessionList updateConvoysSessionList ;

    @Autowired
    private InviteMapper inviteMapper ;


    @Override
    public void execute(BotServiceImpl bot, Message message) throws TelegramApiException {

        String text = message.getText();

        String regex = "https://[\\w.-]+(?:\\/[\\w.-]*)*";
        // 创建 Pattern 对象
        Pattern pattern = Pattern.compile(regex);
        // 创建Matcher对象
        Matcher matcher = pattern.matcher(text);
        // 检查是否找到匹配项
        if (matcher.matches()) {
            // 提取匹配的内容
            String url = matcher.group();
            UpdateConvoysSession userSession = updateConvoysSessionList.getUserSession(message.getFrom().getId());
            Convoys convoys = userSession.getConvoys();
            Long convoysId = convoys.getConvoysId();
            Invite invite = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getInviteId, convoysId));
            SendMessage sendMessage = SendMessage.builder().chatId(message.getChatId()).text("\n《" + invite.getName() + "》\n旧链接:" + invite.getLink() + "\n新链接:" + url + "\n更新成功").disableWebPagePreview(true).build();
            bot.execute(sendMessage);
            invite.setLink(url);
            inviteMapper.updateById(invite);
        }else {
            bot.execute(SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("输入有误,请以https开头")
                    .build());
        }
        updateConvoysSessionList.removeUserSession(message.getFrom().getId());
        }
}
