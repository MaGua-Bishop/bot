from telebot import types
import hashlib
import random
import string
import requests
from decimal import Decimal, ROUND_DOWN
from telebot.types import WebAppInfo


def get_start_reply_markup() -> types.ReplyKeyboardMarkup:
    markup = types.InlineKeyboardMarkup()
    # markup.add(types.InlineKeyboardButton("⬆️转入游戏", callback_data="transfer_to_game"),
    #            types.InlineKeyboardButton("💰转回钱包", callback_data="transfer_to_wallet"))
    # player_url = WebAppInfo(url=player_url)  # 确保使用 WebAppInfo
    # markup.add(types.InlineKeyboardButton("🎰开始游戏", web_app=player_url))
    # markup.add(types.InlineKeyboardButton("🔁刷新", callback_data="refresh"))
    markup.add(types.InlineKeyboardButton("🎰PG电子", callback_data="game_type:0"),
               types.InlineKeyboardButton("🎰JDB电子", callback_data="game_type:1"))
    markup.add(types.InlineKeyboardButton("👥官方群组", url="https://baidu.com"))
    markup.add(types.InlineKeyboardButton("💰充值提现", callback_data="recharge_withdrawal"))
    markup.add(types.InlineKeyboardButton("🙋客服支持", callback_data="support"),
               types.InlineKeyboardButton("👋邀请好友", callback_data="invite_user"))
    markup.add(types.InlineKeyboardButton("🔀分享领奖", switch_inline_query="Invite"))
    # types.InlineKeyboardButton("🌐Language", callback_data="language")
    # markup.add(types.InlineKeyboardButton("🧧发红包", callback_data="hair_package"))
    return markup


def get_game_type_reply_markup(type, player_url) -> types.ReplyKeyboardMarkup:
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("⬆️转入游戏", callback_data=f"transfer_to_game:{type}"),
               types.InlineKeyboardButton("💰转回钱包", callback_data=f"transfer_to_wallet:{type}"))
    player_url = WebAppInfo(url=player_url)
    markup.add(types.InlineKeyboardButton("🎰开始游戏", web_app=player_url))
    markup.add(types.InlineKeyboardButton("🔁刷新", callback_data=f"refresh:{type}"),
               types.InlineKeyboardButton("↩️返回", callback_data="return_start"))
    return markup


def get_recharge_withdrawal_reply_markup(is_notify: bool) -> types.ReplyKeyboardMarkup:
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("➕存款", callback_data="user_recharge"),
               types.InlineKeyboardButton("💵取款", callback_data="user_withdraw"))
    markup.add(types.InlineKeyboardButton("🕐历史账单", callback_data="user_history_bill:1"),
               types.InlineKeyboardButton("📑取款历史", callback_data="user_withdrawal_history:1"))
    if is_notify:
        markup.add(types.InlineKeyboardButton("🔔奖励通知已开启", callback_data="user_is_notify"))
    else:
        markup.add(types.InlineKeyboardButton("🔕奖励通知已关闭", callback_data="user_is_notify"))
    markup.add(types.InlineKeyboardButton("🏠主菜单", callback_data="return_start"))
    return markup


def generate_headers():
    SN = 'jkd'
    SECRET_KEY = '8rNx9k30yicB91n98315eU92d0e92h9w'
    random_str = ''.join(random.choices(string.ascii_lowercase + string.digits, k=random.randint(16, 32)))

    raw_string = random_str + SN + SECRET_KEY
    print(f"sign加密前字符串:{raw_string}")
    sign = hashlib.md5(raw_string.encode('utf-8')).hexdigest()

    headers = {
        'sign': sign,
        'random': random_str,
        'sn': SN,
        'Content-Type': 'application/json'
    }
    print(f"sign:{sign}")
    print(f"random:{random_str}")
    print(f"sn:{SN}")
    print(f"Content-Type:{headers['Content-Type']}")

    return headers


def get_game_type(type=0):
    ''''type:0(pg)1(jdb)'''
    api_host = 'https://ap.api-bet.net'
    currency = 'CNY'
    plat_type = 'pg'
    if type == 1:
        plat_type = 'jdb'
    return plat_type, currency, api_host


def create_game_user(tg_id, type):
    plat_type, currency, api_host = get_game_type(type)
    url = api_host + '/api/server/create'
    headers = generate_headers()
    playerId = "tg" + str(tg_id)[:9]
    json = {
        "playerId": playerId,
        "platType": plat_type,
        "currency": currency,
    }
    print(f"创建玩家返回值:{json}")
    response = requests.post(url, headers=headers, json=json, timeout=30)
    json = response.json()
    if json['code'] == 10000 or json['code'] == 10002:
        return True
    else:
        return False


def get_game_url(tg_id, type):
    plat_type, currency, api_host = get_game_type(type)
    url = api_host + '/api/server/gameUrl'
    headers = generate_headers()

    playerId = "tg" + str(tg_id)[:9]

    json = {
        "playerId": playerId,
        "platType": plat_type,
        "currency": currency,
        "gameType": "2",
        "ingress": "device2"

    }
    response = requests.post(url, headers=headers, json=json, timeout=30)
    json = response.json()
    print(f"登录游戏返回值:{json}")
    if json['code'] == 10000:
        return json['data']['url']
    else:
        return None


def transfer_money(tg_id, type, amount, game_type):
    plat_type, currency, api_host = get_game_type(game_type)
    url = api_host + '/api/server/transfer'
    headers = generate_headers()

    playerId = "tg" + str(tg_id)[:9]
    amount = float(Decimal(amount).quantize(Decimal('0.00'), rounding=ROUND_DOWN))
    json = {
        "playerId": playerId,
        "platType": plat_type,
        "currency": currency,
        "type": type,
        "amount": amount
    }
    response = requests.post(url, headers=headers, json=json, timeout=30)
    json = response.json()
    print(f"转账返回值:{json}")
    if json['code'] == 10000:
        print(f"{game_type}:{type}转账返回值:{json}")
        return True
    else:
        return False


def get_user_pgmoney(tg_id, type):
    plat_type, currency, api_host = get_game_type(type)
    url = api_host + '/api/server/balance'
    headers = generate_headers()

    playerId = "tg" + str(tg_id)[:9]

    json = {
        "playerId": playerId,
        "platType": plat_type,
        "currency": currency,
    }
    response = requests.post(url, headers=headers, json=json, timeout=30)
    json = response.json()
    if json['code'] == 10000:
        print(f"{type}查询玩家余额返回值:{json['data']['balance']}")
        return json['data']['balance']
    else:
        return None


from cachetools import TTLCache, cached

cache = TTLCache(maxsize=100, ttl=60)
headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
    "Accept": "application/json",
}


@cached(cache)
def get_okex():
    url = f"https://www.okx.com/v3/c2c/tradingOrders/books?quoteCurrency=CNY&baseCurrency=USDT&side=buy&paymentMethod=all&userType=all&receivingAds=false&t=1716466060621"
    result = requests.get(url, headers=headers).json()['data']['buy']
    if result:
        price = result[0]['price']
        return price
    else:
        return None


import json
import os


def get_work_group_id():
    file_path = "tg_bot/work_group.json"

    # Check if the file exists
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"The file {file_path} does not exist.")

    # Read the JSON file
    with open(file_path, "r", encoding="utf-8") as file:
        try:
            data = json.load(file)
        except json.JSONDecodeError as e:
            raise ValueError(f"Invalid JSON in {file_path}: {e}")
    chat_id = data.get("chat_id")
    if chat_id == "":
        return None

    return chat_id


def set_work_group_id(chat_id):
    if not isinstance(chat_id, str):
        raise ValueError("chat_id must be a string.")
    file_path = "tg_bot/work_group.json"
    data = {"chat_id": chat_id}
    with open(file_path, "w", encoding="utf-8") as file:
        json.dump(data, file, indent=4, ensure_ascii=False)
