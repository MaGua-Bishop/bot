from .bot_config import bot
from telebot import types
import re
from .utils.file_utils import get_set_file_data, save_set_file_data, GroupButton
from .database.Tables import TgUser, TgJoinGroup, TgRecharge
from django.db.models import Sum
from decimal import Decimal
from django.db import connection

@bot.callback_query_handler(func=lambda call: call.data == 'admin_set_money')
def admin_set_group(call):
    """
        管理员设置入群价格
    """
    data = get_set_file_data()
    bot.send_message(call.message.chat.id, f'当前入群价格:<b>{data.get_money()}</b>\n请输入新的价格:',
                     parse_mode='html')
    bot.register_next_step_handler(call.message, admin_input_money, data)


def admin_input_money(message, data):
    money = message.text
    pattern = r'^\d+(\.\d{1,2})?$'
    if re.match(pattern, money):
        data.set_money(money)
        save_set_file_data(data)
        bot.send_message(message.chat.id, f'设置成功,新的入群价格:<b>{money}</b>', parse_mode='html')
    else:
        bot.send_message(message.chat.id, '无效的金额,请输入有效的金额.')


@bot.callback_query_handler(func=lambda call: call.data == 'admin_set_joinGroup_message')
def admin_set_group(call):
    """
        管理员设置入群消息
    """
    bot.send_message(call.message.chat.id, '请输入入群消息')
    bot.register_next_step_handler(call.message, admin_input_join_group_message)


def admin_input_join_group_message(message):
    chat_id = message.chat.id
    message_id = message.message_id
    data = get_set_file_data()
    data.set_join_group_message(chat_id, message_id)
    save_set_file_data(data)
    bot.send_message(message.chat.id, '入群消息设置成功')


@bot.callback_query_handler(func=lambda call: call.data == 'admin_set_joinGroup_button')
def admin_set_group(call):
    """
        管理员设置入群消息按钮
    """
    data = get_set_file_data()
    group_buttons = data.get_group_buttons()
    markup = types.InlineKeyboardMarkup(row_width=2)
    if group_buttons:
        for button in group_buttons:
            name = button.get_name()
            markup.add(types.InlineKeyboardButton(text=name, callback_data=f'admin_del_button_{name}'))
        markup.add(types.InlineKeyboardButton(text='添加按钮', callback_data='admin_add_button'))
        bot.send_message(call.message.chat.id, '当前已设置的按钮列表\n<b>⚠️注意:点击按钮会直接删除</b>',
                         parse_mode='html', reply_markup=markup)
    else:
        markup.add(types.InlineKeyboardButton(text='添加按钮', callback_data='admin_add_button'))
        bot.send_message(call.message.chat.id, '暂无按钮', reply_markup=markup)


@bot.callback_query_handler(func=lambda call: call.data.startswith('admin_del_button_'))
def admin_add_button(call):
    """
        管理员删除按钮
    """
    button_name = call.data[len('admin_del_button_'):]
    data = get_set_file_data()
    data.remove_group_button(button_name)
    save_set_file_data(data)
    markup = types.InlineKeyboardMarkup(row_width=2)
    markup.add(types.InlineKeyboardButton(text='返回', callback_data='admin_set_joinGroup_button'))
    bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id,
                          text='删除成功', reply_markup=markup, parse_mode='html')


@bot.callback_query_handler(func=lambda call: call.data == 'admin_add_button')
def admin_add_button(call):
    """
        管理员添加按钮
    """
    bot.send_message(call.message.chat.id, '请输入按钮名称')
    bot.register_next_step_handler(call.message, admin_input_button_name)


def admin_input_button_name(message):
    name = message.text
    bot.send_message(message.chat.id, '请输入按钮链接')
    bot.register_next_step_handler(message, admin_input_button_url, name)


def admin_input_button_url(message, name):
    url = message.text
    if re.match(r'^https://', url):
        data = get_set_file_data()
        group_buttons = data.get_group_buttons()
        group_buttons.append(GroupButton(name, url))
        data.set_group_buttons(group_buttons)
        save_set_file_data(data)
        markup = types.InlineKeyboardMarkup()
        markup.add(types.InlineKeyboardButton(text='继续添加', callback_data='admin_add_button'))
        markup.add(types.InlineKeyboardButton(text='返回', callback_data='admin_set_joinGroup_button'))
        bot.send_message(message.chat.id, '添加成功', reply_markup=markup)
    else:
        bot.send_message(message.chat.id, '无效的URL,请输入以 https:// 开头的有效 URL')


@bot.callback_query_handler(func=lambda call: call.data == 'admin_query_income')
def admin_add_button(call):
    """
        管理员查看收入
    """
    count = TgJoinGroup.objects.count()
    total = TgJoinGroup.objects.aggregate(Sum('money'))  # 统计 money 字段的总和
    total_money = total['money__sum']
    bot.send_message(call.message.chat.id, f'当前总人数:<b>{count}</b>\n当前总收入:<b>{total_money}</b>',
                     parse_mode='html')


#


def execute_custom_sql(query):
    with connection.cursor() as cursor:
        cursor.execute(query)
        result = cursor.fetchall()
    return result


@bot.callback_query_handler(func=lambda call: call.data.startswith('user_recharge_'))
def admin_add_button(call):
    """
        用户充值
    """
    amount = call.data[len('user_recharge_'):]
    money = Decimal(amount)
    sql_query = "SELECT count(recharge_id) FROM tg_recharge WHERE create_time >= NOW() - INTERVAL '15 minutes'"  # 你的自定义 SQL 查询

    count = execute_custom_sql(sql_query)
    print(count)
    if count[0][0] == 0:
        money += Decimal('0.01')
    else:
        count = count[0][0]
        money += Decimal('0.01') * Decimal(count + 1)
        print(f'{count}发现重复充值:{Decimal(count + 1)}，增加费用为:{money}')

    recharge = TgRecharge(
        money=money,
        tg_id=call.message.chat.id,
    )
    recharge.save()
    address = 'THAghPyQjk7hbKmNY6psM2UcSEj8M8JzC3'
    text = f'''此订单15分钟内有效，过期后请重新生成订单。\n\n<b>转账地址(点击可复制): </b><code>{address}</code> (TRC-20网络)\n\n转账金额:<b>{money} USDT</b>\n\n请注意<b>转账金额务必与上方的转账金额一致</b>，否则无法自动到账\n支付完成后, 请等待1分钟左右查询，自动到账。'''
    bot.send_message(call.message.chat.id, text, parse_mode='html')
