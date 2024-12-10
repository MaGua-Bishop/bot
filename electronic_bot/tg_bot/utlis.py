from telebot import types
import hashlib
import random
import string
import requests


def get_start_reply_markup() -> types.ReplyKeyboardMarkup:
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("⬆️转入游戏", callback_data="transfer_to_game"),
               types.InlineKeyboardButton("💰转回钱包", callback_data="transfer_to_wallet"))
    markup.add(types.InlineKeyboardButton("🎰开始游戏", callback_data="start_game"))
    markup.add(types.InlineKeyboardButton("🔁刷新", callback_data="refresh"),
               types.InlineKeyboardButton("👥官方群组", url="https://baidu.com"))
    markup.add(types.InlineKeyboardButton("💰充值提现", callback_data="recharge_withdrawal"))
    markup.add(types.InlineKeyboardButton("🙋客服支持", callback_data="support"),
               types.InlineKeyboardButton("👋邀请好友", callback_data="invite_user"))
    markup.add(types.InlineKeyboardButton("🔀分享领奖", switch_inline_query="分享内容"),
               types.InlineKeyboardButton("🌐Language", callback_data="language"))
    # markup.add(types.InlineKeyboardButton("🧧发红包", callback_data="hair_package"))
    return markup


def get_recharge_withdrawal_reply_markup(is_notify: bool) -> types.ReplyKeyboardMarkup:
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("➕存款", callback_data="user_recharge"),
               types.InlineKeyboardButton("💵取款", callback_data="transfer_to_wallet"))
    markup.add(types.InlineKeyboardButton("🕐历史账单", callback_data="user_history_bill:1"),
               types.InlineKeyboardButton("📑取款历史", url="https://baidu.com"))
    if is_notify:
        markup.add(types.InlineKeyboardButton("🔔奖励通知已开启", callback_data="user_is_notify"))
    else:
        markup.add(types.InlineKeyboardButton("🔕奖励通知已关闭", callback_data="user_is_notify"))
    markup.add(types.InlineKeyboardButton("🏠主菜单", callback_data="return_start"))
    return markup


# 生成随机字符串
def generate_random_string(length=16):
    return ''.join(random.choices(string.ascii_lowercase + string.digits, k=length))


# 生成签名
def generate_sign(random_str, sn, secret_key):
    sign_str = random_str + sn + secret_key
    return hashlib.md5(sign_str.encode('utf-8')).hexdigest().lower()


# 通用请求头
def get_game_headers():
    SN = 'jkd'
    SECRET_KEY = '8rNx9k30yicB91n98315eU92d0'
    return {
        'sign': generate_sign(generate_random_string(16), SN, SECRET_KEY),
        'random': generate_random_string(16),
        'sn': SN,
        'Content-Type': 'application/json'
    }


def get_game_type():
    # api_host = 'https://ap.api-bet.net'
    api_host = 'https://sa.api-bet.net'
    plat_type = 'allbet'
    currency = 'CNY'
    return plat_type, currency, api_host


def create_game_user(tg_id):
    plat_type, currency, api_host = get_game_type()
    url = api_host + '/api/server/create'
    headers = get_game_headers()

    playerId = "tg" + str(tg_id)

    json = {
        "playerId": playerId,
        "platType": plat_type,
        "currency": currency,
    }
    response = requests.post(url, headers=headers, json=json, timeout=30)
    return response
