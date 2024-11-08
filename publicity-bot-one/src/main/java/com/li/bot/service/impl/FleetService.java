package com.li.bot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.config.BotConfig;
import com.li.bot.entity.database.Button;
import com.li.bot.entity.database.Convoys;
import com.li.bot.entity.database.ConvoysInvite;
import com.li.bot.entity.database.Invite;
import com.li.bot.enums.ConvoysInviteStatus;
import com.li.bot.mapper.ButtonMapper;
import com.li.bot.mapper.ConvoysInviteMapper;
import com.li.bot.mapper.ConvoysMapper;
import com.li.bot.mapper.InviteMapper;
import com.li.bot.utils.BotMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

/**
 * @Author: li
 * @CreateTime: 2024-10-11
 */
@Service
@Slf4j
public class FleetService {

    @Autowired
    private BotConfig botConfig ;

    @Autowired
    private ConvoysMapper convoysMapper ;

    @Autowired
    private TaskScheduler taskScheduler;


    @Autowired
    private ConvoysInviteMapper convoysInviteMapper ;

    @Autowired
    private InviteMapper inviteMapper ;

    @Autowired
    private ButtonMapper buttonMapper ;

    @Autowired
    private FileService fileService ;

    @Autowired
    private BotServiceImpl bot ;


    private Map<Long, ScheduledFuture<?>> scheduledTasks = new HashMap<>();

    @PostConstruct
    public void init() {
        List<Convoys> convoyList = convoysMapper.selectList(null);
        for (Convoys convoys : convoyList) {
            scheduleConvoyTask(convoys);
        }
    }

    private InlineKeyboardMarkup createInlineKeyboardButton(){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        List<Button> buttons = buttonMapper.selectList(null);

        buttons.forEach(button -> {
            String name = button.getName();
            String url = button.getUrl();
            buttonList.add(InlineKeyboardButton.builder().text(name).url(url).build());
        });

        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    private void scheduleConvoyTask(Convoys convoys) {
        if (scheduledTasks.containsKey(convoys.getConvoysId())) {
            ScheduledFuture<?> future = scheduledTasks.get(convoys.getConvoysId());
            future.cancel(false);
        }

        int intervalMinutes = convoys.getIntervalMinutes();
        if (intervalMinutes <= 0) {
            // 如果间隔时间小于或等于0，不调度任务
            System.out.println("车队 " + convoys.getName() + " 的间隔时间设置为0或负数，不进行调度。");
            return;
        }

        CronTrigger trigger = new CronTrigger("0 */" + convoys.getIntervalMinutes() + " * * * *");
        ScheduledFuture<?> future = taskScheduler.schedule(() -> runFleetTask(convoys), trigger);
        scheduledTasks.put(convoys.getConvoysId(), future);
    }

    private void runFleetTask(Convoys convoys) {
        if(convoys.getIntervalMinutes() <= 0){
            return;
        }
        // 你的定时任务逻辑
        List<ConvoysInvite> convoysInviteList = convoysInviteMapper.selectList(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getConvoysId, convoys.getConvoysId()).eq(ConvoysInvite::getStatus, ConvoysInviteStatus.BOARDED.getCode()));
        if (convoysInviteList.isEmpty()) {
            System.out.println("车队 " + convoys.getName() + " 的定时任务执行了！当前时间：" + new java.util.Date()+"该车队没成员 没自动推送");
            return;
        }
        List<Invite> inviteList = inviteMapper.getInviteListByIds(convoysInviteList.stream().map(ConvoysInvite::getInviteId).collect(Collectors.toList()));

        for (Invite in : inviteList) {
            GetChat getChat = new GetChat();
            getChat.setChatId(in.getChatId());
            try {
                bot.execute(getChat);
            } catch (TelegramApiException e) {
                ConvoysInvite convoysInvite = convoysInviteMapper.selectOne(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getInviteId, in.getInviteId()).eq(ConvoysInvite::getStatus, ConvoysInviteStatus.BOARDED.getCode()));
                inviteMapper.deleteById(in);
                convoysInviteMapper.deleteById(convoysInvite);
                SendMessage send = SendMessage.builder().chatId(in.getTgId()).text("《"+in.getName()+"》\n检测到机器人发不了消息,机器人已自动退出").parseMode("html").build();
                try {
                    bot.execute(send);
                } catch (TelegramApiException ex) {
                    throw new RuntimeException(ex);
                }
                continue;
            }
            StringBuilder builder = new StringBuilder();
            builder.append("<a href=\"https://"+botConfig.getBotname()+"\">" +"🍑桃子色色互推:"+convoys.getName()+"🍑</a>\n\n" );
//            builder.append("<b>"+fileService.getText() + "</b>\n" );
            builder.append(BotMessageUtils.getConvoysMemberInfoList(inviteList));
            builder.append("\n"+"<b>"+fileService.getButtonText()+ "</b>");
            SendMessage send = SendMessage.builder().chatId(in.getChatId()).text(String.valueOf(builder)).parseMode("html").replyMarkup(createInlineKeyboardButton()).disableWebPagePreview(true).build();
            Message execute = null;
            Long cId = null;
            try {
                execute = bot.execute(send);

                ConvoysInvite c = convoysInviteMapper.selectOne(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getInviteId, in.getInviteId()).eq(ConvoysInvite::getConvoysId, convoys.getConvoysId()));
                Integer messageId = c.getMessageId();
                cId= c.getConvoysId() ;
                if (messageId == null) {
                    convoysInviteMapper.updateMessageIdById(execute.getMessageId(),in.getInviteId(),c.getConvoysId());
                } else {
                    try {
                        bot.execute(DeleteMessage.builder()
                                .chatId(in.getChatId())
                                .messageId(c.getMessageId())
                                .build());
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                    convoysInviteMapper.updateMessageIdById(execute.getMessageId(), in.getInviteId(),c.getConvoysId());
                }
            } catch (Exception e) {
                // 更新messageId为空
                convoysInviteMapper.updateMessageIdById(execute.getMessageId(), in.getInviteId(),cId);
                log.error("车队:{}发送消息失败", convoys.getName());
            }
        }
        System.out.println("车队 " + convoys.getName() + " 的定时任务执行了！当前时间：" + new java.util.Date());
    }

    public void updateFleetInterval(Convoys convoys) {
        Convoys c = convoysMapper.selectById(convoys.getConvoysId());
        if (c != null) {
            c.setIntervalMinutes(convoys.getIntervalMinutes());
            convoysMapper.updateById(c);
            scheduleConvoyTask(c);
        }
    }

    public Long addNewConvoy(Convoys convoy) {
        // 插入新的车队记录
        convoysMapper.insert(convoy);

        // 调度新的定时任务
        scheduleConvoyTask(convoy);

        return convoy.getConvoysId();
    }

    public void deleteConvoy(Long convoyId) {
        // 取消定时任务
        if (scheduledTasks.containsKey(convoyId)) {
            ScheduledFuture<?> future = scheduledTasks.get(convoyId);
            future.cancel(false);
            scheduledTasks.remove(convoyId);
        }
        // 从数据库中删除车队记录
        convoysMapper.deleteById(convoyId);
    }




}
