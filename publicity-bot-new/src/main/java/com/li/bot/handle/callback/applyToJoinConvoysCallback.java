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
import com.li.bot.utils.UnitConversionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: li
 * @CreateTime: 2024-10-09
 */
@Component
public class applyToJoinConvoysCallback implements ICallback{

    @Override
    public String getCallbackName() {
        return "applyToJoin";
    }

    @Autowired
    private ConvoysMapper convoysMapper ;

    @Autowired
    private InviteMapper inviteMapper ;

    @Autowired
    private BotConfig botConfig ;

    @Autowired
    private ConvoysInviteMapper convoysInviteMapper ;

    @Autowired
    private UserMapper userMapper ;

    private Long currentConvoysCapacity = 0L ;


    private void getConvoysCapacity(Long convoysId){
        Long countById = convoysInviteMapper.getCountById(convoysId);
        if(countById == null){
            currentConvoysCapacity = 0L;
        }else {
            currentConvoysCapacity = countById ;
        }
    }

    private List<Invite> getConvoysMemberList(Long ConvoysId){
        List<ConvoysInvite> list = convoysInviteMapper.selectList(new LambdaQueryWrapper<ConvoysInvite>().eq(ConvoysInvite::getConvoysId, ConvoysId).eq(ConvoysInvite::getIsReview, true));
        List<Invite> inviteList = new ArrayList<>();
        if(!list.isEmpty()){
            //根据inviteId查出所有的
            List<Long> inviteIds = list.stream().map(ConvoysInvite::getInviteId).collect(Collectors.toList());
            inviteList = inviteMapper.getInviteListByIds(inviteIds);
            //把link字段信息提取
        }
       return inviteList;
    }




    private Convoys selectConvoysInfo(Long convoysId){
        Convoys convoys = convoysMapper.selectOne(new LambdaQueryWrapper<Convoys>().eq(Convoys::getConvoysId, convoysId));
        return convoys;
    }

    public InlineKeyboardMarkup createInlineKeyboardButton(Long tgId, Long convoysId, Long capacity) {
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, tgId));

        if (capacity.equals(currentConvoysCapacity)) {
            buttonList.add(InlineKeyboardButton.builder().text("车队已满").callbackData("null").build());
        } else if (user.getIsAdmin()) {
            List<Invite> invites = inviteMapper.selectList(new LambdaQueryWrapper<Invite>().eq(Invite::getTgId, tgId));
            for (Invite invite : invites) {
                ConvoysInvite convoysInvite = convoysInviteMapper.selectOne(new LambdaQueryWrapper<ConvoysInvite>()
                        .eq(ConvoysInvite::getInviteId, invite.getInviteId())
                        .eq(ConvoysInvite::getConvoysId, convoysId));
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
                        .text(code + invite.getName())
                        .callbackData("channelRequest:" + invite.getInviteId() + ":convoysId:" + convoysId)
                        .build());
            }
        } else {
            List<Invite> inviteList = inviteMapper.getInviteListByChatIdAndMemberCount(tgId);
            if (inviteList.isEmpty()) {
                buttonList.add(InlineKeyboardButton.builder().text("未找到符合要求的频道请添加").callbackData("null").build());
            } else {
                for (Invite invite : inviteList) {
                    ConvoysInvite convoysInvite = convoysInviteMapper.selectOne(new LambdaQueryWrapper<ConvoysInvite>()
                            .eq(ConvoysInvite::getInviteId, invite.getInviteId())
                            .eq(ConvoysInvite::getConvoysId, convoysId));
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
                            .text(code + invite.getName())
                            .callbackData("channelRequest:" + invite.getInviteId() + ":convoysId:" + convoysId)
                            .build());
                }
            }
        }

        // 添加特殊按钮
        buttonList.add(InlineKeyboardButton.builder()
                .text("\uD83C\uDF38手机添加")
                .url("https://" + botConfig.getBotname() + "?startgroup")
                .build());
        buttonList.add(InlineKeyboardButton.builder()
                .text("\uD83C\uDF1E电脑添加")
                .url("https://" + botConfig.getBotname() + "?startchannel=true")
                .build());
        buttonList.add(InlineKeyboardButton.builder()
                .text("\uD83D\uDD03刷新")
                .callbackData("selectConvoysInfo:" + convoysId)
                .build());
        buttonList.add(InlineKeyboardButton.builder()
                .text("\uD83D\uDD19返回")
                .callbackData("returnConvoysList")
                .build());
        buttonList.add(InlineKeyboardButton.builder()
                .text("首页")
                .callbackData("/start")
                .build());

        // 创建行列表
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

        // 先处理普通按钮，每个单独一行
        for (InlineKeyboardButton button : buttonList.subList(0, buttonList.size() - 4)) {
            rowList.add(Arrays.asList(button));
        }

        // 处理特殊按钮，每两个一行
        List<InlineKeyboardButton> specialButtons = buttonList.subList(buttonList.size() - 4, buttonList.size());
        for (int i = 0; i < specialButtons.size(); i += 2) {
            rowList.add(specialButtons.subList(i, Math.min(i + 2, specialButtons.size())));
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();
        return inlineKeyboardMarkup;
    }
    @Override
    public void execute(BotServiceImpl bot, CallbackQuery callbackQuery) throws TelegramApiException {
        String data = callbackQuery.getData();
        String convoysId = data.substring(data.lastIndexOf(":") + 1);
        Long id = Long.valueOf(convoysId);
        getConvoysCapacity(id);
        Convoys convoys = selectConvoysInfo(id);

        String message ="选择频道\n" +
                "\n" +
                "以下只显示您满足要求的频道列表\n" +
                "请确保机器人具备以下权限:"+"\n"+
                "群聊:管理群聊和通过链接邀请用户"+"\n"+
                "频道:更改频道详情和管理信息(3个全需要)和通过链接邀请用户"+"\n"+
                "\n" +
                "要求订阅:"+UnitConversionUtils.toThousands(convoys.getSubscription())+"\n" +
                "图示：\uD83D\uDFE2空闲\uD83D\uDFE1待审核\uD83D\uDFE3已上车\uD83D\uDD34被禁用\n" +
                "已上车的频道再次申请会自动下车\n" +
                "请确认是否已经将机器人拉入并设置管理员\n" +
                "\n" +
                "\uD83D\uDC47请选择一个提交";
        EditMessageText editMessageText = EditMessageText.builder().messageId(callbackQuery.getMessage().getMessageId()).chatId(callbackQuery.getMessage().getChatId().toString()).text(message).replyMarkup(createInlineKeyboardButton(callbackQuery.getFrom().getId(),id,convoys.getCapacity())).parseMode("html").build();
        bot.execute(editMessageText);
    }
}
