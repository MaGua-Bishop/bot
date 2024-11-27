from .bot_config import bot
from telebot import types
import re
from tg_bot.models import TgButton, TgTimingMessage, TGInvite
from tg_bot.utils import check_button_url, check_timing
from tg_bot.utils import create_markup


@bot.callback_query_handler(func=lambda call: call.data.startswith('cancel_button'))
def message_cancel_button(call):
    bot.delete_message(call.message.chat.id, call.message.id)
    timing_message_id = call.data[len('cancel_button:'):]

    try:
        timing_message = TgTimingMessage.objects.get(id=timing_message_id)
        buttons = TgButton.objects.filter(timing_message=timing_message)
        markup = None
        if buttons.exists():
            button_list = [(button.name, button.url) for button in buttons]
            markup = create_markup(button_list)
        bot.copy_message(
            chat_id=timing_message.tg_id,  # 目标聊天 ID
            from_chat_id=timing_message.tg_id,  # 来源聊天 ID
            message_id=timing_message.message_id,  # 消息 ID
            reply_markup=markup
        )
        bot.send_message(
            call.message.chat.id,
            f"创建定时消息成功。\n定时推送时间为：<b>{timing_message.time}</b>",
            parse_mode="html"
        )
    except TgTimingMessage.DoesNotExist:
        bot.send_message(call.message.chat.id, "找不到该定时消息。")



@bot.callback_query_handler(func=lambda call: call.data.startswith('add_button'))
def message_add_button(call):
    timing_message_id = call.data[len('add_button:'):]
    try:
        timing_message = TgTimingMessage.objects.get(id=timing_message_id)
        bot.send_message(call.message.chat.id, "请输入按钮名称：")
        bot.register_next_step_handler(call.message, create_message_button_name, timing_message)
    except TgTimingMessage.DoesNotExist:
        bot.send_message(call.message.chat.id, "找不到该定时消息。")


def create_message_button_name(message, timing_message):
    button_name = message.text
    bot.send_message(message.chat.id, "请输入按钮链接\n链接必须以<b>http://</b>或<b>https://</b>开头",
                     parse_mode="html")
    bot.register_next_step_handler(message, create_message_button_url, timing_message, button_name)


def create_message_button_url(message, timing_message, button_name):
    button_url = message.text
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("创建", callback_data=f"add_button:{timing_message.id}"))
    markup.add(types.InlineKeyboardButton("不创建", callback_data=f"cancel_button:{timing_message.id}"))
    if check_button_url(button_url):
        TgButton.objects.create(
            name=button_name,
            timing_message=timing_message,
            url=button_url
        )
        bot.send_message(message.chat.id, "创建消息导航按钮成功\n请选择是否继续创建按钮", reply_markup=markup,
                         parse_mode="html")
    else:
        bot.send_message(message.chat.id,
                         "创建消息导航按钮失败，链接格式不正确。\n链接必须以<b>http://</b>或<b>https://</b>开头\n请选择是否继续创建按钮",
                         parse_mode="html", reply_markup=markup)


@bot.callback_query_handler(func=lambda call: call.data.startswith('delete_message'))
def delete_message(call):
    timing_message_id = call.data[len('delete_message:'):]
    try:
        TgButton.objects.filter(timing_message_id=timing_message_id).delete()
        TgTimingMessage.objects.filter(id=timing_message_id).delete()
        bot.delete_message(call.message.chat.id, call.message.id)
        bot.send_message(call.message.chat.id, "该定时消息已删除")
    except Exception as e:
        print(f"Error deleting message: {e}")
        bot.answer_callback_query(call.id, text="删除消息时出错，请重试。")


@bot.callback_query_handler(func=lambda call: call.data.startswith('update_message_time'))
def update_message_time(call):
    timing_message_id = call.data[len('update_message_time:'):]
    try:
        bot.send_message(call.message.chat.id, f"请输入新的定时时间，格式为：00:00~23:59")
        bot.register_next_step_handler(call.message, update_time, timing_message_id)
    except Exception as e:
        print(f"Error deleting message: {e}")
        bot.answer_callback_query(call.id, text="消息时出错，请重试。")


def update_time(message, timing_message_id):
    text = message.text
    if check_timing(text):
        timing_message = TgTimingMessage.objects.get(id=timing_message_id)
        timing_message.time = text
        timing_message.save()
        bot.send_message(message.chat.id, f"定时消息时间已更新为：{text}")
    else:
        bot.send_message(message.chat.id, f"时间格式错误，格式为：00:00~23:59")


@bot.callback_query_handler(func=lambda call: call.data.startswith('delete_group'))
def delete_group(call):
    bot.delete_message(call.message.chat.id, call.message.id)
    invite_id = call.data[len('delete_group:'):]
    try:
        invite = TGInvite.objects.get(id=invite_id)
        bot.leave_chat(invite.chat_id)
        invite.delete()
        bot.send_message(call.message.chat.id, f"机器人已退出《{invite.chat_title}》")
    except Exception as e:
        print(f"Error deleting message: {e}")
        bot.answer_callback_query(call.id, text="删除时出错，请重试。")
