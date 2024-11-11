package com.li.bot.handle.key;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.li.bot.entity.Workgroup;
import com.li.bot.entity.database.Business;
import com.li.bot.entity.database.Order;
import com.li.bot.entity.database.User;
import com.li.bot.entity.database.vo.BusinessListVo;
import com.li.bot.enums.OrderStatus;
import com.li.bot.mapper.BusinessMapper;
import com.li.bot.mapper.OrderMapper;
import com.li.bot.mapper.UserMapper;
import com.li.bot.service.impl.BotServiceImpl;
import com.li.bot.service.impl.FileService;
import com.li.bot.utils.UserStartKeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: li
 * @CreateTime: 2024-09-30
 */
@Component
@Slf4j
public class StartKeyImpl implements IKeyboard {

    @Override
    public String getKeyName() {
        return "/start";
    }


    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BusinessMapper businessMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private FileService fileService;

    private Integer getChatType(String chatId) {
        String string = fileService.readFileContent();
        Workgroup workgroup = JSONObject.parseObject(string, Workgroup.class);
        String string02 = fileService.readFileContent02();
        Workgroup workgroup02 = JSONObject.parseObject(string02, Workgroup.class);
        Integer type = null;
        if (workgroup.getGroupList().contains(chatId)) {
            type = 0;
        } else if (workgroup02.getGroupList().contains(chatId)) {
            type = 1;
        }
        return type;
    }

    private void startKey(BotServiceImpl bot, Message message) {
        //查看数据库是否有该用户
        Long tgId = message.getFrom().getId();
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getTgId, tgId);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("用户不存在，插入新用户TGID:" + tgId);
            User newUser = new User();
            newUser.setTgId(tgId);
            String tgName = message.getFrom().getLastName() + message.getFrom().getFirstName();
            newUser.setTgName(tgName);
            userMapper.insert(newUser);
        }
        List<String> userStartKey = UserStartKeyUtils.userStartKeyList;

        List<KeyboardButton> keyList = new ArrayList<>();

        userStartKey.forEach(key -> {
            KeyboardButton button = KeyboardButton.builder().text(key).build();
            keyList.add(button);
        });

        List<List<KeyboardButton>> partition = Lists.partition(keyList, 2);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        partition.forEach(p -> {
            KeyboardRow row = new KeyboardRow(p);
            keyboardRows.add(row);
        });

        ReplyKeyboardMarkup replyKeyboardMarkup = ReplyKeyboardMarkup.builder().keyboard(keyboardRows).build();
        replyKeyboardMarkup.setResizeKeyboard(true);
        SendMessage executeMessage = SendMessage.builder().replyMarkup(replyKeyboardMarkup).text("请选择内置键盘").chatId(message.getChatId().toString()).build();
        try {
            bot.execute(executeMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private InlineKeyboardMarkup createInlineKeyboardButton(Long tgId, Integer type,Message message) {
        //查出全部业务只要名称和主键
        List<BusinessListVo> businessListVos = businessMapper.selectBusinessList(type);
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getTgId, tgId));
        if (!businessListVos.isEmpty()) {
            if(user == null){
                user = new User();
                user.setTgId(tgId);
                user.setTgName(message.getFrom().getFirstName()+message.getFrom().getLastName());
                userMapper.insert(user);
            }
            for (BusinessListVo business : businessListVos) {
                buttonList.add(InlineKeyboardButton.builder().text("(" + business.getSize() + ")" + business.getName()).callbackData("select:businessId:" + String.valueOf(business.getBusinessId())).build());
            }
        } else {
            if(user == null){
                user = new User();
                user.setTgId(tgId);
                user.setTgName(message.getFrom().getFirstName()+message.getFrom().getLastName());
                userMapper.insert(user);
            }
            buttonList.add(InlineKeyboardButton.builder().text("暂无未领取的业务").callbackData("null").build());
        }
        buttonList.add(InlineKeyboardButton.builder().text("接单记录").callbackData("select:reply:records:").build());
        List<List<InlineKeyboardButton>> rowList = Lists.partition(buttonList, 2);
        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder().keyboard(rowList).build();

        return inlineKeyboardMarkup;
    }


    @Override
    public void execute(BotServiceImpl bot, Message message) {
        String type = message.getChat().getType();
        if (type.equals("private")) {
            startKey(bot, message);
        } else if (type.equals("group") || type.equals("supergroup")) {
            Integer chatType = getChatType(message.getChatId().toString());
            InlineKeyboardMarkup inlineKeyboardButton = createInlineKeyboardButton(message.getFrom().getId(), chatType, message);
            SendMessage msg = SendMessage.builder().chatId(message.getChatId().toString()).text("请点击业务").replyMarkup(inlineKeyboardButton).parseMode("html").build();
            try {
                bot.execute(msg);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
