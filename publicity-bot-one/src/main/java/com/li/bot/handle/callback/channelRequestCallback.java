package com.li.bot.handle.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.config.BotConfig;
import com.li.bot.entity.database.Convoys;
import com.li.bot.entity.database.ConvoysInvite;
import com.li.bot.entity.database.Invite;
import com.li.bot.entity.database.User;
import com.li.bot.enums.ConvoysInviteStatus;
import com.li.bot.mapper.ConvoysInviteMapper;
import com.li.bot.mapper.ConvoysMapper;
import com.li.bot.mapper.InviteMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import com.li.bot.utils.UnitConversionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class channelRequestCallback implements ICallback{

    @Override
    public String getCallbackName() {
        return "channelRequest";
    }

    @Autowired
    private ConvoysMapper convoysMapper ;

    @Autowired
    private InviteMapper inviteMapper ;

    @Autowired
    private ConvoysInviteMapper convoysInviteMapper ;



    @Autowired
    private BotConfig botConfig ;

    @Autowired
    private UserMapper userMapper ;

    private Long currentConvoysCapacity = 0L ;

    @Autowired
    private FileService fileService ;


    private void getConvoysCapacity(Long convoysId){
        Long countById = convoysInviteMapper.getCountById(convoysId);
        if(countById == null){
            currentConvoysCapacity = 0L;
        }else {
            currentConvoysCapacity = countById ;
        }
    }

    public InlineKeyboardMarkup createInlineKeyboardButton(Long tgId, Long convoysId, Long capacity) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, tgId));

        if (currentConvoysCapacity >= capacity) {
            buttonList.add(InlineKeyboardButton.builder().text("车队已满").callbackData("null").build());
        } else if (user.getIsAdmin()) {
            List<Invite> invites = inviteMapper.selectList(new LambdaQueryWrapper<Invite>().eq(Invite::getTgId, tgId));
            for (Invite invite : invites) {
                ConvoysInvite convoysInvite = convoysInviteMapper.getConvoysInviteByInviteIdAndStatus(invite.getInviteId());
                Integer status = convoysInvite == null ? ConvoysInviteStatus.IDLE.getCode() : convoysInvite.getStatus();
                String code = "";
                if (status.equals(ConvoysInviteStatus.IDLE.getCode())) {
                    code = "\uD83D\uDFE2";
                } else if (status.equals(ConvoysInviteStatus.REVIEW.getCode())) {
                    code = "\uD83D\uDFE1";
                } else if (status.equals(ConvoysInviteStatus.BOARDED.getCode())) {
                    code = "\uD83D\uDFE3";
                } else if (status.equals(ConvoysInviteStatus.DISABLED.getCode())) {
                    code = "\uD83D\uDD34";
                }
                buttonList.add(InlineKeyboardButton.builder()
                        .text(code + invite.getName()+"|"+UnitConversionUtils.toThousands(invite.getMemberCount()))
                        .callbackData("channelRequest:" + invite.getInviteId() + ":convoysId:" + convoysId)
                        .build());
            }
        } else {
            List<Invite> inviteList = inviteMapper.getInviteListByChatIdAndMemberCount(tgId);
            if (inviteList.isEmpty()) {
                buttonList.add(InlineKeyboardButton.builder().text("未找到符合要求的频道请添加").callbackData("null").build());
            } else {
                for (Invite invite : inviteList) {
                    ConvoysInvite convoysInvite = convoysInviteMapper.getConvoysInviteByInviteIdAndStatus(invite.getInviteId());
                    Integer status = convoysInvite == null ? ConvoysInviteStatus.IDLE.getCode() : convoysInvite.getStatus();
                    String code = "";
                    if (status.equals(ConvoysInviteStatus.IDLE.getCode())) {
                        code = "\uD83D\uDFE2";
                    } else if (status.equals(ConvoysInviteStatus.REVIEW.getCode())) {
                        code = "\uD83D\uDFE1";
                    } else if (status.equals(ConvoysInviteStatus.BOARDED.getCode())) {
                        code = "\uD83D\uDFE3";
                    } else if (status.equals(ConvoysInviteStatus.DISABLED.getCode())) {
                        code = "\uD83D\uDD34";
                    }
                    buttonList.add(InlineKeyboardButton.builder()
                            .text(code + invite.getName()+"|"+UnitConversionUtils.toThousands(invite.getMemberCount()))
                            .callbackData("channelRequest:" + invite.getInviteId() + ":convoysId:" + convoysId)
                            .build());
                }
            }
        }

        // 添加特殊按钮
//        buttonList.add(InlineKeyboardButton.builder()
//                .text("\uD83C\uDF38手机添加")
//                .url("https://" + botConfig.getBotname() + "?startgroup")
//                .build());
//        buttonList.add(InlineKeyboardButton.builder()
//                .text("\uD83C\uDF1E电脑添加")
//                .url("https://" + botConfig.getBotname() + "?startchannel=true")
//                .build());
//        buttonList.add(InlineKeyboardButton.builder()
//                .text("\uD83D\uDD03刷新")
//                .callbackData("selectConvoysInfo:" + convoysId)
//                .build());
        buttonList.add(InlineKeyboardButton.builder()
                .text("\uD83D\uDD19返回")
                .callbackData("returnConvoysList")
                .build());
//        buttonList.add(InlineKeyboardButton.builder()
//                .text("首页")
//                .callbackData("/start")
//                .build());

        // 创建行列表
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

        // 先处理普通按钮，每个单独一行
        for (InlineKeyboardButton button : buttonList.subList(0, buttonList.size() - 1)) {
            rowList.add(Arrays.asList(button));
        }

        // 处理特殊按钮，每两个一行
        List<InlineKeyboardButton> specialButtons = buttonList.subList(buttonList.size() - 1, buttonList.size());
        for (int i = 0; i < specialButtons.size(); i += 2) {
            rowList.add(specialButtons.subList(i, Math.min(i + 2, specialButtons.size())));
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }




    private InlineKeyboardMarkup createInlineKeyboardButton02(Long id){
        List<InlineKeyboardButton> buttonList = new ArrayList<>();

        buttonList.add(InlineKeyboardButton.builder().text("同意").callbackData("adminYesAudi:"+id).build());
        buttonList.add(InlineKeyboardButton.builder().text("拒绝").callbackData("adminNoAudi:"+id).build());

        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 1);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }

    private Convoys selectConvoysInfo(Long convoysId){
        Convoys convoys = convoysMapper.selectOne(new LambdaQueryWrapper<Convoys>().eq(Convoys::getConvoysId, convoysId));
        return convoys;
    }



    @Override
    @Transactional
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();

        Pattern pattern = Pattern.compile(":(\\d+)");
        Matcher matcher = pattern.matcher(data);

        Long inviteId = -1L, convoysId = -1L; // 初始化为-1或其他默认值
        if (matcher.find()) {
            inviteId = Long.valueOf(matcher.group(1));
        }
        if (matcher.find()) {
            convoysId = Long.valueOf(matcher.group(1));
        }
        Convoys convoys = selectConvoysInfo(convoysId);
        getConvoysCapacity(convoysId);

        Long convoysCapacity = convoys.getCapacity();
        Long subscription = convoys.getSubscription();

        Invite invite = inviteMapper.selectOne(new LambdaQueryWrapper<Invite>().eq(Invite::getInviteId, inviteId));
        if(invite == null){
            bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("请检查机器人是否离开了").build());
            return;
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, callbackQuery.getFrom().getId()));
        if (!user.getIsAdmin()) {
            if(invite.getMemberCount() < subscription){
                bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("<b>《"+invite.getName()+"》</b>\n"+"订阅量不满足该车队要求").parseMode("html").build());
                return;
            }
        }

        ConvoysInvite convoysInvite = convoysInviteMapper.getConvoysInviteByInviteIdAndStatus(invite.getInviteId());
        if(convoysInvite != null){
            bot.execute(SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text("您该频道已申请过车队,请勿重复申请").build());
            return;
        }
        convoysInvite = new ConvoysInvite();
        convoysInvite.setConvoysId(convoysId);
        convoysInvite.setInviteId(inviteId);
        convoysInvite.setStatus(ConvoysInviteStatus.REVIEW.getCode());
        convoysInviteMapper.insert(convoysInvite);

        //发消息提示用户
        SendMessage sendMessage = SendMessage.builder().chatId(callbackQuery.getMessage().getChatId()).text(invite.getName() + "已申请,请等待审核").build();
        bot.execute(sendMessage);
        inviteMapper.updateById(invite);

        EditMessageReplyMarkup editMessageReplyMarkup = EditMessageReplyMarkup.builder().chatId(callbackQuery.getMessage().getChatId()).messageId(callbackQuery.getMessage().getMessageId()).replyMarkup(createInlineKeyboardButton(callbackQuery.getFrom().getId(),convoysId,convoys.getCapacity())).build();
        bot.execute(editMessageReplyMarkup);

        //发送消息给频道管理员同意或拒绝加入


        Long string = fileService.getAddAdminGroup().getId();

        Integer status = convoysInvite.getStatus();
        String msg = "";
        String code ="";
        if(status.equals(ConvoysInviteStatus.IDLE.getCode())){
            code = "\uD83D\uDFE2";
            msg = "空闲";
        }else if(status.equals(ConvoysInviteStatus.REVIEW.getCode())){
            code = "\uD83D\uDFE1";
            msg = "待审核";
        }else if(status.equals(ConvoysInviteStatus.BOARDED.getCode())){
            code = "\uD83D\uDFE3";
            msg = "审核成功";
        }else if(status.equals(ConvoysInviteStatus.DISABLED.getCode())){
            code = "\uD83D\uDD34";
            msg = "被禁用";
        }

        String text = "申请车队名: " + convoys.getName() + "\n"
                + "车队类型: 频道\n"
                + "车队介绍: " + convoys.getCopywriter() + "\n"
                + "当前/最大(成员): " +currentConvoysCapacity + "/" + convoys.getCapacity() + "\n"
                + "最低订阅: " + UnitConversionUtils.tensOfThousands(convoys.getSubscription()) + "\n"+
                "最低阅读: " + convoys.getRead() + "\n\n"+
                "申请频道id:" + invite.getChatId() + "\n" +
                "申请频道: <a href=\""+invite.getLink()+"\">"+"" + invite.getName() + "</a>\n" +
                "订阅人数: " + invite.getMemberCount() + "\n" +
                "申请人ID: " + invite.getTgId() + "\n" +
                "申请人名: " + "<a href=\"tg://user?id="+invite.getTgId()+"\">@"+invite.getUserName()+"</a>" +"\n"+
                "申请状态:"+ code+msg;

        SendMessage send = SendMessage.builder().chatId(string).text(text).parseMode("html").replyMarkup(createInlineKeyboardButton02(convoysInvite.getId())).build();
        bot.execute(send);

    }
}
