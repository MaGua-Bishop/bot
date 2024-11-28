from .bot_config import bot
from telebot import types
import re
from tg_bot.models import TgButton, TgTimingMessage, TGInvite, TGInviteTimingMessage
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
        markup_success = types.InlineKeyboardMarkup()
        markup_success.add(types.InlineKeyboardButton("查看定时信息", callback_data="query_message"))
        markup_success.add(types.InlineKeyboardButton("添加定时信息", callback_data="create_message"))
        bot.send_message(
            call.message.chat.id,
            f"创建定时消息成功",
            parse_mode="html",
            reply_markup=markup_success
        )
    except TgTimingMessage.DoesNotExist:
        bot.send_message(call.message.chat.id, "找不到该定时消息")


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
        TGInviteTimingMessage.objects.filter(timing_message_id=timing_message_id).delete()
        TgButton.objects.filter(timing_message_id=timing_message_id).delete()
        TgTimingMessage.objects.filter(id=timing_message_id).delete()
        bot.delete_message(call.message.chat.id, call.message.id)
        bot.send_message(call.message.chat.id, "该定时消息已删除")
    except Exception as e:
        print(f"Error deleting message: {e}")
        bot.answer_callback_query(call.id, text="删除消息时出错，请重试。")


@bot.callback_query_handler(func=lambda call: call.data.startswith('update_message_time'))
def update_message_time(call):
    invite_timing_message = call.data[len('update_message_time:'):]
    try:
        bot.send_message(call.message.chat.id, f"请输入新的定时时间，格式为：00:00~23:59")
        bot.register_next_step_handler(call.message, update_time, invite_timing_message)
    except Exception as e:
        print(f"Error deleting message: {e}")
        bot.answer_callback_query(call.id, text="消息时出错，请重试。")


def update_time(message, invite_timing_message_id):
    text = message.text
    if check_timing(text):
        invite_timing_message = TGInviteTimingMessage.objects.get(id=invite_timing_message_id)
        invite_timing_message.time = text
        invite_timing_message.save()
        markup = types.InlineKeyboardMarkup()
        markup.add(
            types.InlineKeyboardButton("返回", callback_data=f"query_group_info:{invite_timing_message.invite_id}"))
        bot.send_message(message.chat.id, f"定时消息时间已更新为：{text}", reply_markup=markup)
    else:
        bot.send_message(message.chat.id, f"定时消息时间修改失败，格式为：00:00~23:59")


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
        TGInvite.objects.get(id=invite_id).delete()


@bot.callback_query_handler(func=lambda call: call.data == "query_group")
def query_group(call):
    group_list = TGInvite.objects.filter(inviter_id=call.from_user.id)
    print(f"共{len(group_list)}条记录")
    if not group_list.exists():
        bot.send_message(call.message.chat.id, "机器人暂无添加群聊或频道")
        return
    markup = types.InlineKeyboardMarkup()
    for group in group_list:
        chat_type_display = {
            'group': '群聊',
            'supergroup': '超级群聊',
            'channel': '频道'
        }.get(group.chat_type, "未知类型")
        markup.add(
            types.InlineKeyboardButton(f"{chat_type_display}|{group.chat_title}",
                                       callback_data=f"query_group_info:{group.id}"))
    bot.send_message(call.message.chat.id, "点击查看<b>群聊|频道</b>的定时信息", reply_markup=markup,
                     parse_mode="html")


@bot.callback_query_handler(func=lambda call: call.data.startswith('query_group_info'))
def query_group_info(call):
    invite_id = call.data[len('query_group_info:'):]
    try:
        invite = TGInvite.objects.get(id=invite_id)
        chat_type_display = {
            'group': '群聊',
            'supergroup': '超级群聊',
            'channel': '频道'
        }.get(invite.chat_type, "未知类型")
        markup = types.InlineKeyboardMarkup()
        markup.add(types.InlineKeyboardButton("查看定时信息", callback_data=f"group_query_message:{invite.id}"))
        markup.add(types.InlineKeyboardButton("添加定时信息", callback_data=f"group_add_message:{invite.id}"))
        timing_message_count = TGInviteTimingMessage.objects.filter(invite_id=invite.id).count()
        bot.send_message(call.message.chat.id,
                         f"\n🆔:{invite.chat_id}\n<b>标题:</b>《{invite.chat_title}》\n<b>类型:</b>{chat_type_display}\n\n共<b>{timing_message_count}</b>条定时消息",
                         parse_mode="html", reply_markup=markup)
    except Exception as e:
        print(f"Error deleting message: {e}")


@bot.callback_query_handler(func=lambda call: call.data.startswith('group_add_message'))
def query_group_info(call):
    invite_id = call.data[len('group_add_message:'):]
    try:
        # 获取用户的定时消息列表
        timing_message_list = TgTimingMessage.objects.filter(tg_id=call.from_user.id)
        print(f"共{len(timing_message_list)}条记录")

        if not timing_message_list.exists():
            markup_add = types.InlineKeyboardMarkup()
            markup_add.add(types.InlineKeyboardButton("添加定时信息", callback_data="create_message"))
            bot.send_message(call.message.chat.id, "您暂无添加定时信息\n请先添加定时信息", reply_markup=markup_add,
                             parse_mode="html")
            return
        bot.send_message(call.message.chat.id,
                         "点击定时消息下方的<b>添加按钮</b>\n可给该群聊|频道添加定时消息,并设置推送时间。\n在指定时间会自动发送消息",
                         parse_mode="html")
        for timing_message in timing_message_list:
            buttons = TgButton.objects.filter(timing_message=timing_message)
            print(f"共{len(buttons)}条记录")
            markup = types.InlineKeyboardMarkup()
            if buttons.exists():
                button_list = [(button.name, button.url) for button in buttons]
                markup = create_markup(button_list)
            markup.add(
                types.InlineKeyboardButton("添加",
                                           callback_data=f"add_message_group:{invite_id}:{timing_message.id}"))
            print(f"准备发送消息 ID {timing_message.message_id} 到聊天 ID {timing_message.tg_id}")
            # 检查消息是否存在
            try:
                bot.copy_message(
                    chat_id=call.from_user.id,  # 目标聊天 ID
                    from_chat_id=timing_message.tg_id,  # 来源聊天 ID
                    message_id=timing_message.message_id,  # 消息 ID
                    reply_markup=markup  # 附带按钮
                )
            except Exception as e:
                print(f"Error sending message: {e}")
                if "message to copy not found" in str(e):
                    # 删除定时消息记录
                    timing_message.delete()
                    print(f"已删除定时消息 ID {timing_message.id}，因为消息未找到。")

    except Exception as e:
        print(f"Error deleting message: {e}")


@bot.callback_query_handler(func=lambda call: call.data.startswith('add_message_group'))
def add_message_group(call):
    text = call.data
    pattern = r"add_message_group:(\d+):(\d+)"
    match = re.match(pattern, text)
    invite_id = match.group(1)
    timing_message_id = match.group(2)
    bot.send_message(call.message.chat.id, "请输入定时时间\n格式:<b>00:00~23:59</b>", parse_mode="html")
    bot.register_next_step_handler(call.message, add_message_group_time, invite_id, timing_message_id)


def add_message_group_time(message, invite_id, timing_message_id):
    try:
        if check_timing(message.text):
            invite_timing_message = TGInviteTimingMessage.objects.create(invite_id=invite_id,
                                                                         timing_message_id=timing_message_id,
                                                                         time=message.text)
            markup = types.InlineKeyboardMarkup()
            markup.add(types.InlineKeyboardButton("返回", callback_data="query_group_info:" + invite_id))
            bot.send_message(message.chat.id, f"添加定时信息成功\n该信息将在<b>{message.text}</b>推送",
                             parse_mode="html", reply_markup=markup)
        else:
            bot.send_message(message.chat.id, "输入的定时时间不正确\n添加定时信息失败")
    except Exception as e:
        print(f"Error deleting message: {e}")


@bot.callback_query_handler(func=lambda call: call.data.startswith('group_query_message'))
def group_query_message(call):
    invite_id = call.data[len('group_query_message:'):]
    try:
        invite_timing_message_list = TGInviteTimingMessage.objects.filter(invite_id=invite_id)
        # 不存在
        if not invite_timing_message_list.exists():
            bot.send_message(call.message.chat.id, "该群聊|频道暂无定时消息", parse_mode="html")
            return
        bot.send_message(call.message.chat.id, f"以下是该群聊|频道的定时消息")
        for invite_timing_message in invite_timing_message_list:
            timing_message = TgTimingMessage.objects.get(id=invite_timing_message.timing_message_id)
            buttons = TgButton.objects.filter(timing_message=timing_message)
            markup = types.InlineKeyboardMarkup()
            if buttons.exists():
                button_list = [(button.name, button.url) for button in buttons]
                markup = create_markup(button_list)
            markup.add(
                types.InlineKeyboardButton(f"定时时间:{invite_timing_message.time}", callback_data="null"))
            markup.add(
                types.InlineKeyboardButton("删除(⚠️点击直接删除)",
                                           callback_data=f"group_delete_message:{invite_timing_message.id}"))
            markup.add(
                types.InlineKeyboardButton("修改定时时间",
                                           callback_data=f"update_message_time:{invite_timing_message.id}"))
            try:
                bot.copy_message(
                    chat_id=call.from_user.id,  # 目标聊天 ID
                    from_chat_id=timing_message.tg_id,  # 来源聊天 ID
                    message_id=timing_message.message_id,  # 消息 ID
                    reply_markup=markup  # 附带按钮
                )
            except Exception as e:
                print(f"Error sending message: {e}")
                if "message to copy not found" in str(e):
                    # 删除定时消息记录
                    timing_message.delete()
                    print(f"已删除定时消息 ID {timing_message.id}，因为消息未找到。")
    except Exception as e:
        print(f"Error deleting message: {e}")


@bot.callback_query_handler(func=lambda call: call.data.startswith("group_delete_message"))
def group_delete_message(call):
    bot.delete_message(call.message.chat.id, call.message.id)
    invite_timing_message_id = call.data[len("group_delete_message:"):]
    try:
        invite_timing_message = TGInviteTimingMessage.objects.get(id=invite_timing_message_id)
        invite_timing_message.delete()
        markup = types.InlineKeyboardMarkup()
        markup.add(
            types.InlineKeyboardButton("返回", callback_data=f"query_group_info:{invite_timing_message.invite_id}"))
        bot.send_message(call.message.chat.id, "删除成功", reply_markup=markup)
    except Exception as e:
        print(f"Error deleting message: {e}")


@bot.callback_query_handler(func=lambda call: call.data == 'timing_message')
def timing_message(call):
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("查看定时信息", callback_data="query_message"))
    markup.add(types.InlineKeyboardButton("添加定时信息", callback_data="create_message"))
    bot.send_message(call.message.chat.id, "请选择", reply_markup=markup,
                     parse_mode="html")


@bot.callback_query_handler(func=lambda call: call.data == 'query_message')
def query_message(call):
    # 获取用户的定时消息列表
    timing_message_list = TgTimingMessage.objects.filter(tg_id=call.from_user.id)
    print(f"共{len(timing_message_list)}条记录")

    if not timing_message_list.exists():  # 检查是否有定时消息
        bot.send_message(call.message.chat.id, "暂无定时消息")
        return

    for timing_message in timing_message_list:
        buttons = TgButton.objects.filter(timing_message=timing_message)
        print(f"共{len(buttons)}条记录")
        markup = types.InlineKeyboardMarkup()
        if buttons.exists():
            button_list = [(button.name, button.url) for button in buttons]
            markup = create_markup(button_list)
        markup.add(
            types.InlineKeyboardButton("删除该定时消息", callback_data=f"delete_message:{timing_message.id}"))

        # 调试信息
        print(f"准备发送消息 ID {timing_message.message_id} 到聊天 ID {timing_message.tg_id}")

        # 检查消息是否存在
        try:
            bot.copy_message(
                chat_id=call.from_user.id,  # 目标聊天 ID
                from_chat_id=timing_message.tg_id,  # 来源聊天 ID
                message_id=timing_message.message_id,  # 消息 ID
                reply_markup=markup  # 附带按钮
            )
        except Exception as e:
            print(f"Error sending message: {e}")
            if "message to copy not found" in str(e):
                # 删除定时消息记录
                timing_message.delete()
                print(f"已删除定时消息 ID {timing_message.id}，因为消息未找到。")


@bot.callback_query_handler(func=lambda call: call.data == 'create_message')
def create_send_message(call):
    bot.send_message(call.message.chat.id, f"请输入要定时发送的消息")
    bot.register_next_step_handler(call.message, create_send_message_content)


def create_send_message_content(message):
    message_id = message.id
    tg_id = message.from_user.id
    tg_timing_message = TgTimingMessage.objects.create(message_id=message_id, tg_id=tg_id)
    id = tg_timing_message.id
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("创建", callback_data=f"add_button:{id}"))
    markup.add(types.InlineKeyboardButton("不创建", callback_data=f"cancel_button:{id}"))
    bot.send_message(message.chat.id, f"请选择是否需要创建消息导航按钮", reply_markup=markup, parse_mode="html")
