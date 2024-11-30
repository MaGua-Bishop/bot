from telebot import types
from .bot_config import bot
import re
from tg_bot.models import TgButton, TgTimingMessage, TGInvite
from tg_bot.utils import check_timing, create_markup
from django.conf import settings

commands = [
    types.BotCommand("start", "启动机器人"),
    types.BotCommand("help", "帮助")
]
bot.set_my_commands(commands, scope=types.BotCommandScopeAllPrivateChats())


@bot.message_handler(commands=['help'], func=lambda message: message.chat.type == 'private')
def help_message(message):
    bot.send_message(message.chat.id,
                     "<b>使用方法</b>\n\n"
                     "1.在<b>定时信息</b>按钮中设置定时信息\n"
                     "2.将机器人添加到您的群聊或频道中\n"
                     "3.在<b>群聊|频道</b>按钮中选择目标群聊或频道，绑定定时信息，并设置定时时间\n"
                     "4.机器人会自动根据您设置的定时信息，按时将消息推送到指定的群聊或频道\n\n"
                     "⚠️ <b>注意：</b>频道需要给机器人开启<b>发送信息权限</b>，否则无法自动发送消息",
                     parse_mode="html")


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
    markup.add(types.InlineKeyboardButton("群聊|频道", callback_data="query_group"))
    markup.add(types.InlineKeyboardButton("定时信息", callback_data="timing_message"))
    bot.send_message(message.chat.id,
                     f"欢迎使用自动发消息机器人\n\n"
                     f"<b>添加群聊：</b>把机器人邀请进群聊，会自动绑定群聊\n"
                     f"<b>添加频道：</b>先把机器人拉到频道<b>（⚠️需要给机器人发送信息权限）</b>.转发一条频道消息给机器人,会自动绑定频道\n\n"
                     f"可在添加的群聊|频道绑定<b>定时信息</b>,会自动发送信息\n\n"
                     f"请点击下方按钮",
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

        if chat_member.status in ['administrator', 'member']:
            # 检查 chat_id 是否已存在
            if not TGInvite.objects.filter(chat_id=message.chat.id).exists():
                TGInvite.objects.create(
                    inviter_id=inviter.id,
                    chat_id=message.chat.id,
                    chat_title=chat_title,
                    chat_type=chat_type
                )
                markup = types.InlineKeyboardMarkup()
                markup.add(types.InlineKeyboardButton("查看群聊|频道", callback_data="query_group"))
                bot.send_message(inviter.id, f"《{chat_title}》<b>{chat_type_display}</b>\n添加机器人成功",
                                 parse_mode="html", reply_markup=markup)
            else:
                markup = types.InlineKeyboardMarkup()
                markup.add(types.InlineKeyboardButton("查看群聊|频道", callback_data="query_group"))
                bot.send_message(inviter.id, f"《{chat_title}》<b>{chat_type_display}</b>\n添加机器人成功",
                                 parse_mode="html", reply_markup=markup)
        else:
            bot.kick_chat_member(message.chat.id, bot.get_me().id)  # 踢出机器人
            bot.send_message(inviter.id, f"《{chat_title}》<b>{chat_type_display}</b>\n添加机器人失败\n请重新添加机器人")


@bot.message_handler(content_types=['left_chat_member'])
def handle_left_chat_member(message):
    if message.left_chat_member.id == bot.get_me().id:
        TGInvite.objects.filter(chat_id=message.chat.id).delete()
        chat_title = message.chat.title if hasattr(message.chat, 'title') else "无标题"
        inviter_id = message.from_user.id if message.from_user else None
        if inviter_id:
            bot.send_message(inviter_id, f"机器人已离开<b>《{chat_title}》</b>", parse_mode="html")


@bot.message_handler(content_types=['text'])
def handle_forwarded_message(message):
    if message.forward_from_chat:
        channel_id = message.forward_from_chat.id
        user_id = message.from_user.id
        title = message.forward_from_chat.title

        if TGInvite.objects.filter(chat_id=channel_id).exists():
            markup = types.InlineKeyboardMarkup()
            markup.add(types.InlineKeyboardButton("查看群聊|频道", callback_data="query_group"))
            bot.send_message(user_id, f"《{title}》频道已存在", parse_mode="html", reply_markup=markup)
        else:
            try:
                TGInvite.objects.create(chat_id=channel_id, chat_title=title, inviter_id=user_id, chat_type='channel')
                markup = types.InlineKeyboardMarkup()
                markup.add(types.InlineKeyboardButton("查看群聊|频道", callback_data="query_group"))
                bot.send_message(user_id, f"<b>《{title}》</b>频道添加机器人成功", parse_mode="html", reply_markup=markup)
            except Exception as e:
                print(f"Error adding channel: {e}")
    else:
        bot.send_message(message.chat.id, "添加失败，请重新转发频道消息")
