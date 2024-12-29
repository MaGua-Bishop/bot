import threading

from telebot import types
import hashlib
import random
import string
import requests
from decimal import Decimal, ROUND_DOWN
from telebot.types import WebAppInfo
import time
from datetime import datetime, timedelta
from .models import GameHistory
from .bot_config import bot


def get_start_reply_markup() -> types.ReplyKeyboardMarkup:
    markup = types.InlineKeyboardMarkup()
    # markup.add(types.InlineKeyboardButton("⬆️转入游戏", callback_data="transfer_to_game"),
    #            types.InlineKeyboardButton("💰转回钱包", callback_data="transfer_to_wallet"))
    # player_url = WebAppInfo(url=player_url)  # 确保使用 WebAppInfo
    # markup.add(types.InlineKeyboardButton("🎰开始游戏", web_app=player_url))
    # markup.add(types.InlineKeyboardButton("🔁刷新", callback_data="refresh"))
    markup.add(types.InlineKeyboardButton("🎰PG电子", callback_data="game_type:0"),
               types.InlineKeyboardButton("🎰JDB电子", callback_data="game_type:1"))
    markup.add(types.InlineKeyboardButton("👥官方群组", url="https://t.me/dhpg9"))
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


def get_recharge_withdrawal_reply_markup(is_notify: bool, is_admin) -> types.ReplyKeyboardMarkup:
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("➕存款", callback_data="user_recharge"),
               types.InlineKeyboardButton("💵取款", callback_data="user_withdraw"))
    markup.add(types.InlineKeyboardButton("🕐历史账单", callback_data="user_history_bill:1"),
               types.InlineKeyboardButton("📑取款历史", callback_data="user_withdrawal_history:1"))
    if is_notify:
        markup.add(types.InlineKeyboardButton("🔔奖励通知已开启", callback_data="user_is_notify"))
    else:
        markup.add(types.InlineKeyboardButton("🔕奖励通知已关闭", callback_data="user_is_notify"))
    if is_admin:
        markup.add(types.InlineKeyboardButton("查看所有充值记录", callback_data="admin_query_recharge:1"))
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


import requests
from datetime import datetime
from .models import GameHistory


def get_winning_type_reply_markup(type) -> types.ReplyKeyboardMarkup:
    markup = types.InlineKeyboardMarkup()
    url = f"https://t.me/{bot.get_me().username}?start"
    if type == 'pg':
        markup.add(types.InlineKeyboardButton("🎰我也要玩PG电子", url=url))
    else:
        markup.add(types.InlineKeyboardButton("🎰我也要玩JDB电子", url=url))
    return markup


from pathlib import Path
from io import BytesIO


def read_image(name):
    current_directory = Path(__file__).resolve().parent
    image_path = current_directory / 'images' / name
    try:
        with open(image_path, 'rb') as photo_file:
            # 将文件内容读取到内存中的 BytesIO 对象
            return BytesIO(photo_file.read())
    except Exception as e:
        print(f"Error reading image: {e}")
        return None


def send_winning_message(game_type, amount):
    # 获取图片
    if game_type == "pg":
        photo_file = read_image('majianghule.jpg')
        message_text = (
            f"👏 恭喜在【匿名大佬】爆彩啦!\n\n"
            f"👤 匿名大佬 在PG电子游戏中赢得{amount}¥💵。"
        )
    else:
        image_list = ['1.jpg', '2.jpg', '3.jpg']
        image_name = random.choice(image_list)
        photo_file = read_image(image_name)
        message_text = (
            f"👏 恭喜在【匿名大佬】爆彩啦!\n\n"
            f"👤 匿名大佬 在JDB电子游戏中赢得{amount}¥💵。"
        )

    if photo_file:
        try:
            # 发送图片和文本
            bot.send_photo(-1002288238505, photo_file, parse_mode="HTML",
                           reply_markup=get_winning_type_reply_markup(game_type),
                           caption=message_text)
        except Exception as e:
            print(f"发送消息失败: {e}")
    else:
        print("图片读取失败，无法发送消息")


def fetch_all_game_history():
    while True:  # 添加一个无限循环
        print("开始执行 fetch_all_game_history")
        # 设置当前时间为结束时间
        end_time = datetime.now()
        # 设置开始时间为当前时间的 6 小时前
        start_time = end_time - timedelta(hours=6)

        # 确保时间范围不超过 6 小时
        if (end_time - start_time).total_seconds() > 21600:  # 6小时 = 21600秒
            print("开始时间和结束时间不能超过 6 小时")
            return

        page_no = 1
        page_size = 200
        total_pages = 1  # 初始化总页数

        while page_no <= total_pages:
            url = 'https://ap.api-bet.net/api/server/recordHistory'
            headers = generate_headers()
            params = {
                "currency": 'CNY',
                "startTime": start_time.strftime("%Y-%m-%d %H:%M:%S"),
                "endTime": end_time.strftime("%Y-%m-%d %H:%M:%S"),
                "pageNo": page_no,
                "pageSize": page_size
            }

            response = requests.post(url, headers=headers, json=params, timeout=30)
            json_response = response.json()

            if json_response['code'] == 10000:
                records = json_response['data']['list']
                print(f"查到的记录数:{records}")
                total_pages = json_response['data']['total'] // page_size + (
                    1 if json_response['data']['total'] % page_size > 0 else 0)

                for record in records:
                    game_order_id = record['gameOrderId']
                    # 只处理状态为 1 的记录
                    if record['status'] == 1:
                        # 检查数据库中是否已存在该记录
                        if not GameHistory.objects.filter(game_order_id=game_order_id).exists():
                            # 如果不存在，则添加记录
                            try:
                                settled_amount = record['settledAmount']  # 获取已结算金额
                                GameHistory.objects.create(
                                    game_order_id=game_order_id,
                                    player_id=record['playerId'],
                                    plat_type=record['platType'],
                                    currency=record['currency'],
                                    game_type=record['gameType'],
                                    game_name=record['gameName'],
                                    round_number=record['round'],
                                    table_number=record['table'],
                                    seat_number=record['seat'],
                                    bet_amount=record['betAmount'],
                                    valid_amount=record['validAmount'],
                                    settled_amount=settled_amount,
                                    bet_content=record['betContent'],
                                    status=record['status'],
                                    bet_time=datetime.strptime(record['betTime'], "%Y-%m-%d %H:%M:%S"),
                                    last_update_time=datetime.strptime(record['lastUpdateTime'], "%Y-%m-%d %H:%M:%S"),
                                    is_status=False  # 默认状态为 False
                                )

                                # 判断 settledAmount 是否大于 500
                                if settled_amount > 500:
                                    plat_type = record['platType']
                                    send_winning_message(plat_type, settled_amount)
                            except Exception as e:
                                print(f"插入记录失败: {e}")

                # 请求间隔控制
                if page_no % 5 == 0:  # 每 5 次请求后等待 1 分钟
                    print("每小时请求不能超过 5 次，等待 1 分钟...")
                    time.sleep(60)
                else:
                    # 每次请求之间等待 10 秒
                    print("等待 10 秒...")
                    time.sleep(10)

                page_no += 1  # 翻页
            else:
                print(f"请求失败: {json_response['msg']}")
                break  # 如果请求失败，退出循环

        print("等待 30 分钟再执行下一次...")
        time.sleep(1800)  # 等待 30 分钟


# 启动线程
reminder_thread = threading.Thread(target=fetch_all_game_history)
reminder_thread.daemon = True
reminder_thread.start()
