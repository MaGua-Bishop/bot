import random

import schedule

from .bot_config import bot
from telebot import types
import re
from .models import TgRecharge, TgLottery, TgPrizePool, TgLotteryInfo, TgUser, TgTakeout, TgLuckydraw, TGTransactionLog
from .utils import get_start_markup, get_work_group_id, set_work_group_id, divide_red_packet, create_lottery_message, \
    get_return_markup
from decimal import Decimal
from django.utils import timezone
from django.db.models import Count
import uuid
from datetime import datetime, time, timedelta


@bot.callback_query_handler(func=lambda call: call.data.startswith('user_recharge_'))
def user_recharge_USDT(call):
    amount = call.data[len('user_recharge_'):]
    money = Decimal(amount)

    # 获取15分钟内的充值记录
    time_limit = timezone.now() - timedelta(minutes=15)
    recent_recharges = TgRecharge.objects.filter(create_time__gte=time_limit)

    # 统计最近15分钟内的充值记录数量
    count = recent_recharges.aggregate(count=Count('recharge_id'))['count']
    if count is None:
        count = 0

    # 根据充值记录数量调整金额
    if count == 0:
        money += Decimal('0.01')
    else:
        money += Decimal('0.01') * Decimal(count + 1)
        print(f'{count}发现重复充值:{Decimal(count + 1)}，增加费用为:{money}')

    # 创建新的充值记录
    recharge = TgRecharge(
        money=money,
        tg_id=call.message.chat.id,
    )
    recharge.save()
    address = 'TLAsbVyEPi3Z14JdqRYtx262CaKvgsYu9g'
    text = f'''此订单15分钟内有效，过期后请重新生成订单。\n\n<b>转账地址(点击可复制): </b><code>{address}</code> (TRC-20网络)\n\n转账金额:<b>{money} USDT</b>\n\n请注意<b>转账金额务必与上方的转账金额一致</b>，否则无法自动到账\n支付完成后, 请等待1分钟左右查询，自动到账。'''
    bot.send_message(call.message.chat.id, text, parse_mode='html')


def handle_gift(chat_id, lottery_id, type):
    # 从数据库获取对应的 Lottery 记录
    lottery = TgLottery.objects.filter(lottery_id=lottery_id).first()
    money = lottery.virtua_money if lottery.virtua_money else lottery.money
    if lottery:
        # 计算红包金额
        money_list = divide_red_packet(lottery.money, lottery.number)

        # 创建奖池记录
        prize_pool_list = []
        for amount in money_list:
            prize_pool = TgPrizePool(
                prize_pool_id=str(uuid.uuid4()),
                lottery_id=lottery_id,
                money=amount
            )
            prize_pool_list.append(prize_pool)

        # 批量保存奖池记录
        TgPrizePool.objects.bulk_create(prize_pool_list)
        print(f"抽奖id:{lottery_id},奖金池:{money_list}")
        markup = types.InlineKeyboardMarkup()
        # 获取bot名称
        bot_name = bot.get_me().username
        if type == 1:
            markup.add(
                types.InlineKeyboardButton(text="加入群聊",
                                           url=lottery.link))
        elif type == 2:
            markup.add(
                types.InlineKeyboardButton(text="订阅频道",
                                           url=lottery.link))
        markup.add(
            types.InlineKeyboardButton(text="点击抽奖", url=f"https://t.me/{bot_name}?start={lottery.lottery_id}"))
        message = bot.send_message(chat_id,
                                   create_lottery_message(money, lottery.lottery_id, type, lottery.link),
                                   parse_mode="HTML", reply_markup=markup, disable_web_page_preview=True)
        lottery.message_id = message.message_id
        lottery.save()
    else:
        bot.send_message(chat_id,
                         "未找到对应的红包记录，请重试。",
                         parse_mode="HTML")


@bot.callback_query_handler(func=lambda call: call.data.startswith('none:'))
def gift_none(call):
    try:
        lottery_id = call.data[len('none:'):]
        handle_gift(call.message.chat.id, lottery_id, 0)
    except Exception as e:
        print(f"发生错误: {e}")


def gift_set_link(message, lottery_id, type):
    text = message.text
    if not text.startswith("https://t.me/"):
        bot.send_message(message.chat.id,
                         "请输入有效的链接，必须以 https://t.me/ 开头。",
                         parse_mode="HTML")
        return
    lottery = TgLottery.objects.filter(lottery_id=lottery_id).first()
    lottery.link_type = type
    lottery.link = text
    lottery.save()
    handle_gift(message.chat.id, lottery_id, type)


@bot.callback_query_handler(func=lambda call: call.data.startswith('group:'))
def gift_group(call):
    lottery_id = call.data[len('group:'):]
    text = "请输入群聊邀请链接\n⚠\uFE0F注意: \n1.群聊必须是<b>公开群聊</b>\n2.需要把机器人添加<b>到群聊</b>并<b>给管理员权限</b>,否则抽奖无效"
    bot.send_message(call.message.chat.id, text, parse_mode="HTML")
    type = 1
    bot.register_next_step_handler(call.message, gift_set_link, lottery_id, type)


@bot.callback_query_handler(func=lambda call: call.data.startswith('cancel:'))
def gift_cancel(call):
    lottery_id = call.data[len('cancel:'):]
    text = "请输入频道邀请链接\n⚠\uFE0F注意: 需要把机器人添加<b>到频道</b>并<b>给管理员权限</b>,否则抽奖无效"
    bot.send_message(call.message.chat.id, text, parse_mode="HTML")
    type = 2
    bot.register_next_step_handler(call.message, gift_set_link, lottery_id, type)


def user_takeout_money_next(message):
    try:
        money = Decimal(message.text)
        if money <= 0:
            bot.send_message(message.chat.id, "提现积分必须大于0", parse_mode="HTML")
            return
        user = TgUser.objects.filter(tg_id=message.chat.id).first()
        if user.money < money:
            bot.send_message(message.chat.id, "积分不足", parse_mode="HTML")
            return
        user.money -= money
        user.save()
        takeout = TgTakeout(
            money=money,
            tg_id=message.chat.id,
        )
        takeout.save()
        bot.send_message(message.chat.id, "提现申请成功，请等待提现审核", parse_mode="HTML")

        # 发送消息到工作群
        work_group_id = get_work_group_id()
        if takeout.status == 0:
            status_text = "待审核"
        elif takeout.status == 1:
            status_text = "✅ 已同意"
        elif takeout.status == -1:
            status_text = "❌ 已拒绝"
        else:
            status_text = "未知状态"
        # 构建消息文本
        message_text = (
            f"用户名: <a href=\"tg://user?id={user.tg_id}\">{user.tg_name}</a>\n"
            f"ID: <code>{user.tg_id}</code>\n\n"
            f"用户提现积分: <b>{takeout.money}</b>\n"
            f"提现状态: <b>{status_text}</b>\n"
        )
        markup = types.InlineKeyboardMarkup()
        markup.add(
            types.InlineKeyboardButton(text="同意",
                                       callback_data=f"admin:review:takeout:type:0:takeoutId:{takeout.takeout_id}"),
            types.InlineKeyboardButton(text="拒绝",
                                       callback_data=f"admin:review:takeout:type:1:takeoutId:{takeout.takeout_id}")
        )
        bot.send_message(work_group_id, message_text, parse_mode="HTML", reply_markup=markup)
    except Exception as e:
        bot.send_message(message.chat.id, f"请重试", parse_mode="HTML")


@bot.callback_query_handler(func=lambda call: call.data == 'userTakeoutMoney')
def user_takeout_money(call):
    bot.send_message(call.message.chat.id, "请输入提现积分", parse_mode="HTML")
    bot.register_next_step_handler(call.message, user_takeout_money_next)


@bot.callback_query_handler(func=lambda call: re.match(r"admin:review:takeout:type:(\d+):takeoutId:(\d+)", call.data))
def handle_takeout_review(call):
    admin = TgUser.objects.filter(tg_id=call.from_user.id).first()
    if not admin.is_admin:
        return
    # 使用正则表达式提取 type 和 takeoutId
    match = re.match(r"admin:review:takeout:type:(\d+):takeoutId:(\d+)", call.data)

    if match:
        type_value = int(match.group(1))
        takeout_id = match.group(2)

        takeout = TgTakeout.objects.filter(takeout_id=takeout_id, status=0).first()
        if not takeout:
            bot.send_message(call.message.chat.id, "提现记录已处理", parse_mode="HTML")
            return
        user = TgUser.objects.filter(tg_id=takeout.tg_id).first()

        # 处理不同的 type
        if type_value == 0:
            TgTakeout.objects.filter(takeout_id=takeout_id).update(status=1, update_time=datetime.now(),
                                                                   review_tg_id=call.from_user.id)
            status_text = "✅ 已同意"
            markup = types.InlineKeyboardMarkup()
            markup.add(
                types.InlineKeyboardButton(text="已同意",
                                           callback_data=f"null")
            )
            message_text = (
                f"用户名: <a href=\"tg://user?id={user.tg_id}\">{user.tg_name}</a>\n"
                f"ID: <code>{user.tg_id}</code>\n\n"
                f"用户提现积分: <b>{takeout.money}</b>\n"
                f"提现状态: <b>{status_text}</b>\n"
            )
            bot.edit_message_text(message_text, call.message.chat.id, call.message.message_id, parse_mode="HTML",
                                  reply_markup=markup)

            bot.send_message(takeout.tg_id, f"您的提现积分:<b>{takeout.money}</b>\n已同意\n请等待客服处理",
                             parse_mode="HTML")

        elif type_value == 1:
            TgTakeout.objects.filter(takeout_id=takeout_id).update(status=-1, update_time=datetime.now(),
                                                                   review_tg_id=call.from_user.id)
            user.money += takeout.money
            user.save()
            status_text = "❌ 已拒绝"
            markup = types.InlineKeyboardMarkup()
            markup.add(
                types.InlineKeyboardButton(text="已拒绝",
                                           callback_data=f"null")
            )
            message_text = (
                f"用户名: <a href=\"tg://user?id={user.tg_id}\">{user.tg_name}</a>\n"
                f"ID: <code>{user.tg_id}</code>\n\n"
                f"用户提现积分: <b>{takeout.money}</b>\n"
                f"提现状态: <b>{status_text}</b>\n"
            )
            bot.edit_message_text(message_text, call.message.chat.id, call.message.message_id, parse_mode="HTML",
                                  reply_markup=markup)
            bot.send_message(takeout.tg_id, f"您的提现积分:<b>{takeout.money}</b>\n已拒绝", parse_mode="HTML")
    else:
        bot.send_message(call.message.chat.id, "无法解析回调数据")


@bot.callback_query_handler(func=lambda call: call.data == 'return')
def return_callback(call):
    bot.delete_message(call.message.chat.id, call.message.message_id)


@bot.callback_query_handler(func=lambda call: call.data == 'selectReceiveLottery')
def select_receive_lottery(call):
    lottery_list = TgLotteryInfo.objects.filter(tg_id=call.message.chat.id, status__in=[0, 1])
    if not lottery_list:
        bot.send_message(call.message.chat.id, "暂无记录", parse_mode="HTML", reply_markup=get_return_markup())
        return
    # 构建消息文本
    message_text = "说明:序号|中奖id|积分|状态\n"
    for i, lottery_info in enumerate(lottery_list, start=1):
        lottery_id = f"<code>{lottery_info.lottery_info_id}</code>"
        money = lottery_info.money
        if lottery_info.status == 0:
            status = "<b>未核销</b>"
        elif lottery_info.status == 1:
            status = "<b>已核销</b>"
        else:
            status = "<b>未知状态</b>"
        message_text += f"{i}. {lottery_id} {money} {status}\n\n"

    # 发送消息
    bot.send_message(call.message.chat.id, message_text, parse_mode="HTML", reply_markup=get_return_markup())


@bot.callback_query_handler(func=lambda call: call.data == 'selectTakeoutMoney')
def select_takeout_money(call):
    takeout_list = TgTakeout.objects.filter(tg_id=call.message.chat.id)
    if not takeout_list:
        bot.send_message(call.message.chat.id, "暂无记录", parse_mode="HTML", reply_markup=get_return_markup())
        return
    message_text = "说明:序号|提现积分|状态\n"
    for i, takeout in enumerate(takeout_list, start=1):
        money = takeout.money
        if takeout.status == 0:
            status = "<b>未处理</b>"
        elif takeout.status == 1:
            status = "<b>已同意</b>"
        elif takeout.status == -1:
            status = "<b>已拒绝</b>"
        else:
            status = "<b>未知状态</b>"
        message_text += f"{i}.{money} {status}\n"
    bot.send_message(call.message.chat.id, message_text, parse_mode="HTML", reply_markup=get_return_markup())


@bot.callback_query_handler(func=lambda call: call.data == 'selectUserluckydrawInfo')
def select_user_luckydraw_info(call):
    luckydraw_list = TgLuckydraw.objects.filter(tg_id=call.message.chat.id).order_by('-luckydraw_time')
    if not luckydraw_list:
        bot.send_message(call.message.chat.id, "暂无记录", parse_mode="HTML", reply_markup=get_return_markup())
        return
    message_text = "说明:序号|开奖状态|中奖金额|开奖时间\n"
    for i, luckydraw in enumerate(luckydraw_list, start=1):
        luckydraw_time = luckydraw.luckydraw_time
        money = "未中奖" if luckydraw.money is None else luckydraw.money
        if luckydraw.status == 0:
            status = "<b>未开奖</b>"
        elif luckydraw.status == 1:
            status = "已开奖"
        else:
            status = "未知状态"
        message_text += f"{i}.{status} {money} {luckydraw_time.strftime('%Y-%m-%d %H:%M')}\n"
    bot.send_message(call.message.chat.id, message_text, parse_mode="HTML", reply_markup=get_return_markup())


@bot.callback_query_handler(func=lambda call: call.data == 'userTransferLog')
def user_transfer_log(call):
    try:
        transfer_list = TGTransactionLog.objects.filter(sender_id=call.message.chat.id)
        if not transfer_list:
            bot.send_message(call.message.chat.id, "暂无记录", parse_mode="HTML", reply_markup=get_return_markup())
            return
        message_text = "说明:序号|积分|收款人|领取状态\n"
        for i, transfer in enumerate(transfer_list, start=1):
            amount = transfer.amount
            receiver = transfer.receiver_id
            status = "已领取" if transfer.status else "<b>未领取</b>"
            message_text += f"{i}.{amount} <code>{receiver}</code> {status}\n"
        bot.send_message(call.message.chat.id, message_text, parse_mode="HTML", reply_markup=get_return_markup())
    except Exception as e:
        print(e)


@bot.callback_query_handler(func=lambda call: call.data == 'userReceiptLog')
def user_receipt_log(call):
    try:
        transfer_list = TGTransactionLog.objects.filter(receiver_id=call.message.chat.id)
        if not transfer_list:
            bot.send_message(call.message.chat.id, "暂无记录", parse_mode="HTML", reply_markup=get_return_markup())
            return
        message_text = "说明:序号|积分|转账人\n"
        for i, transfer in enumerate(transfer_list, start=1):
            amount = transfer.amount
            sender = transfer.sender_id
            message_text += f"{i}.{amount}<code> {sender}</code>\n"
        bot.send_message(call.message.chat.id, message_text, parse_mode="HTML", reply_markup=get_return_markup())
    except Exception as e:
        print(e)


@bot.callback_query_handler(func=lambda call: call.data == 'userDailyDraw')
def user_daily_draw(call):
    '''
    每日抽奖
    '''
    try:
        # 获取回调查询对象和用户对象
        user = call.from_user
        tg_id = user.id

        # 获取用户信息
        try:
            tg_user = TgUser.objects.get(tg_id=tg_id)
        except TgUser.DoesNotExist:
            bot.send_message(call.message.chat.id, "用户不存在")
            return

        # 用户余额
        user_money = tg_user.money

        # 判断用户是否有足够的10积分
        if user_money >= 10:
            # 检查用户是否已经参与过当天的抽奖
            current_date = timezone.localdate()
            participation_count = TgLuckydraw.objects.filter(tg_id=tg_id, luckydraw_time__date=current_date).count()

            if participation_count > 0:
                bot.send_message(call.message.chat.id, "您今天已经参与过抽奖了！")
                return

            # 扣除用户积分
            tg_user.money -= 10
            tg_user.save()

            # 获取当前时间并判断开奖时间
            current_datetime = timezone.now()
            if current_datetime.time() > time(21, 59):
                lottery_datetime = current_datetime + timedelta(days=1)
                lottery_datetime = lottery_datetime.replace(hour=22, minute=0, second=0, microsecond=0)
            else:
                lottery_datetime = current_datetime.replace(hour=22, minute=0, second=0, microsecond=0)

            formatted_datetime = lottery_datetime.strftime("%Y-%m-%d %H:%M")

            # 创建新的Luckydraw对象
            luckydraw = TgLuckydraw(
                tg_id=tg_id,
                tg_full_name=f"{user.first_name} {user.last_name if user.last_name else ''}",
                tg_username=user.username,
                luckydraw_time=lottery_datetime
            )
            luckydraw.save()

            # 发送成功参与消息
            bot.send_message(call.message.chat.id, f"参与成功\n开奖时间: {formatted_datetime}")

        else:
            bot.send_message(call.message.chat.id, f"积分不足\n当前积分: <b>{user_money}</b>", parse_mode='html')

        # 删除当前的消息
        # bot.delete_message(chat_id=call.message.chat.id, message_id=call.message.message_id)

    except Exception as e:
        bot.send_message(call.message.chat.id, f"请重试")
