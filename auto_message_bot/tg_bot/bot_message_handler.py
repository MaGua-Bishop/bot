from telebot import types
from .bot_config import bot
import re
from tg_bot.models import TgButton, TgTimingMessage, TGInvite
from tg_bot.utils import check_timing, create_markup
from django.conf import settings

commands = [
    types.BotCommand("start", "启动机器人"),
    types.BotCommand("create_message", "创建定时发送消息"),
    types.BotCommand("query_message", "查看定时发送消息"),
    types.BotCommand("query_group", "查看推送的群聊|频道"),
    types.BotCommand("send", "发送"),
    types.BotCommand("help", "帮助")
]
bot.set_my_commands(commands, scope=types.BotCommandScopeAllPrivateChats())


@bot.message_handler(commands=['send'], func=lambda message: message.chat.type == 'private')
def get_start(message):
    invite_list = TGInvite.objects.filter(inviter_id=message.from_user.id)
    if invite_list:
        for invite in invite_list:
            bot.send_message(invite.chat_id, "您好")


@bot.message_handler(commands=['start'], func=lambda message: message.chat.type == 'private')
def get_start(message):
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("邀请进群聊", url="https://t.me/" + settings.TG_BOT_NAME + "?startgroup"))
    markup.add(
        types.InlineKeyboardButton("邀请进频道", url="https://t.me/" + settings.TG_BOT_NAME + "?startchannel=true"))
    bot.send_message(message.chat.id,
                     f"欢迎使用自动发消息机器人\n请先把机器人邀请进群聊或频道<b>⚠️（必须给机器人管理员权限）</b>,否则无法使用\n再创建定时发送消息",
                     parse_mode="html", reply_markup=markup)


@bot.message_handler(content_types=['new_chat_members'])
def welcome_new_member(message):
    # 检查机器人是否为新加入的成员
    if bot.get_me().id in [new_member.id for new_member in message.new_chat_members]:
        chat_member = bot.get_chat_member(message.chat.id, bot.get_me().id)  # 获取机器人的信息
        inviter = message.from_user
        chat_type = "supergroup" if message.chat.type == "supergroup" else "group" if message.chat.type == "group" else "channel"
        chat_title = message.chat.title if hasattr(message.chat, 'title') else "无标题"
        chat_type_display = {
            "supergroup": "超级群聊",
            "group": "群聊",
            "channel": "频道"
        }.get(chat_type, "未知类型")

        print(f"Chat type: {chat_type}")
        print(f"Chat member status: {chat_member.status}")

        if chat_member.status in ['administrator']:
            # 检查 chat_id 是否已存在
            if not TGInvite.objects.filter(chat_id=message.chat.id).exists():
                TGInvite.objects.create(
                    inviter_id=inviter.id,
                    chat_id=message.chat.id,
                    chat_title=chat_title,
                    chat_type=chat_type
                )
                bot.send_message(inviter.id, f"《{chat_title}》<b>{chat_type_display}</b>\n添加机器人成功",
                                 parse_mode="html")
            else:
                bot.send_message(inviter.id, f"《{chat_title}》<b>{chat_type_display}</b>\n添加机器人成功",
                                 parse_mode="html")
        else:
            bot.kick_chat_member(message.chat.id, bot.get_me().id)  # 踢出机器人
            bot.send_message(inviter.id, f"《{chat_title}》<b>{chat_type_display}</b>\n添加机器人失败\n请重新添加机器人")


@bot.message_handler(content_types=['left_chat_member'])
def handle_left_chat_member(message):
    if bot.get_me().id and message.content_type == 'left_chat_member':
        TGInvite.objects.filter(chat_id=message.chat.id).delete()
        chat_title = message.chat.title if hasattr(message.chat, 'title') else "无标题"
        inviter_id = message.from_user.id if message.from_user else None
        if inviter_id:
            bot.send_message(inviter_id, f"机器人已离开<b>《{chat_title}》</b>", parse_mode="html")

@bot.message_handler(commands=['create_message'], func=lambda message: message.chat.type == 'private')
def create_send_message(message):
    bot.send_message(message.chat.id, f"请输入要定时发送的消息")
    bot.register_next_step_handler(message, create_send_message_content)


def create_send_message_content(message):
    message_id = message.message_id
    bot.send_message(message.chat.id, f"请输入要定时发送的消息的时间，格式为：00:00~23:59")
    bot.register_next_step_handler(message, create_send_message_time, message_id)


def create_send_message_time(message, message_id):
    text = message.text
    if check_timing(text):
        tg_id = message.from_user.id
        tg_timing_message = TgTimingMessage.objects.create(message_id=message_id, tg_id=tg_id, time=text)
        id = tg_timing_message.id
        markup = types.InlineKeyboardMarkup()
        markup.add(types.InlineKeyboardButton("创建", callback_data=f"add_button:{id}"))
        markup.add(types.InlineKeyboardButton("不创建", callback_data=f"cancel_button:{id}"))
        bot.send_message(message.chat.id, f"请选择是否需要消息导航按钮", reply_markup=markup,
                         parse_mode="html")
    else:
        bot.send_message(message.chat.id, f"时间格式错误，格式为：00:00~23:59")


@bot.message_handler(commands=['query_message'], func=lambda message: message.chat.type == 'private')
def query_message(message):
    # 获取用户的定时消息列表
    timing_message_list = TgTimingMessage.objects.filter(tg_id=message.from_user.id)
    print(f"共{len(timing_message_list)}条记录")

    if not timing_message_list.exists():  # 检查是否有定时消息
        bot.send_message(message.chat.id, "暂无定时消息")
        return

    for timing_message in timing_message_list:
        buttons = TgButton.objects.filter(timing_message=timing_message)
        print(f"共{len(buttons)}条记录")
        markup = types.InlineKeyboardMarkup()
        if buttons.exists():
            button_list = [(button.name, button.url) for button in buttons]
            markup = create_markup(button_list)
        markup.add(types.InlineKeyboardButton(f"推送时间:{timing_message.time}", callback_data="null"))
        markup.add(
            types.InlineKeyboardButton("修改推送时间", callback_data=f"update_message_time:{timing_message.id}"))
        markup.add(
            types.InlineKeyboardButton("删除该定时消息", callback_data=f"delete_message:{timing_message.id}"))

        # 调试信息
        print(f"准备发送消息 ID {timing_message.message_id} 到聊天 ID {timing_message.tg_id}")

        # 检查消息是否存在
        try:
            bot.copy_message(
                chat_id=message.from_user.id,  # 目标聊天 ID
                from_chat_id=timing_message.tg_id,  # 来源聊天 ID
                message_id=timing_message.message_id,  # 消息 ID
                reply_markup=markup  # 附带按钮
            )
        except Exception as e:
            print(f"Error sending message: {e}")
            # 直接发送一条消息以确认机器人是否能发送
            bot.send_message(message.from_user.id, "无法复制消息，请检查消息 ID 和聊天 ID。")


@bot.message_handler(commands=['query_group'], func=lambda message: message.chat.type == 'private')
def query_group(message):
    group_list = TGInvite.objects.filter(inviter_id=message.from_user.id)
    print(f"共{len(group_list)}条记录")
    if not group_list.exists():
        bot.send_message(message.chat.id, "机器人暂无添加群聊或频道")
        return
    markup = types.InlineKeyboardMarkup(row_width=2)
    for group in group_list:
        chat_type_display = {
            'group': '群聊',
            'supergroup': '超级群聊',
            'channel': '频道'
        }.get(group.chat_type, "未知类型")
        markup.add(
            types.InlineKeyboardButton(f"{chat_type_display}|{group.chat_title}",
                                       callback_data=f"delete_group:{group.id}"))
    bot.send_message(message.chat.id, "⚠️直接点击可删除(机器人会自动退出)", reply_markup=markup,
                     parse_mode="html")
