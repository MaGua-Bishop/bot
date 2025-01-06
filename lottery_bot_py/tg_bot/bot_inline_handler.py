import uuid

from telebot import types
from decimal import Decimal, InvalidOperation

# 处理空查询或提示用户选择操作
from telebot.types import InlineQueryResultArticle, InputTextMessageContent

from .bot_config import bot
import re
from .models import TgUser, TGTransactionLog, TgLottery, TgPrizePool
from .utils import divide_red_packet, create_lottery_message


def get_user(tg_user):
    tg_id = tg_user.id
    tg_fullname = tg_user.first_name + (tg_user.last_name or "")
    tg_username = tg_user.username
    user, _ = TgUser.objects.get_or_create(
        tg_id=tg_id,
        defaults={
            'tg_name': tg_fullname,
            'tg_username': tg_username
        }
    )
    return user


# 处理空查询的内联查询
@bot.inline_handler(lambda query: len(query.query) == 0)
def handle_empty_query(inline_query):
    # 创建两个选项
    transfer_result = InlineQueryResultArticle(
        id="transfer",
        title="转账:请输入转账金额",
        input_message_content=InputTextMessageContent("请输入转账金额")
    )

    red_packet_result = InlineQueryResultArticle(
        id="red_packet",
        title="发红包:请输入发红包 金额 个数",
        input_message_content=InputTextMessageContent("请输入发红包金额和个数")
    )

    # 返回这两个选项
    bot.answer_inline_query(inline_query.id, [transfer_result, red_packet_result], cache_time=1)


# 处理用户直接输入金额进行转账
@bot.inline_handler(lambda query: bool(re.match(r"^\d+(\.\d+)?$", query.query)))
def handle_transfer_inline(query):
    amount_str = query.query.strip()
    try:
        amount = Decimal(amount_str)

        if amount <= 0:
            title = "请输入大于0的金额进行转账"
            text = "请输入大于0的金额进行转账"
            markup = types.InlineKeyboardMarkup()
        else:
            tg_user = get_user(query.from_user)

            if tg_user.money < amount:
                title = "您的积分不足，无法进行转账"
                text = "您的积分不足，无法进行转账"
                markup = types.InlineKeyboardMarkup()
            else:
                # 扣除积分并记录转账日志
                tg_user.money -= amount
                tg_user.save()
                transaction_log = TGTransactionLog.objects.create(
                    sender=tg_user, amount=amount, status=False
                )
                title = f"转账: {amount} 积分"
                text = f"{amount} 积分，点击下方按钮领取"
                markup = types.InlineKeyboardMarkup()
                markup.add(types.InlineKeyboardButton(
                    text="领取积分", callback_data=f"transfer_{transaction_log.id}")
                )
    except InvalidOperation:
        title = "无效金额"
        text = "请输入有效的金额格式"
        markup = types.InlineKeyboardMarkup()
    except Exception as e:
        print(f"Error processing transfer: {e}")
        title = "发生未知错误"
        text = "发生未知错误，请稍后再试"
        markup = types.InlineKeyboardMarkup()

    # 创建内联查询结果并返回
    result = InlineQueryResultArticle(
        id="transfer_result",  # id 必须唯一
        title=title,  # 简化提示
        input_message_content=InputTextMessageContent(text),
        reply_markup=markup  # 按钮
    )

    bot.answer_inline_query(query.id, [result], cache_time=1)


@bot.callback_query_handler(func=lambda call: call.data.startswith("transfer_"))
def handle_transfer_claim(call):
    try:
        transaction_id = int(call.data.split('_')[1])
        transaction = TGTransactionLog.objects.get(id=transaction_id)

        receiver_id = call.from_user.id
        sender_id = transaction.sender.tg_id

        if receiver_id == sender_id:
            bot.answer_callback_query(call.id, "您不能领取自己的转账。", show_alert=True)
            return

        if transaction.status:
            bot.answer_callback_query(call.id, "这笔转账已经被领取。", show_alert=True)
            return

        receiver = get_user(call.from_user)
        receiver.money = Decimal(receiver.money) + transaction.amount
        receiver.save()

        transaction.status = True
        transaction.receiver = receiver
        transaction.save()
        bot.answer_callback_query(call.id, f"领取{transaction.amount}积分成功。", show_alert=True)
        bot.send_message(sender_id, f"您的转账 {transaction.amount} 积分已被 <code>{receiver.tg_id} </code>领取。",
                         parse_mode="html")
        bot.send_message(receiver_id,
                         f"您已成功领取来自<code> {transaction.sender.tg_id}</code> 的 {transaction.amount} 积分。",
                         parse_mode="html")

    except TGTransactionLog.DoesNotExist:
        bot.answer_callback_query(call.id, "无效的转账记录，可能已被处理或不存在。", show_alert=True)
    except Exception as e:
        print(f"Error processing callback: {e}")
        bot.answer_callback_query(call.id, "处理失败，请稍后再试。", show_alert=True)


@bot.inline_handler(lambda query: bool(re.match(r"^发红包 (\d+(\.\d{1,2})?) (\d+)$", query.query)))
def handle_send_red_packet(query):
    query_text = query.query.strip()
    match = re.match(r"^发红包 (\d+(\.\d{1,2})?) (\d+)$", query_text)

    if not match:
        text = "请输入正确的红包格式，例：发红包 100 5"
        markup = types.InlineKeyboardMarkup()
        result = InlineQueryResultArticle(
            id="incorrect_format", title="格式错误", input_message_content=InputTextMessageContent(text),
            reply_markup=markup
        )
        bot.answer_inline_query(query.id, [result], cache_time=1)
        return

    try:
        amount_str = match.group(1)
        num_str = match.group(3)

        amount = Decimal(amount_str)
        num = int(num_str)

        if amount <= 0 or num <= 0:
            text = "红包金额和个数必须大于0"
            title = "红包金额和个数必须大于0"
            markup = types.InlineKeyboardMarkup()
        else:
            # 获取用户信息
            tg_user = get_user(query.from_user)

            # 检查用户积分是否足够
            if tg_user.money < amount:
                text = "您的积分不足，无法发红包"
                title = "您的积分不足，无法发红包"
                markup = types.InlineKeyboardMarkup()
            else:
                # 扣除红包金额并记录红包日志
                tg_user.money -= amount
                tg_user.save()

                lottery = TgLottery(
                    lottery_id=str(uuid.uuid4()),
                    tg_id=tg_user.tg_id,
                    tg_name=tg_user.tg_name,
                    chat_id=tg_user.tg_id,
                    money=amount,
                    number=num
                )
                lottery.save()

                # 计算红包金额
                money_list = divide_red_packet(lottery.money, lottery.number)

                # 批量创建奖池记录
                prize_pool_list = [
                    TgPrizePool(
                        prize_pool_id=str(uuid.uuid4()),
                        lottery_id=lottery.lottery_id,
                        money=amount
                    ) for amount in money_list
                ]
                TgPrizePool.objects.bulk_create(prize_pool_list)
                title = f"发红包: {amount} 积分，{num}个"
                print(f"抽奖id:{lottery.lottery_id}, 奖金池:{money_list}")

                # 创建消息文本
                text = create_lottery_message(amount, lottery.lottery_id, 0, "")
                bot_name = bot.get_me().username
                markup = types.InlineKeyboardMarkup()
                markup.add(types.InlineKeyboardButton(
                    text="点击抽奖", url=f"https://t.me/{bot_name}?start={lottery.lottery_id}")
                )

    except InvalidOperation:
        text = "请输入有效的金额格式"
        title = "请输入有效的金额格式"
        markup = types.InlineKeyboardMarkup()
    except Exception as e:
        print(f"Error processing red packet: {e}")
        text = "发生未知错误，请稍后再试"
        title = "发生未知错误"
        markup = types.InlineKeyboardMarkup()

    # 创建内联查询结果并返回
    result = InlineQueryResultArticle(
        id="send_red_packet_result",  # id 必须唯一
        title=title,
        input_message_content=InputTextMessageContent(text, parse_mode="html"),
        reply_markup=markup,  # 按钮
    )

    bot.answer_inline_query(query.id, [result], cache_time=1)
