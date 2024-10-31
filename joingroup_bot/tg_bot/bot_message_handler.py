from telebot import types
from telebot.util import quick_markup
from .bot_config import bot
from .utils.file_utils import get_set_file_data, save_set_file_data
from threading import Timer
from .database.Tables import TgUser, TgJoinGroup
from decimal import Decimal

commands = [
    types.BotCommand("start", "启动机器人"),
    types.BotCommand("join", "加入群聊"),
    types.BotCommand("help", "帮助")
]
bot.set_my_commands(commands, scope=types.BotCommandScopeAllPrivateChats())


def get_user(message) -> TgUser:
    try:
        user = TgUser.objects.get(tg_id=message.from_user.id)  # 替换为实际的 tg_id
        return user
    except TgUser.DoesNotExist:
        new_user = TgUser(
            tg_id=message.from_user.id,
            tg_username=message.from_user.username,
            tg_full_name=message.from_user.first_name + message.from_user.last_name,
        )
        new_user.save()
        return new_user


@bot.message_handler(commands=['start'])
def get_start_command(message):
    markup = types.ReplyKeyboardMarkup(resize_keyboard=True)
    button1 = types.KeyboardButton("充值")
    button2 = types.KeyboardButton("个人中心")
    markup.add(button1, button2)
    bot.send_message(message.chat.id, "欢迎使用bot\n/join 购买入群链接，并加入群聊", reply_markup=markup,
                     parse_mode='html')
    get_user(message)


@bot.message_handler(commands=['join'])
def get_join_command(message):
    user = get_user(message)
    data = get_set_file_data()
    price = data.get_money()
    price = Decimal(price)
    if user.money < price:
        bot.send_message(message.chat.id, f'余额不足，请充值再使用。\n入群价格:{data.get_money()}')
        return
    chat_id = data.get_chat_id()
    invite_link = bot.create_chat_invite_link(chat_id, member_limit=1)
    # 减余额
    user.money -= price
    user.save()
    markup = quick_markup(
        {
            '加入群聊': {'url': invite_link.invite_link}
        }
    )
    join_group = TgJoinGroup(
        tg_id=message.from_user.id,
        money=price
    )
    join_group.save()
    bot.send_message(message.chat.id, "请点击下方加入群聊按钮", reply_markup=markup)


@bot.message_handler(commands=['help'])
def get_help_command(message):
    text = '''
    欢迎使用bot\n
    <b>使用说明:</b>\n
    /join 购买入群链接，并加入群聊
    '''
    bot.send_message(message.chat.id, text, parse_mode='html')


# 处理内置键盘
@bot.message_handler(func=lambda message: message.text == '个人中心' and message.chat.type == 'private')
def get_personal_center(message):
    full_name = message.from_user.first_name + message.from_user.last_name
    user = get_user(message)
    text = f'''
        个人中心:\nTGID:<code>{message.from_user.id}</code>\n用户名:<a href=\"tg://user?id={message.from_user.id}\">{full_name}</a>\n余额:<b>{user.money}</b>
        '''
    if user.is_admin:
        markup = quick_markup({
            '设置价格': {'callback_data': 'admin_set_money'},
            '设置入群消息': {'callback_data': 'admin_set_joinGroup_message'},
            '设置入群消息按钮': {'callback_data': 'admin_set_joinGroup_button'},
            '群链接收入': {'callback_data': 'admin_query_income'}
        })
        bot.send_message(message.chat.id, text, parse_mode='html', reply_markup=markup)
    else:
        bot.send_message(message.chat.id, text, parse_mode='html')


@bot.message_handler(func=lambda message: message.text == '充值' and message.chat.type == 'private')
def get_recharge(message):
    try:
        full_name = message.from_user.first_name + message.from_user.last_name
        user = get_user(message)
        text = f'''
            TGID:<code>{message.from_user.id}</code>\n用户名:<a href=\"tg://user?id={message.from_user.id}\">{full_name}</a>\n余额:<b>{user.money}</b>\n\n<b>请选择充值的金额</b>'''
        markup = quick_markup({
            '100U': {'callback_data': 'user_recharge_100'},
            '200U': {'callback_data': 'user_recharge_200'},
            '300U': {'callback_data': 'user_recharge_300'},
            '500U': {'callback_data': 'user_recharge_500'},
            '1000U': {'callback_data': 'user_recharge_1000'},
            '2000U': {'callback_data': 'user_recharge_2000'}
        }, row_width=3)
        bot.send_message(message.chat.id, text, parse_mode='html', reply_markup=markup)
    except Exception as e:
        print(e)


@bot.message_handler(func=lambda message: message.text == '设置群聊' and message.chat.type in ['group', 'supergroup'])
def set_group(message):
    user = get_user(message)
    if not user.is_admin:
        return
    try:
        data = get_set_file_data()
        chat_id = data.get_chat_id()
        if chat_id == message.chat.id:
            bot.send_message(message.chat.id, '已设置群聊了')
            return
        data.set_chat_id(message.chat.id)
        save_set_file_data(data)
        bot.send_message(message.chat.id, '设置成功')
    except Exception as e:
        print(e)
        bot.send_message(message.chat.id, '设置失败')


# 监听新成员加入指定群聊
def delete_message_after_timeout(chat_id, message_id, timeout):
    print(f'执行删除消息，消息id:{message_id}')
    bot.delete_message(chat_id, message_id)


@bot.message_handler(content_types=['new_chat_members'])
def welcome_new_member(message):
    for new_member in message.new_chat_members:
        # 获取新成员用户名
        username = new_member.username or new_member.first_name
        chat_id = message.chat.id

        # 获取设置的入群消息
        data = get_set_file_data()
        group_chat_id = data.get_chat_id()
        if chat_id != group_chat_id:
            return
        group_button_list = data.get_group_buttons()

        markup = types.InlineKeyboardMarkup(row_width=2)
        for group_button in group_button_list:
            markup.add(types.InlineKeyboardButton(text=group_button.get_name(), url=group_button.get_url()))
        try:
            send_message = bot.copy_message(
                chat_id=group_chat_id,  # 目标聊天的chat_id
                from_chat_id=data.get_join_group_message().get_chat_id(),
                message_id=data.get_join_group_message().get_message_id(),
            )
            edit_message = bot.edit_message_reply_markup(chat_id=chat_id, message_id=send_message.message_id,
                                                         reply_markup=markup)
            interval = 180
            Timer(interval, delete_message_after_timeout,
                  args=(edit_message.chat.id, edit_message.message_id, interval)).start()
        except Exception as e:
            print(e)
