from .bot_config import bot
from telebot import types
from .models import TgUser, AmountChange, TgRecharge
from decimal import Decimal
from .utlis import get_start_reply_markup, get_recharge_withdrawal_reply_markup
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
    text = f"👋Hi,<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n-----------------------------------------\n💰钱包余额: {user.money} 💴\n🕹PG电子余额: {user.pg_money}"
    bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                          reply_markup=get_start_reply_markup(), parse_mode="HTML")


@bot.callback_query_handler(func=lambda call: call.data == 'transfer_to_game')
def transfer_to_game(call):
    user_id = call.from_user.id
    full_name = call.from_user.full_name
    try:
        user = TgUser.objects.get(tg_id=user_id)
        if user.money > Decimal('0'):
            before_amount = user.money
            user.pg_money += user.money
            user.money = Decimal('0')
            after_amount = user.money
            user.save()

            AmountChange.objects.create(
                user=user,
                change_type='+',
                name='钱包转到PG电子',
                change_amount=user.money,
                before_amount=before_amount,
                after_amount=after_amount
            )

            text = f"👋Hi,<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n-----------------------------------------\n💰钱包余额: {user.money} 💴\n🕹PG电子余额: {user.pg_money}"
            bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                                  parse_mode="HTML", reply_markup=get_start_reply_markup())
            bot.answer_callback_query(call.id, "✅ 成功将钱包余额转入游戏！", show_alert=True)
        else:
            bot.answer_callback_query(call.id, "最低需要0.1的余额才能转入游戏！", show_alert=True)
    except TgUser.DoesNotExist:
        bot.answer_callback_query(call.id, "❌ 用户信息未找到，请先/start启动机器人", show_alert=True)


@bot.callback_query_handler(func=lambda call: call.data == 'transfer_to_wallet')
def transfer_to_wallet(call):
    user_id = call.from_user.id
    full_name = call.from_user.full_name
    try:
        user = TgUser.objects.get(tg_id=user_id)
        before_amount = user.pg_money
        user.money += user.pg_money
        user.pg_money = Decimal('0')
        after_amount = user.pg_money
        user.save()
        AmountChange.objects.create(
            user=user,
            change_type='+',
            name='PG电子转回钱包',
            change_amount=user.pg_money,
            before_amount=before_amount,
            after_amount=after_amount
        )
        text = f"👋Hi,<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n-----------------------------------------\n💰钱包余额: {user.money} 💴\n🕹PG电子余额: {user.pg_money}"
        bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                              parse_mode="HTML", reply_markup=get_start_reply_markup())
        bot.answer_callback_query(call.id, f"✅ 成功取回到钱包,金额:{user.pg_money}", show_alert=True)
    except TgUser.DoesNotExist:
        bot.answer_callback_query(call.id, "❌ 用户信息未找到，请先/start启动机器人", show_alert=True)


@bot.callback_query_handler(func=lambda call: call.data == 'refresh')
def refresh(call):
    user_id = call.from_user.id
    full_name = call.from_user.full_name
    user = TgUser.objects.get(tg_id=user_id)
    text = f"👋Hi,<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n-----------------------------------------\n💰钱包余额: {user.money} 💴\n🕹PG电子余额: {user.pg_money}"
    bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                          parse_mode="HTML", reply_markup=get_start_reply_markup())
    bot.answer_callback_query(call.id, "✅ 刷新成功")


@bot.callback_query_handler(func=lambda call: call.data == 'recharge_withdrawal')
def recharge_withdrawal(call):
    user_id = call.from_user.id
    user = TgUser.objects.get(tg_id=user_id)
    text = f"💰充值提现\n\n💴余额:{user.money}"
    bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                          reply_markup=get_recharge_withdrawal_reply_markup(user.is_notify))


@bot.callback_query_handler(func=lambda call: call.data == 'user_recharge')
def user_recharge(call):
    user_id = call.from_user.id
    user = TgUser.objects.get(tg_id=user_id)
    text = f"请点击按钮选择您的存款方式\n💡当前汇率 1 USDT = 7.25 CNY\n-----------------------------------------"
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


def recharge_cny(message):
    user_id = message.from_user.id
    text = message.text
    try:
        amount = Decimal(text)
        if amount <= 0:
            bot.send_message(user_id, "充值金额必须大于0。")
            return
        recharge = TgRecharge.objects.create(
            tg_id=user_id,
            money=amount,
            amount_type='CNY',
            pay_type='OKPAY',
        )
        markup = types.InlineKeyboardMarkup()
        markup.add(types.InlineKeyboardButton("🏧去支付",
                                              url=f"https://t.me/OkayPayBot?start=shop_deposit--{recharge.recharge_id}"))
        bot.send_message(user_id, f"➕存款:{amount}CNY", reply_markup=markup)
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
        bot.register_next_step_handler(call.message, recharge_cny)
    if recharge_type == 1:
        # okpay USDT
        pass
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
        bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                              reply_markup=markup)


@bot.callback_query_handler(func=lambda call: call.data.startswith('user_recharge_'))
def user_recharge_USDT(call):
    """
         用户充值
     """
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
    address = 'SNjkdKEY8rNx9k30yicB91n98315eU92d0'
    text = f'''此订单15分钟内有效，过期后请重新生成订单。\n\n<b>转账地址(点击可复制): </b><code>{address}</code> (TRC-20网络)\n\n转账金额:<b>{money} USDT</b>\n\n请注意<b>转账金额务必与上方的转账金额一致</b>，否则无法自动到账\n支付完成后, 请等待1分钟左右查询，自动到账。'''
    bot.send_message(call.message.chat.id, text, parse_mode='html')


@bot.callback_query_handler(func=lambda call: call.data.startswith("user_set_recharge_number:"))
def user_set_recharge_number(call):
    user_id = call.from_user.id
    deposit_reward = int(call.data[len("user_set_recharge_number:"):])
    user = TgUser.objects.get(tg_id=user_id)
    user.deposit_reward = deposit_reward
    user.save()
    text = f"请点击按钮选择您的存款方式\n💡当前汇率 1 USDT = 7.25 CNY\n-----------------------------------------"
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

    amount_changes = AmountChange.objects.filter(user=user).order_by('-create_time')
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
                          reply_markup=get_recharge_withdrawal_reply_markup(user.is_notify))


@bot.callback_query_handler(func=lambda call: call.data == "invite_user")
def invite_user(call):
    try:
        user_id = call.from_user.id
        user = TgUser.objects.get(tg_id=user_id)
        count = TgUser.objects.filter(invite_tg_id=user_id).count()
        url = f"https://t.me/{settings.TG_BOT_NAME}?start={user.tg_id}"
        text = f"👬 推荐计划\n邀请你的朋友，赚取所有赌注的0.2%，无论他们是赢还是输!\n💡拉好友进群，自动绑定代理哦\n\n👥 已邀请人数 : {count}\n🔗 推荐链接 : \n{url}"
        markup = types.InlineKeyboardMarkup()
        markup.add(types.InlineKeyboardButton("🏠主菜单", callback_data="return_start"))
        bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                              reply_markup=markup, parse_mode="HTML")
    except Exception as e:
        print(e)


@bot.callback_query_handler(func=lambda call: call.data == "support")
def support(call):
    text = f"👩‍💻 Support<a href='https://t.me/{2142298091}'>@linanming</a>"
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("🏠主菜单", callback_data="return_start"))
    bot.edit_message_text(chat_id=call.message.chat.id, message_id=call.message.message_id, text=text,
                          reply_markup=markup, parse_mode="HTML")
