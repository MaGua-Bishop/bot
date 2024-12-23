from .bot_config import bot
from telebot import types
from .models import TgUser, AmountChange, TgRecharge, WithdrawalRecord
from decimal import Decimal, ROUND_DOWN

from .okay_pay import OkayPay
from .utlis import get_start_reply_markup, get_recharge_withdrawal_reply_markup, get_okex, transfer_money, \
    get_user_pgmoney, get_game_url, get_game_type_reply_markup, get_work_group_id
from django.utils.timezone import localtime
from django.conf import settings
from django.db.models import Count
from django.utils import timezone
from datetime import timedelta


@bot.callback_query_handler(func=lambda call: call.data == 'return_start')
def return_start(call):
    user_id = call.from_user.id
    full_name = call.from_user.full_name
    user = TgUser.objects.get(tg_id=user_id)
    text = f"👋Hi,<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n-----------------------------------------\n💰钱包余额: {user.money} "
    bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                          reply_markup=get_start_reply_markup(), parse_mode="HTML")


@bot.callback_query_handler(func=lambda call: call.data.startswith("game_type:"))
def game_type(call):
    user_id = call.from_user.id
    full_name = call.from_user.full_name
    game_type = int(call.data[len("game_type:"):])
    user = TgUser.objects.get(tg_id=user_id)

    if game_type == 0:
        pg_money = get_user_pgmoney(user_id, 0)
        pg_game_url = get_game_url(user_id, 0)
        if pg_money is None or pg_game_url is None:
            bot.answer_callback_query(call.id, "❌ 用户信息未找到，请先/start启动机器人", show_alert=True)
            return
        text = f"👋Hi,<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n-----------------------------------------\n💰钱包余额: {user.money} 💴\n🕹PG电子余额: {pg_money}"
        bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                              reply_markup=get_game_type_reply_markup(game_type, pg_game_url), parse_mode="HTML")
    else:
        jdb_money = get_user_pgmoney(user_id, 1)
        jdb_game_url = get_game_url(user_id, 1)
        if jdb_money is None or jdb_game_url is None:
            bot.answer_callback_query(call.id, "❌ 用户信息未找到，请先/start启动机器人", show_alert=True)
            return
        text = f"👋Hi,<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n-----------------------------------------\n💰钱包余额: {user.money} 💴\n🕹JDB电子余额: {jdb_money}"
        bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                              reply_markup=get_game_type_reply_markup(game_type, jdb_game_url), parse_mode="HTML")


@bot.callback_query_handler(func=lambda call: call.data.startswith("transfer_to_game:"))
def transfer_to_game(call):
    user_id = call.from_user.id
    full_name = call.from_user.full_name
    user = TgUser.objects.get(tg_id=user_id)
    transfer_to_game_type = int(call.data[len("transfer_to_game:"):])

    if user.money < Decimal('0.9'):
        bot.answer_callback_query(call.id, "最低需要1的余额才能转入游戏！", show_alert=True)
        return

    if transfer_to_game_type == 0:
        before_amount = user.money
        is_success = transfer_money(user_id, 1, user.money, transfer_to_game_type)
        if not is_success:
            bot.answer_callback_query(call.id, "❌ 转入失败，请重新尝试！", show_alert=True)
            return
        user.money = Decimal('0')
        user.save()
        AmountChange.objects.create(
            user=user,
            change_type='-',
            name='钱包转到PG电子',
            change_amount=user.money,
            before_amount=before_amount,
            after_amount=user.money
        )
        pg_game_url = get_game_url(user_id, 0)
        text = f"👋Hi,<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n-----------------------------------------\n💰钱包余额: {user.money} 💴\n🕹PG电子余额: {before_amount}"
        bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                              parse_mode="HTML", reply_markup=get_game_type_reply_markup(0, pg_game_url))
        bot.answer_callback_query(call.id, "✅ 成功将钱包余额转入PG电子！", show_alert=True)
    else:
        before_amount = user.money
        is_success = transfer_money(user_id, 1, user.money, transfer_to_game_type)
        if not is_success:
            bot.answer_callback_query(call.id, "❌ 转入失败，请重新尝试！", show_alert=True)
            return
        user.money = Decimal('0')
        user.save()
        AmountChange.objects.create(
            user=user,
            change_type='-',
            name='钱包转到JDB电子',
            change_amount=user.money,
            before_amount=before_amount,
            after_amount=user.money
        )
        pg_game_url = get_game_url(user_id, 1)
        text = f"👋Hi,<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n-----------------------------------------\n💰钱包余额: {user.money} 💴\n🕹JDB电子余额: {before_amount}"
        bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                              parse_mode="HTML", reply_markup=get_game_type_reply_markup(0, pg_game_url))
        bot.answer_callback_query(call.id, "✅ 成功将钱包余额转入JDB电子！", show_alert=True)


@bot.callback_query_handler(func=lambda call: call.data.startswith("transfer_to_wallet:"))
def transfer_to_wallet(call):
    user_id = call.from_user.id
    full_name = call.from_user.full_name
    transfer_to_game_type = int(call.data[len("transfer_to_wallet:"):])
    user = TgUser.objects.get(tg_id=user_id)
    before_amount = user.money
    if transfer_to_game_type == 0:
        pg_money = get_user_pgmoney(user_id, 0)
        if pg_money is None:
            bot.answer_callback_query(call.id, "❌ 失败，请重试", show_alert=True)
            return
        is_success = transfer_money(user_id, 2, pg_money, transfer_to_game_type)
        if not is_success:
            bot.answer_callback_query(call.id, "❌ 转出失败，请重试或取回失败,最小金额1！", show_alert=True)
            return
        user.money += Decimal(pg_money)
        user.save()
        AmountChange.objects.create(
            user=user,
            change_type='+',
            name='PG电子转回钱包',
            change_amount=pg_money,
            before_amount=before_amount,
            after_amount=user.money
        )
        pg_game_url = get_game_url(user_id, 0)
        text = f"👋Hi,<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n-----------------------------------------\n💰钱包余额: {user.money} 💴\n🕹PG电子余额: {0}"
        bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                              reply_markup=get_game_type_reply_markup(0, pg_game_url), parse_mode="HTML")
        bot.answer_callback_query(call.id, "✅ 成功将PG电子余额取回钱包！", show_alert=True)
    else:
        jdb_money = get_user_pgmoney(user_id, 1)
        if jdb_money is None:
            bot.answer_callback_query(call.id, "❌请重试!", show_alert=True)
            return
        is_success = transfer_money(user_id, 2, jdb_money, transfer_to_game_type)
        if not is_success:
            bot.answer_callback_query(call.id, "❌ 请重试或取回失败,最小金额1!", show_alert=True)
            return
        user.money += Decimal(jdb_money)
        user.save()
        AmountChange.objects.create(
            user=user,
            change_type='+',
            name='JDB电子转回钱包',
            change_amount=jdb_money,
            before_amount=before_amount,
            after_amount=user.money
        )
        pg_game_url = get_game_url(user_id, 1)
        text = f"👋Hi,<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n-----------------------------------------\n💰钱包余额: {user.money} 💴\n🕹JDB电子余额: {0}"
        bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                              reply_markup=get_game_type_reply_markup(1, pg_game_url), parse_mode="HTML")
        bot.answer_callback_query(call.id, "✅ 成功将JDB电子余额取回钱包！", show_alert=True)


@bot.callback_query_handler(func=lambda call: call.data.startswith("refresh:"))
def refresh(call):
    user_id = call.from_user.id
    full_name = call.from_user.full_name
    user = TgUser.objects.get(tg_id=user_id)
    refresh_type = int(call.data[len("refresh:"):])
    if refresh_type == 0:
        pg_money = get_user_pgmoney(user_id, 0)
        pg_game_url = get_game_url(user_id, 0)
        if pg_money is None or pg_game_url is None:
            bot.answer_callback_query(call.id, "❌ 用户信息未找到，请先/start启动机器人", show_alert=True)
            return
        text = f"👋Hi,<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n-----------------------------------------\n💰钱包余额: {user.money} 💴\n🕹PG电子余额: {pg_money}"
        bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                              reply_markup=get_game_type_reply_markup(refresh_type, pg_game_url), parse_mode="HTML")
    else:
        jdb_money = get_user_pgmoney(user_id, 1)
        jdb_game_url = get_game_url(user_id, 1)
        if jdb_money is None or jdb_game_url is None:
            bot.answer_callback_query(call.id, "❌ 用户信息未找到，请先/start启动机器人", show_alert=True)
            return
        text = f"👋Hi,<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n-----------------------------------------\n💰钱包余额: {user.money} 💴\n🕹JDB电子余额: {jdb_money}"
        bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                              reply_markup=get_game_type_reply_markup(refresh_type, jdb_game_url), parse_mode="HTML")
    bot.answer_callback_query(call.id, "✅ 刷新成功")


@bot.callback_query_handler(func=lambda call: call.data == 'recharge_withdrawal')
def recharge_withdrawal(call):
    user_id = call.from_user.id
    user = TgUser.objects.get(tg_id=user_id)
    text = f"💰充值提现\n\n💴余额:{user.money}"
    bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                          reply_markup=get_recharge_withdrawal_reply_markup(user.is_notify, user.is_admin))


@bot.callback_query_handler(func=lambda call: call.data == 'user_recharge')
def user_recharge(call):
    user_id = call.from_user.id
    user = TgUser.objects.get(tg_id=user_id)
    okex = get_okex()
    text = f"请点击按钮选择您的存款方式\n💡当前汇率 1 USDT = {okex} CNY\n-----------------------------------------"
    if user.deposit_reward != 0:
        text += f"\n\n🎁存款奖励：{user.deposit_reward}%\n投注要求：{user.deposit_reward}x"
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("🆗Okpay(人民币)💴", callback_data="recharge_type:0"))
    markup.add(types.InlineKeyboardButton("🆗Okpay(USDT)🔥", callback_data="recharge_type:1"))
    markup.add(types.InlineKeyboardButton("💰USDT(TRC20)💵", callback_data="recharge_type:2"))
    markup.add(types.InlineKeyboardButton("🎁存款奖励设置", callback_data="user_set_recharge"))
    markup.add(types.InlineKeyboardButton("↩️返回", callback_data="recharge_withdrawal"))
    bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                          reply_markup=markup)


@bot.callback_query_handler(func=lambda call: call.data == 'user_set_recharge')
def user_set_recharge(call):
    user_id = call.from_user.id
    text = "🎁存款红利\n您可以选择存款红利，但请注意，您必须达到要求的投注金额才可以取款\n🎁存款奖励：3%,投注要求：3x\n🎁存款奖励：5%,投注要求：5x"
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("0️⃣0%", callback_data="user_set_recharge_number:0"))
    markup.add(types.InlineKeyboardButton("🎁3%", callback_data="user_set_recharge_number:3"))
    markup.add(types.InlineKeyboardButton("🎁5%", callback_data="user_set_recharge_number:5"))
    markup.add(types.InlineKeyboardButton("↩️返回", callback_data="user_recharge"))
    bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                          reply_markup=markup)


from decimal import Decimal, InvalidOperation


def recharge_okpay(message, recharge_type):
    okpay = OkayPay()
    user_id = message.from_user.id
    text = message.text
    try:
        amount = Decimal(text)
        if amount < 1:
            bot.send_message(user_id, "充值金额必须大于1。")
            return

        if recharge_type == 0:  # CNY
            okex_rate = Decimal(get_okex())
            if okex_rate is not None:
                usdt_amount = amount / okex_rate
                usdt_amount = usdt_amount.quantize(Decimal('0.00'), rounding=ROUND_DOWN)
                amount_type = 'CNY'
                # 创建充值记录
                recharge = TgRecharge.objects.create(
                    tg_id=user_id,
                    money=usdt_amount,
                    amount_type=amount_type,
                    pay_type='OKPAY',
                )

                # 创建支付链接
                pay_link_response = okpay.payLink(
                    unique_id=str(recharge.recharge_id),
                    name='充值',  # 显示信息
                    amount=float(amount),  # 充值金额
                    coin='CNY'  # 货币类型
                )

                if 'data' in pay_link_response and 'pay_url' in pay_link_response['data']:
                    pay_url = pay_link_response['data']['pay_url']
                else:
                    bot.reply_to(message, "创建支付失败，请稍后再试。")
                    return

        elif recharge_type == 1:  # USDT
            amount_type = 'USDT'
            recharge = TgRecharge.objects.create(
                tg_id=user_id,
                money=amount,
                amount_type=amount_type,
                pay_type='OKPAY',
            )
            pay_link_response = okpay.payLink(
                unique_id=str(recharge.recharge_id),
                name='充值',
                amount=float(amount),
                coin='USDT'
            )

            if 'data' in pay_link_response and 'pay_url' in pay_link_response['data']:
                pay_url = pay_link_response['data']['pay_url']
            else:
                bot.reply_to(message, "创建支付失败，请稍后再试。")
                return

        # 创建支付按钮
        markup = types.InlineKeyboardMarkup()
        markup.add(types.InlineKeyboardButton("🏧去支付", url=pay_url))  # 使用创建的支付链接

        bot.send_message(user_id, f"➕存款:{amount:.2f} USDT", reply_markup=markup)

    except (InvalidOperation, ValueError) as e:
        bot.send_message(user_id, "金额格式错误")


@bot.callback_query_handler(func=lambda call: call.data.startswith("recharge_type:"))
def recharge_type(call):
    user_id = call.from_user.id
    recharge_type = int(call.data[len("recharge_type:"):])
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("🚫取消", callback_data="user_recharge"))
    if recharge_type == 0:
        # okpay 人民币
        bot.send_message(call.message.chat.id, "请输入您要充值的金额(<b>CNY</b>)", parse_mode="HTML",
                         reply_markup=markup)
        bot.register_next_step_handler(call.message, recharge_okpay, recharge_type)
    if recharge_type == 1:
        # okpay USDT
        bot.send_message(call.message.chat.id, "请输入您要充值的金额(<b>USDT</b>)", parse_mode="HTML",
                         reply_markup=markup)
        bot.register_next_step_handler(call.message, recharge_okpay, recharge_type)
    if recharge_type == 2:
        # USDT
        text = "请选择充值的金额"
        markup = types.InlineKeyboardMarkup()
        markup.add(types.InlineKeyboardButton(text='4U', callback_data='user_recharge_4'),
                   types.InlineKeyboardButton(text='8U', callback_data='user_recharge_8'),
                   types.InlineKeyboardButton(text='12U', callback_data='user_recharge_12'))
        markup.add(types.InlineKeyboardButton(text='20U', callback_data='user_recharge_20'),
                   types.InlineKeyboardButton(text='50U', callback_data='user_recharge_50'),
                   types.InlineKeyboardButton(text='100U', callback_data='user_recharge_100'))
        markup.add(types.InlineKeyboardButton(text='自定义金额', callback_data='user_custom_recharge'))
        bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                              reply_markup=markup)


@bot.callback_query_handler(func=lambda call: call.data.startswith('user_recharge_'))
def user_recharge_USDT(call):
    amount = call.data[len('user_recharge_'):]
    money = Decimal(amount)

    # 获取15分钟内的充值记录
    time_limit = timezone.now() - timedelta(minutes=15)
    recent_recharges = TgRecharge.objects.filter(create_time__gte=time_limit, pay_type='USDT')

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
        amount_type='USDT',
        tg_id=call.message.chat.id,
    )
    recharge.save()
    address = 'TLAsbVyEPi3Z14JdqRYtx262CaKvgsYu9g'
    text = f'''此订单15分钟内有效，过期后请重新生成订单。\n\n<b>转账地址(点击可复制): </b><code>{address}</code> (TRC-20网络)\n\n转账金额:<b>{money} USDT</b>\n\n请注意<b>转账金额务必与上方的转账金额一致</b>，否则无法自动到账\n支付完成后, 请等待1分钟左右查询，自动到账。'''
    bot.send_message(call.message.chat.id, text, parse_mode='html')


@bot.callback_query_handler(func=lambda call: call.data == 'user_custom_recharge')
def user_recharge_custom_handler(call):
    bot.send_message(
        call.message.chat.id,
        "请输入您想充值的金额："
    )
    bot.register_next_step_handler(call.message, handle_custom_recharge_amount)


import re


def handle_custom_recharge_amount(message):
    try:
        # 检查用户输入的金额是否有效
        amount_str = message.text.strip()
        # 验证金额格式：正数，最多两位小数
        if not re.fullmatch(r'\d+(\.\d{1,2})?', amount_str):
            bot.reply_to(message, "请输入一个有效的充值金额。")
            return

        amount = Decimal(amount_str)
        if amount <= 0:
            bot.reply_to(message, "请输入一个大于 0 的金额。")
            return

        # 获取15分钟内的充值记录
        time_limit = timezone.now() - timedelta(minutes=15)
        recent_recharges = TgRecharge.objects.filter(create_time__gte=time_limit, pay_type='USDT')

        # 统计最近15分钟内的充值记录数量
        count = recent_recharges.aggregate(count=Count('recharge_id'))['count']
        if count is None:
            count = 0

        # 根据充值记录数量调整金额
        if count == 0:
            adjusted_amount = amount + Decimal('0.01')
        else:
            adjusted_amount = amount + Decimal('0.01') * Decimal(count + 1)

        recharge = TgRecharge(
            money=adjusted_amount,
            amount_type='USDT',
            tg_id=message.chat.id,
        )
        recharge.save()

        # 显示支付信息
        address = 'TLAsbVyEPi3Z14JdqRYtx262CaKvgsYu9g'
        text = f'''此订单15分钟内有效，过期后请重新生成订单。\n\n<b>转账地址(点击可复制): </b><code>{address}</code> (TRC-20网络)\n\n转账金额:<b>{adjusted_amount:.2f} USDT</b>\n\n请注意<b>转账金额务必与上方的转账金额一致</b>，否则无法自动到账\n支付完成后, 请等待1分钟左右查询，自动到账。'''
        bot.send_message(message.chat.id, text, parse_mode='html')

    except Exception as e:
        bot.reply_to(message, f"处理充值请求时发生错误: {e}")


@bot.callback_query_handler(func=lambda call: call.data.startswith("user_set_recharge_number:"))
def user_set_recharge_number(call):
    user_id = call.from_user.id
    deposit_reward = int(call.data[len("user_set_recharge_number:"):])
    user = TgUser.objects.get(tg_id=user_id)
    user.deposit_reward = deposit_reward
    user.save()
    okex = get_okex()
    text = f"请点击按钮选择您的存款方式\n💡当前汇率 1 USDT = {okex} CNY\n-----------------------------------------"
    if user.deposit_reward != 0:
        text += f"\n\n🎁存款奖励：{user.deposit_reward}%\n投注要求：{user.deposit_reward}x"
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("🆗Okpay(人民币)💴", callback_data="recharge_type:0"))
    markup.add(types.InlineKeyboardButton("🆗Okpay(USDT)🔥", callback_data="recharge_type:1"))
    markup.add(types.InlineKeyboardButton("💰USDT(TRC20)💵", callback_data="recharge_type:2"))
    markup.add(types.InlineKeyboardButton("🎁存款奖励设置", callback_data="user_set_recharge"))
    markup.add(types.InlineKeyboardButton("↩️返回", callback_data="recharge_withdrawal"))
    bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                          reply_markup=markup)


@bot.callback_query_handler(func=lambda call: call.data.startswith("user_history_bill:"))
def user_history_bill(call):
    page = int(call.data[len("user_history_bill:"):])  # 获取当前页码
    user_id = call.from_user.id
    full_name = call.from_user.full_name
    user = TgUser.objects.get(tg_id=user_id)

    amount_changes = AmountChange.objects.filter(user=user, change_type="+").order_by('-create_time')
    total_changes = amount_changes.count()
    changes_to_display = amount_changes[(page - 1) * 5: page * 5]

    # 构建显示文本
    text = f"<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n账变日志\n\n"
    for change in changes_to_display:
        formatted_time = localtime(change.create_time).strftime("%Y-%m-%d %H:%M:%S")
        text += f"类型:{change.change_type}\n名字:{change.name}\n金额:{change.change_amount}\n货币:{change.amount_type}\n变动后余额:{change.after_amount}\n日期:{formatted_time}\n\n"

    # 添加分页信息
    total_pages = (total_changes // 5) + (1 if total_changes % 5 > 0 else 0)
    text += f"\n当前页: {page}/{total_pages}"

    markup = types.InlineKeyboardMarkup()
    if page > 1:
        markup.add(types.InlineKeyboardButton("上一页", callback_data=f"user_history_bill:{page - 1}"))
    if page < total_pages:
        markup.add(types.InlineKeyboardButton("下一页", callback_data=f"user_history_bill:{page + 1}"))
    markup.add(types.InlineKeyboardButton("↩️返回", callback_data="recharge_withdrawal"))
    markup.add(types.InlineKeyboardButton("🏠主菜单", callback_data="return_start"))
    bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                          reply_markup=markup, parse_mode="HTML")


@bot.callback_query_handler(func=lambda call: call.data.startswith("user_withdrawal_history:"))
def user_withdrawal_history(call):
    try:
        # 获取页码和用户信息
        page = int(call.data[len("user_withdrawal_history:"):])  # 当前页码
        user_id = call.from_user.id
        full_name = call.from_user.full_name
        user = TgUser.objects.get(tg_id=user_id)

        # 查询提现记录并分页
        withdrawal_records = WithdrawalRecord.objects.filter(user=user).order_by('-created_at')
        total_records = withdrawal_records.count()
        records_to_display = withdrawal_records[(page - 1) * 5: page * 5]

        # 构建显示文本
        text = f"<a href='tg://user?id={user_id}'>{full_name}</a> 🆔<code>{user_id}</code>\n提现日志\n\n"
        for record in records_to_display:
            formatted_time = localtime(record.created_at).strftime("%Y-%m-%d %H:%M:%S")
            formatted_reviewed_time = (
                localtime(record.reviewed_at).strftime("%Y-%m-%d %H:%M:%S") if record.reviewed_at else ""
            )
            text += (
                f"状态: <b>{record.get_status_display()}</b>\n"
                f"提现金额: {record.withdraw_amount:.2f} U\n"
                f"扣除余额: {record.withdraw_cny:.2f}\n"
                f"钱包余额:{record.after_balance:.2f}\n"
                f"提现地址: <code>{record.withdraw_address}</code>\n"
                f"提现时间: {formatted_time}\n"
                f"审核时间:{formatted_reviewed_time}\n\n"
            )

        # 添加分页信息
        total_pages = (total_records // 5) + (1 if total_records % 5 > 0 else 0)
        text += f"\n当前页: {page}/{total_pages}"

        # 构建分页按钮
        markup = types.InlineKeyboardMarkup()
        if page > 1:
            markup.add(types.InlineKeyboardButton("上一页", callback_data=f"user_withdrawal_history:{page - 1}"))
        if page < total_pages:
            markup.add(types.InlineKeyboardButton("下一页", callback_data=f"user_withdrawal_history:{page + 1}"))
        markup.add(types.InlineKeyboardButton("↩️返回", callback_data="recharge_withdrawal"))
        markup.add(types.InlineKeyboardButton("🏠主菜单", callback_data="return_start"))

        # 编辑消息内容
        bot.edit_message_text(
            chat_id=call.message.chat.id,
            message_id=call.message.message_id,
            text=text,
            reply_markup=markup,
            parse_mode="HTML"
        )

    except TgUser.DoesNotExist:
        bot.answer_callback_query(call.id, "用户信息不存在，请检查账户状态。")
    except Exception as e:
        bot.answer_callback_query(call.id, f"处理提现历史时发生错误: {e}")


@bot.callback_query_handler(func=lambda call: call.data.startswith("admin_query_recharge:"))
def user_history_bill(call):
    page = int(call.data[len("admin_query_recharge:"):])  # 获取当前页码
    user_id = call.from_user.id
    full_name = call.from_user.full_name

    # 查询 TgRecharge 表，条件为 status=1
    recharge_records = TgRecharge.objects.filter(status=1).order_by('-create_time')
    total_changes = recharge_records.count()
    changes_to_display = recharge_records[(page - 1) * 5: page * 5]

    # 构建显示文本
    text = f"<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n查询所有用户充值记录\n\n"
    for record in changes_to_display:
        formatted_time = localtime(record.create_time).strftime("%Y-%m-%d %H:%M:%S")
        text += f"用户ID: <b>{record.tg_id}</b>\n金额: <b>{record.money:.2f}</b>\n时间: <b>{formatted_time}</b>\n\n"

    # 添加分页信息
    total_pages = (total_changes // 5) + (1 if total_changes % 5 > 0 else 0)
    text += f"\n当前页: {page}/{total_pages}"

    markup = types.InlineKeyboardMarkup()
    if page > 1:
        markup.add(types.InlineKeyboardButton("上一页", callback_data=f"admin_query_recharge:{page - 1}"))
    if page < total_pages:
        markup.add(types.InlineKeyboardButton("下一页", callback_data=f"admin_query_recharge:{page + 1}"))
    markup.add(types.InlineKeyboardButton("↩️返回", callback_data="recharge_withdrawal"))
    markup.add(types.InlineKeyboardButton("🏠主菜单", callback_data="return_start"))

    bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                          reply_markup=markup, parse_mode="HTML")


@bot.callback_query_handler(func=lambda call: call.data == "user_is_notify")
def user_is_notify(call):
    user_id = call.from_user.id
    user = TgUser.objects.get(tg_id=user_id)
    user.is_notify = not user.is_notify
    user.save()
    text = f"💰充值提现\n\n💴余额:{user.money}"
    if user.is_notify:
        bot.answer_callback_query(call.id, f"🔔奖励通知已开启", show_alert=True)
    else:
        bot.answer_callback_query(call.id, "🔕奖励通知已关闭", show_alert=True)
    bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                          reply_markup=get_recharge_withdrawal_reply_markup(user.is_notify, user.is_admin))


@bot.callback_query_handler(func=lambda call: call.data == "user_withdraw")
def user_withdraw(call):
    try:
        user_id = call.from_user.id
        try:
            user = TgUser.objects.get(tg_id=user_id)
        except TgUser.DoesNotExist:
            bot.answer_callback_query(call.id, "用户信息不存在，请检查您的账户。")
            return

        try:
            okex = get_okex()
            okex = Decimal(okex)
        except Exception as e:
            bot.answer_callback_query(call.id, f"获取汇率时发生错误: {e}")
            return
        try:
            wallet_balance = Decimal(user.money)
            extractable_balance = wallet_balance / okex
        except InvalidOperation:
            bot.send_message(call.message.chat.id, "处理用户余额时发生错误，请联系管理员。")
            return
        text = (
            f"💡当前汇率 1 USDT = {okex:.2f} CNY\n"
            "-----------------------------------------\n"
            f"钱包余额: <b>{wallet_balance:.2f}</b>\n"
            f"当前可提取: <b>{extractable_balance:.2f} U</b>\n"
            "-----------------------------------------\n"
            "请输入要提取多少U:"
        )
        bot.send_message(
            call.message.chat.id,
            text,
            parse_mode="HTML"
        )
        bot.register_next_step_handler(call.message, user_withdraw_handler)

    except Exception as e:
        return


def user_withdraw_handler(message):
    try:
        user_id = message.from_user.id
        try:
            user = TgUser.objects.get(tg_id=user_id)
        except TgUser.DoesNotExist:
            bot.reply_to(message, "用户信息不存在，请检查您的账户。")
            return

        # 获取汇率
        try:
            okex = get_okex()
            okex = Decimal(okex)
        except Exception as e:
            bot.reply_to(message, f"获取汇率时发生错误: {e}")
            return

        try:
            wallet_balance = Decimal(user.money)
        except InvalidOperation:
            return

        try:
            withdraw_amount = Decimal(message.text.strip())
            if withdraw_amount < 1:
                bot.reply_to(message, "提现金额最低1，请重新输入。")
                return

            withdraw_cny = withdraw_amount * okex

            if withdraw_cny > wallet_balance:
                bot.reply_to(message,
                             f"提现失败，您的余额不足。\n当前余额: {wallet_balance:.2f} \n提现所需: {withdraw_cny:.2f} ")
                return
        except InvalidOperation:
            bot.reply_to(message, "无效的金额输入，请输入一个有效的数字。")
            return
        bot.send_message(message.chat.id, "请输入您的提现地址:")
        bot.register_next_step_handler(message, user_withdraw_address_handler, user, withdraw_amount, withdraw_cny,
                                       okex)
    except Exception as e:
        bot.reply_to(message, f"处理提现请求时发生错误: {e}")


def user_withdraw_address_handler(message, user, withdraw_amount, withdraw_cny, okex):
    try:
        withdraw_address = message.text.strip()
        # 验证地址
        if not withdraw_address:
            bot.reply_to(message, "无效的提现地址，请重新输入。")
            return
        before_balance = user.money
        user.money -= withdraw_cny
        user.save()

        withdrawalRecord = WithdrawalRecord.objects.create(
            user=user,
            withdraw_amount=withdraw_amount,
            withdraw_cny=withdraw_cny,
            before_balance=before_balance,
            after_balance=user.money,
            withdraw_address=withdraw_address
        )
        bot.reply_to(
            message,
            (
                f"提现申请已提交，请等待审核。\n"
                f"提现金额: <b>{withdraw_amount:.2f} U</b>\n"
                f"扣除金额: <b>{withdraw_cny:.2f}</b>\n"
                f"钱包余额: <b>{user.money:.2f}</b>\n"
                f"提现地址: <code>{withdraw_address}</code>"
            ),
            parse_mode="HTML"
        )

        # 通知管理员
        admin_review_user_withdrawal(message, user, withdraw_amount, withdraw_address,
                                     withdrawalRecord.id)

    except Exception as e:
        bot.reply_to(message, f"处理提现地址时发生错误: {e}")


def admin_review_user_withdrawal(message, user, withdraw_amount, withdraw_address, withdrawalRecord_id):
    try:
        work_group_id = get_work_group_id()
        markup = types.InlineKeyboardMarkup()
        markup.add(types.InlineKeyboardButton("✅ 同意", callback_data=f"withdraw_approve:{withdrawalRecord_id}"))
        markup.add(types.InlineKeyboardButton("❌ 拒绝", callback_data=f"withdraw_reject:{withdrawalRecord_id}"))
        bot.send_message(
            work_group_id,
            (
                f"💸 提现审核通知\n"
                f"用户id: <code>{user.tg_id}</code>\n"
                f"用户名: <a href='tg://user?id={user.tg_id}'>@{message.from_user.full_name}</a>\n"
                f"提现金额: {withdraw_amount:.2f} U\n"
                f"提现地址: <code>{withdraw_address}</code>\n"
                f"用户当前余额: {user.money:.2f} \n"
                f"请管理员及时处理。"
            )
            , parse_mode="HTML"
            , reply_markup=markup
        )
    except Exception as e:
        bot.reply_to(message, f"通知管理员时发生错误: {e}")


@bot.callback_query_handler(
    func=lambda call: call.data.startswith("withdraw_approve") or call.data.startswith("withdraw_reject"))
def handle_withdrawal_review(call):
    try:
        admin_id = call.from_user.id
        try:
            admin = TgUser.objects.get(tg_id=admin_id)
            if not admin.is_admin:
                bot.answer_callback_query(call.id, "您不是管理员，无法审核提现申请。")
                return
        except TgUser.DoesNotExist:
            return
        callback_data = call.data.split(":")
        action = callback_data[0]  # "withdraw_approve" 或 "withdraw_reject"
        withdrawal_id = int(callback_data[1])  # 提现记录 ID
        markup = types.InlineKeyboardMarkup()
        try:
            withdrawal = WithdrawalRecord.objects.get(id=withdrawal_id)
        except WithdrawalRecord.DoesNotExist:
            bot.answer_callback_query(call.id, "提现记录不存在。")
            return

        if withdrawal.status != "pending":
            bot.answer_callback_query(call.id, "该提现申请已处理，请勿重复操作。")
            return

        # 审核逻辑
        if action == "withdraw_approve":
            withdrawal.status = "approved"
            withdrawal.admin_tg_id = admin_id
            formatted_reviewed_at = localtime(timezone.now()).strftime("%Y-%m-%d %H:%M:%S")
            withdrawal.reviewed_at = formatted_reviewed_at
            withdrawal.save()

            # 通知用户
            formatted_created_at = localtime(withdrawal.created_at).strftime("%Y-%m-%d %H:%M:%S")
            bot.send_message(
                withdrawal.user.tg_id,
                (
                    f"🎉 提现申请已通过审核！\n"
                    f"提现金额: {withdrawal.withdraw_amount:.2f} U\n"
                    f"提现地址: <code>{withdrawal.withdraw_address}</code>\n"
                    f"提现时间:{formatted_created_at}\n"
                    f"审核时间:{withdrawal.reviewed_at}\n"
                    f"感谢您的使用。"
                ),
                parse_mode="HTML"
            )

            # 回复管理员
            bot.answer_callback_query(call.id, "提现申请已通过。")
            markup.add(types.InlineKeyboardButton("✅ 已通过", callback_data="null"))

            bot.edit_message_reply_markup(
                chat_id=call.message.chat.id,
                message_id=call.message.message_id,
                reply_markup=markup
            )

        elif action == "withdraw_reject":
            try:
                # 拒绝提现
                withdrawal.status = "rejected"
                withdrawal.admin_tg_id = admin_id
                formatted_reviewed_at = localtime(timezone.now()).strftime("%Y-%m-%d %H:%M:%S")
                withdrawal.reviewed_at = formatted_reviewed_at

                # 将提现金额返还到用户账户
                user = withdrawal.user
                user.money += withdrawal.withdraw_cny  # 返还提现时扣除的 CNY
                user.save()  # 保存用户余额

                withdrawal.save()  # 保存提现记录状态变更

                # 格式化时间
                formatted_created_at = localtime(withdrawal.created_at).strftime("%Y-%m-%d %H:%M:%S")

                # 通知用户提现被拒绝并返还金额
                bot.send_message(
                    withdrawal.user.tg_id,
                    (
                        f"⚠️ 很遗憾，您的提现申请未通过审核。\n"
                        f"提现金额: {withdrawal.withdraw_amount:.2f} U\n"
                        f"提现地址: <code>{withdrawal.withdraw_address}</code>\n"
                        f"提现时间: {formatted_created_at}\n"
                        f"审核时间: {withdrawal.reviewed_at}\n"
                        f"已将金额 <b>{withdrawal.withdraw_cny:.2f} CNY</b> 返还至您的钱包。\n"
                        f"如有疑问，请联系客服。"
                    ),
                    parse_mode="HTML"
                )

                # 通知管理员操作成功
                bot.answer_callback_query(call.id, "提现申请已拒绝。")

                # 修改按钮为 "❌ 已拒绝"
                markup = types.InlineKeyboardMarkup()
                markup.add(types.InlineKeyboardButton("❌ 已拒绝", callback_data="null"))

                bot.edit_message_reply_markup(
                    chat_id=call.message.chat.id,
                    message_id=call.message.message_id,
                    reply_markup=markup
                )
            except Exception as e:
                bot.answer_callback_query(call.id, f"处理提现申请时发生错误: {e}")
    except Exception as e:
        bot.answer_callback_query(call.id, f"处理提现审核时发生错误: {e}")


# @bot.callback_query_handler(func=lambda call: call.data.startswith("withdraw_type:"))
# def recharge_type(call):
#     user_id = call.from_user.id
#     user = TgUser.objects.get(tg_id=user_id)
#     withdraw_type = int(call.data[len("withdraw_type:"):])
#     markup = types.InlineKeyboardMarkup()
#     markup.add(types.InlineKeyboardButton("🚫取消", callback_data="recharge_withdrawal"))
#     text = f"💴 {user.money} CNY\n请回复您要提取的金额"
#     bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id,
#                           text=text, reply_markup=markup)
#     bot.register_next_step_handler(call.message, withdraw, withdraw_type)
#
#
# def withdraw(message, withdraw_type):
#     text = message.text
#     user_id = message.from_user.id
#     user = TgUser.objects.get(tg_id=user_id)
#
#     try:
#         amount = Decimal(text)
#         if amount <= 0:
#             markup = types.InlineKeyboardMarkup()
#             markup.add(types.InlineKeyboardButton("🚫取消", callback_data="recharge_withdrawal"))
#             text = f"取款金额必须大于0。"
#             bot.edit_message_text(chat_id=message.chat.id, message_id=message.message_id,
#                                   text=text, reply_markup=markup)
#             return
#     except (InvalidOperation, ValueError):
#         markup = types.InlineKeyboardMarkup()
#         markup.add(types.InlineKeyboardButton("🚫取消", callback_data="recharge_withdrawal"))
#         text = f"金额格式错误"
#         bot.edit_message_text(chat_id=message.chat.id, message_id=message.message_id,
#                               text=text, reply_markup=markup)
#         return
#
#     withdraw_limits = {
#         0: 5,  # OkPay人民币
#         1: 10,  # USDT-OkPay
#         2: 100  # USDT-TRC20
#     }
#
#     # 检查是否超过最小取款金额
#     min_amount = withdraw_limits.get(withdraw_type, float('inf'))
#     if amount < min_amount:
#         markup = types.InlineKeyboardMarkup()
#         markup.add(types.InlineKeyboardButton("🚫取消", callback_data="recharge_withdrawal"))
#         text = f"取款金额必须大于{min_amount}。"
#         bot.edit_message_text(chat_id=message.chat.id, message_id=message.message_id,
#                               text=text, reply_markup=markup)
#         return
#
#     # 检查余额是否足够
#     if user.money < amount:
#         markup = types.InlineKeyboardMarkup()
#         markup.add(types.InlineKeyboardButton("🚫取消", callback_data="recharge_withdrawal"))
#         text = f"您的余额不足。"
#         bot.edit_message_text(chat_id=message.chat.id, message_id=message.message_id,
#                               text=text, reply_markup=markup)
#         return
#
#     # 根据不同的 withdraw_type 处理取款逻辑
#     if withdraw_type == 0:
#         withdraw_cny_okpay(user, amount)
#     elif withdraw_type == 1:
#         withdraw_usdt_okpay(user, amount)
#     elif withdraw_type == 2:
#         withdraw_usdt_usdt(user, amount)
#
#
# def withdraw_amount_change(user, name, change_amount, before_amount, after_amount):
#     AmountChange.objects.create(
#         user=user,
#         change_type='-',
#         name=name,
#         change_amount=change_amount,
#         before_amount=before_amount,
#         after_amount=user.money
#     )
#
#
# def withdraw_cny_okpay(user, amount):
#     # CNY-OkPay取款处理逻辑
#     before_amount = user.money
#     user.money -= amount
#     user.save()
#     withdraw_amount_change(user, "CNY-OkPay", amount, before_amount, user.money)
#     bot.send_message(user.tg_id, f"✅已从您的账户中扣除{amount}元,CNY-OkPay")
#
#
# def withdraw_usdt_okpay(user, amount):
#     # USDT-OkPay取款处理逻辑(需要把余额转成USDT)
#     before_amount = user.money
#     user.money -= amount
#     user.save()
#     withdraw_amount_change(user, "USDT-OkPay", amount, before_amount, user.money)
#     bot.send_message(user.tg_id, f"✅已从您的账户中扣除{amount}元,USDT-OkPay")
#
#
# def withdraw_usdt_usdt(user, amount):
#     # USDT-USDT取款处理逻辑
#     before_amount = user.money
#     user.money -= amount
#     user.save()
#     withdraw_amount_change(user, "USDT", amount, before_amount, user.money)
#     bot.send_message(user.tg_id, f"✅已从您的账户中扣除{amount}元,USDT")


@bot.callback_query_handler(func=lambda call: call.data == "invite_user")
def invite_user(call):
    try:
        user_id = call.from_user.id
        user = TgUser.objects.get(tg_id=user_id)
        count = TgUser.objects.filter(invite_tg_id=user_id).count()

        # 获取被邀请用户的列表
        invited_users = TgUser.objects.filter(invite_tg_id=user_id)
        invited_users_text = []

        for invited_user in invited_users:
            try:
                # 使用 get_chat 获取用户信息
                user_info = bot.get_chat(invited_user.tg_id)
                full_name = f"{user_info.first_name} {user_info.last_name if user_info.last_name else ''}".strip()
                # 创建可点击的链接
                invited_users_text.append(f"<a href='tg://user?id={invited_user.tg_id}'>@{full_name}</a>\t")
            except Exception as e:
                print(f"获取用户 {invited_user.tg_id} 的信息失败: {e}")

        invited_users_text = "\n".join(invited_users_text) if invited_users_text else "没有邀请任何用户"

        url = f"https://t.me/{bot.get_me().username}?start={user.tg_id}"
        text = (f"👬 推荐计划\n邀请你的朋友，赚取所有赌注的0.2%，无论他们是赢还是输!\n"
                f"💡拉好友进群，自动绑定代理哦\n\n"
                f"👥 已邀请人数 : {count}\n"
                f"👥 已邀请用户 : \n{invited_users_text}\n"
                f"🔗 推荐链接 : \n{url}")

        markup = types.InlineKeyboardMarkup()
        markup.add(types.InlineKeyboardButton("🏠主菜单", callback_data="return_start"))
        bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                              reply_markup=markup, parse_mode="HTML")
    except Exception as e:
        print(e)


@bot.callback_query_handler(func=lambda call: call.data == "support")
def support(call):
    text = f"🙋客服支持<a href='https://t.me/dhkf9'>@鼎豪客服 阿伟</a>"
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("🏠主菜单", callback_data="return_start"))
    bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                          reply_markup=markup, parse_mode="HTML")
